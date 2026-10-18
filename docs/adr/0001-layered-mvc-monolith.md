# ADR-0001: Use a layered MVC monolith

- **Status:** Accepted
- **Date:** 2026-10-18
- **Deciders:** LabLink team

## Context

LabLink needs a front end, a database, authentication with roles, and CRUD on several entities, delivered by a four-person student team across one semester. Team members are new to building applications. The application has a small domain (users, labs, equipment, reservations) and modest load (one campus).

## Decision

Build LabLink as a single Spring Boot application organized into presentation (controllers + Thymeleaf views), business (services), persistence (Spring Data repositories) and domain (JPA entities) layers. Each layer only calls the layer below it. Business rules live in services, not in controllers or templates.

## Consequences

**Positive**
- One codebase, one build, one deployment; simple to run locally (`mvn spring-boot:run`).
- Clear place for every kind of code; layers map onto team roles.
- Business rules can be unit/integration tested without a browser.
- Matches Spring's conventions and most tutorials the team will find.

**Negative**
- The whole application scales as one unit (acceptable at campus scale).
- Discipline is needed to keep controllers thin; code review checks for business logic in controllers.

**Alternatives rejected:** microservices (operational overhead far exceeds benefit at this size); separate REST API + React SPA (two codebases and skill sets; can add a REST endpoint later if needed).
