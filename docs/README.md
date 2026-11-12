# Milestone 2 — Design & Architecture Deliverables

| Required deliverable | Where |
|---|---|
| Architecture document: pattern selection with justification, component diagram, technology stack | [architecture.md](architecture.md) |
| UML models: use case, class, sequence (×4), activity (updated for Milestone 3) | [uml.md](uml.md) |
| Database design (Week 11 PDF: [LabLink_Week11_Database_Design.pdf](LabLink_Week11_Database_Design.pdf)): ER diagram, 3NF, DDL, ORM, migrations, seed data | [database-design.md](database-design.md) · [migrations](../src/main/resources/db/migration) |
| Architecture Decision Records (5) | [adr/0001](adr/0001-layered-mvc-monolith.md) · [adr/0002](adr/0002-server-side-rendering-and-session-auth.md) · [adr/0003](adr/0003-h2-dev-mysql-prod-handwritten-ddl.md) · [adr/0004](adr/0004-prevent-double-booking-in-service-layer.md) · [adr/0005](adr/0005-flyway-migrations-and-sql-seed-data.md) |
| Working prototype demonstrating core architecture | [`src/`](../src) — run instructions in the [main README](../README.md) |
| Updated project plan | [project-plan.md](project-plan.md) |
| Sprint 3 burndown and progress tracking (Milestone 3) | [sprint3-progress.md](sprint3-progress.md) |

Diagrams are written in Mermaid (render directly on GitHub); the component diagram is Graphviz (`diagrams/component.dot`). All deliverables combined: [LabLink_Milestone2_Design.pdf](LabLink_Milestone2_Design.pdf).
