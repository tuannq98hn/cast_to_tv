---
name: writing-plans
description: Turn a goal into a short execution plan and small, ordered tasks with files, acceptance criteria, and checks.
---
# Writing plans

1. Copy `docs/exec-plans/active/execution-plan-template.md` → `docs/exec-plans/active/YYYY-MM-DD-<feature>.md`.
2. Fill: problem, scope, **non-goals**, approach, files per layer (`ui/`, `domain/`, `data/` — see
   `ARCHITECTURE.md`), risks (data loss, migrations, privacy), validation.
3. Split into tasks, each ≈ one reviewable change (≤ ~5 files / ~300 lines), in dependency order. Typical:
   domain model + repository interface + tests → data implementation (Room) → ViewModel + tests → screen.
4. Each task: title starting with a verb, acceptance criteria that can be checked, the test to add,
   dependencies.
5. Create them: `node harness/cli.mjs task add "..." --accept "..."` (see `node harness/cli.mjs help`).

New dependency or architecture change → note it in the plan and add/update an ADR task first.
