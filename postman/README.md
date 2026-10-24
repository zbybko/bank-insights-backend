# Bank Insights API collection

Cloud collection: https://www.postman.com/collections/45223050-ca1732b3-52d3-4508-8d1e-5be3dc0d26c4

Workspace: My Workspace (personal). Existing PayPal APIs collection is unchanged.

1. Start the backend and PostgreSQL yourself.
2. Send List transactions; its pre-request script initializes the optional collection variables in the cloud copy.
3. Set `baseURL1` (default `http://localhost:8080`).
4. For AI enrichment, manually set `transaction_id` to a synthetic UNCLASSIFIED record and `allow_ai_enrichment` to `true`. Set it back to `false` afterwards. No transaction is selected automatically.
5. Missing-ID and malformed-ID requests expect 404 and 400. The zero UUID must not exist in the database.

The AI request sends stored description, amount and currency to the configured provider and may consume credits. Already classified AI/USER records are skipped by the current service. The transaction response includes category suggestions and review fields.

Do not store PayPal or Gateway keys in this collection. No API authentication is implemented yet; this collection targets local development. Requests were configured from the source code; no live backend calls or AI calls were made when creating it.

The YAML files use JSON syntax, which is valid YAML. No real bank data or captured responses are included.

## Confirm a category

Open **Confirm transaction category** (PATCH), set `transaction_id`, and edit the JSON body, for example `{"category":"GROCERIES"}`. Sending it changes that transaction in the database without an AI call. Tests check the returned ID, selected category, `categorySource=USER`, `categoryReviewRequired=false`, and empty `categoryQuestion`. The previous AI suggestion remains available. The request was configured and linted, but not executed against the backend.
