# Bank Insights API collection

Cloud collection: https://www.postman.com/collections/45223050-ca1732b3-52d3-4508-8d1e-5be3dc0d26c4

Workspace: My Workspace (personal). Existing PayPal APIs collection is unchanged.

## Folders and variables

The cloud and local collection use four folders: **01 - Overview**, **02 - Spending**,
**03 - Income**, and **04 - Error cases**. Collection defaults are stored in
`collections/Bank Insights/.resources/definition.yaml`.

| Variable | Purpose | Default |
| --- | --- | --- |
| `baseURL1` | Backend URL; an environment can override this | `http://localhost:8080` |
| `transaction_id` | Expense transaction selected for spending requests | Empty; select manually |
| `income_transaction_id` | Incoming transaction selected for income requests | Empty; select manually |
| `allow_ai_enrichment` | Explicit permission to send an AI enrichment request | `false` |
| `summary_month` | Calendar month in `YYYY-MM` format | Empty; request uses current month |
| `expense_category` | Spending category sent by the confirmation PATCH | `GROCERIES` |
| `income_type` | Income meaning sent by the confirmation PATCH | `SALARY` |

Open the collection's **Variables** tab to edit these values. Income and spending
requests deliberately use separate IDs. No transaction is selected automatically.
The schema files do not automatically sync with Postman cloud.

1. Start the backend and PostgreSQL yourself.
2. Send List transactions; its pre-request script initializes the optional collection variables in the cloud copy.
3. Set `baseURL1` (default `http://localhost:8080`).
4. For AI enrichment, manually set `transaction_id` to a synthetic UNCLASSIFIED record and `allow_ai_enrichment` to `true`. Set it back to `false` afterwards. No transaction is selected automatically.
5. Missing-ID and malformed-ID requests expect 404 and 400. The zero UUID must not exist in the database.

The AI request sends stored description, amount and currency to the configured provider and may consume credits. Already classified AI/USER records are skipped by the current service. The transaction response includes category suggestions and review fields.

Do not store PayPal or Gateway keys in this collection. No API authentication is implemented yet; this collection targets local development. Requests were configured from the source code; no live backend calls or AI calls were made when creating it.

The YAML files use JSON syntax, which is valid YAML. No real bank data or captured responses are included.

## Confirm a category

Open **02 - Spending / Confirm spending category - expenses only** (PATCH),
set `transaction_id` and `expense_category`. Sending it changes the stored category
without AI. Positive and zero amounts reject spending labels with 400; using
`UNCATEGORIZED` permits clearing an old incorrect label. The previous AI suggestion
remains available. For incoming payments use **03 - Income**, set
`income_transaction_id`, and use `income_type` with **Confirm income meaning**.
