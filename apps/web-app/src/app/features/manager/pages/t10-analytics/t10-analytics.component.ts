import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { RouterLink } from '@angular/router';
import { ManagerService } from '../../services/manager.service';
import { AnalyticsSummaryResponseDto } from '../../models/manager.models';

@Component({
  selector: 'app-t10-analytics',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './t10-analytics.component.html',
})
export class T10AnalyticsComponent implements OnInit {
  private managerService: ManagerService;
  private sanitizer: DomSanitizer;

  summary = signal<AnalyticsSummaryResponseDto | null>(null);
  safeEmbedUrl = signal<SafeResourceUrl | string | null>(null);
  isOfflineFallback = signal<boolean>(false);
  isLoading = signal<boolean>(false);
  errorMessage = signal<string | null>(null);

  constructor(managerService?: ManagerService, sanitizer?: DomSanitizer) {
    try {
      this.managerService = managerService ?? inject(ManagerService);
    } catch {
      this.managerService = managerService as ManagerService;
    }
    try {
      this.sanitizer = sanitizer ?? inject(DomSanitizer);
    } catch {
      this.sanitizer = sanitizer as DomSanitizer;
    }
  }

  ngOnInit(): void {
    this.loadSummary();
  }

  loadSummary(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.managerService.getAnalyticsSummary().subscribe({
      next: (res) => {
        this.summary.set(res);
        this.isLoading.set(false);

        const url = res.lookerStudioEmbedUrl?.trim();
        if (url && url.length > 0) {
          this.safeEmbedUrl.set(this.sanitizer.bypassSecurityTrustResourceUrl(url));
          this.isOfflineFallback.set(false);
        } else {
          this.safeEmbedUrl.set(null);
          this.isOfflineFallback.set(true);
        }
      },
      error: () => {
        this.errorMessage.set('Erro ao carregar indicadores executivos de inteligência operacional.');
        this.isLoading.set(false);
        // Display default fallback state so UI remains functional
        this.isOfflineFallback.set(true);
      },
    });
  }

  refresh(): void {
    this.loadSummary();
  }
}
