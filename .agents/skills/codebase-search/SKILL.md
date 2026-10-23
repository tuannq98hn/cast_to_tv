---
name: codebase-search
description: Find code cheaply and answer with paths and line ranges. Use before reading files broadly, or ask the finder agent.
---
# Codebase search

- Many-file questions → `node harness/cli.mjs ask finder "where is …"`. One quick lookup → grep yourself.
- Code: `app/src/main/java/com/apps/dearme/journal/notes/` (`ui/`, `domain/`, `data/`);
  resources `app/src/main/res/`; tests `app/src/test`, `app/src/androidTest`;
  dependencies `gradle/libs.versions.toml`, `app/build.gradle.kts`.
- Search exact symbols first: `grep -rn "class EntryRepository" app/src`, then callers: `grep -rn "EntryRepository" app/src`.
- Resource usage: `grep -rn "@string/name\|R.string.name" app/src`.
- Skip `build/`, `app/build/`, `.gradle/`, `.idea/`, `.harness/`.
- Answer with `path:line-range — what it is`, not file dumps.
