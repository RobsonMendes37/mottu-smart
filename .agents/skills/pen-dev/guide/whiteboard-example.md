# Worked example: choose an issue tracker

A team of 30 wants GitHub integration, prefers SSO, and has a budget of €400 per month. Compare Linear, Jira, and Shortcut. Use the same customer task for all three, such as linking a pull request to an issue. Use pricing pages for costs and documentation for the workflow.

## Place the overview and sources

Start with an editable overview at the top left. Each tracker gets a column below it with room for its browser and finding. A fourth tracker widens the board. A fifth starts a second row.

```js
const sites = [["Linear", "https://linear.app/pricing"], ["Jira", "https://www.atlassian.com/software/jira/pricing"], ["Shortcut", "https://www.shortcut.com/pricing"]]
const columns = Math.min(sites.length, 4)
const rows = Math.ceil(sites.length / columns)
const size = {width: 48 + columns * 1440, height: 360 + rows * 1448}
const pos = FindEmptySpace({...size, padding: 120})
boardId = Insert(document, {type: "frame", name: "Issue tracker comparison", ...size, x: pos.x, y: pos.y, layout: "none", fill: "#FAFAF7", placeholder: true})
overviewId = Insert(boardId, {type: "frame", name: "Decision", x: 48, y: 48, width: 960, height: "fit_content", layout: "vertical", gap: 12})
questionId = Insert(overviewId, {type: "text", name: "Question", content: "Which tracker fits a team of 30 for €400/month?", fontSize: 28, fontWeight: "bold", fill: "#111111"})
answerId = Insert(overviewId, {type: "text", name: "Answer so far", content: "We need GitHub integration and would prefer SSO.", width: 900, textGrowth: "fixed-width", fontSize: 18, fill: "#333333"})
browserIds = []
for (const [i, [name, url]] of sites.entries()) {
  const x = 48 + (i % columns) * 1440
  const y = 360 + Math.floor(i / columns) * 1448
  browserIds.push(Insert(boardId, {type: "browser", name, url, x, y, width: 1000, height: 1400}))
}
```

## Inspect one plan at a time

Use `browser` to inspect a plan card and identify its selector. In a `cdp` call with `Runtime.evaluate` and `returnByValue: true`, adapt this expression to the actual page. Read the card's wording and any footnotes before interpreting entitlement.

```js
[...document.querySelectorAll("SELECTOR_FOR_PLAN_CARD")].map(card => {
  const lines = card.innerText.split("\n").map(line => line.trim()).filter(Boolean)
  return {
    plan: card.querySelector("h2, h3")?.innerText,
    priceEvidence: lines.filter(line => /[€$£]|contact sales/i.test(line)),
    ssoEvidence: lines.filter(line => /\b(?:SSO|SAML)\b|single[ -]sign[ -]on/i.test(line)),
  }
})
```

If the SSO evidence is empty, write **Unknown from this page** and check the plan comparison or docs if it matters to the decision. Record “excluded” only for an explicit exclusion. “SSO add-on” needs an add-on price or a cost caveat.

Place each verified finding 24 px to the right of its browser, aligned with its top, and wrap the text at 360 px. Add a source line with the matching browser name, the URL you actually inspected as visible text, and the observation date. Use the same browser name in the overview. Below the question, compare the cost for 30 seats on the same billing basis and currency. Update `answerId` with the supported finding, its trade-off, and the uncertainty that could reverse it. Check the overview and clear the board placeholder.

If the team grows to 45, inspect the same nodes. Recheck seat tiers and minimums, recalculate the affected totals, and update the comparison and `answerId` in place. Keep the workflow evidence and the team's annotations.
