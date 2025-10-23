# ExpenseShare — Expense Sharing App (Ledger-first)

This repository contains a production-minded backend for an expense sharing app (ledger-first design).
Project name: ExpenseShare 
Description: A backend service for managing shared expenses among users in groups, with features for tracking expenses, settlements, and balances.
It’s a modular Spring Boot application designed with clear separation between the web, business, and persistence layers, secured with Google OAuth2, and ensures data consistency through transactional operations.
* Future architectural improvements planned include implementing event sourcing and CQRS patterns to enhance scalability and maintainability.
* The API is documented with OpenAPI (Swagger) for easy integration and exploration.
* Observability is integrated using Prometheus for metrics and Jaeger for tracing.
* The service is containerized with Docker and orchestrated using Docker Compose for easy deployment and scalability.
  ┌────────────────────────────────────┐
  │           Google OAuth2            │
  │  (Authentication Provider)         │
  └────────────────────────────────────┘
  │
  ▼
  ┌──────────────────────────────────────────────────────────────────┐
  │                        ExpenseShare Service                      │
  │                                                                  │
  │  ┌────────────────────────────────────────────────────────────┐  │
  │  │                     Controller Layer                        │  │
  │  │  - GroupController                                          │  │
  │  │  - ExpenseController                                        │  │
  │  │  - SettlementController                                     │  │
  │  │  REST APIs → /api/groups, /api/expenses, /api/settlements   │  │
  │  └────────────────────────────────────────────────────────────┘  │
  │                              │                                   │
  │                              ▼                                   │
  │  ┌────────────────────────────────────────────────────────────┐  │
  │  │                       Service Layer                        │  │
  │  │  - GroupService: group & member management                 │  │
  │  │  - ExpenseService: add expenses, split logic               │  │
  │  │  - SettlementService: transactional settlements             │  │
  │  │  - BalanceService: compute/recompute balances               │  │
  │  │  @Transactional ensures ACID updates                        │  │
  │  └────────────────────────────────────────────────────────────┘  │
  │                              │                                   │
  │                              ▼                                   │
  │  ┌────────────────────────────────────────────────────────────┐  │
  │  │                       Repository Layer                     │  │
  │  │  - UserRepository                                          │  │
  │  │  - GroupRepository                                         │  │
  │  │  - ExpenseRepository                                       │  │
  │  │  - SettlementRepository                                    │  │
  │  │  - UserGroupBalanceRepository                              │  │
  │  │  (Spring Data JPA + H2 DB)                                 │  │
  │  └────────────────────────────────────────────────────────────┘  │
  │                              │                                   │
  │                              ▼                                   │
  │  ┌────────────────────────────────────────────────────────────┐  │
  │  │                           Database                         │  │
  │  │          H2 (for dev) / PostgreSQL (for prod)               │  │
  │  │  Tables: users, groups, expenses, settlements, balances     │  │
  │  └────────────────────────────────────────────────────────────┘  │
  └──────────────────────────────────────────────────────────────────┘


## ✨ Why This Project?
- **End-to-end backend skills:** API design, security, persistence, docs, and DevOps.
- **Production-readiness:** Health checks, OpenAPI, concurrency, Dockerization, and migrations.
- **Real-world patterns:** observability, error handling, and testability.

---
## CI/CD Pipeline
![CI](https://github.com/k-leadingsolutions/expenseshare-service/actions/workflows/ci.yml/badge.svg)]
(https://github.com/k-leadingsolutions/expenseshare-service/blob/main/.github/workflows/ci.yml)

## How to View Pipeline

- Click the **CI badge** at the top of this README to see build details and history on GitHub.

## Features
- Create & manage groups
- Create & manage expenses within groups
- Create & manage settlements between users
- Calculate and list balances per user in a group


## Tech Stack
| Layer         | Technology                  |
|---------------|-----------------------------|
| Language      | Java 17                     |
| Framework     | Spring Boot 3               |
| DB            | H2 and PostgreSQL 15        |
| Migrations    | Flyway                      |
| Auth          | OAuth2                      |
| Rate Limiting | Bucket4j                    |
| Docs          | springdoc-openapi           |
| Packaging     | Docker / Compose            |
| CI/CD         | GitHub CI                   |
| Prometheus       | Observability |
| Jaeger       | Tracing                      |  
| Messaging       | Kafka                     |

Quickstart (H2, fast demo)
1. Build:
   mvn clean package -DskipTests
2. Run:
   java -jar target/expenseshare-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev

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
Email: keamp84@gmail.com

---
## Future Improvements
- Migrate to microservices architecture. (Split by domain: Group, Expense, Settlement, Auth)
- Add more comprehensive tests (unit, integration, e2e).
- Implement additional features like notifications, user profiles, etc.
- Enhance error handling and logging.
- Optimize performance for large datasets. — Pagination, caching, etc.
- Add frontend client (web/mobile) to interact with the API.
- Implement distributed tracing with Jaeger.
- Add CI/CD deployment to cloud (e.g. AWS, GCP).
- Implement GraphQL API alongside REST.


## License

MIT: https://opensource.org/license/MIT

```
