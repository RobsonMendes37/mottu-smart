import { Component, ElementRef, OnDestroy, OnInit, ViewChild, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { AccidentService } from '../../services/accident.service';
import { RiderAccidentReportDetails } from '../../models/accident.models';

@Component({
  selector: 'app-accident-quote',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './accident-quote.component.html',
})
export class AccidentQuoteComponent implements OnInit, OnDestroy {
  private accidentService: AccidentService;
  private route: ActivatedRoute | null;
  private router: Router | null;
  private sseSub?: Subscription;

  @ViewChild('videoPlayer') videoPlayer?: ElementRef<HTMLVideoElement>;

  reportId = signal<string | null>(null);
  details = signal<RiderAccidentReportDetails | null>(null);
  isLoading = signal<boolean>(true);
  currentStepLabel = signal<string>('Conectando ao laudo...');

  constructor(
    accidentService?: AccidentService,
    route?: ActivatedRoute,
    router?: Router
  ) {
    try {
      this.accidentService = accidentService ?? inject(AccidentService);
    } catch {
      this.accidentService = accidentService as AccidentService;
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
      this.router?.navigate(['/rider/garage']);
      return;
    }

    this.reportId.set(id);
    this.loadDetails(id);
    this.listenToSse(id);
  }

  ngOnDestroy(): void {
    this.sseSub?.unsubscribe();
  }

  private loadDetails(id: string): void {
    this.accidentService.getAccidentDetails(id).subscribe({
      next: (data) => {
        this.details.set(data);
        if (
          data.status === 'ESTIMATED_PENDING_WORKSHOP' ||
          data.status === 'WORKSHOP_CONFIRMED'
        ) {
          this.isLoading.set(false);
        }
      },
      error: () => {
        this.isLoading.set(false);
      },
    });
  }

  private listenToSse(id: string): void {
    this.sseSub = this.accidentService.subscribeAccidentLiveStatus(id).subscribe({
      next: (event) => {
        this.currentStepLabel.set(event.stepLabel || 'Processando...');
        if (
          event.status === 'ESTIMATED_PENDING_WORKSHOP' ||
          event.status === 'WORKSHOP_CONFIRMED'
        ) {
          this.loadDetails(id);
        }
      },
      error: () => {
        // SSE network issue - polling fallback if needed
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

  onBackToGarage(): void {
    this.router?.navigate(['/rider/garage']);
  }
}

