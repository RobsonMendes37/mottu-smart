import http from 'node:http';
import { parse as parseUrl } from 'node:url';
import crypto from 'node:crypto';
import {
  UserRole,
  AccountStatus,
  MotorcycleModel,
  MotorcycleStatus,
  ReportedOriginContext,
  SafetyRiskLevel,
  DamageVerdict,
  QueuePriorityReason,
  InspectionStatus,
  AppointmentShift,
  PRIORITY_WEIGHTS,
} from '../contracts/enums.js';
import {
  SEED_USERS,
  SEED_YARDS,
  SEED_MOTORCYCLES,
  SEED_PARTS,
  STANDARD_SETTINGS,
} from '../fixtures/seed-data.js';

export interface ServerState {
  users: any[];
  motorcycles: any[];
  contracts: any[];
  inspections: Map<string, any>;
  idempotencyKeys: Map<string, string>;
  appointments: any[];
  parts: any[];
  settings: any;
  auditLogs: any[];
  outbox: any[];
  accidents: Map<string, any>;
}

export function createInitialState(): ServerState {
  const users = [
    {
      id: '00000000-0000-0000-0000-000000000001',
      ...SEED_USERS.MANAGER,
      status: AccountStatus.ACTIVE,
    },
    {
      id: '00000000-0000-0000-0000-000000000002',
      ...SEED_USERS.MECHANIC,
      status: AccountStatus.ACTIVE,
    },
    {
      id: '00000000-0000-0000-0000-000000000003',
      ...SEED_USERS.RIDER,
      status: AccountStatus.ACTIVE,
    },
  ];

  const motorcycles = JSON.parse(JSON.stringify(SEED_MOTORCYCLES));
  const parts = JSON.parse(JSON.stringify(SEED_PARTS));
  const settings = { ...STANDARD_SETTINGS };

  const contracts = [
    {
      id: 'contract-seed-rider-001',
      riderId: users[2].id,
      motorcycleId: motorcycles[0].id,
      pickedUpAt: new Date(Date.now() - 12 * 3600 * 1000).toISOString(), // 12 hours ago
      initialOdometerKm: 1000,
      endedAt: null,
      active: true,
    },
  ];

  return {
    users,
    motorcycles,
    contracts,
    inspections: new Map(),
    idempotencyKeys: new Map(),
    appointments: [],
    parts,
    settings,
    auditLogs: [],
    outbox: [],
    accidents: new Map(),
  };
}

export class MockContractServer {
  private server: http.Server | null = null;
  public state: ServerState = createInitialState();
  public port: number;

  constructor(port: number = 18099) {
    this.port = port;
  }

  public resetState() {
    this.state = createInitialState();
  }

  public async start(): Promise<string> {
    return new Promise((resolve) => {
      this.server = http.createServer((req, res) => this.handleRequest(req, res));
      this.server.listen(this.port, () => {
        resolve(`http://localhost:${this.port}`);
      });
    });
  }

  public async stop(): Promise<void> {
    return new Promise((resolve, reject) => {
      if (!this.server) return resolve();
      this.server.close((err) => {
        if (err) reject(err);
        else resolve();
      });
    });
  }

  private sendJson(res: http.ServerResponse, status: number, data: any) {
    res.writeHead(status, {
      'Content-Type': 'application/json',
      'X-Correlation-Id': crypto.randomUUID(),
    });
    res.end(JSON.stringify(data));
  }

  private sendProblemDetail(
    res: http.ServerResponse,
    status: number,
    title: string,
    detail: string,
    correlationId = crypto.randomUUID()
  ) {
    res.writeHead(status, {
      'Content-Type': 'application/problem+json',
      'X-Correlation-Id': correlationId,
    });
    res.end(
      JSON.stringify({
        title,
        status,
        detail,
        correlationId,
      })
    );
  }

  private async parseBody(req: http.IncomingMessage): Promise<any> {
    return new Promise((resolve, reject) => {
      let body = '';
      req.on('data', (chunk) => (body += chunk));
      req.on('end', () => {
        if (!body) return resolve({});
        try {
          resolve(JSON.parse(body));
        } catch (e) {
          reject(e);
        }
      });
      req.on('error', reject);
    });
  }

  private extractAuthUser(req: http.IncomingMessage): any {
    const auth = req.headers['authorization'];
    if (!auth || !auth.startsWith('Bearer ')) return null;
    const token = auth.substring(7);
    const match = token.match(/^TOKEN_[A-Z]+_([a-zA-Z0-9-]+)$/);
    if (match) {
      const found = this.state.users.find((u) => u.id === match[1]);
      if (found) return found;
    }
    if (token === 'TOKEN_MANAGER') {
      return this.state.users.find((u) => u.role === UserRole.MANAGER);
    }
    if (token === 'TOKEN_MECHANIC') {
      return this.state.users.find((u) => u.role === UserRole.MECHANIC);
    }
    if (token === 'TOKEN_RIDER') {
      return this.state.users.find((u) => u.role === UserRole.RIDER);
    }
    // Try to match any registered user
    return this.state.users.find((u) => u.id === token) || this.state.users[2];
  }

  private async handleRequest(req: http.IncomingMessage, res: http.ServerResponse) {
    const parsed = parseUrl(req.url || '/', true);
    const path = parsed.pathname || '/';
    const method = req.method?.toUpperCase() || 'GET';

    // CORS headers for browser testing
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, PATCH, DELETE, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization, Idempotency-Key, X-Correlation-Id');

    if (method === 'OPTIONS') {
      res.writeHead(204);
      return res.end();
    }

    try {
      // 1. /api/v1/auth/public-config
      if (method === 'GET' && path === '/api/v1/auth/public-config') {
        return this.sendJson(res, 200, { publicDemoMode: false });
      }

      // 2. /api/v1/auth/register
      if (method === 'POST' && path === '/api/v1/auth/register') {
        const body = await this.parseBody(req);
        if (body.role === UserRole.MANAGER) {
          return this.sendProblemDetail(res, 400, 'Bad Request', 'Auto-cadastro proibido para perfil MANAGER');
        }
        if (!body.fullName || !body.cpf || !body.email || !body.password) {
          return this.sendProblemDetail(res, 400, 'Bad Request', 'Campos obrigatórios ausentes');
        }
        const newUser = {
          id: crypto.randomUUID(),
          fullName: body.fullName,
          cpf: body.cpf,
          phone: body.phone,
          email: body.email,
          role: body.role,
          status: AccountStatus.PENDING_APPROVAL,
          password: body.password,
        };
        this.state.users.push(newUser);
        return this.sendJson(res, 201, {
          userId: newUser.id,
          status: AccountStatus.PENDING_APPROVAL,
          message: 'Cadastro recebido! Aguarde a liberação do Gestor.',
        });
      }

      // 3. /api/v1/auth/login
      if (method === 'POST' && path === '/api/v1/auth/login') {
        const body = await this.parseBody(req);
        const user = this.state.users.find((u) => u.email === body.email);
        if (!user || user.password !== body.password) {
          return this.sendProblemDetail(res, 401, 'Unauthorized', 'E-mail ou senha inválidos');
        }
        if (user.status === AccountStatus.PENDING_APPROVAL) {
          return this.sendProblemDetail(res, 403, 'Acesso Negado', 'Seu cadastro está aguardando aprovação do Gestor');
        }
        if (user.status === AccountStatus.REJECTED) {
          return this.sendProblemDetail(res, 403, 'Acesso Negado', 'Seu cadastro foi recusado pelo Gestor');
        }
        return this.sendJson(res, 200, {
          accessToken: `TOKEN_${user.role}_${user.id}`,
          userId: user.id,
          role: user.role,
          fullName: user.fullName,
        });
      }

      // 4. /api/v1/yards
      if (method === 'GET' && path === '/api/v1/yards') {
        return this.sendJson(res, 200, SEED_YARDS);
      }

      // 5. /api/v1/rider/garage
      if (method === 'GET' && path === '/api/v1/rider/garage') {
        const user = this.extractAuthUser(req);
        if (!user || user.role !== UserRole.RIDER) {
          return this.sendProblemDetail(res, 403, 'Forbidden', 'Apenas RIDER tem acesso à garagem');
        }
        const contract = this.state.contracts.find((c) => c.riderId === user.id && c.active);
        const moto = contract ? this.state.motorcycles.find((m) => m.id === contract.motorcycleId) : null;
        
        let openCount = 0;
        const recentList: any[] = [];
        for (const [_, insp] of this.state.inspections.entries()) {
          if (insp.riderId === user.id) {
            if ([InspectionStatus.QUEUED_AI, InspectionStatus.AWAITING_AI_RECOVERY, InspectionStatus.HUMAN_TRIAGE].includes(insp.status)) {
              openCount++;
            }
            recentList.push({
              id: insp.id,
              status: insp.status,
              finalVerdict: insp.finalVerdict,
              totalEstimatedBrl: insp.totalEstimatedBrl || 0.0,
              createdAt: insp.createdAt,
            });
          }
        }

        return this.sendJson(res, 200, {
          activeContract: contract && moto ? {
            contractId: contract.id,
            pickedUpAt: contract.pickedUpAt,
            initialOdometerKm: contract.initialOdometerKm,
            motorcycle: {
              id: moto.id,
              plate: moto.plate,
              model: moto.model,
              status: moto.status,
              currentOdometerKm: moto.currentOdometerKm,
              yardName: moto.yardName || SEED_YARDS[0].name,
            },
          } : null,
          openInspectionsCount: openCount,
          recentInspections: recentList,
        });
      }

      // 6. /api/v1/rider/inspections/upload-url
      if (method === 'POST' && path === '/api/v1/rider/inspections/upload-url') {
        const body = await this.parseBody(req);
        const validTypes = ['image/jpeg', 'image/png', 'image/webp'];
        if (!validTypes.includes(body.contentType)) {
          return this.sendProblemDetail(res, 400, 'Bad Request', 'Formato de imagem inválido. Use JPG, PNG ou WEBP');
        }
        if (body.fileSizeBytes > 10485760) {
          return this.sendProblemDetail(res, 400, 'Bad Request', 'Tamanho do arquivo excede 10 MB');
        }
        const photoKey = `inspections/2026/11/${crypto.randomUUID()}.${body.contentType.split('/')[1]}`;
        return this.sendJson(res, 200, {
          uploadUrl: `http://localhost:19000/mottu-inspections/${photoKey}`,
          photoObjectKey: photoKey,
          expiresInSeconds: 300,
        });
      }

      // 7. /api/v1/rider/inspections (Create)
      if (method === 'POST' && path === '/api/v1/rider/inspections') {
        const user = this.extractAuthUser(req);
        if (!user || user.role !== UserRole.RIDER) {
          return this.sendProblemDetail(res, 403, 'Forbidden', 'Apenas RIDER pode abrir vistoria');
        }

        const idempotencyKey = req.headers['idempotency-key'] as string;
        if (!idempotencyKey) {
          return this.sendProblemDetail(res, 400, 'Bad Request', 'Idempotency-Key header é obrigatório');
        }

        if (this.state.idempotencyKeys.has(idempotencyKey)) {
          const existingId = this.state.idempotencyKeys.get(idempotencyKey)!;
          const existing = this.state.inspections.get(existingId);
          return this.sendJson(res, 202, { inspectionId: existing.id, status: existing.status });
        }

        // Check open inspections count <= 3
        let openCount = 0;
        for (const [_, insp] of this.state.inspections.entries()) {
          if (insp.riderId === user.id && [InspectionStatus.QUEUED_AI, InspectionStatus.AWAITING_AI_RECOVERY, InspectionStatus.HUMAN_TRIAGE].includes(insp.status)) {
            openCount++;
          }
        }
        if (openCount >= 3) {
          return this.sendProblemDetail(res, 422, 'Limite Excedido', 'Máximo de 3 chamados em andamento por contrato');
        }

        const body = await this.parseBody(req);
        const inspectionId = crypto.randomUUID();

        // Check contract & motorcycle
        const contract = this.state.contracts.find((c) => c.riderId === user.id && c.active);
        const moto = contract ? this.state.motorcycles.find((m) => m.id === contract.motorcycleId) : this.state.motorcycles[0];

        // Anti-fraud check: if photo key contains 'fraud' or 'duplicate'
        const isFraud = body.photoObjectKey?.includes('fraud') || body.photoObjectKey?.includes('duplicate');
        
        // Calculate hours and km since pickup
        const pickupDate = contract ? new Date(contract.pickedUpAt).getTime() : Date.now() - 12 * 3600 * 1000;
        const hoursSincePickup = body.simulatedHoursSincePickup !== undefined ? body.simulatedHoursSincePickup : ((Date.now() - pickupDate) / (3600 * 1000));
        const kmDriven = (body.reportedOdometerKm || 1035) - (contract ? contract.initialOdometerKm : 1000);

        let status = InspectionStatus.QUEUED_AI;
        let priorityWeight: number | null = null;
        let priorityReason: QueuePriorityReason | null = null;
        let finalVerdict: DamageVerdict | null = null;
        let aiConfidence = body.simulatedAiConfidence !== undefined ? body.simulatedAiConfidence : 0.92;
        let laborFee = 0.0;
        let totalEstimated = 0.0;
        let items: any[] = [];

        if (isFraud) {
          status = InspectionStatus.HUMAN_TRIAGE;
          priorityReason = QueuePriorityReason.FRAUD_SUSPICION;
          priorityWeight = PRIORITY_WEIGHTS[QueuePriorityReason.FRAUD_SUSPICION];
        } else if (aiConfidence < 0.80) {
          status = InspectionStatus.HUMAN_TRIAGE;
          priorityReason = QueuePriorityReason.LOW_AI_CONFIDENCE;
          priorityWeight = PRIORITY_WEIGHTS[QueuePriorityReason.LOW_AI_CONFIDENCE];
        } else if (body.reportedContext === ReportedOriginContext.RECEIVED_LIKE_THIS) {
          if (hoursSincePickup <= 48.0 || kmDriven <= 100) {
            status = InspectionStatus.AUTO_RESOLVED;
            finalVerdict = DamageVerdict.PRE_EXISTENTE;
            laborFee = 0.0;
            totalEstimated = 0.0;
            items = [{ partName: 'Manete de Freio Direito', unitPriceBrl: 25.0, addedBy: 'AI' }];
          } else {
            status = InspectionStatus.HUMAN_TRIAGE;
            priorityReason = QueuePriorityReason.LOW_AI_CONFIDENCE;
            priorityWeight = PRIORITY_WEIGHTS[QueuePriorityReason.LOW_AI_CONFIDENCE];
          }
        } else if (body.reportedContext === ReportedOriginContext.NORMAL_USE_WEAR) {
          status = InspectionStatus.AUTO_RESOLVED;
          finalVerdict = DamageVerdict.DESGASTE_NATURAL;
          laborFee = 0.0;
          totalEstimated = 0.0;
          items = [{ partName: 'Lona de Freio Traseira', unitPriceBrl: 33.0, addedBy: 'AI' }];
        } else {
          // ACCIDENT_OR_DROP -> MAU_USO
          status = InspectionStatus.AUTO_RESOLVED;
          finalVerdict = DamageVerdict.MAU_USO;
          laborFee = this.state.settings.standardLaborFeeBrl || 35.0;
          totalEstimated = 25.0 + laborFee;
          items = [{ partName: 'Manete de Freio Direito', unitPriceBrl: 25.0, addedBy: 'AI' }];
        }

        const newInsp = {
          id: inspectionId,
          contractId: contract?.id || 'contract-001',
          riderId: user.id,
          riderName: user.fullName,
          motorcycleId: moto?.id || 'moto-001',
          plate: moto?.plate || 'MOT1A23',
          motorcycleModel: moto?.model || MotorcycleModel.MOTTU_SPORT_110I,
          photoObjectKey: body.photoObjectKey,
          reportedContext: body.reportedContext,
          safetyRisk: body.safetyRisk,
          reportedOdometerKm: body.reportedOdometerKm,
          riderNotes: body.riderNotes,
          status,
          priorityWeight,
          priorityReason,
          aiConfidence,
          aiVerdict: finalVerdict,
          finalVerdict,
          laborFeeBrl: laborFee,
          totalEstimatedBrl: totalEstimated,
          customerFeedbackText: 'Vistoria avaliada com sucesso.',
          canContest: status === InspectionStatus.AUTO_RESOLVED,
          contestedOnce: false,
          items,
          appointment: null,
          hoursSincePickup,
          kmDrivenSincePickup: kmDriven,
          fraudSuspected: isFraud,
          createdAt: new Date().toISOString(),
          resolvedAt: status === InspectionStatus.AUTO_RESOLVED ? new Date().toISOString() : null,
        };

        this.state.inspections.set(inspectionId, newInsp);
        this.state.idempotencyKeys.set(idempotencyKey, inspectionId);

        return this.sendJson(res, 202, { inspectionId, status });
      }

      // 8. /api/v1/rider/inspections/{id}
      const riderInspMatch = path.match(/^\/api\/v1\/rider\/inspections\/([a-zA-Z0-9-]+)$/);
      if (method === 'GET' && riderInspMatch) {
        const id = riderInspMatch[1];
        const user = this.extractAuthUser(req);
        if (!user || user.role !== UserRole.RIDER) {
          return this.sendProblemDetail(res, 403, 'Forbidden', 'Apenas RIDER tem acesso');
        }
        const insp = this.state.inspections.get(id);
        if (!insp) {
          return this.sendProblemDetail(res, 404, 'Not Found', 'Chamado não encontrado');
        }
        // IDOR check: cannot view other rider's inspection
        if (insp.riderId !== user.id) {
          return this.sendProblemDetail(res, 404, 'Not Found', 'Chamado não encontrado');
        }
        return this.sendJson(res, 200, {
          id: insp.id,
          status: insp.status,
          photoDownloadUrl: `http://localhost:19000/mottu-inspections/${insp.photoObjectKey}`,
          reportedContext: insp.reportedContext,
          safetyRisk: insp.safetyRisk,
          reportedOdometerKm: insp.reportedOdometerKm,
          riderNotes: insp.riderNotes,
          finalVerdict: insp.finalVerdict,
          aiConfidence: insp.aiConfidence,
          laborFeeBrl: insp.laborFeeBrl,
          totalEstimatedBrl: insp.totalEstimatedBrl,
          customerFeedbackText: insp.customerFeedbackText,
          canContest: insp.status === InspectionStatus.AUTO_RESOLVED && !insp.contestedOnce,
          contestedOnce: insp.contestedOnce,
          items: insp.items,
          appointment: insp.appointment,
          createdAt: insp.createdAt,
          resolvedAt: insp.resolvedAt,
        });
      }

      // 9. /api/v1/rider/inspections/{id}/stream (SSE)
      const sseMatch = path.match(/^\/api\/v1\/rider\/inspections\/([a-zA-Z0-9-]+)\/stream$/);
      if (method === 'GET' && sseMatch) {
        const id = sseMatch[1];
        const insp = this.state.inspections.get(id) || { id, status: InspectionStatus.AUTO_RESOLVED };
        res.writeHead(200, {
          'Content-Type': 'text/event-stream',
          'Cache-Control': 'no-cache',
          'Connection': 'keep-alive',
        });
        res.write(`event: status-change\ndata: ${JSON.stringify({ inspectionId: id, status: insp.status, stepLabel: 'Resultado pronto!' })}\n\n`);
        return res.end();
      }

      // 10. /api/v1/rider/inspections/{id}/contest
      const contestMatch = path.match(/^\/api\/v1\/rider\/inspections\/([a-zA-Z0-9-]+)\/contest$/);
      if (method === 'POST' && contestMatch) {
        const id = contestMatch[1];
        const user = this.extractAuthUser(req);
        if (!user || user.role !== UserRole.RIDER) {
          return this.sendProblemDetail(res, 403, 'Forbidden', 'Apenas RIDER pode contestar');
        }
        const insp = this.state.inspections.get(id);
        if (!insp) return this.sendProblemDetail(res, 404, 'Not Found', 'Chamado não encontrado');
        if (insp.contestedOnce || insp.status !== InspectionStatus.AUTO_RESOLVED) {
          return this.sendProblemDetail(res, 422, 'Regra Violada', 'Chamado não pode ser contestado');
        }
        const body = await this.parseBody(req);
        if (!body.reason || body.reason.trim().length < 10) {
          return this.sendProblemDetail(res, 400, 'Bad Request', 'Justificativa deve ter no mínimo 10 caracteres');
        }

        insp.status = InspectionStatus.HUMAN_TRIAGE;
        insp.contestedOnce = true;
        insp.contestationReason = body.reason;
        insp.priorityReason = QueuePriorityReason.RIDER_CONTESTATION;
        insp.priorityWeight = PRIORITY_WEIGHTS[QueuePriorityReason.RIDER_CONTESTATION];
        return this.sendJson(res, 200, {
          inspectionId: insp.id,
          status: insp.status,
          priorityReason: insp.priorityReason,
        });
      }

      // 11. /api/v1/rider/inspections/{id}/appointment
      const appointmentMatch = path.match(/^\/api\/v1\/rider\/inspections\/([a-zA-Z0-9-]+)\/appointment$/);
      if (method === 'POST' && appointmentMatch) {
        const id = appointmentMatch[1];
        const insp = this.state.inspections.get(id);
        if (!insp) return this.sendProblemDetail(res, 404, 'Not Found', 'Chamado não encontrado');
        const body = await this.parseBody(req);
        
        // Date validation: scheduledDate must not be in the past
        const todayStr = new Date().toISOString().split('T')[0];
        if (body.scheduledDate < todayStr) {
          return this.sendProblemDetail(res, 400, 'Bad Request', 'Data de agendamento não pode ser no passado');
        }
        const yard = SEED_YARDS.find((y) => y.id === body.yardId) || SEED_YARDS[0];
        const appt = {
          appointmentId: crypto.randomUUID(),
          yardId: yard.id,
          yardName: yard.name,
          scheduledDate: body.scheduledDate,
          shift: body.shift || AppointmentShift.MORNING,
        };
        insp.appointment = appt;
        insp.status = InspectionStatus.SCHEDULED;
        return this.sendJson(res, 201, {
          appointmentId: appt.appointmentId,
          inspectionStatus: InspectionStatus.SCHEDULED,
          yardName: appt.yardName,
          scheduledDate: appt.scheduledDate,
          shift: appt.shift,
        });
      }

      // 12. /api/v1/mechanic/queue
      if (method === 'GET' && path === '/api/v1/mechanic/queue') {
        const user = this.extractAuthUser(req);
        if (!user || ![UserRole.MECHANIC, UserRole.MANAGER].includes(user.role)) {
          return this.sendProblemDetail(res, 403, 'Forbidden', 'Apenas MECHANIC ou MANAGER têm acesso à fila');
        }

        const triageList: any[] = [];
        for (const [_, insp] of this.state.inspections.entries()) {
          if (insp.status === InspectionStatus.HUMAN_TRIAGE) {
            triageList.push({
              inspectionId: insp.id,
              plate: insp.plate,
              motorcycleModel: insp.motorcycleModel,
              riderName: insp.riderName,
              priorityWeight: insp.priorityWeight || 3,
              priorityReason: insp.priorityReason || QueuePriorityReason.LOW_AI_CONFIDENCE,
              safetyRisk: insp.safetyRisk,
              fraudSuspected: !!insp.fraudSuspected,
              aiConfidence: insp.aiConfidence,
              aiSuggestedVerdict: insp.aiVerdict,
              createdAt: insp.createdAt,
            });
          }
        }
        // Strict ordering: priorityWeight ASC, createdAt ASC
        triageList.sort((a, b) => {
          if (a.priorityWeight !== b.priorityWeight) {
            return a.priorityWeight - b.priorityWeight;
          }
          return new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime();
        });

        return this.sendJson(res, 200, {
          items: triageList,
          page: 0,
          size: 20,
          totalElements: triageList.length,
          totalPages: 1,
        });
      }

      // 13. /api/v1/mechanic/inspections/{id}
      const mechanicDetailMatch = path.match(/^\/api\/v1\/mechanic\/inspections\/([a-zA-Z0-9-]+)$/);
      if (method === 'GET' && mechanicDetailMatch) {
        const id = mechanicDetailMatch[1];
        const insp = this.state.inspections.get(id);
        if (!insp) return this.sendProblemDetail(res, 404, 'Not Found', 'Chamado não encontrado');
        return this.sendJson(res, 200, {
          ...insp,
          photoDownloadUrl: `http://localhost:19000/mottu-inspections/${insp.photoObjectKey}`,
        });
      }

      // 14. /api/v1/mechanic/inspections/{id}/resolve
      const resolveMatch = path.match(/^\/api\/v1\/mechanic\/inspections\/([a-zA-Z0-9-]+)\/resolve$/);
      if (method === 'POST' && resolveMatch) {
        const id = resolveMatch[1];
        const user = this.extractAuthUser(req);
        if (!user || ![UserRole.MECHANIC, UserRole.MANAGER].includes(user.role)) {
          return this.sendProblemDetail(res, 403, 'Forbidden', 'Apenas MECHANIC ou MANAGER podem resolver laudo');
        }
        const insp = this.state.inspections.get(id);
        if (!insp) return this.sendProblemDetail(res, 404, 'Not Found', 'Chamado não encontrado');
        const body = await this.parseBody(req);

        insp.finalVerdict = body.finalVerdict;
        insp.mechanicNotes = body.mechanicNotes;
        insp.reviewedByMechanicId = user.id;
        insp.status = InspectionStatus.HUMAN_RESOLVED;
        insp.resolvedAt = new Date().toISOString();

        if (body.finalVerdict === DamageVerdict.MAU_USO) {
          insp.laborFeeBrl = this.state.settings.standardLaborFeeBrl || 35.0;
          insp.totalEstimatedBrl = 25.0 + insp.laborFeeBrl;
        } else {
          insp.laborFeeBrl = 0.0;
          insp.totalEstimatedBrl = 0.0;
        }

        insp.customerFeedbackText = `Olá! Nosso mecânico revisou sua foto com atenção e confirmou laudo como ${body.finalVerdict}.`;
        
        // Audit log
        this.state.auditLogs.push({
          id: crypto.randomUUID(),
          inspectionId: insp.id,
          actorId: user.id,
          actorRole: user.role,
          newStatus: InspectionStatus.HUMAN_RESOLVED,
        });

        // Outbox event
        this.state.outbox.push({
          inspectionId: insp.id,
          finalVerdict: insp.finalVerdict,
          totalEstimatedBrl: insp.totalEstimatedBrl,
          exported: false,
        });

        return this.sendJson(res, 200, {
          inspectionId: insp.id,
          status: InspectionStatus.HUMAN_RESOLVED,
          finalVerdict: insp.finalVerdict,
          totalEstimatedBrl: insp.totalEstimatedBrl,
          customerFeedbackText: insp.customerFeedbackText,
        });
      }

      // 15. /api/v1/fleet/motorcycles
      if (path === '/api/v1/fleet/motorcycles') {
        if (method === 'GET') {
          return this.sendJson(res, 200, {
            items: this.state.motorcycles,
            page: 0,
            size: 20,
            totalElements: this.state.motorcycles.length,
            totalPages: 1,
          });
        }
        if (method === 'POST') {
          const body = await this.parseBody(req);
          if (body.currentOdometerKm !== undefined && body.currentOdometerKm < 0) {
            return this.sendProblemDetail(res, 400, 'Bad Request', 'Odômetro não pode ser negativo');
          }
          const yard = SEED_YARDS.find((y) => y.id === body.yardId) || SEED_YARDS[0];
          const newMoto = {
            id: crypto.randomUUID(),
            plate: body.plate,
            model: body.model,
            yardId: yard.id,
            yardName: yard.name,
            status: MotorcycleStatus.AVAILABLE,
            currentOdometerKm: body.currentOdometerKm || 0,
            activeRiderName: null,
          };
          this.state.motorcycles.push(newMoto);
          return this.sendJson(res, 201, newMoto);
        }
      }

      // 16. /api/v1/manager/users/pending
      if (method === 'GET' && path === '/api/v1/manager/users/pending') {
        const user = this.extractAuthUser(req);
        if (!user || user.role !== UserRole.MANAGER) {
          return this.sendProblemDetail(res, 403, 'Forbidden', 'Apenas MANAGER tem acesso');
        }
        const pending = this.state.users.filter((u) => u.status === AccountStatus.PENDING_APPROVAL);
        return this.sendJson(res, 200, pending);
      }

      // 17. /api/v1/manager/users/{id}/approve
      const approveMatch = path.match(/^\/api\/v1\/manager\/users\/([a-zA-Z0-9-]+)\/approve$/);
      if (method === 'POST' && approveMatch) {
        const id = approveMatch[1];
        const user = this.state.users.find((u) => u.id === id);
        if (!user) return this.sendProblemDetail(res, 404, 'Not Found', 'Usuário não encontrado');
        user.status = AccountStatus.ACTIVE;
        const body = await this.parseBody(req);
        let contractId: string | null = null;
        if (user.role === UserRole.RIDER && body.motorcycleId) {
          const moto = this.state.motorcycles.find((m) => m.id === body.motorcycleId);
          if (moto) {
            moto.status = MotorcycleStatus.RENTED;
            moto.activeRiderName = user.fullName;
            contractId = crypto.randomUUID();
            this.state.contracts.push({
              id: contractId,
              riderId: user.id,
              motorcycleId: moto.id,
              pickedUpAt: new Date().toISOString(),
              initialOdometerKm: moto.currentOdometerKm,
              active: true,
            });
          }
        }
        return this.sendJson(res, 200, {
          userId: user.id,
          status: AccountStatus.ACTIVE,
          contractId,
        });
      }

      // 18. /api/v1/manager/users/{id}/reject
      const rejectMatch = path.match(/^\/api\/v1\/manager\/users\/([a-zA-Z0-9-]+)\/reject$/);
      if (method === 'POST' && rejectMatch) {
        const id = rejectMatch[1];
        const user = this.state.users.find((u) => u.id === id);
        if (!user) return this.sendProblemDetail(res, 404, 'Not Found', 'Usuário não encontrado');
        user.status = AccountStatus.REJECTED;
        return this.sendJson(res, 200, {
          userId: user.id,
          status: AccountStatus.REJECTED,
        });
      }

      // 19. /api/v1/manager/catalog/sync-mottu-store
      if (method === 'POST' && path === '/api/v1/manager/catalog/sync-mottu-store') {
        const user = this.extractAuthUser(req);
        if (!user || user.role !== UserRole.MANAGER) {
          return this.sendProblemDetail(res, 403, 'Forbidden', 'Apenas MANAGER pode sincronizar catálogo');
        }
        return this.sendJson(res, 200, {
          syncedCount: 48,
          usedFallback: false,
          syncedAt: new Date().toISOString(),
          message: 'Catálogo sincronizado com loja.mottu.com.br!',
        });
      }

      // 20. /api/v1/manager/catalog/parts
      if (method === 'GET' && path === '/api/v1/manager/catalog/parts') {
        return this.sendJson(res, 200, {
          items: this.state.parts,
          page: 0,
          size: 20,
          totalElements: this.state.parts.length,
          totalPages: 1,
        });
      }

      const updatePartMatch = path.match(/^\/api\/v1\/manager\/catalog\/parts\/([a-zA-Z0-9-]+)$/);
      if (method === 'PATCH' && updatePartMatch) {
        const id = updatePartMatch[1];
        const part = this.state.parts.find((p) => p.id === id);
        if (!part) return this.sendProblemDetail(res, 404, 'Not Found', 'Peça não encontrada');
        const body = await this.parseBody(req);
        if (body.unitPriceBrl !== undefined) {
          part.unitPriceBrl = body.unitPriceBrl;
          part.priceOverridden = true;
        }
        if (body.isNaturalWear !== undefined) {
          part.isNaturalWear = body.isNaturalWear;
        }
        return this.sendJson(res, 200, part);
      }

      // 21. /api/v1/manager/settings
      if (path === '/api/v1/manager/settings') {
        if (method === 'GET') {
          return this.sendJson(res, 200, { standardLaborFeeBrl: this.state.settings.standardLaborFeeBrl });
        }
        if (method === 'PATCH') {
          const body = await this.parseBody(req);
          if (body.standardLaborFeeBrl !== undefined) {
            this.state.settings.standardLaborFeeBrl = body.standardLaborFeeBrl;
          }
          return this.sendJson(res, 200, { standardLaborFeeBrl: this.state.settings.standardLaborFeeBrl });
        }
      }

      // 22. /api/v1/manager/analytics/summary
      if (method === 'GET' && path === '/api/v1/manager/analytics/summary') {
        const user = this.extractAuthUser(req);
        if (!user || user.role !== UserRole.MANAGER) {
          return this.sendProblemDetail(res, 403, 'Forbidden', 'Apenas MANAGER tem acesso a analytics');
        }
        let queueSize = 0;
        for (const [_, insp] of this.state.inspections.entries()) {
          if (insp.status === InspectionStatus.HUMAN_TRIAGE) queueSize++;
        }
        return this.sendJson(res, 200, {
          activeMotorcyclesCount: 124,
          autoResolvedPercentage: 78.5,
          currentHumanQueueSize: queueSize,
          estimatedMechanicHoursSaved: 36.2,
          lookerStudioEmbedUrl: 'https://lookerstudio.google.com/embed/reporting/placeholder',
        });
      }

      // 23. /api/v1/rider/accidents/upload-video-url
      if (method === 'POST' && path === '/api/v1/rider/accidents/upload-video-url') {
        const user = this.extractAuthUser(req);
        if (!user || user.role !== UserRole.RIDER) {
          return this.sendProblemDetail(res, 403, 'Forbidden', 'Apenas RIDER pode solicitar upload de vídeo de acidente');
        }
        const body = await this.parseBody(req);
        const allowedTypes = ['video/mp4', 'video/webm', 'video/quicktime'];
        if (!body.contentType || !allowedTypes.includes(body.contentType)) {
          return this.sendProblemDetail(res, 400, 'Bad Request', 'Tipo de arquivo não suportado. Envie um vídeo MP4, WebM ou QuickTime.');
        }
        if (!body.fileSize || body.fileSize > 31457280) { // 30 MB
          return this.sendProblemDetail(res, 400, 'Bad Request', 'Vídeo excede o limite máximo permitido de 30MB.');
        }
        if (body.durationSeconds !== undefined && body.durationSeconds > 30) {
          return this.sendProblemDetail(res, 400, 'Bad Request', 'Duração do vídeo não pode exceder 30 segundos.');
        }
        const videoObjectKey = `accidents/videos/${crypto.randomUUID()}.mp4`;
        return this.sendJson(res, 200, {
          uploadUrl: `http://localhost:19000/mottu-inspections/${videoObjectKey}?presigned=true`,
          videoObjectKey,
          expiresAt: new Date(Date.now() + 15 * 60 * 1000).toISOString(),
        });
      }

      // 24. /api/v1/rider/accidents/upload-photo-url
      if (method === 'POST' && path === '/api/v1/rider/accidents/upload-photo-url') {
        const user = this.extractAuthUser(req);
        if (!user || user.role !== UserRole.RIDER) {
          return this.sendProblemDetail(res, 403, 'Forbidden', 'Apenas RIDER pode solicitar upload de foto de acidente');
        }
        const body = await this.parseBody(req);
        const allowedTypes = ['image/jpeg', 'image/png', 'image/webp'];
        if (!body.contentType || !allowedTypes.includes(body.contentType)) {
          return this.sendProblemDetail(res, 400, 'Bad Request', 'Formato de imagem não suportado. Envie WebP, JPEG ou PNG.');
        }
        if (!body.fileSize || body.fileSize > 10485760) { // 10 MB
          return this.sendProblemDetail(res, 400, 'Bad Request', 'Foto excede o limite máximo de 10MB.');
        }
        const photoObjectKey = `accidents/photos/${crypto.randomUUID()}.webp`;
        return this.sendJson(res, 200, {
          uploadUrl: `http://localhost:19000/mottu-inspections/${photoObjectKey}?presigned=true`,
          photoObjectKey,
          expiresAt: new Date(Date.now() + 15 * 60 * 1000).toISOString(),
        });
      }

      // 25. /api/v1/rider/accidents
      if (method === 'POST' && path === '/api/v1/rider/accidents') {
        const user = this.extractAuthUser(req);
        if (!user || user.role !== UserRole.RIDER) {
          return this.sendProblemDetail(res, 403, 'Forbidden', 'Apenas RIDER pode registrar acidente');
        }
        const idempotencyKey = (req.headers['idempotency-key'] as string) || '';
        if (!idempotencyKey) {
          return this.sendProblemDetail(res, 400, 'Bad Request', 'Cabeçalho Idempotency-Key é obrigatório');
        }
        if (this.state.idempotencyKeys.has(idempotencyKey)) {
          const existingId = this.state.idempotencyKeys.get(idempotencyKey)!;
          const existing = this.state.accidents.get(existingId);
          if (existing) {
            return this.sendJson(res, 200, {
              accidentReportId: existing.id,
              status: existing.status,
              message: 'Relatório já processado anteriormente (Idempotência).',
              estimatedAt: existing.estimatedAt,
            });
          }
        }

        const body = await this.parseBody(req);
        if (!body.videoObjectKey) {
          return this.sendProblemDetail(res, 400, 'Bad Request', 'Chave do vídeo de acidente é obrigatória');
        }
        if (body.drivability !== 'DRIVABLE' && body.drivability !== 'NEEDS_TOWING') {
          return this.sendProblemDetail(res, 400, 'Bad Request', 'Condição de rodagem inválida');
        }
        if (body.reportedOdometerKm === undefined || body.reportedOdometerKm < 0) {
          return this.sendProblemDetail(res, 400, 'Bad Request', 'Odômetro não pode ser negativo');
        }

        const activeContract = this.state.contracts.find((c) => c.riderId === user.id && c.active);
        const moto = activeContract
          ? this.state.motorcycles.find((m) => m.id === activeContract.motorcycleId)
          : this.state.motorcycles[0];

        if (body.drivability === 'NEEDS_TOWING' && moto) {
          moto.status = MotorcycleStatus.IN_MAINTENANCE;
        }

        const accidentId = crypto.randomUUID();
        const items = [
          {
            id: crypto.randomUUID(),
            partName: 'Guidão Completo',
            unitPriceBrl: 145.0,
            videoTimestampSeconds: 4,
          },
          {
            id: crypto.randomUUID(),
            partName: 'Manete de Freio Dianteiro',
            unitPriceBrl: 48.0,
            videoTimestampSeconds: 8,
          },
        ];

        const partsSubtotalBrl = 193.0;
        const laborFeeBrl = 35.0; // Tiered: 2 parts = R$ 35
        const towingFeeBrl = body.drivability === 'NEEDS_TOWING' ? 80.0 : 0.0;
        const totalCostBrl = partsSubtotalBrl + laborFeeBrl + towingFeeBrl;
        const now = new Date().toISOString();

        const accidentRecord = {
          id: accidentId,
          riderId: user.id,
          riderName: user.fullName || 'Rider Mottu',
          riderPhone: user.phone || '(11) 98765-4321',
          motorcycleId: moto ? moto.id : 'moto-seed-001',
          plate: moto ? moto.plate : 'MOT-2026',
          model: moto ? moto.model : 'SPORT_110I',
          videoObjectKey: body.videoObjectKey,
          photoObjectKeys: body.photoObjectKeys || [],
          hasInjuries: !!body.hasInjuries,
          involvesThirdParty: !!body.involvesThirdParty,
          policeReportNumber: body.policeReportNumber || null,
          drivability: body.drivability,
          reportedOdometerKm: body.reportedOdometerKm,
          description: body.description || '',
          status: 'ESTIMATED_PENDING_WORKSHOP',
          partsSubtotalBrl,
          laborFeeBrl,
          towingFeeBrl,
          totalCostBrl,
          items,
          estimatedAt: now,
          confirmedAt: null,
          mechanicNotes: null,
        };

        this.state.accidents.set(accidentId, accidentRecord);
        this.state.idempotencyKeys.set(idempotencyKey, accidentId);

        return this.sendJson(res, 201, {
          accidentReportId: accidentId,
          status: 'ESTIMATED_PENDING_WORKSHOP',
          message: 'Relatório registrado com sucesso.',
          estimatedAt: now,
        });
      }

      // 26. /api/v1/rider/accidents/:id
      if (method === 'GET' && path.startsWith('/api/v1/rider/accidents/')) {
        const id = path.replace('/api/v1/rider/accidents/', '');
        const accident = this.state.accidents.get(id);
        if (!accident) {
          return this.sendProblemDetail(res, 404, 'Not Found', 'Relatório de acidente não encontrado');
        }
        return this.sendJson(res, 200, {
          id: accident.id,
          status: accident.status,
          plate: accident.plate,
          model: accident.model,
          hasInjuries: accident.hasInjuries,
          involvesThirdParty: accident.involvesThirdParty,
          policeReportNumber: accident.policeReportNumber,
          drivability: accident.drivability,
          reportedOdometerKm: accident.reportedOdometerKm,
          partsSubtotalBrl: accident.partsSubtotalBrl,
          laborFeeBrl: accident.laborFeeBrl,
          towingFeeBrl: accident.towingFeeBrl,
          totalCostBrl: accident.totalCostBrl,
          items: accident.items,
          estimatedAt: accident.estimatedAt,
        });
      }

      // 27. /api/v1/mechanic/accidents
      if (method === 'GET' && path === '/api/v1/mechanic/accidents') {
        const user = this.extractAuthUser(req);
        if (!user || (user.role !== UserRole.MECHANIC && user.role !== UserRole.MANAGER)) {
          return this.sendProblemDetail(res, 403, 'Forbidden', 'Apenas MECHANIC ou MANAGER podem acessar a fila de acidentes');
        }
        const items = Array.from(this.state.accidents.values()).map((a) => ({
          id: a.id,
          status: a.status,
          plate: a.plate,
          model: a.model,
          riderName: a.riderName,
          hasInjuries: a.hasInjuries,
          drivability: a.drivability,
          totalCostBrl: a.totalCostBrl,
          itemsCount: a.items.length,
          createdAt: a.estimatedAt,
        }));
        return this.sendJson(res, 200, {
          items,
          page: 0,
          size: 20,
          totalElements: items.length,
          totalPages: 1,
        });
      }

      // 28. /api/v1/mechanic/accidents/:id/confirm
      if (method === 'POST' && path.match(/^\/api\/v1\/mechanic\/accidents\/([^\/]+)\/confirm$/)) {
        const user = this.extractAuthUser(req);
        if (!user || (user.role !== UserRole.MECHANIC && user.role !== UserRole.MANAGER)) {
          return this.sendProblemDetail(res, 403, 'Forbidden', 'Apenas MECHANIC ou MANAGER podem confirmar laudo');
        }
        const match = path.match(/^\/api\/v1\/mechanic\/accidents\/([^\/]+)\/confirm$/);
        const id = match ? match[1] : '';
        const accident = this.state.accidents.get(id);
        if (!accident) {
          return this.sendProblemDetail(res, 404, 'Not Found', 'Relatório de acidente não encontrado');
        }

        const body = await this.parseBody(req);
        const confirmedPartIds: string[] = body.confirmedPartIds || [];
        const confirmedItems = accident.items.filter((item: any) => confirmedPartIds.includes(item.id));
        const confirmedPartsSubtotal = confirmedItems.reduce((acc: number, item: any) => acc + item.unitPriceBrl, 0);

        let confirmedLabor = 0.0;
        if (confirmedItems.length >= 3) confirmedLabor = 90.0;
        else if (confirmedItems.length >= 1) confirmedLabor = 35.0;

        const confirmedTotal = confirmedPartsSubtotal + confirmedLabor + accident.towingFeeBrl;
        const now = new Date().toISOString();

        accident.status = 'WORKSHOP_CONFIRMED';
        accident.confirmedAt = now;
        accident.mechanicNotes = body.mechanicNotes || 'Oficina confirmou danos estruturais';
        accident.partsSubtotalBrl = confirmedPartsSubtotal;
        accident.laborFeeBrl = confirmedLabor;
        accident.totalCostBrl = confirmedTotal;

        this.state.outbox.push({
          id: crypto.randomUUID(),
          eventType: 'ACCIDENT_REPORT_CONFIRMED',
          accidentReportId: id,
          payload: {
            accidentReportId: id,
            status: 'WORKSHOP_CONFIRMED',
            totalCostBrl: confirmedTotal,
            confirmedAt: now,
          },
          createdAt: now,
        });

        return this.sendJson(res, 200, {
          accidentReportId: id,
          status: 'WORKSHOP_CONFIRMED',
          confirmedTotalCostBrl: confirmedTotal,
          message: 'Laudo de oficina confirmado.',
          confirmedAt: now,
        });
      }

      // 29. /api/v1/mechanic/accidents/:id
      if (method === 'GET' && path.startsWith('/api/v1/mechanic/accidents/')) {
        const user = this.extractAuthUser(req);
        if (!user || (user.role !== UserRole.MECHANIC && user.role !== UserRole.MANAGER)) {
          return this.sendProblemDetail(res, 403, 'Forbidden', 'Apenas MECHANIC ou MANAGER podem acessar o dossiê');
        }
        const id = path.replace('/api/v1/mechanic/accidents/', '');
        const accident = this.state.accidents.get(id);
        if (!accident) {
          return this.sendProblemDetail(res, 404, 'Not Found', 'Relatório de acidente não encontrado');
        }
        return this.sendJson(res, 200, {
          id: accident.id,
          status: accident.status,
          plate: accident.plate,
          model: accident.model,
          riderName: accident.riderName,
          riderPhone: accident.riderPhone,
          hasInjuries: accident.hasInjuries,
          involvesThirdParty: accident.involvesThirdParty,
          policeReportNumber: accident.policeReportNumber,
          drivability: accident.drivability,
          reportedOdometerKm: accident.reportedOdometerKm,
          partsSubtotalBrl: accident.partsSubtotalBrl,
          laborFeeBrl: accident.laborFeeBrl,
          towingFeeBrl: accident.towingFeeBrl,
          totalCostBrl: accident.totalCostBrl,
          items: accident.items,
          estimatedAt: accident.estimatedAt,
          confirmedAt: accident.confirmedAt,
          mechanicNotes: accident.mechanicNotes,
          videoDownloadUrl: `http://localhost:19000/mottu-inspections/${accident.videoObjectKey}?download=true`,
        });
      }

      // Default 404
      return this.sendProblemDetail(res, 404, 'Not Found', `Rota não encontrada: ${method} ${path}`);
    } catch (err: any) {
      return this.sendProblemDetail(res, 500, 'Internal Server Error', err?.message || 'Erro inesperado');
    }
  }
}
