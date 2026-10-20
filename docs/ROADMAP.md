# Backend checklist

Checked items describe code inspected on 2026-10-16. Open items are proposals,
not completed or promised integrations. Work one small issue at a time.
The matching frontend checklist is in the companion mobile repository.

## Current prototype

- [x] Java 21 / Spring Boot / Maven wrapper project.
- [x] Completed Revolut CSV records become `Transaction` objects.
- [x] Decimal amounts and fees; money serialized as JSON strings.
- [x] Booking timestamps and newest-first ordering.
- [x] GET endpoint consumed by the iPhone app.
- [x] Private CSV directory excluded from this repository's tracked files.

## M0 — A reviewer can run and understand the project

- [ ] **B-01 · Synthetic demo and config.** Commit a fictional CSV, document its
  expected count and totals, provide example config, and run from a fresh clone
  without private exports. Remove unused generated metadata from the POM.
- [ ] **B-02 · Parser tests.** Cover quoted commas, BOM/UTF-8, pending/reverted
  records, valid/invalid dates, missing columns, empty cells, malformed records,
  and exact decimal values. Define whole-file failure behavior explicitly.
- [ ] **B-03 · Money semantics.** Verify Amount/Fee treatment, positive and negative
  amounts, refunds, own-account transfers, and zero values. No double counting
  of fees or FX legs; no summing different currencies.
- [ ] **B-04 · Error contract.** Return a consistent status, safe error code, and
  row number where applicable. Do not return bank data, filesystem paths, or
  stack traces. Test success and failure through HTTP with synthetic input.
- [ ] **B-05 · CI.** Run Maven verification on pull requests with synthetic test
  configuration. The context test must not require a real export or secret.

## M1 — Prove that automatic Revolut access is available

- [ ] **B-06 · Provider feasibility / ADR.** Compare at least two eligible routes
  using official provider information: Revolut entity/country coverage, individual
  developer access, sandbox vs live, cost, history window, refresh/re-consent
  limits, revocation, and available stable transaction IDs. Record a dated
  decision with the user. A successful sandbox alone is not proof of live access.
- [ ] **B-07 · One sandbox account read.** Build a small server-side spike with
  synthetic data. Demonstrate account and transaction retrieval; no keys or
  bank credentials in mobile or source control. If live eligibility fails,
  document the blocker and keep CSV working.

## M2 — Reliable stored transactions

- [ ] **B-08 · Service boundary and source interface.** Move orchestration out of
  the controller. CSV and future API adapters produce a common transaction
  draft. Keep separate bank-specific parsing and shared ingestion logic.
- [ ] **B-09 · Storage decision / migrations.** Choose a database with the user
  (PostgreSQL is proposed), then add versioned migrations, account identity,
  source/external ID, booking date, decimal values, and import metadata. No
  database is selected or implemented yet.
- [ ] **B-10 · Atomic, idempotent import.** Enforce a unique database identity
  scoped by account/provider. Prefer stable external IDs; document fallback CSV
  ambiguity. Test reimport, overlap, identical legitimate purchases, rollback,
  concurrent imports, and pending-to-completed changes.
- [ ] **B-11 · CSV upload.** Accept bounded multipart input, validate format and
  return inserted/duplicate counts. Keep ingestion separate from GET requests;
  GET must query stored transactions rather than parse a file each time.
- [ ] **B-12 · Transaction queries.** Stable IDs, explicit pagination/order,
  account/month/currency filters, and documented JSON schema. Test boundary
  dates and empty results. Coordinate response changes with mobile.

## M3 — Read-only synchronization

- [ ] **B-13 · Authentication and ownership.** Protect transactions and connection
  endpoints before remote access. Test account isolation. Use TLS for deployed
  access; connection callbacks validate state and follow the provider's flow.
- [ ] **B-14 · Consent and token lifecycle.** Store provider credentials only on
  the server using appropriate secret storage; support expiry, re-consent and
  disconnect/revocation. Disconnect behavior must describe retained/deleted data.
- [ ] **B-15 · Sync orchestration.** Manual sync first, then scheduled refresh
  within provider limits. Use pagination/checkpoints, bounded retries/backoff,
  timeouts and per-connection locking. Rerunning a failed job must be safe.
- [ ] **B-16 · Sync status endpoint.** Expose last successful refresh, progress,
  stale/error/consent-expired states and a safe retry action. Record operational
  counters without logging raw descriptions, credentials, or response bodies.

## M4 — Useful analytics before AI

- [ ] **B-17 · Monthly summaries.** Exact incoming/outgoing/net per currency and
  month; define fee, refund, own-transfer, timezone and zero-activity semantics.
  Distinguish cash flow from spending and balance. Tests reconcile to fixtures.
- [ ] **B-18 · Categories and edits.** Define a small taxonomy and manual category
  correction API. Preserve user corrections and provenance for later enrichment.
- [ ] **B-19 · Dashboard data.** Monthly trend, category breakdown and merchant
  totals, filtered consistently. Charts receive backend-calculated aggregates.
- [ ] **B-20 · Recurring-payment candidates.** Explainable merchant/interval/amount
  heuristics; label as candidates, support confirmation/dismissal, test false
  positives. Not a guarantee that a payment is a subscription.

## M5 — AI that can be evaluated

- [ ] **B-21 · Data policy and provider interface.** Agree what may leave the
  server, minimize fields, require explicit opt-in for real-data model calls,
  and support fake providers for tests. Never send raw CSV or bank tokens.
- [ ] **B-22 · Structured categorization.** Validate category and bounded confidence,
  record model/prompt version, bounded retry and manual-review outcome. Treat
  merchant descriptions as untrusted data, never as instructions. User edits win.
- [ ] **B-23 · Evaluation baseline.** Use a reviewed synthetic/anonymized labeled
  set and a holdout split; avoid template/merchant leakage. Compare simple rules
  with AI; publish measured accuracy, review rate, per-category failures,
  latency and cost. Do not select a model or invent results before evaluation.
- [ ] **B-24 · Batch processing and budget.** Cache by normalized input/version,
  avoid recategorizing unchanged data, track usage including retry limitations,
  and enforce run limits. Model calls never run in ordinary CI tests.
- [ ] **B-25 · Grounded insights.** Backend computes all monetary facts. The model
  explains supplied aggregates, references their time range and currency, and
  reports insufficient evidence. Evaluate unsupported-claim behavior; no model
  arithmetic or invented transactions.

## Suggested next issue

Start **B-06** as a bounded access investigation while **B-01/B-02** make the
current CSV path reproducible and testable. Write the code yourself; ask the
assistant for concepts, small scaffolds and review. Do not build a scheduler
until a usable provider route and idempotent storage are proven.

## Definition of done

- [ ] You can explain the change and its tradeoff without reading generated text.
- [ ] Relevant behavior has focused, offline tests and green configured checks.
- [ ] README/API contract updated if behavior changes.
- [ ] No secrets, real exports or personal-data screenshots in the diff.
- [ ] PR explains the problem, resulting behavior, validation, and limitations.
