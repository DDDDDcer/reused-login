# msg-service

Reusable message notification mock service implemented from the design document.

## Run

1. Execute `src/main/resources/sql/schema.sql` in MySQL.
2. Adjust `src/main/resources/application.yml` if needed.
3. Run `mvn spring-boot:run`.
4. Open `http://localhost:18082/swagger-ui.html`.

## Mock conventions

- `X-Sender-Id` scopes message record queries and identifies the sending system.
- `X-User-Id` identifies the current receiver for local-message APIs.
- `Idempotency-Key` prevents duplicate task creation.
- Enabled carrier accounts use a mock provider. A receiver beginning with `FAIL_` simulates provider failure.
- Scheduled tasks are scanned every five seconds.
