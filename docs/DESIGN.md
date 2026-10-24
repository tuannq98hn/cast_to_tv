# DESIGN.md

This document defines product and UI design rules shared across platforms.

## Design Principles

- Preserve user intent and reduce cognitive load.
- Prefer clear hierarchy over visual decoration.
- Use consistent spacing, typography, color, iconography, and states.
- Empty, loading, success, and error states must be designed intentionally.
- Do not add UI elements that are not required by product spec or design source.

## Source of Truth

The Figma file `CastToTV` / `Page 1` ("Dark Cinema") is the design source (ADR-0002). The shipped APK 14.7.4
stays the source for behaviour and copy (strings), not for looks.

- Frames: `docs/references/figma-dark-cinema/<frame>.png` + `nodes/<frame>.json`; generated `tokens.md`.
  Frame → screen map: `docs/exec-plans/completed/2026-10-23-dark-cinema-ui.md`.
- Tokens in code: `@color/dc_*`, `DC.*` styles, `dc_bg_*` drawables, dark `Theme.CastTV`.
  Summary: `docs/references/design-system-reference-llms.txt`.
- Screens without a frame are self-styled from those shared tokens/components only.
- Frame elements that need behaviour the app lacks are left out; placeholder copy is never shipped.
- No real blur below API 31: translucent gradient + 1dp stroke.

## Visual Parity Rule

When implementing from Figma, Stitch, screenshots, or design docs:

1. Match layout, spacing, typography, color, and state behavior.
2. Do not invent missing UI.
3. If implementation constraints require deviation, document it in the execution plan.
4. Add screenshots or manual verification notes when possible.

## Accessibility Baseline

- Interactive elements must have clear labels.
- Color must not be the only way to communicate status.
- Text should scale where the platform supports it.
- Tap/click targets should meet platform norms.
