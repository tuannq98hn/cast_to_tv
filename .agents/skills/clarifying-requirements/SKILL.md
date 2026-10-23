---
name: clarifying-requirements
description: Find the real goal, constraints, and acceptance criteria before planning. Use when a request is vague or could be read several ways.
---
# Clarifying requirements

1. Restate the goal in one sentence: who, what, why.
2. Check what exists: `docs/PRODUCT_SENSE.md`, `docs/product-specs/`, current code.
3. List the open questions that change the design. For a journal app, usually:
   - Where is the data stored, and is it backed up / exported / synced? (privacy, `docs/SECURITY.md`)
   - What happens to unsaved text (back press, rotation, process death)?
   - Empty, error, and long-content states? Dark mode? Font scaling?
   - Is anything deleted? Undo or confirm?
4. Answer what you can from docs/code with a sensible default and mark it **Assumption**.
5. Only questions the human must decide go to them (`node harness/cli.mjs issue add "..." --type question`).
6. Write acceptance criteria as checkable statements before any task is created.
