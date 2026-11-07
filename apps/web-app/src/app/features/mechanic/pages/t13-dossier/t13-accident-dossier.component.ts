import { Component, ElementRef, OnInit, ViewChild, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { AccidentMechanicService } from '../../services/accident-mechanic.service';
import { MechanicAccidentDetailsResponse } from '../../models/accident-dossier.models';

@Component({
  selector: 'app-t13-accident-dossier',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './t13-accident-dossier.component.html',
})
export class T13AccidentDossierComponent implements OnInit {
  private accidentMechanicService: AccidentMechanicService;
  private route: ActivatedRoute | null;
  private router: Router | null;

  @ViewChild('videoPlayer') videoPlayer?: ElementRef<HTMLVideoElement>;

  details = signal<MechanicAccidentDetailsResponse | null>(null);
  isLoading = signal<boolean>(true);
  confirmedPartIds = signal<Set<string>>(new Set());
  mechanicNotes = signal<string>('');
  isConfirming = signal<boolean>(false);

  constructor(
    accidentMechanicService?: AccidentMechanicService,
    route?: ActivatedRoute,
    router?: Router
  ) {
    try {
      this.accidentMechanicService = accidentMechanicService ?? inject(AccidentMechanicService);
    } catch {
      this.accidentMechanicService = accidentMechanicService as AccidentMechanicService;
    }
    try {
      this.route = route ?? inject(ActivatedRoute, { optional: true });
    } catch {
      this.route = route ?? null;
    }
    try {
      this.router = router ?? inject(Router, { optional: true });
    } catch {
      this.router = router ?? null;
    }
  }

  ngOnInit(): void {
    const id = this.route?.snapshot.paramMap.get('id');
    if (!id) {
      this.router?.navigate(['/mechanic/queue']);
      return;
    }

    this.accidentMechanicService.getAccidentDetails(id).subscribe({
      next: (data) => {
        this.details.set(data);
        // By default, mark all detected parts as confirmed
        const initialConfirmed = new Set<string>();
        data.items.forEach((item) => {
          const key = item.catalogPartId || item.partName;
          initialConfirmed.add(key);
        });
        this.confirmedPartIds.set(initialConfirmed);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      },
    });
  }

  jumpToSecond(seconds: number): void {
    if (this.videoPlayer?.nativeElement) {
      this.videoPlayer.nativeElement.currentTime = seconds;
      this.videoPlayer.nativeElement.play().catch(() => {});
    }
  }

  formatTime(seconds?: number): string {
    if (seconds === undefined || seconds === null) return '00:00';
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
  }

  isPartConfirmed(partKey: string): boolean {
    return this.confirmedPartIds().has(partKey);
  }

  togglePartConfirmation(partKey: string): void {
    const updated = new Set(this.confirmedPartIds());
    if (updated.has(partKey)) {
      updated.delete(partKey);
    } else {
      updated.add(partKey);
    }
    this.confirmedPartIds.set(updated);
  }

  calculateConfirmedSubtotal(): number {
    const det = this.details();
    if (!det) return 0;
    return det.items
      .filter((item) => this.isPartConfirmed(item.catalogPartId || item.partName))
      .reduce((sum, item) => sum + item.unitPriceBrl * item.quantity, 0);
  }

  calculateConfirmedLabor(): number {
    const det = this.details();
    if (!det) return 0;
    const count = det.items.filter((item) =>
      this.isPartConfirmed(item.catalogPartId || item.partName)
    ).length;
    if (count === 0) return 0;
    if (count <= 2) return 35.0;
    return 90.0;
  }

  calculateConfirmedTotal(): number {
    const det = this.details();
    if (!det) return 0;
    const subtotal = this.calculateConfirmedSubtotal();
    const labor = this.calculateConfirmedLabor();
    const towing = det.towingFeeBrl || 0;
    return subtotal + labor + towing;
  }

  onMechanicNotesInput(event: Event): void {
    const val = (event.target as HTMLTextAreaElement).value;
    this.mechanicNotes.set(val);
  }

  onConfirmAccident(): void {
    const det = this.details();
    if (!det || this.isConfirming()) return;

    this.isConfirming.set(true);

    const confirmedList = Array.from(this.confirmedPartIds());

    this.accidentMechanicService
      .confirmAccident(det.id, {
        confirmedPartIds: confirmedList,
        mechanicNotes: this.mechanicNotes(),
      })
      .subscribe({
        next: (resp) => {
          this.isConfirming.set(false);
          this.details.update((prev) => (prev ? { ...prev, status: resp.status } : null));
        },
        error: () => {
          this.isConfirming.set(false);
        },
      });
  }

  onBackToQueue(): void {
    this.router?.navigate(['/mechanic/queue']);
  }
}
