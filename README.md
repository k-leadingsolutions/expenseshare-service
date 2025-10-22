```markdown
# ExpenseShare — Expense Sharing App (Ledger-first)

This repository contains a production-minded backend for an expense sharing app (ledger-first design).
Project name: ExpenseShare

---
## CI/CD Pipeline
![CI](https://github.com/k-leadingsolutions/expenseshare-service/actions/workflows/ci.yml/badge.svg)](https://github.com/k-leadingsolutions/expenseshare-service/blob/main/.github/workflows/ci.yml)

## How to View

- Click the **CI badge** at the top of this README to see build details and history on GitHub.

## Features
- Create & manage groups
- Create & manage expenses within groups
- Create & manage settlements between users
- Calculate and list balances per user in a group

## ✨ Why This Project?
- **End-to-end backend skills:** API design, security, persistence, docs, and DevOps.
- **Production-readiness:** Health checks, OpenAPI, concurrency, Dockerization, and migrations.
- **Real-world patterns:** observability, error handling, and testability.

## Tech Stack
| Layer         | Technology                  |
|---------------|-----------------------------|
| Language      | Java 17                     |
| Framework     | Spring Boot 3               |
| DB            | H2 and PostgreSQL 15 Docker|
| Migrations    | Flyway                      |
| Auth          | OAuth2             |
| Rate Limiting | Bucket4j                    |
| Docs          | springdoc-openapi           |
| Packaging     | Docker / Compose            |
| CI/CD         | GitHub CI                   |
| Prometheus       | Observability |
| Jaeger       | Tracing                     |  
| Messaging       | Kafka |

Quickstart (H2, fast demo)
1. Build:
   mvn clean package -DskipTests
2. Run:
   java -jar target/expenseshare-0.0.1-SNAPSHOT.jar --spring.profiles.active=h2

Open:
- Swagger UI: http://localhost:8082/swagger-ui.html
- Actuator: http://localhost:8082/actuator/health

Run full stack (Postgres + Kafka + Prometheus + Jaeger)
1. Build the image:
   mvn clean package -DskipTests
   docker build -t expenseshare:local .
2. Start:
   docker-compose up --build
3. App will be reachable at http://localhost:8080
4. Swagger UI: http://localhost:8080/swagger-ui.html
5. Prometheus: http://localhost:9090/api/v1/targets
6. Jaeger UI: http://localhost:16686

API specification
-----------------
A machine-readable OpenAPI (v3) specification is included in the repo at:
- `src/main/resources/static/openapi.json`

You can use this file to:
- View the API in Swagger UI:
    - If your app exposes Swagger UI (e.g. `springdoc` or `springfox`), you can point the UI at the local file by opening Swagger UI and using the "Explore" / "Import URL" feature with:
      `http://localhost:8082/openapi.json` (if you serve the static file at that path)
    - Or run a separate Swagger UI instance (docker image) and point it at `http://<host>/openapi.json`.

- Import to Postman:
     - In Postman, choose Import → Link or File and select the `ExpenseShare API.postman_collection.json` file from `src/main/resources/static`.
    - In Postman, choose Import → Link or File and select the `openapi.json` file from `src/main/resources/static`.

- Generate clients or server stubs:
    - Use openapi-generator: `openapi-generator-cli generate -i src/main/resources/static/openapi.json -g java -o generated-client`

Example endpoints (from the spec)
- POST /api/expenses — create an expense (body: CreateExpenseRequest)
- GET  /api/groups — list groups for current user
- POST /api/settlements — create a settlement
- GET  /api/groups/{groupId}/balances — list balances for a group

## Contributing / Feedback

Feedback, bug reports, and pull requests are welcome!

---

## License

MIT: https://opensource.org/license/MIT

```
