import {
  Component,
  ElementRef,
  Input,
  ViewChild,
  computed,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { DamageBoundingBox } from '../../models/mechanic.models';

@Component({
  selector: 'app-photo-zoom-lens',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div
      #container
      class="relative w-full h-[460px] bg-[#1C1F1E] rounded-lg overflow-hidden select-none cursor-crosshair border border-[#E2E2E2] flex items-center justify-center"
      (mousemove)="onMouseMove($event)"
      (mouseenter)="isHovering.set(true)"
      (mouseleave)="isHovering.set(false)"
      (touchstart)="onTouchMove($event)"
      (touchmove)="onTouchMove($event)"
      (touchend)="isHovering.set(false)"
    >
      <!-- Base High-Res Photo -->
      @if (activePhotoUrl()) {
        <img
          #baseImage
          [src]="activePhotoUrl()"
          (error)="onPhotoError()"
          alt="Foto da avaria em alta resolução"
          class="w-full h-full object-contain pointer-events-none"
        />
      } @else {
        <div class="flex flex-col items-center justify-center text-white/60 gap-2">
          <svg class="w-10 h-10 text-white/30" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"/>
          </svg>
          <span class="text-xs font-medium">Nenhuma imagem carregada</span>
        </div>
      }

      <!-- Highlighted Part Markers -->
      @for (box of boundingBoxesSignal(); track box.label) {
        <div
          class="absolute border-2 border-dashed border-white/85 bg-white/10 pointer-events-none transition-all z-10 rounded-sm"
          [style.left.%]="box.x"
          [style.top.%]="box.y"
          [style.width.%]="box.width"
          [style.height.%]="box.height"
        >
          <span
            class="absolute -top-6 left-0 bg-white text-[#141414] text-[11px] font-semibold px-2 py-0.5 rounded shadow whitespace-nowrap"
          >
            {{ box.label }}
          </span>
        </div>
      }

      <!-- Magnifying Loupe (2x - 4x) -->
      @if (isHovering() && activePhotoUrl()) {
        <div
          class="absolute rounded-full pointer-events-none border-2 border-white shadow-2xl z-20 overflow-hidden"
          [style.width.px]="loupeSize"
          [style.height.px]="loupeSize"
          [style.left.px]="loupeLeft()"
          [style.top.px]="loupeTop()"
          [style.background-image]="'url(' + activePhotoUrl() + ')'"
          [style.background-repeat]="'no-repeat'"
          [style.background-size]="bgSize()"
          [style.background-position]="bgPosition()"
        >
          <div class="absolute inset-0 flex items-center justify-center opacity-50">
            <div class="w-full h-[1px] bg-white"></div>
            <div class="h-full w-[1px] bg-white absolute"></div>
          </div>
        </div>
      }

      <!-- Zoom Level Controls -->
      <div
        class="absolute bottom-3 right-3 flex items-center gap-1 bg-black/75 border border-white/15 rounded-md px-2 py-1 z-30"
      >
        <span class="text-[11px] font-medium text-white/75 mr-1">Lupa</span>
        @for (level of [2, 3, 4]; track level) {
          <button
            type="button"
            (click)="setZoom(level, $event)"
            [class]="
              zoomLevel() === level
                ? 'px-2 py-0.5 text-xs font-mono font-bold bg-white text-[#141414] rounded-sm'
                : 'px-2 py-0.5 text-xs font-mono text-white/80 hover:text-white rounded-sm transition-colors'
            "
          >
            {{ level }}x
          </button>
        }
      </div>
    </div>
  `,
})
export class PhotoZoomLensComponent {
  photoUrlSignal = signal<string>('');
  activePhotoUrl = signal<string>('');
  boundingBoxesSignal = signal<DamageBoundingBox[]>([]);

  @Input() set photoUrl(val: string) {
    this.photoUrlSignal.set(val || '');
    this.resolveActivePhoto(val);
  }
  get photoUrl(): string {
    return this.photoUrlSignal();
  }

  resolveActivePhoto(url: string): void {
    if (!url) {
      this.activePhotoUrl.set('/demo-photos/avaria-retrovisor.webp');
      return;
    }
    const lower = url.toLowerCase();
    if (lower.includes('manete') || lower.includes('guidao')) {
      this.activePhotoUrl.set(url.startsWith('http') ? url : '/demo-photos/avaria-manete.webp');
    } else if (lower.includes('pneu') || lower.includes('roda')) {
      this.activePhotoUrl.set(url.startsWith('http') ? url : '/demo-photos/avaria-pneu.webp');
    } else if (lower.includes('farol') || lower.includes('otica')) {
      this.activePhotoUrl.set(url.startsWith('http') ? url : '/demo-photos/avaria-farol.webp');
    } else if (lower.includes('escapamento') || lower.includes('queda')) {
      this.activePhotoUrl.set(url.startsWith('http') ? url : '/demo-photos/avaria-escapamento.webp');
    } else {
      this.activePhotoUrl.set(url);
    }
  }

  onPhotoError(): void {
    const current = this.activePhotoUrl();
    if (current && current.startsWith('/demo-photos/')) return;
    const lower = (this.photoUrlSignal() || '').toLowerCase();
    if (lower.includes('manete') || lower.includes('guidao')) {
      this.activePhotoUrl.set('/demo-photos/avaria-manete.webp');
    } else if (lower.includes('pneu') || lower.includes('roda')) {
      this.activePhotoUrl.set('/demo-photos/avaria-pneu.webp');
    } else if (lower.includes('farol') || lower.includes('otica')) {
      this.activePhotoUrl.set('/demo-photos/avaria-farol.webp');
    } else if (lower.includes('escapamento') || lower.includes('queda')) {
      this.activePhotoUrl.set('/demo-photos/avaria-escapamento.webp');
    } else {
      this.activePhotoUrl.set('/demo-photos/avaria-retrovisor.webp');
    }
  }

  @Input() set boundingBoxes(val: DamageBoundingBox[]) {
    this.boundingBoxesSignal.set(val || []);
  }
  get boundingBoxes(): DamageBoundingBox[] {
    return this.boundingBoxesSignal();
  }

  @ViewChild('container') containerRef!: ElementRef<HTMLDivElement>;

  readonly loupeSize = 160;
  isHovering = signal<boolean>(false);
  zoomLevel = signal<number>(2);
  cursorPos = signal<{ x: number; y: number }>({ x: 0, y: 0 });

  loupeLeft = computed(() => this.cursorPos().x - this.loupeSize / 2);
  loupeTop = computed(() => this.cursorPos().y - this.loupeSize / 2);

  bgSize = computed(() => {
    const el = this.containerRef?.nativeElement;
    if (!el) return 'auto';
    const rect = el.getBoundingClientRect();
    return `${rect.width * this.zoomLevel()}px ${rect.height * this.zoomLevel()}px`;
  });

  bgPosition = computed(() => {
    const el = this.containerRef?.nativeElement;
    if (!el) return '0 0';
    const x = this.cursorPos().x;
    const y = this.cursorPos().y;
    const zoom = this.zoomLevel();
    const bgX = -(x * zoom - this.loupeSize / 2);
    const bgY = -(y * zoom - this.loupeSize / 2);
    return `${bgX}px ${bgY}px`;
  });

  onMouseMove(event: MouseEvent): void {
    const el = this.containerRef?.nativeElement;
    if (!el) return;
    const rect = el.getBoundingClientRect();
    const x = Math.max(0, Math.min(event.clientX - rect.left, rect.width));
    const y = Math.max(0, Math.min(event.clientY - rect.top, rect.height));
    this.cursorPos.set({ x, y });
  }

  onTouchMove(event: TouchEvent): void {
    if (!event.touches.length) return;
    const touch = event.touches[0];
    const el = this.containerRef?.nativeElement;
    if (!el) return;
    const rect = el.getBoundingClientRect();
    const x = Math.max(0, Math.min(touch.clientX - rect.left, rect.width));
    const y = Math.max(0, Math.min(touch.clientY - rect.top, rect.height));
    this.isHovering.set(true);
    this.cursorPos.set({ x, y });
  }

  setZoom(level: number, event: Event): void {
    event.stopPropagation();
    this.zoomLevel.set(level);
  }
}
