import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MechanicService } from '../../services/mechanic.service';
import { PhotoZoomLensComponent } from '../../components/photo-zoom-lens/photo-zoom-lens.component';
import {
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
  imports: [CommonModule, FormsModule, PhotoZoomLensComponent],
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
    return !this.isSubmitting() && d !== null && d.status === 'HUMAN_TRIAGE';
  });

  boundingBoxes = computed<DamageBoundingBox[]>(() => {
    const d = this.details();
    if (!d || !d.items || d.items.length === 0) return [];
    // Provide visual bounding chips based on items
    return d.items.map((item, idx) => ({
      label: item.partName,
      confidence: Math.round((d.aiConfidence || 0.85) * 100),
      x: 20 + (idx % 2) * 30,
      y: 25 + (idx % 2) * 25,
      width: 35,
      height: 30,
    }));
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

  backToQueue(): void {
    this.router.navigate(['/mechanic/queue']);
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
