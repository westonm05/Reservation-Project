# Architecture Decision Records

An **ADR** is a short note that records one important design decision: the situation
we were in, what we decided, and what that decision costs us. They are numbered and
never edited after acceptance — if we change our minds, we add a new ADR that
*supersedes* the old one, so the history of our reasoning stays visible.

| # | Title | Status | Date |
|---|---|---|---|
| [0001](0001-layered-monolith.md) | Use a layered monolith with Spring MVC | Accepted | 2026-10-13 |
| [0002](0002-server-side-thymeleaf.md) | Render pages on the server with Thymeleaf | Accepted | 2026-10-13 |
| [0003](0003-mysql-h2-schema-first.md) | MySQL in production, H2 in development, schema-first DDL | Accepted | 2026-10-13 |
| [0004](0004-session-auth-spring-security.md) | Session-based form login with Spring Security and BCrypt | Accepted | 2026-10-13 |
| [0005](0005-reservation-target-and-conflicts.md) | Model reservation targets and prevent double booking | Accepted | 2026-10-13 |

Template for new ADRs: copy any existing file and keep the headings
*Status / Context / Decision / Consequences / Alternatives considered*.
