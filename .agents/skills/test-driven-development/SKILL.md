---
name: test-driven-development
description: Write a failing test first, then the minimum code to pass, then refactor. Use for new logic and bug fixes.
---
# Test-driven development

1. **Red.** Write one test for the next behavior. Run it and see it fail for the right reason:
   `bash scripts/test/run_unit_tests.sh --tests 'com.apps.dearme.journal.notes.<pkg>.<Class>Test'`
2. **Green.** Write the least code that passes. No extra features.
3. **Refactor.** Clean names/duplication with tests green. Re-run.
4. Repeat per behavior: success, empty, error, edge.

Where:
- Domain logic, ViewModels, mappers → `app/src/test/java/...` (JUnit4, runs in the gate). Prefer this.
- Room DAOs/migrations, Espresso → `app/src/androidTest/java/...` (`bash scripts/test/run_ui_tests.sh`, needs a device).

Tips: fake repositories (in-memory class implementing the domain interface) over mocks;
`kotlinx-coroutines-test` `runTest` for suspend/Flow code (add to the version catalog when first needed);
name tests by behavior: `` fun `empty entry is not saved`() ``.
Skip TDD only for pure layout/resource changes — then use `ui-evidence`.
