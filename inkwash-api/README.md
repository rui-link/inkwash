# inkwash-api

> Backend of the Inkwash CMS — a modular-monolith Spring Boot service providing authentication, RBAC, content management, auditing, monitoring and the built-in license gate.

**English | [中文](README.zh-CN.md)**

## Tech Stack

| Component | Version / Detail |
|-----------|------------------|
| Java | 25 |
| Spring Boot | 4.1.1 |
| MyBatis | mybatis-spring-boot-starter 4.1.0 (annotation-driven SQL, **not** MyBatis-Plus) |
| JSON | Jackson 3 (`tools.jackson`) |
| Databases | MySQL (prod, default) / H2 in MySQL mode (dev) / PostgreSQL driver present, no shipped schema |
| Cache | Caffeine (dev) / Redis (prod) |
| JWT | jjwt 0.13.0 — **HS384**, secret pinned to ≥ 48 bytes |
| Build | Maven (wrapper: `mvnw`) |

Artifact: `top.ruilink:inkwash:0.5.1`. Version: `0.5.1`.

## Module Architecture

inkwash-api is a **single Maven artifact** (`top.ruilink:inkwash`) built as a modular monolith.
The five "modules" below are Java packages under `top.ruilink.inkwash`, decoupled by interfaces:
keep `base` + `security` + `system` + `monitor` and drop `cms` if you are building something that
is not a CMS.

| Module | Responsibility |
|--------|----------------|
| **base** | shared entities, enums, exceptions, utilities, file storage, global exception handling |
| **security** | JWT authentication, multi-strategy login (password+captcha / QR / SMS / OAuth2 GitHub), XSS protection, license enforcement, user profiles |
| **system** | users, accounts, groups, roles, permissions, menus, notices, identity management with method-level `@PreAuthorize` |
| **monitor** | login audit, operation audit, system metrics, health checks, license status |
| **cms** | articles, categories, tags, comments, interactions, sensitive-word filtering, role-based workflow (author / editor / admin) |

## Features

- **Login flows**: password + captcha, QR code scan, SMS code, OAuth2 (GitHub); one user can bind multiple accounts
- **Session**: JWT access (1 day) + refresh (7 day) tokens signed with **HS384**, plus an `HttpOnly` cookie session protected by an origin check in place of `csrf` tokens. Refresh tokens rotate under a shared family ID; replaying a consumed token revokes the whole family. Logout blacklists the token for its remaining lifetime.
- **RBAC**: `User → UserGroup → GroupRole → RolePermission → Permission`, enforced with `@PreAuthorize("hasAuthority('module:resource:action')")`
- **CMS**: draft / commit / review / publish / retract workflow, categories, tags, comments, sensitive-word dictionary
- **Audit & monitoring**: login journal, operation traces (AOP, with sensitive-parameter redaction), Hikari + JMX metrics
- Notices, per-role dashboards, file storage (local backend behind a pluggable interface), i18n (`zh-CN` / `en`), SSE
- **License gate**: signature + machine-binding + tier/module enforcement on admin APIs (see below)

## Requirements

- JDK 25+
- Maven 3.9+ (or use the bundled `./mvnw` / `.\mvnw.cmd` wrapper)

## Configuration

No environment variable blocks startup, but these must be set before any real deployment:

| Variable | Purpose |
|----------|---------|
| `AUTH_JWT_SECRET` | HS384 signing key. Must be ≥ 48 bytes and must not be one of the blacklisted placeholders — `JwtTokenConfig` refuses to start otherwise |
| `MYSQL_PASSWORD` / `REDIS_PASSWORD` | Production datasource and cache |

Other credentials have **development-only defaults** in `src/main/resources/application-dev.yaml`
(`AUTH_JWT_SECRET`, GitHub OAuth placeholders). The default dev JWT
secret is public knowledge — never run `dev` anywhere reachable. `application-prod.yaml` ships a
commented-out PostgreSQL datasource and an SMTP template.

Set `auth.cookie.secure: true` when serving over real HTTPS (the prod profile still defaults to
`false`, flagged with a `TODO` in the file).

## Run — development (H2, in-memory)

```bash
cd inkwash-api
./mvnw spring-boot:run        # Windows: .\mvnw.cmd spring-boot:run
```

- Default profile is `dev`: an in-memory H2 database, schema and seed data applied on startup from `database/`.
- API base: `http://localhost:9080`
- H2 console: `http://localhost:9080/h2-console`
- Actuator: `http://localhost:9080/actuator/health`, `http://localhost:9080/actuator/info`

## Run — production (MySQL)

```bash
cd inkwash-api
./mvnw clean package -DskipTests
java -jar target/inkwash-0.5.1.jar --spring.profiles.active=prod
```

## Database initialization

The `dev` profile runs `base/DatabaseInitializer.java` on startup, applying
`database/h2-schema.sql` then `database/init-data.sql`. Production sets `spring.sql.init.mode: never`,
so you apply the schema yourself:

```bash
mysql -u root -p inkwash < database/mysql-schema.sql
mysql -u root -p inkwash < database/init-data.sql
# optional demo content (孔子 / 孟子 / 李白 / 苏轼 …)
mysql -u root -p inkwash < database/demo-data.sql
```

23 tables across four schema groups: `sys_*` (RBAC, notices, preferences), `mon_*` (login info,
journal), `media_file`, and `cms_*` (articles, taxonomy, comments, interactions, sensitive words).

Seeded by `init-data.sql`:

- Roles: `ROLE_SYSTEM`, `ROLE_ADMIN`, `ROLE_EDITOR`, `ROLE_USER` (92 permissions)
- Groups: `Systems`, `Admins`, `Editors`, `Users`
- Accounts: `admin` and `system`, both ``

> **Security:** these are public development-only defaults — **change the password at first login**
> before any production use.

## Tests & packaging

```bash
./mvnw test
./mvnw clean package -DskipTests
```

74 test source files (JUnit 5 + Mockito, `@MybatisTest` slices against H2 in MySQL mode, plus
`*IT` integration tests and GreenMail for the email path). No Testcontainers or Docker required.

## License integration

- The backend reads `license.dat` from the **classpath** (`src/main/resources/license.dat`). A local
  development licence ships with the repo; generate your own with the CLI before deploying
  (see `inkwash-cmd/README.md`).
- **The app starts without a valid licence.** `LicenseInterceptor` gates requests at runtime and
  returns `403` with a `requireLicense` body — it is not a startup check.
- `LICENSE_PUBLIC_KEY` is the trust anchor: generate your own key pair with `inkwash-cmd keygen`,
  configure the public key, then replace `license.dat` with one signed for your tier.
- Verification: `SHA256withRSA` over the Base64 envelope, then machine binding. Expiry is recorded
  but deliberately **not** enforced — see the `LicenseValidator` javadoc.
- The backend logs its machine code at startup. It comes from `HardwareFingerprinter`, which is a
  byte-identical copy of the one in `inkwash-cmd`, so `inkwash-cmd machine-code` prints the same
  value on the same host. Virtual adapters (docker, vbox, vmnet, WSL, Hyper-V, …) are excluded.
- Tier limits gate modules by live user count: exceeding the cap narrows `/api/system/**`,
  `/api/cms/**` and `/api/monitor/**` accordingly (`personal`/`trial` keep only `system`).
- Gated paths: `/api/system/**`, `/api/cms/**`, `/api/monitor/**` (excluding `/api/license/**`).
  Public reads such as `GET /api/cms/articles` are exempt. Note that `/api/support/file/**`,
  `/api/profile/**` and `/api/preference/**` are **not** licence-gated.

## License

GPL-3.0 — see the repository root `LICENSE`.