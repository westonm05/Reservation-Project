# ADR-0002: Server-side rendering with Thymeleaf and session-based Spring Security

- **Status:** Accepted
- **Date:** 2026-10-18
- **Deciders:** LabLink team

## Context

The project requires a front end and authentication with role-based access control. Options were (a) server-rendered HTML with Thymeleaf and form login backed by an HTTP session, or (b) a JavaScript single-page app calling a REST API secured with JSON Web Tokens (JWT).

## Decision

Render pages on the server with Thymeleaf and use Spring Security form login with server-side sessions. Roles (`STUDENT`, `FACULTY`, `LAB_MANAGER`) are stored on the user and mapped to Spring authorities (`ROLE_…`). Access is enforced twice: URL rules in `SecurityConfig`, and role/ownership checks in the service layer. Passwords are hashed with BCrypt. Only students can self-register.

## Consequences

**Positive**
- Entire application is Java; no separate front-end build or framework to learn.
- CSRF protection, session handling, login/logout pages and password hashing come built in.
- `sec:authorize` in templates hides buttons a user is not allowed to use.

**Negative**
- Pages reload on every action (less "app-like" than an SPA).
- A future mobile client would need a REST API with token auth added alongside.

**Alternatives rejected:** JWT + SPA — more moving parts (token storage, refresh, CORS) and a second language/toolchain for the team.
