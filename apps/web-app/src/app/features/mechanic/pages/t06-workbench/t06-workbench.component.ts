import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MechanicService } from '../../services/mechanic.service';
import { PhotoZoomLensComponent } from '../../components/photo-zoom-lens/photo-zoom-lens.component';
import { DesktopShellComponent } from '../../../../shared/layout/desktop-shell/desktop-shell.component';
import {
  Baseline360Photos,
  CommitteeEvaluationData,
  DamageBoundingBox,
  DamageVerdict,
  MechanicInspectionDetailsResponse,
  MotorcycleModel,
  ResolveInspectionRequest,
  ResolveInspectionResponse,
} from '../../models/mechanic.models';

@Component({
  selector: 'app-t06-workbench',
  standalone: true,
  imports: [CommonModule, FormsModule, PhotoZoomLensComponent, DesktopShellComponent],
  templateUrl: './t06-workbench.component.html',
})
export class T06WorkbenchComponent implements OnInit {
  private mechanicService: MechanicService;
  private route: ActivatedRoute;
  private router: Router;

  inspectionId = signal<string>('');
  details = signal<MechanicInspectionDetailsResponse | null>(null);
  isLoading = signal<boolean>(true);
  isSubmitting = signal<boolean>(false);
  errorMessage = signal<string | null>(null);
  resolutionResult = signal<ResolveInspectionResponse | null>(null);

  selectedVerdict = signal<DamageVerdict>('MAU_USO');
  selectedPartIds = signal<Set<string>>(new Set());
  mechanicNotes = signal<string>('');
  readonly standardLaborFee = 35.0;

  activePhotoTab = signal<'current' | 'baseline'>('current');
  selectedBaselineAngle = signal<'front' | 'rear' | 'right' | 'left' | 'odometer'>('front');
  isPresentialModalOpen = signal<boolean>(false);
  presentialReasonInput = signal<string>('Necessidade de avaliação presencial na bancada da oficina Mottu.');
  isConvoking = signal<boolean>(false);

  partsCost = computed(() => {
    const d = this.details();
    if (!d || this.selectedVerdict() !== 'MAU_USO') return 0.0;
    const set = this.selectedPartIds();
    return d.items
      .filter((item) => item.catalogPartId && set.has(item.catalogPartId))
      .reduce((sum, item) => sum + Number(item.unitPriceBrl), 0.0);
  });

  laborCost = computed(() => {
    return this.selectedVerdict() === 'MAU_USO' ? this.standardLaborFee : 0.0;
  });

  totalEstimated = computed(() => {
    return this.partsCost() + this.laborCost();
  });

  canSubmit = computed(() => {
    const d = this.details();
    return !this.isSubmitting() && !this.isConvoking() && d !== null && d.status === 'HUMAN_TRIAGE';
  });

  committeeEvaluation = computed<CommitteeEvaluationData | null>(() => {
    const raw = this.details()?.committeeEvaluationJson;
    if (!raw) return null;
    try {
      return typeof raw === 'string' ? JSON.parse(raw) : (raw as unknown as CommitteeEvaluationData);
    } catch {
      return null;
    }
  });

  baselinePhotos = computed<Baseline360Photos | null>(() => {
    const raw = this.details()?.pickupPhotosJson;
    if (!raw) return null;
    try {
      return typeof raw === 'string' ? JSON.parse(raw) : (raw as unknown as Baseline360Photos);
    } catch {
      return null;
    }
  });

  hasBaselinePhotos = computed(() => {
    const b = this.baselinePhotos();
    return !!(b && (b.frontPhotoUrl || b.rearPhotoUrl || b.rightSidePhotoUrl || b.leftSidePhotoUrl));
  });

  currentBaselinePhotoUrl = computed(() => {
    const b = this.baselinePhotos();
    if (!b) return '/demo-photos/avaria-retrovisor.webp';
    switch (this.selectedBaselineAngle()) {
      case 'front':
        return b.frontPhotoUrl || '/demo-photos/avaria-retrovisor.webp';
      case 'rear':
        return b.rearPhotoUrl || '/demo-photos/avaria-retrovisor.webp';
      case 'right':
        return b.rightSidePhotoUrl || '/demo-photos/avaria-retrovisor.webp';
      case 'left':
        return b.leftSidePhotoUrl || '/demo-photos/avaria-retrovisor.webp';
      case 'odometer':
        return b.odometerPhotoUrl || '/demo-photos/avaria-retrovisor.webp';
    }
  });

  boundingBoxes = computed<DamageBoundingBox[]>(() => {
    const d = this.details();
    if (!d || !d.items || d.items.length === 0) return [];
    return d.items.map((item, idx) => {
      const name = (item.partName || '').toLowerCase();
      if (name.includes('retrovisor') || name.includes('espelho')) {
        return { label: item.partName, confidence: Math.round((d.aiConfidence || 0.94) * 100), x: 50, y: 14, width: 28, height: 26 };
      }
      if (name.includes('manete') || name.includes('freio') || name.includes('guidão')) {
        return { label: item.partName, confidence: Math.round((d.aiConfidence || 0.92) * 100), x: 44, y: 28, width: 32, height: 24 };
      }
      if (name.includes('pneu') || name.includes('roda')) {
        return { label: item.partName, confidence: Math.round((d.aiConfidence || 0.96) * 100), x: 26, y: 22, width: 48, height: 48 };
      }
      if (name.includes('farol') || name.includes('lâmpada') || name.includes('ótico')) {
        return { label: item.partName, confidence: Math.round((d.aiConfidence || 0.91) * 100), x: 28, y: 18, width: 42, height: 36 };
      }
      if (name.includes('escapamento') || name.includes('pedaleira')) {
        return { label: item.partName, confidence: Math.round((d.aiConfidence || 0.89) * 100), x: 35, y: 44, width: 44, height: 28 };
      }
      return {
        label: item.partName,
        confidence: Math.round((d.aiConfidence || 0.88) * 100),
        x: 20 + (idx % 2) * 30,
        y: 25 + (idx % 2) * 25,
        width: 35,
        height: 30,
      };
    });
  });

  constructor(
    mechanicService?: MechanicService,
    route?: ActivatedRoute,
    router?: Router
  ) {
    try {
      this.mechanicService = mechanicService ?? inject(MechanicService);
    } catch {
      this.mechanicService = mechanicService as MechanicService;
    }
    try {
      this.route = route ?? inject(ActivatedRoute);
    } catch {
      this.route = route as ActivatedRoute;
    }
    try {
      this.router = router ?? inject(Router);
    } catch {
      this.router = router as Router;
    }
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id') || '';
    this.inspectionId.set(id);
    if (id) {
      this.loadDetails(id);
    } else {
      this.errorMessage.set('ID da vistoria não informado.');
      this.isLoading.set(false);
    }
  }

  loadDetails(id: string): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.mechanicService.getInspectionDetails(id).subscribe({
      next: (data) => {
        this.details.set(data);
        if (data.aiSuggestedVerdict) {
          this.selectedVerdict.set(data.aiSuggestedVerdict);
        }
        // Pre-select AI detected parts
        const initialPartIds = new Set<string>();
        data.items.forEach((item) => {
          if (item.catalogPartId) {
            initialPartIds.add(item.catalogPartId);
          }
        });
        this.selectedPartIds.set(initialPartIds);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('Não foi possível carregar os detalhes da vistoria.');
        this.isLoading.set(false);
      },
    });
  }

  setVerdict(verdict: DamageVerdict): void {
    this.selectedVerdict.set(verdict);
  }

  togglePart(partId: string): void {
    const set = new Set(this.selectedPartIds());
    if (set.has(partId)) {
      set.delete(partId);
    } else {
      set.add(partId);
    }
    this.selectedPartIds.set(set);
  }

  resolveInspection(): void {
    if (!this.canSubmit()) return;

    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    const payload: ResolveInspectionRequest = {
      finalVerdict: this.selectedVerdict(),
      catalogPartIds: Array.from(this.selectedPartIds()),
      mechanicNotes: this.mechanicNotes().trim(),
    };

    this.mechanicService.resolveInspection(this.inspectionId(), payload).subscribe({
      next: (res) => {
        this.isSubmitting.set(false);
        this.resolutionResult.set(res);
        // Update local details status
        const current = this.details();
        if (current) {
          this.details.set({
            ...current,
            status: 'HUMAN_RESOLVED',
            finalVerdict: res.finalVerdict,
            totalEstimatedBrl: res.totalEstimatedBrl,
            customerFeedbackText: res.customerFeedbackText,
          });
        }
      },
      error: () => {
        this.isSubmitting.set(false);
        this.errorMessage.set('Não foi possível emitir o laudo definitivo.');
      },
    });
  }

  openPresentialModal(): void {
    this.isPresentialModalOpen.set(true);
  }

  closePresentialModal(): void {
    this.isPresentialModalOpen.set(false);
  }

  submitPresentialConvocation(): void {
    if (!this.canSubmit()) return;

    this.isConvoking.set(true);
    this.errorMessage.set(null);

    const reason = this.presentialReasonInput().trim() || 'Necessidade de avaliação presencial na bancada da oficina Mottu.';
    const payload: ResolveInspectionRequest = {
      finalVerdict: 'DESGASTE_NATURAL',
      catalogPartIds: [],
      mechanicNotes: reason,
      requestPresentialInspection: true,
    };

    this.mechanicService.resolveInspection(this.inspectionId(), payload).subscribe({
      next: (res) => {
        this.isConvoking.set(false);
        this.closePresentialModal();
        this.resolutionResult.set(res);
        const current = this.details();
        if (current) {
          this.details.set({
            ...current,
            status: 'HUMAN_RESOLVED',
            finalVerdict: res.finalVerdict,
            totalEstimatedBrl: res.totalEstimatedBrl,
            customerFeedbackText: res.customerFeedbackText,
            presentialInspectionRequested: true,
            presentialRequestReason: reason,
          });
        }
      },
      error: () => {
        this.isConvoking.set(false);
        this.errorMessage.set('Não foi possível registrar a convocação presencial.');
      },
    });
  }

  backToQueue(): void {
    this.router.navigate(['/mechanic/queue']);
  }

  formatCustomerMessage(rawText: string | null | undefined): string {
    if (!rawText) return 'Laudo concluído pela oficina com sucesso.';
    return rawText
      .replace(/\*\*(.*?)\*\*/g, '$1')
      .replace(/\*(.*?)\*/g, '$1')
      .replace(/^#{1,6}\s+/gm, '')
      .replace(/^\s*[-*]\s+/gm, '• ')
      .trim();
  }

  formatModel(model: MotorcycleModel | string): string {
    switch (model) {
      case 'MOTTU_SPORT_110I':
        return 'Mottu Sport 110i';
      case 'HONDA_POP_110I':
        return 'Honda Pop 110i';
      case 'TVS_APACHE_160':
        return 'TVS Apache 160';
      case 'TVS_RAIDER_125':
        return 'TVS Raider 125';
      default:
        return model || 'Mottu Sport';
    }
  }
}
