# Code website

Read this when the user wants a website design on the canvas built as code and refined from the chat and the canvas. Translate nodes to components with [code.md](./guide/code.md), and [tailwind.md](./guide/tailwind.md) when the project uses Tailwind.

If there is no design yet, design it first and get the user's go-ahead. Browser nodes exist only in the desktop app; elsewhere, check the page with your own browser tooling.

## Setup

1. Read the selected design or the top-level page frames, `GetVariables()`, and the components they use.
2. Use the project's framework, styling, icons, and existing components. In an empty folder default to Vite, React, TypeScript, and Tailwind CSS v4, and say so.
3. Start the dev server in the background, or reuse the one already running.
4. Place desktop and phone `browser` nodes at the document root, top-aligned to the right of the page frame. Match the desktop preview to the frame's resolved width and height, up to 2000 px tall. Match the phone preview to its mobile design, up to 1600 px tall. With only a desktop design, start the phone at 390 × 1600 and shorten it if the mobile page ends sooner. Let longer pages scroll inside each preview. Resize existing previews in place when reusing them.

```js
const [page] = Get(pageId, (node, ctx) => ctx.bounds, {depth: 0})
const desktopHeight = Math.min(page.height, 2000)
const phoneWidth = 390, phoneHeight = 1600, gap = 80
const pos = FindEmptySpace({
  nodeId: pageId, direction: "right", padding: gap,
  width: page.width + gap + phoneWidth, height: Math.max(desktopHeight, phoneHeight)
})
const url = "http://localhost:5173/"
siteId = Insert(document, {type: "browser", name: "Home · Desktop", url,
  x: pos.x, y: pos.y, width: page.width, height: desktopHeight})
phoneId = Insert(document, {type: "browser", name: "Home · Phone", url, deviceId: "phone",
  x: pos.x + page.width + gap, y: pos.y, width: phoneWidth, height: phoneHeight})
Print("Desktop:", siteId, "Phone:", phoneId)
```

Drive each canvas preview through the `browser` tool. Pass its actual ID as `nodeId` on **every call**, including CDP input, screenshots, DOM reads and reloads. Omitting `nodeId` opens the sidebar. When resuming a chat or reusing previews, get their current IDs from `get_app_state` or `Get`. If an ID is stale, look it up again.

## Building

Go section by section, top to bottom, and check each one in the browser node before the next.

- Variables become theme tokens with the same names (`$text-primary` → `--text-primary`); design themes become light and dark modes.
- Design components become code components with the same names.
- Each top-level section becomes a semantic element with an `id` from its frame name (`Pricing` → `#pricing`), so you can query the same section on both sides.
- `Export` images and artwork into the project instead of redrawing them. Use the icon package of the node's `library`.

## Code quality

- The site works at every width: flexbox and grid, not absolute positions or fixed pixel widths from the canvas.
- Tokens, not hardcoded colors and sizes.
- Semantic, accessible HTML: ordered headings, links vs buttons, labels, `alt` text, focus states.
- Same copy, icons, radii, and spacing as the design. Don't invent content.
- Never ship the `html-tailwind` export as the site.
- Typecheck, lint, and build pass before you call a page done.

## Verifying

- Compare `return-screenshot` on the desktop node (`target: "query"`, `querySelector: "#pricing"`) with `TakeScreenshot` of the design frame. Dev servers live-reload. Use `load-page` on that node only if its change doesn't show.
- On a blank page or error overlay, read the dev server output.
- For exact values, compare `ctx.bounds` from `Get` with `getBoundingClientRect()` and `getComputedStyle()` via `cdp` `Runtime.evaluate`.
- Check narrow widths and exercise the mobile menu in the phone node. Replace the placeholder with the printed phone ID below. If the viewport reads zero while the preview loads, recheck that node once before sending input. Send the follow-up screenshot to the same ID.

```json
{"description":"Check phone viewport","action":"cdp","nodeId":"<phone node ID>","method":"Runtime.evaluate","params":{"expression":"({width: innerWidth, height: innerHeight})","returnByValue":true}}
```

## Iterating

Keep design and code in sync.

- Design changed on the canvas: re-read the frame and port only the difference to code.
- Change asked in chat: change the code, verify it, and mirror visual changes in the design.
- Element picked in the site: `return-element` with `target: "selection"`, then find it in code. Node selected in the design: find the code component with the same name.
- Options requested: design variants next to the section on the canvas and let the user pick before coding.
- `import-to-canvas` only when asked; imported layers aren't linked to the design's components or variables.

Don't put secrets in client code, submit real forms, or send Escape through `cdp`. Finish with what's done, what you decided that the design didn't specify, and a link to the dev server URL.
