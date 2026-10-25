# Income meaning and confirmed history

Income meaning is separate from spending categories. Available types are SALARY,
SALARY_ADVANCE, BONUS, INTERNAL_TRANSFER, REFUND, DEPOSIT_RETURN (returned security
deposit), INTEREST, SALES, PERSONAL_TRANSFER, OTHER and UNKNOWN.

## API

- `POST /api/transactions/{id}/income/enrichment`: explicit AI request; returns an
  assessment and caches it. Repeating it returns the stored result without AI.
- `PATCH /api/transactions/{id}/income` with `{"type":"SALARY"}`: confirms or
  corrects an assessment without AI; a prior AI request is not required.
- `GET /api/transactions/{id}/income`: returns the stored assessment, or 404 if
  the transaction or assessment is missing.

All three accept completed positive transactions only (otherwise 400). Missing
transactions return 404; invalid or missing PATCH type returns 400. Responses
contain transactionId, type, suggestedType, source (AI/USER), explanation,
reviewRequired, question and updatedAt. A pending suggestion keeps type UNKNOWN.
User confirmation keeps the prior suggestedType for comparison. Existing
monthly cash-flow calculations are unchanged. Spending enrichment and non-UNCATEGORIZED
spending category confirmation require a negative transaction amount; positive
and zero amounts return 400 before any AI call or save. A legacy incorrect
spending label on an incoming transaction can be cleared using the category
endpoint with `{"category":"UNCATEGORIZED"}`. That does not classify its income
meaning; use the income endpoint separately.

## Retrieval

For each explicit AI request, PostgreSQL selects at most ten earlier completed
positive transactions of the same currency with a case-insensitive, trimmed exact
description match and a USER-confirmed non-UNKNOWN income type. AI-only guesses,
later payments, other descriptions and currencies are excluded. Correcting a
confirmation changes the context used for subsequent new classifications; cached
assessments are not automatically reclassified. There is no vector store.

Description matching is evidence, not sender identity. The current single-user
prototype has no account/owner boundary. Before adding authentication or multiple
users, scope both transactions and memory queries to the authenticated owner.
The model receives only the selected transaction's description, exact amount,
currency, booking date, source type/product and matching history with confirmed
types. This sends personal financial data to the configured AI gateway only when
the explicit enrichment endpoint is called. Automated tests use synthetic data.

Payroll types always require review, even if the model requests automatic
acceptance. No salary-period boundary is created yet. A familiar employer may
also send a bonus or advance. No motives for transfers are inferred as facts.
Optimistic versioning protects edits to existing assessments. Concurrent first
requests may conflict on the transaction primary key; they do not silently
overwrite a confirmation. A confirmation audit log is not implemented yet.

## Source import

Migration V4 adds optional source_type/source_product fields to transactions and
the income_assessments table. New CSV imports retain Type and Product verbatim.
Existing imported rows remain null; the deduplicating import does not backfill
them. Do not delete data or replay an export to backfill these fields. Internal
transfer pairing and removal from monthly totals are separate follow-up work.

## Verification

`./mvnw clean verify -Pintegration-tests` tests persistence, retrieval exclusions,
manual correction, cached results, payroll review and invalid model output.

For a real HTTP client check without provider access:

```sh
postman simulation run postman/simulations/income.sim.yaml --port 4907 --no-history --output ndjson
# In a separate terminal:
./mvnw test -Dtest=IncomeGatewaySimulationTest -Dincome.simulation.url=http://127.0.0.1:4907/astropods
```

The local mock follows the existing OpenAI-compatible AstropodsClient envelope;
an Astropods workspace mock was not found by the Postman workspace search. This
proves request/envelope handling, not model quality or live provider compatibility.
