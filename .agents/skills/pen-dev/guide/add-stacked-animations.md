---
name: add-stacked-animations
description: Add staggered entrances and scroll reveals to a website while preserving its layout, interactions, and reduced-motion behavior.
---

# Add Stacked Animations

Choose a group the reader experiences together, such as a heading, its supporting text, and its action. Reveal them in reading order. Keep the hero and primary navigation available on arrival. Reuse the site's motion tokens and existing reveal mechanism where possible.

## Set the rhythm

Start with opacity and a short vertical translation. For text, try 12–24px over 450–650ms with 50–90ms between siblings. Larger images can settle more slowly. Preview one group before applying its timing elsewhere. Reset the stagger index for each group and cap the delay on long lists so later items appear promptly. Animate an outer wrapper when the card already uses an inner transform for hover, tilt, or dragging.

## Trigger when the content can be seen

- Keep the base markup visible. For a short stack, observe its container and reveal the children together with individual delays. If a grid becomes a long column on mobile, observe its rows or items so later cards still enter as the reader reaches them.
- Use a shared `IntersectionObserver` for targets with the same timing. Only enable the pending state after observation is available. Leave initially visible content readable, including after a deep link or restored scroll position. For tall media, observe its leading edge or use `threshold: 0` with `entry.isIntersecting`. A tall target may never reach a high intersection ratio.
- Reveal once and unobserve the target. Disconnect on unmount. Put any hidden state and transitions under `prefers-reduced-motion: no-preference` so reduced motion shows the final state immediately. A focused control and its enclosing item must become visible immediately, including during a stagger delay.

## Verify in the browser

Reload at the top and midway down the page. Scroll slowly, then quickly, through a complete group on desktop and a short phone viewport. Tab into a pending group and activate a control. Check that hover transforms still work and touch scrolling over cards stays smooth. Repeat with reduced motion and JavaScript disabled. Report which conditions you exercised and whether every item was readable.
