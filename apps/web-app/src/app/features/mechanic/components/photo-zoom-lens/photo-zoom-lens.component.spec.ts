import { describe, it, expect, beforeEach } from 'vitest';
import { PhotoZoomLensComponent } from './photo-zoom-lens.component';

describe('PhotoZoomLensComponent (Damage Photo Magnifier & AI Overlays)', () => {
  let comp: PhotoZoomLensComponent;

  beforeEach(() => {
    comp = new PhotoZoomLensComponent();
    // Mock containerRef
    comp.containerRef = {
      nativeElement: {
        getBoundingClientRect: () => ({
          left: 100,
          top: 50,
          width: 600,
          height: 400,
          right: 700,
          bottom: 450,
          x: 100,
          y: 50,
          toJSON: () => {},
        }),
      } as HTMLDivElement,
    };
  });

  it('TC-PZL-1: Initializes with default zoom level and inactive hover', () => {
    expect(comp.zoomLevel()).toBeGreaterThanOrEqual(2);
    expect(comp.isHovering()).toBe(false);
  });

  it('TC-PZL-2: setZoom updates zoomLevel', () => {
    const mockEvent = { stopPropagation: () => {} } as any;
    comp.setZoom(4, mockEvent);
    expect(comp.zoomLevel()).toBe(4);
  });

  it('TC-PZL-3: onMouseMove clamps coordinates within container bounds', () => {
    // Inside bounds: clientX: 250 (offset = 150), clientY: 150 (offset = 100)
    const mouseEvent = { clientX: 250, clientY: 150 } as MouseEvent;
    comp.onMouseMove(mouseEvent);

    expect(comp.cursorPos()).toEqual({ x: 150, y: 100 });

    // Below zero clamp: clientX: 50 (offset = -50 -> clamped to 0)
    const outsideEvent = { clientX: 50, clientY: 20 } as MouseEvent;
    comp.onMouseMove(outsideEvent);
    expect(comp.cursorPos()).toEqual({ x: 0, y: 0 });

    // Above max clamp: clientX: 800 (offset = 700 -> clamped to width 600)
    const overshootEvent = { clientX: 800, clientY: 550 } as MouseEvent;
    comp.onMouseMove(overshootEvent);
    expect(comp.cursorPos()).toEqual({ x: 600, y: 400 });
  });

  it('TC-PZL-4: Loupe position centers around cursor position', () => {
    comp.cursorPos.set({ x: 200, y: 200 });
    const half = comp.loupeSize / 2;
    expect(comp.loupeLeft()).toBe(200 - half);
    expect(comp.loupeTop()).toBe(200 - half);
  });

  it('TC-PZL-5: bgSize and bgPosition compute magnified background coordinates', () => {
    comp.zoomLevel.set(2);
    comp.cursorPos.set({ x: 100, y: 100 });

    // width 600 * 2 = 1200px, height 400 * 2 = 800px
    expect(comp.bgSize()).toBe('1200px 800px');

    // bgX = -(100 * 2 - 80) = -(200 - 80) = -120px
    // bgY = -(100 * 2 - 80) = -(200 - 80) = -120px
    expect(comp.bgPosition()).toBe('-120px -120px');
  });

  it('TC-PZL-6: onTouchMove activates hover and clamps touch coordinates', () => {
    const touchEvent = {
      touches: [{ clientX: 300, clientY: 200 }],
    } as any;

    comp.onTouchMove(touchEvent);
    expect(comp.isHovering()).toBe(true);
    expect(comp.cursorPos()).toEqual({ x: 200, y: 150 });
  });
});
