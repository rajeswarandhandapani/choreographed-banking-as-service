# Spring Boot 4 modernization

The parent is Spring Boot **4.1.1**, with Spring Cloud **2025.1.3**, verified against the stable release and compatibility listings on 2026-09-07. Java 21 remains the application baseline.

## Run the choreographed stack

```sh
mvn -B -ntp verify
bash scripts/run-local.sh --skip-build
```

The runner starts project-scoped infrastructure, waits for the identity provider, copies built JARs into `.local/run/`, and starts each microservice after the preceding one becomes healthy. Logs are in `.local/logs/`. Ctrl+C stops only the JVMs started by that runner. It never kills unrelated Java processes or containers. `start.sh` and `start-all-services.sh` delegate to this runner. Stop its infrastructure explicitly with `docker compose -f compose.local.yml down` when finished.

| Component | Local port |
| --- | --- |
| API gateway | 18080 |
| Accounts / Users / Payments | 18081 / 18082 / 18083 |
| Transactions / Notifications / Audit | 18084 / 18085 / 18086 |
| Eureka | 18761 |
| Keycloak | 18089 |
| MySQL / Kafka | 13306 / 19092 |
| Angular choreographed configuration | 4201 |

These resources are separate from the orchestrated banking stack. The local realm imports `user1` / `user123`, `user2` / `user123` and `baas-admin` / `admin123`. They are development fixtures. In the sibling `baas-bank-ui`, run `npm run start:choreographed`, then open http://localhost:4201. “Set up your account” signs in an existing identity and starts event-driven onboarding using `POST /api/users`.

Ordinary Maven verification skips Docker builds. To build the container images explicitly, use `mvn package -Djib.skip=false`; image names start with `rajeswaran/choreographed-baas-` to avoid overwriting orchestrated service images. The supplied `docker-compose.yml` uses these names. The isolated local runner is the tested development workflow.

## Dependency migration

- Inherit the actual Spring Boot stable parent instead of importing a Boot BOM without the parent's plugin configuration.
- Let Boot manage MySQL Connector/J, Lombok, SLF4J and build-plugin versions. Remove obsolete mysql and javax Servlet coordinates.
- Use Boot 4 MVC and OAuth resource-server starters and the Gateway server WebFlux starter/configuration namespace.
- Replace Hibernate's legacy UUID generator with `@UuidGenerator` and let Hibernate detect its database dialect.
- Use explicit compiler release 21 and annotation processing for Lombok. Preserve test-slice filters through `@SpringBootApplication(scanBasePackages=...)`.

## SOLID and correctness changes

- `EventPublisher` is a transport-independent application port; the Spring Stream adapter checks rejected sends. Business code no longer depends directly on `StreamBridge`.
- `AccountTransfers` separates the event consumer from the transfer use case. `TransferPolicy` owns input, ownership, account-status and balance rules. `TransactionalAccountTransfers` owns locking and persistence.
- Both balances and a durable payment receipt commit atomically. A missing destination cannot leave the source debited. Non-positive/non-finite amounts, same-account transfers, inactive accounts and unauthorized sources are rejected.
- Accounts are locked in consistent order. Replayed and concurrently duplicated payment events reuse the persisted receipt without transferring twice. A changed payload cannot reuse a receipt ID.
- Publication failures propagate for binder retries rather than reporting a successful debit as a failed transfer. The receipt makes balance changes safe to retry.
- `TransferLedger` records debit and credit entries in one transaction. A unique reference/type constraint and existing-entry lookup prevent repeated event delivery from duplicating ledger rows.
- Payment creation accepts a validated DTO rather than a persistence entity, derives the initiator at the controller boundary, and returns HTTP 202. Descriptions travel through payment and balance events into transaction history.
- User setup returns an existing registered user rather than blindly duplicating it. Account numbers use UUID-derived numeric values instead of a process-local counter that resets on restart.
- The gateway's admin-composition client derives its own port, and the dead route to a nonexistent orchestrator is removed.

## Integration with the shared UI

The new Angular `choreographed` configuration changes the identity-provider URL and onboarding/payment endpoints without altering the default orchestrated configuration. Admin views show real audit events instead of invented saga instances. The UI supports UUID account IDs, the choreographed `PROCESSED` payment state, and its notification/user DTO differences.

## Scope and limitations

The inherited monetary representation remains `double`; switching to decimal money is a separate schema and API migration. Receipt and ledger protections cover successful transfer-event replays; this is not an end-to-end exactly-once guarantee. There is no durable transactional outbox for initial payment/user publication, and audit/notification consumers may record repeated deliveries. Existing databases require review before adding the ledger uniqueness constraint; the integration tests use a fresh isolated database. The separately deployed chatbot is not supplied by these repositories.

Sources: https://docs.spring.io/spring-boot/index.html and https://spring.io/projects/spring-cloud/.

## Validation results

On 2026-09-07, `mvn -B -ntp verify` passed all 10 reactor modules and 17 tests. The shared UI passed both production configurations and four unit tests. Its three live choreographed browser tests passed against this isolated stack, covering successful transfers, unchanged balances on failure, debit/credit ledger entries, notifications, audit data and admin views. Desktop and mobile screenshots were visually inspected. See the sibling UI's `CHOREOGRAPHED-VALIDATION.md` and `artifacts/choreographed/` for details and generated evidence.
