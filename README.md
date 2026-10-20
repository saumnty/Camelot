# Camelot – Store & Sales Management Platform

Camelot is a **multi-store sales management platform** designed to support small businesses in handling products, inventory, purchases, and administrative validation workflows.  
The system focuses on **real-world business rules**, role-based access, and operational traceability rather than being a simple CRUD application.

## Overview
Camelot allows multiple stores to operate within the same platform, with **independent inventories, users, and sales workflows per store**.  
A key design goal is to ensure that **sales are validated by administrators before stock is finalized**, reflecting how many real shops operate.

## Key Features

### Store & User Management
- Multi-store architecture with isolated data per store.
- Role-based access control (**ADMIN / CLIENT**) per store.
- Default CLIENT role with controlled elevation to ADMIN.
- Store ownership verification flow.

### Sales Workflow (Core Feature)
- Clients can place orders even when product stock is undefined (`NULL`).
- Orders are initially marked as **PENDING**.
- Store administrators can:
  - Validate available stock.
  - Edit or remove items before approval.
  - **Confirm** or **Cancel** purchases with a recorded reason.
- Order state tracking: **PENDING → CONFIRMED / CANCELED**.
- Automatic stock deduction upon confirmation.

### Inventory & Products
- Product and category management per store.
- Support for `NULL` stock values for flexible inventory control.
- Stock validation enforced at checkout approval stage.

### Tickets & Records
- Purchase tickets generated after confirmation.
- Read-only sales history and transaction records.
- Visual status indicators (badges) for purchase states.

### Reports & Administration
- Administrative views for daily sales tracking.
- Planned reporting by product, category, and provider.
- External purchase registration for accurate daily totals.

### Role-based navigation & notifications
- Single sidebar (`templates/fragments/sidebar.html`): every user sees the **customer** menu;
  store admins also get a **"Mi tienda"** section; inside a store you don't own, a store sub-menu appears.
- Notification center: owner requests, frequent-customer requests, order updates, low stock.

### Frequent customers & store credit ("fiado")
- Customers ask to become frequent customers; admins approve from the notification center
  with an optional credit limit. Credit purchases are charged when the admin confirms them.

### Payments
- Per-store payment methods: card (simulated gateway), cash on pickup, store credit.
- Refunds for paid orders (`REEMBOLSADA`), per-store tax (IVA) breakdown on tickets.

### Camera Module (In Progress)
- Store-level camera registration (RTSP).
- Admin-only access to camera feeds.
- Diagnostics for camera connectivity and codecs.

## Technology Stack

### Backend
- Java
- Spring Boot
- Spring Data JPA
- PostgreSQL

### Frontend
- Thymeleaf
- HTML / CSS
- Bootstrap

### Architecture & Concepts
- MVC architecture
- Role-based authorization per store
- Session-based active store resolution
- Business-rule-driven workflows
- Database-level integrity and validation

## Business Flow Example
1. A client places an order.
2. The order is saved with status **PENDING**.
3. An administrator reviews the order.
4. The administrator edits items if needed and validates stock.
5. The order is either:
   - **CONFIRMED** (stock is deducted, ticket generated), or
   - **CANCELED** (reason recorded).
6. The transaction becomes part of the immutable sales record.

## Running the Project

### Local
1. Create a PostgreSQL database named `clientes_ventas`.
2. Set `DB_USER` / `DB_PASSWORD` (or edit nothing and use `postgres`/`postgres`).
3. `./mvnw spring-boot:run` and open http://localhost:8080.

### Render + Supabase
Render must define these environment variables (never commit passwords):

| Variable | Example |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://<supabase-host>:5432/postgres?sslmode=require` |
| `SPRING_DATASOURCE_USERNAME` | `postgres.<project>` |
| `SPRING_DATASOURCE_PASSWORD` | *(your Supabase password)* |

Hibernate (`ddl-auto=update`) creates new tables/columns on startup. Data fixes live in
`db/migraciones/`: run `01_antes_de_desplegar.sql` **before** deploying and
`02_despues_de_desplegar.sql` **after** the first deploy (Supabase → SQL Editor).

### Demo catalog
Load ~75 typical Mexican grocery products into an existing store (id 5 here):

```
./mvnw spring-boot:run -Dspring-boot.run.arguments="--camelot.semilla.tienda-id=5"
```

For real products (names, brands, photos) from Open Food Facts, run
`python3 scripts/openfoodfacts_a_csv.py` and pass `--camelot.semilla.archivo=file:productos_off.csv`.

### Payments (simulated)
`camelot.pagos.proveedor=simulado` — no real money moves. Test cards:
`4242 4242 4242 4242` (approved), `5555 5555 5555 4444` (approved),
`4000 0000 0000 0002` (declined); any future date and CVV.
To go live, implement `Pago/ProveedorPago` for Mercado Pago, Stripe, Openpay or Conekta
(card data is tokenized by the provider and never reaches this server).

### Tests
`./mvnw verify` (needs a PostgreSQL; GitHub Actions starts one automatically, see `.github/workflows/ci.yml`).

## Project Status
Camelot is an **active and evolving project**, with ongoing improvements focused on:
- Advanced sales reports.
- UI/UX enhancements and responsiveness.
- Camera live streaming integration.
- Theme customization (light/dark).

## Author
Santiago Saucedo Mendoza