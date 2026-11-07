import { test, expect } from '../../src/fixtures/test-context.js';
import {
  ReportedOriginContext,
  SafetyRiskLevel,
  DamageVerdict,
  InspectionStatus,
  AppointmentShift,
} from '../../src/contracts/enums.js';

test.describe('Tier 4: Scenario 2 - Normal Wear & Tear Preventive Journey', () => {
  test('Complete Journey: Veteran Rider with 5,000 km reports brake wear -> Auto-approved R$ 0.00 preventive exchange', async ({
    riderClient,
    mockServer,
  }) => {
    if (mockServer) mockServer.resetState();

    // 1. Rider accesses garage
    const garage = await riderClient.getGarage();
    expect(garage.status).toBe(200);

    // 2. Rider requests upload URL
    const upload = await riderClient.getUploadUrl({
      contentType: 'image/webp',
      fileSizeBytes: 350000,
    });
    expect(upload.status).toBe(200);

    // 3. Rider submits inspection for brake wear
    const insp = await riderClient.createInspection({
      photoObjectKey: upload.data.photoObjectKey,
      reportedContext: ReportedOriginContext.NORMAL_USE_WEAR,
      safetyRisk: SafetyRiskLevel.SAFE_TO_RIDE,
      reportedOdometerKm: 6000, // 5,000 km driven
      riderNotes: 'Pastilha de freio gasta pelo uso diário nas entregas.',
    });
    expect(insp.status).toBe(202);

    // 4. AI detects natural wear item in catalog -> DESGASTE_NATURAL with R$ 0.00
    const detail = await riderClient.getInspection(insp.data.inspectionId);
    expect(detail.status).toBe(200);
    expect(detail.data.status).toBe(InspectionStatus.AUTO_RESOLVED);
    expect(detail.data.finalVerdict).toBe(DamageVerdict.DESGASTE_NATURAL);
    expect(detail.data.totalEstimatedBrl).toBe(0.0);

    // 5. Rider schedules preventive maintenance
    const yards = await riderClient.getYards();
    const apptDate = new Date(Date.now() + 3 * 86400000).toISOString().split('T')[0];
    const appt = await riderClient.createAppointment(insp.data.inspectionId, {
      yardId: yards.data[0].id,
      scheduledDate: apptDate,
      shift: AppointmentShift.AFTERNOON,
    });
    expect(appt.status).toBe(201);
    expect(appt.data.inspectionStatus).toBe(InspectionStatus.SCHEDULED);
  });
});
