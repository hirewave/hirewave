# Hirewave

## Team
- **Team Name:** Hirewave
- **Members:**
  - Opran Andrei - Freelancer profiles (CRUD), skills + hourly rate validation; Client profiles (CRUD); owns `Freelancer` + `Client` domain + API. **Core Logic**: Freelancer availability limits (blocks new bids if working on max concurrent projects), Freelancer rating system (average score updated on project completion, visible to clients), Freelancer skill-based bid eligibility (freelancer must share at least one required skill with the project to bid), and Client reputation score (derived from completed vs. cancelled project ratio).
  - Prizlopan Iustin-George - Projects (CRUD), project status machine + validations (restrict edits by status); owns `Project` domain + API
  - Eftimie Traian - Bids (core business logic), prevent duplicate bids, accept/reject flow (cascade: accept → reject others, project → IN_PROGRESS); owns `Bid` domain + API

## Project Description

HireWave is a REST API-based SaaS for a freelance marketplace where clients publish projects and freelancers place bids to win work. The platform focuses on clean business rules (status transitions, validation, and anti-duplicate constraints) and a simple but realistic domain model suitable for production engineering practices.

The service will be implemented in Java 21 + Spring Boot and backed by MongoDB for persistence. The project is designed to support end-to-end workflows (create freelancer + create project + submit bids + accept a bid) and to be production-ready: containerized with Docker, testable via CI, and observable via metrics/logging for monitoring and alerting.

### Key Features
- Freelancer management: create/list/update/delete freelancer profiles with validated skills and hourly rate + unique email constraint. **Business logic**: Availability limits — a freelancer cannot place new bids if they are already working on 3 concurrent active (`IN_PROGRESS`) projects. Rating system — when a project moves to `COMPLETED`, the owning client can submit a rating (1–5) for the freelancer who was awarded the project; the freelancer's average rating is recalculated, stored, and exposed on the freelancer profile for clients to evaluate. Skill-based bid eligibility — a freelancer can only submit a bid on a project if at least one of their skills matches a required skill listed on the project; mismatched bids are rejected with a validation error.
- Client management: create/list/update/delete client profiles with unique email constraint; clientId required on project creation. **Business logic**: Reputation score — automatically maintained as `completedProjects / (completedProjects + cancelledProjects)`; score is exposed on the client profile.
- Project management: create/list/update projects with a strict status machine (OPEN → IN_PROGRESS → COMPLETED, OPEN → CANCELLED) and edit restrictions by status
- Bid system: submit bids with duplicate prevention + accept/reject logic that updates related entities (accept one bid → reject others, project moves to IN_PROGRESS)
- API-first workflow + testing/observability: REST Client requests, unit/integration/E2E test coverage, metrics for monitoring and alerting

### Technical Stack
- **Backend:** Spring Boot (Java 21)
- **Database:** MongoDB
- **API:** RESTful
- **Testing:** JUnit, Mockito, Cucumber
- **Monitoring:** Prometheus, Grafana
- **Deployment:** Docker

## Contributing

All team members follow trunk-based development:
1. Create feature branch from `main`
2. Make changes and commit with clear messages
3. Create PR and request review
4. Address feedback
5. Merge after approval
