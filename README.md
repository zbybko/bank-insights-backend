# Bank Insights · Backend

**A Java API that turns bank statements into a transaction feed for an iPhone app.**

Bank Insights is a personal-finance project built with Java 21 and Spring Boot.
The current application reads a local Revolut CSV statement, keeps completed
transactions, and returns them through a REST endpoint. Money uses `BigDecimal`
internally and decimal strings in JSON.

The direction is automatic account synchronization, useful spending dashboards,
and AI-assisted categorization with measurable quality. These are planned
capabilities, not features of the current prototype. Independent project; not
affiliated with Revolut or any other bank.

**Companion:** [React Native / Expo mobile app](https://github.com/zbybko/bank-insights-mobile).

**Legacy implementation:** [Python bank-insights](https://github.com/zbybko/bank-insights).

[Backend checklist](docs/ROADMAP.md) · [Architecture and decisions](docs/ARCHITECTURE.md)
· [Development workflow](CONTRIBUTING.md) · [GitHub setup](docs/REPOSITORY_SETUP.md)

## What works today

- Revolut CSV parsing using Apache Commons CSV, with UTF-8 input and named columns.
- `COMPLETED` records only; amount and fee remain separate exact decimal values.
- Booking timestamps parsed from `Completed Date` and sorted newest first.
- `GET /api/transactions` returns transactions to the Expo mobile app.
- The CSV path comes from `REVOLUT_CSV_PATH`, not from the mobile request.

```mermaid
flowchart LR
    CSV[Local Revolut CSV] --> Parser[RevolutCsvParser]
    Parser --> Controller[TransactionController]
    Controller --> JSON[REST / JSON]
    JSON --> App[React Native / Expo]
```

The prototype reads the file again for each request. It has no persistence,
cross-import deduplication, authentication, automatic synchronization, or AI yet.

## Run locally

Requires JDK 21. The Maven wrapper downloads Maven; no global Maven installation
is required. Commands below run from the backend repository root.

```bash
./mvnw -version
mkdir -p data
# Put your own CSV at data/statement.csv; this file is not included in Git.
REVOLUT_CSV_PATH="data/statement.csv" ./mvnw spring-boot:run
```

Open [localhost:8080/api/transactions](http://localhost:8080/api/transactions).
The current development config binds to `0.0.0.0` to allow iPhone testing on a
trusted LAN. The endpoint has no authentication: use synthetic data for shared
demos and keep this development service off the public internet. Loopback-only
testing can use `SERVER_ADDRESS=127.0.0.1` when launching.

For iPhone testing, use the Mac's LAN address instead of `localhost` and follow
the companion mobile README. A bundled, synthetic demo statement is still a
checklist item; a fresh clone cannot demonstrate parsing without a CSV file yet.

## API example

`GET /api/transactions` — current response shape; example values are fictional:

```json
[
  {
    "description": "Example Coffee",
    "currency": "EUR",
    "state": "COMPLETED",
    "amount": "-3.50",
    "fee": "0.00",
    "bookingDate": "2026-10-16T09:30:00"
  }
]
```

`bookingDate` is a local date-time without a timezone: the current CSV format
does not supply one. Do not silently treat it as UTC. Amount and fee are kept
separate until the bank's statement semantics are verified in tests. Cash flow
is not an account balance, and transfers between own accounts are not income.

## Verification and current gaps

```bash
REVOLUT_CSV_PATH="data/statement.csv" ./mvnw test
```

The existing test is a Spring application-context smoke test. It does not prove
CSV correctness or HTTP behavior. Parser, money, error-response, and API contract
tests are first-priority tasks. There is no configured CI pipeline yet; no
coverage or AI accuracy claim is made.

## Automatic synchronization: access gate

Revolut's [Open Banking API](https://developer.revolut.com/docs/guides/build-banking-apps/introduction-to-the-open-banking-api/introduction)
is intended for regulated third-party providers. Its
[Business API](https://developer.revolut.com/docs/guides/manage-accounts/get-started/sign-up-for-revolut-business-account)
requires a Business account for production access. Neither is a personal-account
API key available to this project today. Access rules checked on 2026-10-16.

The first integration task is to verify a suitable Open Banking provider's
Revolut coverage, individual-developer eligibility, price, consent flow, data
history, and refresh limits. No provider or free production access is assumed.
Keep CSV as a reproducible fallback; no bank-app scraping or credential capture.

## Roadmap

1. Reproducible demo, focused tests, and consistent API errors.
2. Provider feasibility proof plus persistent, idempotent ingestion.
3. Read-only account synchronization with visible freshness and failure states.
4. Currency-separated analytics and mobile dashboards.
5. Evaluated AI categories, manual corrections, and grounded explanations.

See the [checklist](docs/ROADMAP.md) for task IDs and acceptance criteria.
