# ADR-0003: H2 for development, MySQL for production, schema from hand-written DDL

- **Status:** Accepted
- **Date:** 2026-10-18
- **Deciders:** LabLink team

## Context

Every developer must be able to run the application immediately, CI must run tests without a database server, and the final demo needs a persistent production database. The course also requires DDL scripts as a design deliverable. Hibernate can generate tables automatically (`ddl-auto=update`), but then the real schema can drift from the documented one.

## Decision

- Use **H2 in memory, in MySQL compatibility mode,** as the default database for development and tests. Data is recreated on every start and filled with demo accounts by `DevDataSeeder`.
- Use **MySQL 8** in production via the `mysql` Spring profile, with credentials from environment variables.
- Create tables from one hand-written script, **`src/main/resources/db/schema.sql`**, run by Spring at startup (`spring.sql.init.mode=always`). Hibernate schema generation is turned off (`ddl-auto=none`). The script uses `CREATE TABLE IF NOT EXISTS` so it is safe to run repeatedly, and only SQL that both H2 and MySQL accept.

## Consequences

**Positive**
- Zero setup for teammates and CI; tests run against the same DDL as production.
- The DDL in the design document is literally what the application runs, including CHECK, UNIQUE and FOREIGN KEY constraints.
- Constraints protect data even if application code has a bug.

**Negative**
- Schema changes must be made by hand in `schema.sql` and in the entity classes together.
- `IF NOT EXISTS` does not alter existing tables; once production holds real data, we will adopt a migration tool (Flyway) — revisit in Milestone 3.
- H2's MySQL mode is close to, but not identical to, MySQL; we will run the app against a real MySQL instance before the final demo.
