# Student Management

A small full-stack app for managing students, courses and enrollments.

- **Backend:** Java 21, Spring Boot 3.5, Spring Data JPA, H2 (file-based) or PostgreSQL
- **Frontend:** Angular 21 (standalone, zoneless), Angular Material, RxJS stores

```
.
├── backend/    Spring Boot REST API  (controller → service → repository)
└── frontend/   Angular SPA           (core / shared / lazy-loaded features)
```

## Prerequisites

| Tool | Version | Notes |
|---|---|---|
| JDK | 21 | `JAVA_HOME` must point at it. Maven isn't needed; use the bundled wrapper (`mvnw` / `mvnw.cmd`). |
| Node.js | `^20.19`, `^22.12` or `>=24` | Required by Angular 21. |
| npm | 10+ | Ships with Node. |

## Running the apps

Start the backend first. The frontend expects the API at `http://localhost:8080/api`.

### Backend (port 8080)

```bash
cd backend
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```

- Data is stored in `backend/data/student-management.mv.db` and survives restarts. Delete the `data/` folder to start fresh.
- On first start each table is seeded from `data.sql`: 5 students, 6 courses and a few enrollments. Seeding only runs while a table is empty, so your edits aren't overwritten.
- `spring-boot:run` starts a separate JVM. Stop it with Ctrl+C; killing only the parent shell can leave the H2 file locked.

**H2 console:** <http://localhost:8080/h2-console>

| Field | Value |
|---|---|
| JDBC URL | `jdbc:h2:file:./data/student-management` |
| User | `sa` |
| Password | *(empty)* |

**PostgreSQL profile:**

```bash
DB_URL=jdbc:postgresql://localhost:5432/student_management DB_USERNAME=student DB_PASSWORD=student \
  ./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
```

The profile turns off the H2 console and seed data, and Hibernate creates or updates the schema. It is configured but hasn't been run against a real PostgreSQL instance; the tests and manual checks all used H2.

### Frontend (port 4200)

```bash
cd frontend
npm ci
npm start
```

Open <http://localhost:4200>. The backend allows CORS from this origin only (`app.cors.allowed-origins`).

## API

Base path is `/api`. Errors are returned as RFC 7807 `application/problem+json`.

| Method | Path | Request | Success | Error statuses |
|---|---|---|---|---|
| GET | `/students` | `search`, `page`, `size`, `sort` (`firstName`, `lastName`, `email`, `enrollmentDate`) | 200 `PageResponse<StudentResponse>` | 400 unsupported sort |
| GET | `/students/{id}` | | 200 `StudentResponse` | 404 |
| POST | `/students` | `StudentRequest` | 201 + `Location` | 400, 409 email taken |
| PUT | `/students/{id}` | `StudentRequest` | 200 | 400, 404, 409 |
| DELETE | `/students/{id}` | | 204 (also removes their enrollments) | 404 |
| GET | `/students/email-available` | `email`, `excludeId?` | 200 `{ "available": bool }` | 400 |
| GET | `/students/{id}/enrollments` | | 200 `EnrollmentResponse[]` | 404 |
| POST | `/students/{id}/enrollments` | `{ "courseId": n }` | 201 + `Location` | 400, 404, 409 already enrolled, 422 `MAX_COURSES_EXCEEDED` |
| DELETE | `/students/{id}/enrollments/{courseId}` | | 204 | 404 |
| GET | `/courses` | `search`, `page`, `size`, `sort` (`code`, `title`, `credits`) | 200 `PageResponse<CourseResponse>` | 400 |
| GET | `/courses/{id}` | | 200 `CourseResponse` | 404 |
| GET | `/courses/{id}/students` | | 200 `StudentResponse[]` | 404 |
| POST | `/courses` | `CourseRequest` | 201 + `Location` | 400, 409 code taken, 422 `COURSE_CODE_NOT_IN_CATALOG` |
| PUT | `/courses/{id}` | `CourseRequest` | 200 | 400, 404, 409, 422 |
| DELETE | `/courses/{id}` | `force` (default `false`) | 204 | 404, 422 `COURSE_HAS_ENROLLMENTS` |
| GET | `/courses/code-available` | `code`, `excludeId?` | 200 `{ "available": bool }` | 400 |

**Request and response shapes:**

```text
StudentRequest     { firstName*, lastName*, email*, dateOfBirth* (past), enrollmentDate (not future, defaults to today) }
StudentResponse    { id, firstName, lastName, email, dateOfBirth, enrollmentDate }
CourseRequest      { code* (2-4 letters + 3 digits, case-insensitive), title*, credits* (1-6), description }
CourseResponse     { id, code, title, credits, description, enrolledCount }
EnrollmentResponse { courseId, code, title, credits, enrolledAt }
PageResponse<T>    { content, page, size, totalElements, totalPages }
```

**Error body.** `code` is always present. `errors` lists field-level problems where there are any (validation, duplicates, catalog rejection):

```json
{
  "type": "urn:problem-type:validation-failed",
  "title": "Validation failed",
  "status": 400,
  "detail": "The request contains 2 invalid field(s)",
  "instance": "/api/courses",
  "code": "VALIDATION_FAILED",
  "errors": [
    { "field": "credits", "message": "must be less than or equal to 6" },
    { "field": "code", "message": "must be 2-4 letters followed by 3 digits, e.g. CS101" }
  ]
}
```

**Course catalog stub.** When a course code is new or changed, the backend checks it with `GET {catalog.base-url}/courses/{code}`. Locally that URL points at a stub inside the same app (`/stub/catalog`). The stub accepts the department prefixes `CS`, `MATH`, `PHYS`, `CHEM`, `BIO`, `ENG`, `HIST` and `ECON`, so `ZZ101` is rejected. Timeouts are set with `catalog.connect-timeout` and `catalog.read-timeout`.

## Tests

```bash
cd backend  && ./mvnw test                # 82 tests
cd frontend && npm test -- --watch=false  # 17 tests (Vitest)
cd frontend && npm run build              # production build
```

| Layer | What it covers |
|---|---|
| Service unit tests (JUnit 5, Mockito, AssertJ) | Happy paths plus every business rule and its limits: duplicate email/code, 6th course allowed / 7th rejected, already enrolled, `force` deletion order, catalog found / not found / unavailable, default enrollment date via a fixed `Clock`. |
| `@WebMvcTest` | Status codes, `Location` headers, field-level validation errors, ProblemDetail `type`/`code`/`errors`, sort whitelist, generic 500 that doesn't leak the exception message. |
| `@DataJpaTest` | Search (name, full name, email, case, literal `%`/`_`), paging, the `@Formula` enrolled count, eager course loading, bulk delete, cascades, unique constraint names. |
| `@RestClientTest` | Catalog client: found, not found, 5xx and timeout fall back to "unavailable". |
| `@SpringBootTest` + `TestRestTemplate` | Full enroll flow over HTTP, plus two simultaneous 6th-course requests: exactly one gets 201 and the other 422. This test fails reliably if the row lock is removed. |
| Frontend (Vitest) | Unsaved-changes guard, validators (wait-before-checking, own-value skip, failure tolerance), server-error mapping, `StudentStore` (enroll/unenroll propagation, stale request cancellation). |

## Design decisions and trade-offs

### Backend

- **Package by feature, layered inside.** Each of `student/`, `course/`, `enrollment/` has its own controller, service, repository, mapper and `dto/`. `common/` holds only genuinely shared code: exceptions, the error handler, paging.
- **Entities never leave a transaction.** Services return DTOs (records), and `open-in-view` is off, so a missed fetch fails in tests instead of quietly running extra queries during JSON rendering. Mappers are small static classes; MapStruct would be overkill for three types.
- **An explicit `Enrollment` entity instead of `@ManyToMany`.** The link carries `enrolledAt`, "has enrollments" is a simple count, and it avoids Hibernate's delete-and-reinsert behaviour on many-to-many collections. `Student` owns the collection; `Course` has none.
- **Atomic enroll.** The student row is locked (`PESSIMISTIC_WRITE`) for the whole transaction, then the "not already enrolled" and "fewer than 6" checks run and the insert happens. Without the lock, two concurrent requests could both see 5 courses. The DB unique constraint is the backstop for duplicates.
- **Uniqueness is checked twice.** The service checks first and returns a friendly 409 with the field name. Named DB constraints catch races, and the handler maps a constraint violation back to the field.
- **Input is normalised in the request records' constructors** (trimmed, email lower-cased, code upper-cased). A plain unique index then behaves case-insensitively, and `cs101` is accepted.
- **Error contract.** `BusinessRuleException` maps to 422 regardless of the rule, so services stay unaware of HTTP. Every error, including framework ones, carries a machine-readable `code` so the UI never parses message text.
- **`@Formula` for `enrolledCount`.** It avoids one query per course on the list page, at the cost of being Hibernate-specific.
- **Catalog client.** `RestTemplate` with a root URL and connect/read timeouts. A timeout or 5xx logs a warning and allows the write, so a flaky external service doesn't block data entry. The trade-off is that the call happens inside the write transaction and holds a DB connection for up to the 2 s read timeout.
- **Seed data only fills empty tables.** User changes survive restarts. Flyway would be the production answer; see below.

### Frontend

- **The student tabs are child routes** (`/students/:id/profile|courses|enroll`) rendered with `mat-tab-nav-bar`, not a plain `mat-tab-group`. `mat-tab-group` has no way to cancel a tab switch before it happens. With routes, switching tabs is a navigation, so one `CanDeactivate` guard plus the shared `ConfirmDialog` covers both tab switches and leaving the page, and every tab has its own URL.
- **Stores are RxJS services.** State lives in a `BehaviorSubject`, is replaced whole on every update, and components only see read-only observables through the async pipe. Loads go through a Subject into `switchMap`, so a stale search or a quickly abandoned student can't overwrite newer data. `enroll`/`unenroll` update the store only after the server confirms, which is how the Enroll tab, the Courses tab and the header stay in sync.
- **Signals are used only for local UI flags** (`saving`, `loadFailed`) and for component inputs. Shared state stays in RxJS, as the brief asked.
- **Layered error handling.** One interceptor turns any failed request into a snackbar, then rethrows it so forms can put `errors[].field` onto the matching controls. Background requests (the async validators) opt out of the snackbar and the loading bar through an `HttpContext` token. The loading bar only appears after 150 ms, so fast responses don't flicker.
- **Async uniqueness validator.** It waits 400 ms before calling the server, and Angular drops the previous check on every keystroke, so only the latest value is sent. It skips the record's own current value, and a failed lookup doesn't block submission because the server re-checks anyway.
- **Zoneless with `OnPush` everywhere** (the Angular 21 default). Forms expose `statusChanges` through the async pipe so results from async validators and the server re-render.
- **Deleting a course with enrollments** uses the `enrolledCount` already on the card to warn in the confirm dialog, then deletes with `force=true`. The server still enforces the rule if the count changed in the meantime.
- **Trade-offs:**
  - The six-course limit is duplicated in the UI (`MAX_COURSES_PER_STUDENT`) for immediate feedback.
  - The Enroll tab loads at most 100 courses (the API page cap).
  - The initial bundle is about 583 kB raw / 136 kB gzipped, mostly Angular Material code needed at startup, so the build's warning threshold is 650 kB.

### Dependencies

Every direct dependency is pinned to an exact version, and the frontend lockfile was resolved with `npm install --before=<date>` so no package is less than a week old. Angular 22 wasn't used because it requires Node 22.22 or newer. Vitest is on 4.0.x because 4.1's optional peer dependencies crash npm 10.9's installer.

## What I'd improve with more time

- **Flyway migrations** instead of `ddl-auto=update` and `data.sql`, and running the PostgreSQL profile in CI with Testcontainers.
- **Optimistic locking** (`@Version` carried through the DTOs, a 409 on stale edits) so two users editing the same student don't silently overwrite each other.
- **Move the catalog call out of the write transaction** and add a circuit breaker and caching (Resilience4j), or switch to `RestClient`.
- **OpenAPI / Swagger UI** generated from the controllers, and TypeScript API types generated from it.
- **Frontend:**
  - Component tests for the tab flows.
  - A Playwright end-to-end suite in CI; the flows were checked manually with a Playwright script.
  - A `beforeunload` prompt for browser refresh or close.
  - A typeahead for large course catalogs.
- **Security:** authentication and role-based access. The API is currently open to anyone who can reach it.
- **Operations:** Actuator health endpoints, structured logging with request ids, a Dockerfile and docker-compose for both apps plus PostgreSQL.
- **Repository hygiene:** a `.gitattributes` so line endings stay consistent across Windows and Unix checkouts.

## Commit history

The work was committed in small, logical steps, oldest first:

1. `first commit`: monorepo skeleton, Maven build, domain entities
2. `feat(backend): add RFC 7807 error handling and shared web helpers`
3. `feat(backend): configure H2 and PostgreSQL profiles, seed data and CORS`
4. `feat(backend): add course catalog client with local stub`
5. `feat(backend): add student CRUD API with search and pagination`
6. `feat(backend): add course CRUD API with guarded deletion`
7. `feat(backend): add atomic enroll and unenroll endpoints`
8. `refactor(backend): enable configuration properties next to their consumers`
9. `build(backend): attach Mockito agent explicitly in Surefire`
10. `test(backend): cover service happy paths and every business rule`
11. `test(backend): verify status codes and ProblemDetail shape in web layer`
12. `test(backend): cover custom queries, formula column and constraints`
13. `test(backend): verify catalog client fallback on errors and timeouts`
14. `test(backend): add end-to-end enroll flow including concurrent requests`
15. `chore(frontend): scaffold Angular 21 workspace with Material`
16. `feat(frontend): add observable store base and HTTP interceptors`
17. `feat(frontend): add confirm dialog, unsaved-changes guard and form helpers`
18. `feat(frontend): add courses feature with card grid and editor`
19. `feat(frontend): add students feature with routed detail tabs`
20. `feat(frontend): wire app shell, lazy feature routes and theme`
21. `docs: add README with setup, API reference and design notes`

The first commit bundles the skeleton and the entities because it was made before this sequence was agreed. The frontend commits add code that the app shell (step 20) wires together, so the frontend builds from step 20 onward.
