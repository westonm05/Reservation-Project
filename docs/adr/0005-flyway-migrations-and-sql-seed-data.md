# ADR-0005: Flyway migrations and SQL seed data

- **Status:** Accepted (supersedes the schema-management part of ADR-0003)
- **Date:** 2026-11-03
- **Deciders:** LabLink team

## Context

ADR-0003 created tables from one hand-written `schema.sql` using `CREATE TABLE IF NOT EXISTS`, and noted that this cannot alter tables that already exist. Week 11 adds four tables and three foreign-key columns to existing tables, and the course requires migration scripts that build the schema from scratch plus seed scripts with at least 20 rows per major table.

## Decision

- Use **Flyway** (`flyway-core`, `flyway-mysql`). Versioned scripts live in `src/main/resources/db/migration` (`V1__` … `V4__`); Flyway runs any not yet applied at startup and records them in `flyway_schema_history`.
- `schema.sql` becomes `V1__create_core_tables.sql`. Production MySQL uses `baseline-on-migrate` at version 1 so a database created from the old script continues from V2.
- Hibernate runs with `ddl-auto=validate`: the app fails to start if an entity doesn't match the migrated tables.
- Demo/test data moves from the Java `DevDataSeeder` to a repeatable Flyway script, `db/seed/R__seed_test_data.sql`. The `db/seed` location is on by default (H2 development and tests) and off in the `mysql` profile unless the `seed` profile is added.

## Consequences

**Positive**
- Any empty database reaches the current schema with one command; every schema change is a reviewed file in Git.
- Seed data is plain SQL that can be read, graded and run outside the app.
- Tests run against exactly the migrated schema and seed data.

**Negative**
- Applied migrations must never be edited; fixes need a new `V<n>` file.
- The seed script deletes all rows before inserting, so the `seed` profile must only be used on a demo database.
- Scripts must stay in the SQL subset both H2 (MySQL mode) and MySQL 8 accept.
