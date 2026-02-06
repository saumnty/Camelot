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
1. Configure PostgreSQL and create the database.
2. Set database credentials in environment variables or application properties.
3. Build the project using Maven.
4. Run the Spring Boot application.
5. Access the platform via browser.

## Project Status
Camelot is an **active and evolving project**, with ongoing improvements focused on:
- Advanced sales reports.
- UI/UX enhancements and responsiveness.
- Camera live streaming integration.
- Theme customization (light/dark).

## Author
Santiago Saucedo Mendoza