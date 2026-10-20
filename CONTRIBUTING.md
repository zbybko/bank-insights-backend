# Development and learning workflow

This is an actively developed learning project. The owner writes the application
code. Assistant support is for explanations, bounded scaffolds, reviews and
verification; it must not silently complete the learning tasks.

1. Pick one task ID from `docs/ROADMAP.md` and agree on the acceptance criteria.
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
