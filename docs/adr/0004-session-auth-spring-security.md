# ADR-0004: Session-based form login with Spring Security and BCrypt

* **Status:** Accepted
* **Date:** 2026-10-13
* **Deciders:** Weston Mattingly (authentication owner), team

## Context

The course requires authentication with role-based access control. LabLink has three
roles: **Student** and **Faculty** (request and manage their own reservations) and
**Lab Manager** (approve requests, manage inventory). Pages are server-rendered
(ADR-0002), so the browser talks only to our own server. Passwords must never be
stored in readable form.

## Decision

* Use **Spring Security form login** with a server-side **HTTP session** (cookie).
  Our `LabLinkUserDetailsService` loads users from the `users` table by email.
* Store passwords as **BCrypt** hashes (`BCryptPasswordEncoder`, strength 10).
* Store the role as one column (`users.role`) mapped to the `Role` enum and exposed to
  Spring as the authority `ROLE_<role>`.
* Enforce access in **two places** (defense in depth):
  1. URL rules in `SecurityConfig`: public pages are `/`, `/login`, `/register`, `/css/**`;
     `/manager/**` requires `LAB_MANAGER`; everything else requires sign-in.
  2. Service checks: `ReservationService.review` verifies the reviewer is a lab
     manager; `cancel` verifies the caller owns the reservation.
* Keep **CSRF protection on** (Spring's default); Thymeleaf adds the token to forms.
* Self-registration creates `STUDENT` or `FACULTY` accounts only; `LAB_MANAGER`
  accounts are created by an administrator (seeded in `data.sql` for now).

## Consequences

**Positive**
* Almost no custom security code — the framework handles login, logout, session
  fixation protection and password comparison.
* Ownership and role rules are tested both at the HTTP level (`SecurityAccessTest`)
  and in the service (`ReservationServiceTest`).
* One role per user keeps the schema and the UI simple.

**Negative / risks**
* Sessions live in server memory: a restart signs everyone out, and running several
  server instances would need shared sessions. Acceptable for one instance.
* One role per user cannot express "faculty member who is also a lab manager". If
  needed, a `user_roles` join table would replace the column (new ADR).
* No password reset or email verification yet (candidate Milestone 3/4 features).

## Alternatives considered

* **JWT tokens** — designed for separate front ends and APIs; adds token storage and
  refresh complexity we do not need with server-rendered pages.
* **University SSO / OAuth2 login** — ideal for a real deployment, but requires campus
  IT registration. Spring Security supports adding it later alongside form login.
* **Writing our own login and hashing** — rejected; security code is easy to get wrong.
