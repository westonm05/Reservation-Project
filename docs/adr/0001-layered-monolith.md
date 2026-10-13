# ADR-0001: Use a layered monolith with Spring MVC

* **Status:** Accepted
* **Date:** 2026-10-13
* **Deciders:** Weston Mattingly, Isaiah Bain, Dawson Brown, PJ

## Context

LabLink must deliver a web front end, a database, authentication with roles, CRUD on
two or more entities and one advanced feature within one semester. The team has four
people, most of whom are building their first application; everyone knows Java. The
expected user base is one campus (hundreds of users, not thousands of concurrent ones).
Work is graded at four milestones, and Git history is part of each person's grade, so
the structure must let each person own a clear piece of the code.

## Decision

Build LabLink as **one Spring Boot application** with four layers, each in its own
package, where each layer only calls the one below it:

`web` (controllers + form DTOs) → `service` (business rules, transactions) →
`repository` (Spring Data JPA) → `model` (entities) → database.

Security (`config`) wraps the whole application as a servlet filter chain. Controllers
contain no business rules; templates contain no logic beyond display.

## Consequences

**Positive**
* One codebase, one build, one deployable JAR — simple to run, test and demo.
* The structure mirrors Spring's own stereotypes (`@Controller`, `@Service`,
  `@Repository`), so tutorials and docs apply directly.
* Business rules are concentrated in services, so they can be unit- and
  integration-tested without a browser.
* Work divides naturally by feature slice (auth, reservations, inventory, UI) while
  every slice looks the same, which keeps code reviews quick.

**Negative / risks**
* All features scale together; we cannot scale reservations independently of
  inventory. Not a concern at campus scale.
* Discipline is needed to keep rules out of controllers. Mitigation: code-review
  checklist item "no business logic in controllers or templates".
* A change to a shared class (e.g. `User`) can affect everyone. Mitigation: entity
  changes go through PJ (database owner) and require a reviewed PR.

## Alternatives considered

* **Microservices** — rejected: networking, multiple deployments and distributed data
  add large overhead with no benefit at this size.
* **SPA + REST API** — rejected for now; see ADR-0002.
* **Hexagonal architecture** — rejected as more abstraction than a five-table system
  needs; our service layer already isolates business rules from the web layer.
