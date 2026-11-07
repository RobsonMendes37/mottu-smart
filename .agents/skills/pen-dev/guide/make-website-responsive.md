---
name: make-website-responsive
description: Reflow a fixed-width website or HTML export for desktop, tablet, and mobile while preserving its design.
---

# Make Website Responsive

Open the page at its designed desktop width. Capture a baseline and identify the relationships worth keeping, such as a headline aligned with an image edge or a caption that belongs to a particular photo.

## Find the first broken layout

Resize continuously until something clips, overlaps, or becomes hard to use. Check the viewport meta tag on mobile. Compare `document.documentElement.scrollWidth` with `clientWidth`, then inspect the offending element and its containing block. A page can have equal widths and still lose text to a fixed-height section. Inspect the winning styles at the failure width. Runtime Tailwind utilities or inline export styles can override a matching media query. Fix the winning constraint before introducing another breakpoint.

## Reflow the affected section

- Keep the wide composition at its intended maximum width. Use the available space to decide when a grid loses a column or a navigation bar needs a compact menu. Use `minmax(0, 1fr)` for a flexible grid track or `min-width: 0` for a shrinking flex item. Let long labels wrap.
- For an absolute-positioned export, move meaningful siblings into grid or flex flow. Reset their positioning, insets, and fixed dimensions together. Let section height follow its content. Keep absolute positioning and clipping local to intentional overlays. Scaling the whole artboard or hiding root overflow can conceal the original failure.
- Keep headings, media, captions, and actions together in reading and keyboard order. Preserve image proportions with intrinsic dimensions and fluid width. Give empty background-image boxes an explicit aspect ratio and check the crop at each layout.
- Retain type hierarchy and readable gutters. Make hover-only actions reachable by touch and keyboard. If a card tilts on pointer movement, allow vertical scrolling over it on touch.

## Verify in the browser

Compare the result with the desktop baseline. Check both sides of each breakpoint, a tablet width, and a narrow phone (320 CSS pixels). At increased text size, check the longest heading and navigate to the primary action. Look for clipped text, unintended sideways scrolling, misplaced captions, and unreachable controls. Report the widths and interactions you actually checked.
