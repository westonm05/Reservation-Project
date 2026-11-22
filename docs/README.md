# LabLink documentation index

Everything is current as of release **v1.0.0** (Week 15). Start with the [main README](../README.md).

## Using LabLink

| Document | For |
|---|---|
| [user-guide.md](user-guide.md) | Students, faculty and lab managers: how to do everything in the app |
| [api.md](api.md) | Developers of other campus systems: the public REST availability API |

## Design and architecture

| Document | Covers |
|---|---|
| [architecture.md](architecture.md) | Architectural pattern and justification, component diagram, technology stack, security, deployment view, testing strategy |
| [uml.md](uml.md) | Use case, class, sequence (×5) and activity diagrams |
| [database-design.md](database-design.md) | ER diagram, 3NF, DDL, ORM mapping, Flyway migrations, seed data |
| ADRs | [0001 layered MVC monolith](adr/0001-layered-mvc-monolith.md) · [0002 server-side rendering and session auth](adr/0002-server-side-rendering-and-session-auth.md) · [0003 H2 dev / MySQL prod](adr/0003-h2-dev-mysql-prod-handwritten-ddl.md) · [0004 double-booking prevention](adr/0004-prevent-double-booking-in-service-layer.md) · [0005 Flyway migrations](adr/0005-flyway-migrations-and-sql-seed-data.md) |

## Testing and quality

| Document | Covers |
|---|---|
| [system-test-report.md](system-test-report.md) | Week 15: 13 end-to-end scenarios, results, defects found |
| [main README → Running the tests](../README.md#running-the-tests) | How to run lint, tests and coverage |

## Deployment and operations (Week 14)

| Document | Covers |
|---|---|
| [containerization.md](containerization.md) | Dockerfile and Compose design |
| [deployment.md](deployment.md) | Production deployment, environment variables, HTTPS, backups, releases, rollback |
| [monitoring.md](monitoring.md) | Metrics, logged events, alerting, incident response |
| [maintenance.md](maintenance.md) | Corrective, adaptive, perfective and preventive maintenance |

## Project management

| Document | Covers |
|---|---|
| [project-plan.md](project-plan.md) | Plan, roles, schedule |
| [sprint3-progress.md](sprint3-progress.md) | Sprint 3 burndown and progress |
| [git-workflow.md](git-workflow.md) | Branching, commits, pull requests |
| [presentation-rehearsal.md](presentation-rehearsal.md) | Final presentation run-of-show and rehearsal notes |
| [../RELEASE_NOTES.md](../RELEASE_NOTES.md) | Release notes for v1.0.0 |

## Earlier deliverable PDFs

[Milestone 2 design](LabLink_Milestone2_Design.pdf) · [Week 11 database design](LabLink_Week11_Database_Design.pdf) · [Week 14 deployment and operations](LabLink_Week14_Deployment_Operations.pdf)

Diagrams are Mermaid (render on GitHub) or Graphviz (`diagrams/*.dot`).
