# Development and learning workflow

This is an actively developed learning project. The owner writes the application
code. Assistant support is for explanations, bounded scaffolds, reviews and
verification; it must not silently complete the learning tasks.

1. Pick one GitHub issue and agree on the acceptance criteria.
2. Discuss material alternatives before implementation. Record accepted
   architecture choices in a short ADR under `docs/adr/`.
3. Use a small feature branch, for example `feat/csv-parser-tests`.
4. Implement the behavior, then verify it with focused tests using synthetic data.
5. Review the diff for correctness and private data. Do not stage all files blindly.
6. Open a PR with the problem, behavior, validation and known limitations.
7. Before merging, explain the important decisions and error cases yourself.

Use descriptive commits, for example `test: cover pending Revolut transactions`.
Prefer a few meaningful tests to superficial coverage or tests that reproduce
the implementation. No coverage target, green CI badge or benchmark result may
be claimed before it is configured and measured.

Keep screenshots, fixtures and logs synthetic. Ignore local configuration and
bank exports. Never put bank/provider/model credentials in mobile, commits or
chat messages. Real-data AI processing is an explicit opt-in decision.

The existing backend and mobile histories are independent. Changes spanning
both repositories should link their PRs and document the API compatibility
requirements. Do not merge or rewrite either history as part of documentation.

## Backend verification

Run `./mvnw test` for unit and standalone HTTP tests. Run
`./mvnw clean verify -Pintegration-tests` with Docker running to include a
throwaway PostgreSQL database, migrations, persistence and import rollback.
No `.env` or external AI key is needed for automated tests. Use synthetic fixtures
under `src/test/resources/fixtures/`; never copy personal statements there.

Keep feature code in `transaction`, `categorization`, `csv`, or the relevant
`integration` package. Manual Spring check runners belong in `dev` and only run
under explicitly selected profiles. Add new migration versions rather than
editing SQL already applied to a database.

## Branches, pull requests and issue completion

`main` contains integrated work. Start each bounded task from an up-to-date `main`
in a short-lived branch: `feat/<issue>-<topic>`, `fix/<issue>-<topic>` or
`docs/<topic>`. An epic is a tracker, not a shared long-lived implementation branch.
Contributors work in separate branches; use Git worktrees for parallel local tasks.

Open a draft PR early and link its issue. Use `Refs #N` for partial work and
`Closes #N` only when the PR meets that issue's entire acceptance criteria. An
individual PR must not auto-close a multi-task epic. An issue stays open while its
code exists only in a feature branch; close it after merge and acceptance checks.
A green build alone is not proof that the product requirements are complete.

If an issue mixes completed and future work, record an explicit scope decision and
link the remaining tasks before changing or closing it. Do not mark unimplemented
requirements complete. Keep architecture-only PRs independent of feature branches
where possible; if one PR genuinely depends on another, name the base/dependency.

Before merging: review scope and diff, run relevant checks, address review and
verify compatibility with other repositories. Merge through GitHub, then update
local `main` and start the next task from it. Do not push feature work directly to
`main`. Server-side branch protection/rulesets are separate repository settings;
this document and the PR template do not enforce them automatically.
