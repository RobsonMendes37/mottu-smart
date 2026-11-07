# Research and planning on the canvas

Use a whiteboard when the user wants information they can explore. Inspect the selection and existing board with `get_app_state` and `Get`. If the user selects five articles and asks where they agree, start with those five. Open another source only for a gap that matters to the question and falls within the requested scope.

## Start with the question

On a new board, use `FindEmptySpace` at the document root and create a frame with `placeholder: true`. Put the user's question and constraints at the top left. As soon as one source gives a useful finding, write it beside that source. When it changes the answer, update the overview with the finding, the trade-off, and any question that could change the decision. Name the decisive source there.

Choose the layout by what the reader needs to see. Use table rows for options with common criteria, a tree for parent–child relationships, and a sequence for steps or dates. For a brief, lead with the two or three findings that could change what the reader does. Use each finding as its heading, such as “The last train leaves ten minutes before you can reach it.” Put the supporting evidence beside it.

## Put findings beside their sources

- Reuse relevant open pages. When more browsing is needed, work in batches of two or three sources, each in a named `browser` node directly under the board.
- For competitive research, recruiting, sitemaps, and travel, default to a landscape results board with portrait browsers. Size each browser at 1000 × 1400 canvas pixels by default, with a minimum of 1000 px on each axis, including reused nodes.
- Arrange related sections left to right. Beneath the overview, give each source a column with its finding beside the browser. Widen the board as sources arrive. Seven sources, for example, fit in two rows of four and three. If more rows would turn the board into a tall stack, start a labeled top-level frame to the right for the next group.
- Keep each browser and its note directly under the same board frame. Use `layout: "none"` for placement and `textGrowth: "fixed-width"` for readable notes.
- Pass the correct `nodeId` to `browser`. Orient with `return-screenshot`, then inspect the relevant element or extract a few fields with `cdp`. Use `screenshot-to-canvas` for a visual detail the user needs to compare. Keep useful pages open at the evidence.
- Label findings with the source name. For web pages, show the exact URL as visible text beside the findings it supports. Date changing facts such as prices or availability. Distinguish observations, estimates, and interpretations. If a source is silent or blocked, record what is still unknown.

## Revise the existing board

For a follow-up, inspect the current board and identify which assumption changed. Reuse its nodes and sources, keeping the user's notes and edits. Add or update just the affected evidence, carry the change through dependent rows, totals, or plans, then revise the answer at the top. If the conclusion holds, say why. Briefly tell the user what changed.

Check the board at two scales with `TakeScreenshot`. In the overview, can you find the answer at the top left and follow the sections across the canvas? At reading size, can you see a finding and its evidence together? Check final browser dimensions and adjust spacing or placement where needed. Inspect `ctx.problems` for clipping or overlaps and clear any placeholders you set.

Browser nodes share a session. Recheck the page before interacting and tell the user before navigating away from a page they're using. Leave sign-in, personal details, form submissions, purchases, and bookings to the user. Avoid sending Escape through CDP because the editor intercepts it.

For sample canvas calls and plan-level evidence extraction, see the [optional issue tracker comparison](./guide/whiteboard-example.md).
