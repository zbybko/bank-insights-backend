# Publish two clear, linked repositories

## Initial local state (recorded 2026-10-16)

- `backend/` and `mobile/` each have a Git repository and existing commits.
- The parent directory is not a Git repository. Keep it that way for this setup.
- Neither repository currently has a Git remote.
- Mobile has an uncommitted `App.tsx`; preserve that work.
- No tracked paths under `data/`, `.env` files, or common private-key extensions
  were found in either current tracked-file list. This is a path check, not a
  complete historical secret/content audit.
- No remote repositories, commits, pushes, issues or PRs were created by the
  assistant while preparing these documents.

## Publication checklist

- [x] Repository names: `zbybko/bank-insights-backend` and
  `zbybko/bank-insights-mobile`. Publication authorized by the owner on 2026-10-20.
- [ ] Finish B-01: ship a synthetic statement so a reviewer can run the app.
- [ ] Add CI in both repositories and verify the jobs before adding badges.
- [x] Review tracked files and Git history for private exports, credentials,
  personal-data screenshots and logs. Ignoring a file does not remove it from
  history. The publication scan found no private-data paths or known secret
  patterns in candidate files or historical blobs; this is not a guarantee that
  every possible secret format is detected.
- [x] Commit documentation and application changes as separate logical commits;
  use a branch for new work. Review `git diff` and staged paths first.
- [x] Create two empty GitHub repositories without generated README/license/
  gitignore so the existing histories can be pushed directly.
- [x] Add the correct remote to each local repository; inspect before pushing.
- [x] Replace both READMEs' local companion links with actual GitHub URLs. Search
  for `../mobile` and `../backend` in documentation and resolve all cross-repo
  references before publication. Keep within-repository links relative.
- [x] Push each repository separately. Do not run `git init` in the parent and
  do not delete either nested `.git` directory.
- [ ] Set descriptions/topics, protect main where supported by your GitHub plan,
  and create issues from the task checklists. Link corresponding cross-repo work.
- [ ] Add two screenshots using a synthetic statement: mobile transaction feed
  and, when implemented, dashboard. Never use the real statement for screenshots.
- [ ] Pin the primary repository in your GitHub profile and link the companion.
- [ ] Choose a license deliberately; inspect the mobile template's existing
  license before publishing. Do not invent a license or copyright owner.

## Suggested presentation

Backend description: "Java/Spring Boot personal-finance API with exact decimal
money and Revolut CSV ingestion. Open Banking sync and evaluated AI planned."

Mobile description: "React Native/Expo iPhone client for Bank Insights: a
transaction feed today, spending dashboards and bank connection flows next."

Backend topics: `java`, `spring-boot`, `personal-finance`, `csv`, `rest-api`.
Mobile topics: `react-native`, `expo`, `typescript`, `ios`, `personal-finance`.
Add `postgresql`, `open-banking` or AI-related topics when the corresponding
implementation exists; roadmap labels are not evidence of delivered features.

The README should answer: what works, how to run it, how money and identity are
handled, how it is tested, what is deliberately limited, and what is next.
Readable code, focused PRs and reproducible results matter more than badges.

## Publication record — 2026-10-20

Published as two public repositories with the original local histories preserved:

- [Backend](https://github.com/zbybko/bank-insights-backend)
- [Mobile](https://github.com/zbybko/bank-insights-mobile)

Both READMEs link to the companion repository and the original
[legacy Python implementation](https://github.com/zbybko/bank-insights).
The legacy repository's About description is marked LEGACY and its website
points to the new backend. Its code, branches, issues and open PR are preserved;
it was not deleted or archived.

Before publication, the Spring context smoke test and mobile TypeScript check
passed. A pattern/path scan of candidate files and historical blobs found no
real-export paths or recognized secret patterns. Real CSV files remained ignored.
CI and the synthetic demo remain outstanding tasks, not publication claims.
