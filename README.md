<div align="center">

# 水墨 · Inkwash

### A batteries-included content platform you can take apart — Spring Boot 4 modular monolith + dual Vue 3 frontends

English | [中文](README.zh-CN.md)

[![License: GPL-3.0](https://img.shields.io/badge/License-GPL--3.0-blue.svg?style=flat-square)](LICENSE)
[![Version](https://img.shields.io/badge/version-0.5.1-informational?style=flat-square)]()
[![Java](https://img.shields.io/badge/Java-25-orange?style=flat-square)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-brightgreen?style=flat-square)]()
[![Vue](https://img.shields.io/badge/Vue-3.5-42b883?style=flat-square)]()
[![Docs](https://img.shields.io/badge/docs-system--design-8b949e?style=flat-square)](reference/system-design.md)
[![Commercial support](https://img.shields.io/badge/commercial-support_available-dorange?style=flat-square)](COMMERCIAL-LICENSE.md)
[![PRs welcome](https://img.shields.io/badge/PRs-welcome-brightgreen?style=flat-square)](CONTRIBUTING.md)

</div>

---

## Four ways to log in, one identity.

Password + captcha, QR scan, SMS code, GitHub OAuth2 —
each is a **separate `sys_account` record** pointing at the same `sys_user`. All four are bound to it;
use whichever you like.

The browser session rides an `HttpOnly` Cookie. **The token never lands in `localStorage` and is never
exposed to any JavaScript variable.**

This is not four login pages. It is one identity with four doors. Below are the five concrete design
decisions behind it — each solves a real problem, rather than existing to show off a stack.

---

## Running in 30 seconds

The default `dev` profile needs **neither MySQL nor Redis** — in-memory H2 plus Caffeine, with the schema
and seed data applied automatically on first start.

```bash
# 1 · Start the backend (Java 25 · Maven wrapper included)
cd inkwash-api
./mvnw spring-boot:run          # Windows: .\mvnw.cmd spring-boot:run

# 2 · Start the frontends (Node ≥ 22.12 · pnpm 9)
cd ../inkwash-site
pnpm install
pnpm dev:admin                  # console → http://localhost:9090
pnpm dev:web                    # portal  → http://localhost:9089
# 3 · Sign in
```

| User | Password |
|---|---|
| `admin` | `Super99*` |
| `system` | `Super99*` |

→ API on `http://localhost:9080` · H2 console at `/h2-console` · Actuator health at `/actuator/health`

> **Change these two passwords before any deployment that is not a laptop.** They are seeded by
> `database/init-data.sql` and are public development defaults — printed here so the quick start works.

---

## 1 · Refresh tokens that detect theft

Every refresh rotates the token under the same **family ID**. Replaying an already-consumed refresh
token revokes the **entire token family** and forces re-authentication — rather than rejecting just
that one request.

On logout, a blacklist entry is written keyed by token, with a TTL equal to that token's remaining
lifetime. As the token expires naturally, so does its blacklist entry — no cleanup job required.

Tokens themselves are signed with **HS384** via jjwt. The signing key length is validated at startup:
it must not be a well-known placeholder value and must be ≥ 48 bytes, or the application will not start.

> **Why a Cookie instead of `localStorage`?** An `HttpOnly` token cannot be read by any script, so a
> single XSS cannot steal the session. The cost is CSRF protection — Inkwash validates the `Origin`
> header instead of adding a token round-trip.

---

## 2 · A permission string you can read aloud

```java
@PostMapping("/articles/{id}/review")
@PreAuthorize("hasAuthority('cms:article:review')")   // module:resource:action
```

**84** seeded permissions (`system` 38 · `cms` 39 · `monitor` 7), **4** roles, and **one** resolution chain:

```
sys_user → sys_user_group → sys_group → sys_group_role
         → sys_role → sys_role_permission → sys_permission
```

There is **no shortcut** from a user to a role — it must go through a group. If a permission cannot be
reached via a group, it does not exist for that user — so answering "why can this person see this
article?" takes reading three tables, not explaining a resolution algorithm.

The menu tree **is** the result of that chain. Add a permission to a role and the page appears — no
frontend redeploy.

---

## 3 · The editorial workflow is a state machine

Six states; each action has exactly one legal transition:

| From | Action | To |
|---|---|---|
| `DRAFT(1)` | `submit` | `PENDING(2)` |
| `PENDING(2)` | `approve` | `APPROVED(3)` |
| `PENDING(2)` | `reject` | `REJECTED(4)` |
| `REJECTED(4)` | `resubmit` | `PENDING(2)` |
| `APPROVED(3)` | `publish` | `PUBLISHED(5)` |
| `PUBLISHED(5)` | `retract` | `RETRACTED(6)` |

![Article lifecycle](reference/diagrams/article-state.png)

Enforcement lives in the **domain object** (`cms/domain/Article.java`), not in the controller. Each
action validates the source state itself, so an illegal transition is **unreachable** — not merely
hidden by the UI.

Three rules: an author may only manage their own articles; **review must be done by someone else**
(an author cannot review their own draft); an administrator may retract published content.

Every transition is a **compare-and-set update** — two concurrent reviewers cannot both succeed. This
is not application-level locking; it is a conditional update at the SQL layer.

---

## 4 · XSS filtering that knows what a field means

Most filters have two modes: escape everything, or pass everything through. Inkwash handles each field
by **field name**:

| Field | Treatment |
|---|---|
| `content` | Treated as Markdown, formatting preserved |
| `title` / `summary` / `keywords` | Treated as rich text, inline markup allowed |
| Passwords, captcha codes, tokens | **Passed through verbatim** — escaping would corrupt them |

Built on the OWASP Java HTML Sanitizer. This is field-aware, not a blanket escape.

User-submitted content gets a second pass on the frontend: `markdown-it` for rendering, DOMPurify for
 param($m) 'sanitiz' + $m.Groups[1].Value .

---

## 5 · The business layer can be deleted outright

Between `cms` (articles, comments, categories, tags, interactions, review) and the platform layer
(`base` / `security` / `system` / `monitor`) there is a **one-way dependency**. Delete the entire `cms`
directory and the remaining four packages still pass `./mvnw clean package`.

| You want… | Then use… |
|---|---|
| **A content platform running today** | The whole thing as-is — `cms` already implements articles, the workflow, comments, and categories/tags |
| **Your own product on a mature foundation** | Just the four platform packages. They have no idea what an article is; you write the module |

`cms` is a **reference implementation of a business module**. Read it, copy it, or delete it entirely.

![Overall architecture](reference/diagrams/architecture.png)

Four tests turn red when you cross that line — [`BaseLayerBoundaryTest`](inkwash-api/src/test/java/top/ruilink/inkwash/base/domain/BaseLayerBoundaryTest.java)
blocks any `base → security` import, [`PackageLayoutTest`](inkwash-api/src/test/java/top/ruilink/inkwash/base/PackageLayoutTest.java)
guards the package-role separation, [`ComponentWiringTest`](inkwash-api/src/test/java/top/ruilink/inkwash/base/ComponentWiringTest.java)
blocks package moves that compile but explode at runtime, and
[`SchemaParityTest`](inkwash-api/src/test/java/top/ruilink/inkwash/base/SchemaParityTest.java)
catches schema drift between H2 and MySQL.

---

## Interface

**Console** — Navigation is driven by the server. Multiple tabs, `keep-alive`, two switchable layouts,
and five themes (`ink-dark` is genuine dark mode, not an inverted light palette).

<img src="reference/images/inkwash-01.png" width="800" alt="Console dashboard">

<div align="center"><sub>Dashboard — the home page differs by role: editors see pending-review count, administrators see the user total</sub></div>

<img src="reference/images/inkwash-03.png" width="800" alt="Review workflow">

<div align="center"><sub>Review — approve or reject with a comment, side by side</sub></div>

**Portal** — Reading requires no login, responsive down to 360px.

<img src="reference/images/inkwash-05.png" width="800" alt="Portal desktop">

<div align="center"><sub>Portal — comment, like, favorite, and share once signed in</sub></div>

<img src="reference/images/inkwash-06.png" width="240" alt="Portal mobile">

<div align="center"><sub>Mobile at 360px</sub></div>

> **SMS is a wired-up extension point, not a working channel.** `POST /api/auth/sms/code` validates the
> captcha, generates a 6-digit code and stores it, but **no SMS provider is integrated**, so messages
> are never actually delivered. The login page keeps the tab but greys it out and marks it
> "not yet available" — making the gap visible rather than silently failing.

---

## Tech stack

**Backend** — Java 25 · Spring Boot 4.1.1 · MyBatis 4.1.0 with hand-written annotated SQL · Jackson 3 ·
jjwt 0.13.0 (HS384) · H2 (dev) / MySQL (prod) · Caffeine / Redis · hutool-captcha · ZXing ·
OWASP Java HTML Sanitizer · AspectJ · JUnit 5 + Mockito + GreenMail

**Frontend** — Vue 3.5 · Vite 8 · Element Plus 2.14 · Tailwind CSS 4 · Pinia 4 · Vue Router 5 ·
vue-i18n 11 · ECharts 6 · markdown-it 14 · md-editor-v3 6 · cropperjs · axios + axios-retry ·
Vitest 4 · Playwright · oxlint + oxfmt

**CLI** — Java 25 · Picocli 4.7.7 · Jackson 3 · Jansi

**Test surface** — 98 backend test source files · 24 frontend spec files · 31 CLI test cases.
H2 is a first-class development target, so **running the tests requires no Docker**.

---

## Feature tour

| Package | Responsibility |
|---|---|
| **base** | Base entities, RFC 7807 error handling, cache abstraction, i18n, local file storage with path-traversal protection |
| **security** | JWT, token rotation with replay detection, `HttpOnly` Cookie sessions, Origin-based CSRF, field-aware XSS filtering, 4 login strategies |
| **system** | `user`, `account`, `identity`, `group`, `role`, `permission`, `menu`, `notice`, `preference` |
| **monitor** | Login logs, AOP operation audit, Hikari + JMX metrics, health probes |
| **cms** | Articles, categories, tags, comments, interactions, sensitive words, editorial workflow, dashboard statistics — **pluggable** |

**Console** — Two layouts · Multiple tabs with `keep-alive` · Articles (list / edit / review / publish) ·
Users, groups, roles, permissions, menus, notices · Login logs, operation logs, live Actuator metrics ·
Five themes

**Portal** — Public browsing · `markdown-it` + DOMPurify · Comment / like / favorite / share ·
Four list views under "My Space" · Collapsible navigation below 768px

---

## Configuration

| Variable | Required | Purpose |
|---|:---:|---|
| `AUTH_JWT_SECRET` | **Required in prod** | HS384 signing key. Must be ≥ 48 bytes and not a placeholder value, or startup fails |
| `MYSQL_PASSWORD` | **Required in prod** | Production datasource |
| `REDIS_PASSWORD` | **Required in prod** | Production cache |
| `LICENSE_PUBLIC_KEY` | Optional | RSA public key used to validate `license.dat` |
| `OAUTH2_GITHUB_CLIENT_ID` / `_SECRET` | Optional | Enables the GitHub login tab |
| `VITE_API_URL` | Optional | Frontend API base URL |

> **Never commit credentials.** The root `.gitignore` already excludes `*.pem` and `license.dat`.

To raise the user limit, request a license for your host:

```bash
cd inkwash-cmd && mvn clean package -DskipTests
java -jar target/inkwash-cmd-0.5.1.jar machine-code    # 32-character hex fingerprint
```

Send that fingerprint to **dyllons@163.com** and you will receive a signed `license.dat`.
Place it under the API's external config path and restart. The application **runs without a license**.

---

## Repository layout

```
inkwash/
├── inkwash-api/          Spring Boot backend · base/ security/ system/ monitor/ cms/
├── inkwash-cmd/          License CLI (picocli)
├── inkwash-site/         pnpm monorepo · share/ admin/ web/
├── reference/
│   ├── system-design.md  Normative design document (1901 lines · includes the D1–D12 change-cost list)
│   ├── diagrams/         Static exports of the design diagrams
│   └── images/           Product screenshots
└── LICENSE               GPL-3.0
```

Per-project READMEs: [inkwash-api](inkwash-api/README.md) ·
[inkwash-cmd](inkwash-cmd/README.md) · [inkwash-site](inkwash-site/README.md)

---

## Contributing

Please read **[CONTRIBUTING.md](CONTRIBUTING.md)** first — [中文](CONTRIBUTING.zh-CN.md) —
for the verified commands per subproject, the code style rules, the D1–D12 design constraints that must
not be casually broken, and the licensing terms.

```bash
cd inkwash-api && ./mvnw test            # backend
cd inkwash-cmd && mvn test               # CLI
cd inkwash-site && pnpm lint             # frontend
cd inkwash-site && pnpm test:unit --run  # frontend tests
```

Java is indented with **TABs**; JS/Vue with 2 spaces. `pnpm test:unit` needs `--run`, otherwise it
enters watch mode.

---

## Supporting this project

If you find this project useful, you are welcome to support it. Your support gives me more time for
maintenance and new feature work.

> **The QR codes below accept donations only.** They are **not** a way to buy a license, a paid tier,
> or any commercial entitlement. A donation grants no rights. Commercial terms are in the
> [license document](COMMERCIAL-LICENSE.md); nothing else is offered.

<details>
<summary>🇨🇳 Alipay / WeChat</summary>
<br>

| Alipay | WeChat |
|:---:|:---:|
| <img src="https://raw.githubusercontent.com/rui-link/inkwash/a8e97edb6404d172e59e919088b2bfc83ff2ad4e/.github/sponsor/alipay.png" width="240" alt="Alipay QR code"> | <img src="https://raw.githubusercontent.com/rui-link/inkwash/a8e97edb6404d172e59e919088b2bfc83ff2ad4e/.github/sponsor/wechat.png" width="240" alt="WeChat QR code"> |

</details>


Or just leave a ⭐️ Star — every one of them keeps me going.

---

## License

Inkwash is released under the **GNU General Public License v3.0** — full text in [`LICENSE`](LICENSE).

If you want to be exempt from GPL-3.0's copyleft obligations, Inkwash also offers a
[COMMERCIAL LICENSE](COMMERCIAL-LICENSE.md); contact `dyllons@163.com` about licensing.

Using Inkwash under GPL-3.0 — **including for commercial purposes** — requires no permission at all.

| You want… | Then use… | What you owe |
|---|---|---|
| Use, modify, and deploy Inkwash, **including commercially** | **GPL-3.0** | Your own modifications must be published under GPL-3.0. No user cap, no feature limits, no permission request |
| Be exempt from the copyleft obligation to publish your modifications | [Commercial License](COMMERCIAL-LICENSE.md) | No copyleft obligation. The licensed scope is defined by the signed agreement |

> **No warranty.** Under either path, **no software warranty is provided** — GPL-3.0 sections 15–16
> (disclaimer of warranty and limitation of liability) continue to apply. A commercial license likewise
> implies no warranty: it is a permission to use the software, not a guarantee of availability or
> fitness for purpose. See the disclaimer in [`COMMERCIAL-LICENSE.md`](COMMERCIAL-LICENSE.md).

The `license.dat` supplied with a commercial license is **a record of entitlements, not DRM**.
Validation happens inside your own process, using the public key distributed with the source. Anyone can
read that validation code, and anyone sufficiently motivated can modify it. We make no claim that it
resists tampering.

---

## Acknowledgements

[Spring Boot](https://spring.io/projects/spring-boot) · [Spring Security](https://spring.io/projects/spring-security) ·
[MyBatis](https://mybatis.org/mybatis-3/) · [Element Plus](https://element-plus.org/) ·
[Tailwind CSS](https://tailwindcss.com/) · [Pinia](https://pinia.vuejs.org/) ·
[Vue Router](https://router.vuejs.org/) · [ECharts](https://echarts.apache.org/) ·
[md-editor-v3](https://github.com/VueDevTools/md-editor-v3) ·
[OWASP Java HTML Sanitizer](https://github.com/OWASP/java-html-sanitizer) ·
[ZXing](https://github.com/zxing/zxing) · [JJWT](https://github.com/jwtk/jjwt) ·
[Caffeine](https://github.com/ben-manes/caffeine) · [JUnit 5](https://junit.org/junit5/)
