---
name: writing-skills
description: Turn a procedure that agents keep repeating into a new skill in .agents/skills.
---
# Writing skills

Write a skill when the same multi-step procedure has been explained or rediscovered twice.

1. `mkdir .agents/skills/<kebab-name>` and create `SKILL.md`:
   ```md
   ---
   name: <kebab-name>
   description: <what it does + when to use it, one sentence — agents choose by this>
   ---
   # Title
   numbered steps with exact commands and paths for this repo
   ```
2. Keep it under ~40 lines; link docs instead of copying them.
3. Add it to the `skills:` line of the roles that need it (`agents/<role>.md`).
4. `node harness/cli.mjs skills sync` (copies to `.claude/skills/`). Never edit the `.claude/skills` copy.
