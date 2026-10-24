# ADR-0002: Figma "Dark Cinema" replaces APK 14.7.4 as the design source

## Status

Accepted (2026-10-23, user decision; implemented by plan `docs/exec-plans/completed/2026-10-23-dark-cinema-ui.md`)

## Context

ADR-0001 carried the shipped APK's layouts and drawables over unchanged, so the APK was the visual reference.
The user commissioned a new design, Figma `CastToTV` / `Page 1` ("Dark Cinema": dark glass cards, cyan + violet
accents, 18 frames). It covers about half of the app's 39 screens and shows features the app does not have.

## Decision

- The Figma file is the design source for every screen. The APK stays the source for **behaviour** only.
- Figma data is exported into the repo (`docs/references/figma-dark-cinema/`: PNG + node JSON per frame,
  `tokens.md`), so agents need no Figma/MCP access.
- Tokens live in code as `@color/dc_*`, `DC.Text.*` / `DC.*` styles and `dc_bg_*` drawables (summary:
  `docs/references/design-system-reference-llms.txt`). `Theme.CastTV` is dark.
- Screens without a frame are "self-styled" from those shared tokens/components.
- UI only: design elements that need behaviour the app lacks are left out, not built; view IDs are kept so
  click handlers and logic stay untouched.
- Placeholder copy in the frames is never shipped; existing strings, fonts (`pro_display_*`) and data are used.

## Alternatives Considered

1. Keep the APK look — rejected by the user.
2. Implement the Figma frames fully, including new features — out of scope; changes behaviour.
3. Restyle only screens with a frame — leaves a mixed light/dark app.

## Consequences

### Positive

- One consistent dark look; new screens only need the shared `dc_*` components.

### Negative

- Screens visibly differ from the Figma frames where features were left out (listed per task in the plan).
- Many legacy drawables/raws may now be unused (TD-011).
- Promo video/music removed from Purchase (possible conversion impact, product decision).
- New strings added English only (`restore_purchase`, `remote_tab_*`, `touchpad_hint_*`).

### Follow-up

- TD-011 unused legacy resources; I-022 splash Lottie not rendering.
- If designers send frames for self-styled screens, update `docs/references/figma-dark-cinema/` and restyle.
