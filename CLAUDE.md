# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project state

MiCita backend: the Spring Boot REST API for an appointment-booking product. Cross-cutting infrastructure is in place: hexagonal package layout, JWT login, error handling, Swagger, profiles, and test/coverage tooling. Implemented modules: `auth` (HU-01, HU-02), `business` (onboarding, HU-03/HU-04) and `employee` (registration, HU-17). `service` so far only has the `services` table and the `service_employees` association (ADR-002); its endpoints (HU-09, HU-12) are pending.


## Stack

- Java 26, Spring Boot 4.1.1 (parent POM), Maven via the bundled wrapper.
- Spring MVC, Bean Validation, Spring Data JPA on PostgreSQL, Spring Security as an OAuth2 resource server (JWT), springdoc-openapi 3.x, MapStruct 1.6 (`defaultComponentModel=spring`, `unmappedTargetPolicy=ERROR`).
- Planned, not yet added: the Cloud Firestore Admin SDK, for appointments (ADR-001).
- Base package: `com.micita.micita_project`.

## Commands

Use the Maven wrapper (`./mvnw` in Bash, `.\mvnw.cmd` in PowerShell on Windows):

```bash
./mvnw spring-boot:run                       # run with the dev profile (needs local PostgreSQL)
./mvnw spring-boot:test-run                  # run against a throwaway PostgreSQL container (needs Docker)
./mvnw test                                  # run all tests + JaCoCo report (target/site/jacoco/index.html)
./mvnw verify                                # tests + enforce the JaCoCo coverage minimum
./mvnw test -Dtest=LoginCommandHandlerTest   # single test class
./mvnw test -Dtest=LoginCommandHandlerTest#loginReturnsBearerTokenForAuthenticatedUser  # single method
./mvnw clean package                         # build the executable jar into target/
```

If the local JDK is older than 26, `-Djava.version=<installed version>` (17+) overrides the compiler release for a local build. Don't change `java.version` in `pom.xml`.

No linter or formatter is configured. Existing source uses tab indentation.

## System context (C4)

The API is the single backend behind the following containers and external systems:

- Two Angular SPAs: the **admin dashboard** (manages services, employees, schedules and appointments) and the **public booking form** (4 steps: service → date/time → customer data → confirmation).
- **PostgreSQL**: users, roles and business configuration.
- **Cloud Firestore**: appointments.
- **n8n**: orchestrates WhatsApp Business API traffic in both directions.
  - Inbound: it calls the API with parsed incoming messages.
  - Outbound: the API calls its webhook to send confirmations.
- **Google Calendar**: the API creates events on behalf of the customer.

## Architecture: hexagonal (ports and adapters)

```
core/<module>/
├── domain/                                # 1. DOMINIO PURÍSIMO
│   ├── models/                            # Objetos de dominio puros (sin JPA)
│   ├── exceptions/                        # Excepciones de negocio
│   └── services/                          # SOLO lógica que involucra múltiples modelos (Ej: CalcularInteresCompuesto)
│
├── application/                           # 2. CAPA DE APLICACIÓN (Casos de Uso)
│   ├── ports/                             # Los puertos viven aquí porque definen qué hace la aplicación
│   │   ├── in/                            # Interfaces de Casos de Uso (Ej: CreateUserUseCase)
│   │   └── out/                           # Interfaces de salida (Repositories, Gateways) divididos en command/query
│   ├── command/                           # Handlers (Casos de uso de escritura) que implementan ports/in
│   ├── query/                             # Handlers (Casos de uso de lectura) que implementan ports/in
│   ├── dto/request|response/              # Objetos de transferencia de datos
│   └── mappers/                           # MapStruct DTO <-> Domain
│
└── infrastructure/                        # 3. INFRAESTRUCTURA (Detalles técnicos)
    ├── adapters/in/rest/                  # Controllers web (Command / Query)
    ├── adapters/out/security/             # Adaptadores de salida que no son BD (JWT, Spring Security)
    └── adapters/out/persistence/jpa/      # Base de datos
        ├── entities/                      # Entidades JPA (@Entity)
        ├── repositories/                  # Interfaces Spring Data JPA
        ├── command/                       # Implementación de ports/out/command (Guarda en DB)
        ├── query/                         # Implementación de ports/out/query (Busca en DB)
        └── mappers/                       # MapStruct Entity <-> Domain (`*BDMapper`)

common/shared/                             # Transversal, fuera de core
├── config/
├── enums/
├── exception/                             # Excepciones comunes + GlobalExceptionHandler
└── security/
```



## API conventions

- **Routes:** everything lives under `/api`, and resource names are in English: `/api/auth/login`, `/api/business/onboarding`, `/api/services`, `/api/employees/{id}/schedule`, `/api/appointments`, `/api/available-slots`, `/api/messages/incoming`. Sub-resources follow suit: `/{id}/status`, `/services/{id}/employees`, `/{id}/future-appointments`, `/appointments/{id}/notification`. The sequence diagrams use the same names but omit the `/api` prefix, so add it when reading them. Java code, DTO field names and error messages are in English too.
- **Status codes used by the flows:**

  | Code | When |
  |---|---|
  | 400 | Validation failure. The `ProblemDetail` carries an `errors` map of field → message. Ids that are unknown, of another business or deleted (`InvalidReferencesException`) also add `rejectedIds`. |
  | 401 | Bad credentials, or a missing/expired token. |
  | 403 | The caller lacks permission. |
  | 404 | The resource doesn't exist. |
  | 409 | Duplicate service name or duplicate employee email/phone, or a time slot that is no longer available. |
  | 502 | Google Calendar failed. |

  401s raised by the security filter itself (missing or invalid token) come from Spring Security and have an empty body.
- **Swagger:** springdoc 3.x, served at `/swagger-ui.html` and `/v3/api-docs` (both disabled in `prod`).
  - `OpenApiConfig` applies the `bearerAuth` scheme globally. Public endpoints opt out with an empty `@SecurityRequirements`.
  - Annotate controllers with `@Tag`, endpoints with `@Operation` and `@ApiResponse`, and DTOs with `@Schema`. Document error responses with `schema = @Schema(implementation = ProblemDetail.class)`.

## Security and auth

Target flow, from the "Navegación general del administrador" sequence diagram and HU-01/HU-02:

- `POST /api/auth/register` is public.
  - Required fields: first name, last name, address, phone, email, username, password.
  - Email and username must be unique.
  - The password needs at least 6 characters with lowercase, uppercase and special characters.
  - New users start with **no roles**.
- `POST /api/auth/login` returns a short-lived **access token** (JWT) and a **refresh token**. Only a hash of the refresh token is stored in PostgreSQL.
  - The access token carries `roles`. It must also carry a `hasBusiness` flag: when it's false, the frontend sends the user to `POST /api/business/onboarding`.
- `POST /api/auth/refresh` issues a new access token. `POST /api/auth/logout` revokes the refresh token.

Implemented today:

- Registration (HU-01).
  - Validation lives on `RegisterUserRequest`.
  - Emails are stored lowercased, so uniqueness is case-insensitive. Usernames are kept as typed.
  - Duplicates raise `DuplicateUserException` (409). That includes races caught by the DB unique constraints.
- Users live in PostgreSQL: `users` plus `user_roles`, which holds bare role names such as `ADMIN`.
  - Passwords are hashed with the delegating `PasswordEncoder`, which is BCrypt and stores a `{bcrypt}` prefix.
  - BCrypt caps passwords at 72 characters.
- Login runs through `JpaUserDetailsService`, which returns `AccountUserDetails` so the user id reaches the token issuer.
- The bootstrap user (`micita.security.bootstrap-user.*`) is seeded at startup with the ADMIN role if its username doesn't exist yet. It's the only admin until role management exists.
- Stateless HS256 JWTs signed by the app (`JwtTokenIssuer`) and validated by the resource server.
  - Roles go in the `roles` claim with the `ROLE_` prefix applied by the issuer, so `hasRole(...)` and `@PreAuthorize` work as usual. `@EnableMethodSecurity` is on, and a denied `@PreAuthorize` returns a 403 `ProblemDetail`.
  - `sub` holds the username and the `uid` claim holds the user's UUID. Read it with `CurrentUserId.from(jwt)`, which gives 401 if it's missing. Anything owned by the user (such as the business) is resolved from it, never from a request parameter.
  - The `hasBusiness` claim comes from `BusinessQueryPort`. `BusinessModuleQueryAdapter` answers it through the business module's `BusinessOwnershipQueryUseCase`, so it reflects PostgreSQL at login and refresh.
- Business onboarding (HU-03). `POST /api/business/onboarding` (authenticated) creates the caller's business in `businesses`: `ACTIVE`, time zone `America/Bogota`, one per user (`owner_id` unique, so a second one or a race gives 409 `BusinessAlreadyExistsException`).
  - The current access token keeps `hasBusiness=false`: the frontend calls `POST /api/auth/refresh` after onboarding to get one with `true`.
- Business required (HU-04). `BusinessRequiredInterceptor`, registered by `BusinessRequiredWebConfig` on `/api/**`, answers 403 `ProblemDetail` with `code: "BUSINESS_REQUIRED"` (`ForbiddenException`) when a JWT-authenticated user has no business.
  - It is on by default, so new endpoints are covered. Exempt: `/api/auth/**` and `/api/business/onboarding` (`EXEMPT_PATTERNS`). Anonymous requests to public endpoints pass through.
  - Ownership is read from PostgreSQL on every request, not from the `hasBusiness` claim, so access opens right after onboarding.
  - `BusinessRequiredOpenApiCustomizer` adds that 403 to every secured, non-exempt operation in Swagger.
  - The bootstrap ADMIN is not exempt: it also needs a business to use protected endpoints.
  - Modules that scope data by business get its id from `BusinessOwnershipQueryUseCase.requireBusinessIdOf(uid)` (403 `BUSINESS_REQUIRED` if there is none).
- Employees (HU-17). `POST /api/employees` creates an employee in the caller's business (`employees`, `ACTIVE`/`INACTIVE`; `DELETED` is the soft delete).
  - Email (stored lowercased) and phone must not be used by another non-deleted employee of the business (409 `DuplicateEmployeeException`). The check lives in the use case: the partial unique indexes wait for migrations, so two concurrent requests can still slip through.
  - `serviceIds` are written to `service_employees` only through the service module's `AssignEmployeeServicesUseCase` (ADR-002), in the same transaction. Unknown, foreign or deleted services give 400 with `rejectedIds` and nothing is stored.
  - `EmployeeQueryUseCase.findAssignableIds` is the port the service module will use to validate `employeeIds` (HU-12).
- Sessions (HU-02). Access tokens last 15 min (`JWT_EXPIRATION`) and refresh tokens 7 days (`JWT_REFRESH_EXPIRATION`).
  - Login, and every refresh, goes through `SessionTokensIssuer`, which issues an access token plus an opaque refresh token (256 random bits).
  - Only the token's SHA-256 is stored, in `refresh_tokens`. It's looked up by that hash, so it is deliberately not BCrypt.
  - `POST /api/auth/refresh` **rotates** the token: the one presented is revoked with a conditional update, so only one concurrent use wins, and a new pair is returned. Unknown, expired or revoked tokens give 401 (`InvalidRefreshTokenException`).
  - `POST /api/auth/logout` revokes the refresh token and returns 204 even for unknown tokens.
  - Both endpoints are public and take the refresh token in the body, not a JWT.

Still missing:

- An authentication mechanism for the n8n → API webhook. It won't carry a user JWT.
- Revoking all of a user's refresh tokens (for example, on password change), and purging expired rows from `refresh_tokens`.

## Persistence: ADR-001 (hybrid)

PostgreSQL holds users, refresh tokens, businesses, services, employees, `service_employees` (owned by the service module, ADR-002), schedules, breaks, non-working days, minimum notice, WhatsApp conversations and `processed_message`, all accessed through JPA. **Appointments** and the per-employee-per-day lock documents live in **Cloud Firestore**, accessed through the Admin SDK behind `AppointmentRepositoryPort` / `FirestoreAppointmentAdapter`.

Rules that cut across modules:

- **No cross-store transactions.** Read the PostgreSQL configuration *before* the Firestore transaction. Appointments that already exist are kept even if the configuration changes later.
- **Soft deletes only** for services and employees, because Firestore can't enforce foreign keys. Each appointment stores a `snapshot` of service name, duration, price and employee name.
- **Booking conflicts:**
  - Booking runs inside `runTransaction`. It reads the lock document `businesses/{businessId}/employeeDays/{employeeId}_{yyyyMMdd}`, then the employee's appointments for that day, and checks for overlap using half-open intervals `[start, end)`. Back-to-back appointments don't collide.
  - It writes the appointment and bumps the lock's `version`.
  - Rescheduling touches both the source and the target lock documents.
  - Statuses that occupy the agenda: `PENDING`, `CONFIRMED`, `IN_PROGRESS`.
- **Time:** `date` is computed in the business's time zone (`business.time_zone`, default `America/Bogota`). `startAt` and `endAt` are stored in UTC. Appointments never cross midnight.
- **Notifications:**
  - An appointment is created with `notificationStatus=PENDING`.
  - After commit, the API calls n8n `POST /webhook/notificar-cita` asynchronously.
  - n8n reports back on `PATCH /api/appointments/{id}/notification` with `SENT` or `FAILED`.
  - A Spring `@Scheduled` job retries `PENDING`/`FAILED` appointments up to N attempts (suggested: 3 attempts, 5 minutes apart), then alerts the admin.
- **Cross-store queries** such as "future appointments of a service" query Firestore by `serviceId` or `employeeId` first, then enrich the results from PostgreSQL.
- **Firestore limit:** an `in` filter accepts at most 30 values, so batch longer employee lists.
- **WhatsApp ingestion** deduplicates on `message.id` (`processed_message`) before processing.

The Firestore document model and composite indexes are in ADR-001. Still open there: the employee-assignment policy (default: first free employee, with ties broken by the lowest load that day) and appointment retention.

## Configuration profiles

- `application.properties` holds shared settings and sets `dev` as the default profile.
- `application-dev.properties` has local defaults: PostgreSQL `micita/micita@localhost:5432/micita`, `ddl-auto=update`, and a dev JWT secret.
- `application-prod.properties` reads everything from environment variables with no fallbacks, uses `ddl-auto=validate` and disables Swagger.
- `src/test/resources/application-test.properties` is for tests.
- Environment variables: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` (≥ 32 bytes, enforced at startup by `JwtProperties`), `JWT_EXPIRATION` (access token, default `15m`), `JWT_REFRESH_EXPIRATION` (refresh token: required in prod, `7d` in dev), `BOOTSTRAP_USERNAME`, `BOOTSTRAP_PASSWORD`.
- There are no schema migrations yet, so prod's `validate` needs the schema (`users`, `user_roles`, `refresh_tokens`, `businesses`, `services`, `service_employees`, `employees`) to already exist.

## Testing (JUnit + Mockito)

Every user story ships with tests derived from its acceptance criteria. JUnit Jupiter and Mockito come through `spring-boot-starter-webmvc-test`, so don't add them to `pom.xml` separately. Boot 4.1 manages JUnit 6.x, and the Jupiter API is the same.

- **Use cases (`application`):** `@ExtendWith(MockitoExtension.class)`, with `ports.out` interfaces as `@Mock` and the handler under test as `@InjectMocks`. No Spring context. See `LoginCommandHandlerTest`.
- **Driven adapters:** unit-test the translation logic with Mockito (`SpringSecurityCredentialsAuthenticatorTest`), or with the real library where that's cheap (`JwtTokenIssuerTest`).
- **Controllers:** `@WebMvcTest(XController.class)` + `@Import(SecurityConfig.class)` + `@ActiveProfiles("test")`, with the `ports.in` use case mocked through `@MockitoBean`.
  - `@MockBean` was removed in Boot 4, so don't use it.
  - `@WebMvcTest` now lives in `org.springframework.boot.webmvc.test.autoconfigure`.
  - Use `SecurityMockMvcRequestPostProcessors.jwt()` for authenticated requests, adding the `uid` claim (`jwt().jwt(token -> token.claim(SecurityConfig.USER_ID_CLAIM, id))`) when the endpoint needs the user id or is guarded by the HU-04 interceptor.
  - The HU-04 interceptor is part of every web slice, so also mock `BusinessOwnershipQueryUseCase` with `@MockitoBean` (stub it to `true` unless the test is about onboarding). `BusinessProbeController` and `AdminProbeController` are test-only private endpoints.
- **Integration (`@SpringBootTest`):** use `@Import(TestcontainersConfiguration.class)` (PostgreSQL through `@ServiceConnection`) and `@Testcontainers(disabledWithoutDocker = true)`, so these tests are skipped rather than failed when Docker isn't running.
  - Testcontainers is 2.x, so container classes live in module packages (`org.testcontainers.postgresql.PostgreSQLContainer`).
  - ADR-001 calls for running Firestore adapters against the **Firestore emulator** once they exist.
- **Coverage:** JaCoCo enforces a minimum bundled line coverage (`jacoco.minimum.line.coverage` in `pom.xml`, currently 0.80) during `verify`. `MicitaProjectApplication` is excluded from the measurement.
- Test packages mirror main packages.
