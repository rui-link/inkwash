# Contributing to Inkwash

[中文版](CONTRIBUTING.zh-CN.md)

Thank you for considering a contribution. Inkwash is a modular monolith — Spring Boot 4 on the
back end, Vue 3 on the front — and the design makes a number of decisions that are deliberate and
expensive to reverse.

**This document is normative.** It describes how the project *should* be changed. Where the code
currently deviates, that is a bug in the code, not a licence to add more of the same.

## How to read this document

Every rule below carries one of three markers. They tell you what will actually happen to your pull
request, which matters more than what we would prefer:

| Marker | Meaning |
|---|---|
| 🔒 **Enforced** | A tool or test will reject your change. It will not merge until you fix it. |
| 📋 **Convention** | Not machine-checked. A reviewer will hold you to it. |
| ⏳ **Not yet set up** | Intended, but no automation exists yet. Nothing will stop you. |

If you find yourself wanting to violate one, open an issue **before** writing the code. A design
change that comes with a discussion is welcome; one that arrives as a surprise in a diff is not.

## Table of contents

1. [Code of Conduct](#1-code-of-conduct)
2. [Before you start — licensing](#2-before-you-start--licensing)
3. [Prerequisites](#3-prerequisites)
4. [Commands](#4-commands)
5. [Repository layout](#5-repository-layout)
6. [Contribution workflow](#6-contribution-workflow)
7. [Commit messages](#7-commit-messages)
8. [Code style](#8-code-style)
9. [Design constraints you must not casually break](#9-design-constraints-you-must-not-casually-break)
10. [Testing](#10-testing)
11. [Pull request checklist](#11-pull-request-checklist)
12. [Security vulnerabilities](#12-security-vulnerabilities)
13. [What is not automated](#13-what-is-not-automated)
14. [License](#14-license)

---

## 1. Code of Conduct

⏳ **Not yet set up** — a standalone `CODE_OF_CONDUCT.md` does not exist yet. The terms below apply
in the meantime; they will be moved out into their own file once there is enough activity to justify
it.

Inkwash adopts the [Contributor Covenant v2.1](https://www.contributor-covenant.org/version/2/1/code_of_conduct/code_of_conduct.md).
In short: be respectful; assume good faith; critique code, not people; no harassment or personal
attacks; accept that a maintainer may decline your contribution.

Report unacceptable behaviour to `dyllons@163.com`. Reports go to the maintainer privately. We will
respect the confidentiality of the reporter.

---

## 2. Before you start — licensing

📋 **Convention** · ⏳ **Not yet set up** (no CLA bot)

This project is **dual-licensed**: [GPL-3.0](LICENSE) for everyone, plus a
[commercial licence](COMMERCIAL-LICENSE.md) for organisations that cannot meet GPL's copyleft
obligations.

By contributing you agree that:

- your contribution is licensed under **GPL-3.0**, and
- the project may additionally offer that contribution under the commercial terms in
  `COMMERCIAL-LICENSE.md`.

**Check with your employer before you start.** Some employers require that contributions to
open-source work be pre-approved in writing, or that you retain certain rights. If yours does, raise
it in an issue *before* you write the code. It is far easier to sort out in advance than to unpick a
merged commit later.

You keep the copyright in your own contribution. This project does **not** require a CLA today.
⏳ A CLA bot (likely `cla-assistant`) is planned, because the dual-licence structure currently assumes
the maintainer is the sole copyright holder — which stops being true the moment outside code lands.
If a CLA is introduced later it will be announced well in advance and will not be retroactively
applied to work already merged.

---

## 3. Prerequisites

🔒 **Enforced** (build fails without these)

| Tool | Version | Where |
|---|---|---|
| JDK | 25 | `inkwash-api`, `inkwash-cmd` |
| Node.js | ≥ 22.12 | `inkwash-site` |
| pnpm | 9.0.0 (`packageManager` field pins this) | `inkwash-site` |
| Maven | 3.9.16 via `./mvnw` | `inkwash-api` |
| MySQL / Redis | **not needed** | the `dev` profile uses in-memory H2 + Caffeine |

The `dev` profile needs no external database. H2 is a **first-class development target**, not a
convenience (see [D9](#9-design-constraints-you-must-not-casually-break)). Do not add a solution that
requires Docker or Testcontainers to run the tests.

> **Note** `inkwash-api` ships a Maven wrapper. **`inkwash-cmd` does not** — use a system `mvn` there.

Full quick start: [README §5](README.md#5-getting-started).

---

## 4. Commands

🔒 **Enforced** for build, test, and lint. These are the commands the project runs; do not
substitute your own.

### Backend — `inkwash-api` (Maven)

| Purpose | Command |
|---|---|
| Run (dev, H2) | `./mvnw spring-boot:run`  ·  Windows: `.\mvnw.cmd spring-boot:run` |
| Build | `./mvnw clean package -DskipTests` |
| Test | `./mvnw test` |
| Single test class | `./mvnw test -Dtest=ArticleServiceTest` |
| Single test method | `./mvnw test -Dtest=ArticleDomainTest#someMethodName` |

API on `http://localhost:9080`. H2 console at `/h2-console`. Actuator health at `/actuator/health`.

There is no Java linter or formatter configured. Do not add one as part of an unrelated change.

### CLI — `inkwash-cmd` (Maven, no wrapper)

| Purpose | Command |
|---|---|
| Build | `mvn clean package -DskipTests` |
| Test | `mvn test` |
| Run | `java -jar target/inkwash-cmd-0.5.1.jar --help` |

### Frontend — `inkwash-site` (pnpm workspace)

Run these from `inkwash-site/`, not from an individual package.

| Purpose | Command |
|---|---|
| Install | `pnpm install` |
| Dev — console | `pnpm dev:admin` → `http://localhost:9090` |
| Dev — portal | `pnpm dev:web` → `http://localhost:9089` |
| Build | `pnpm build:share` then `pnpm build:admin` / `pnpm build:web` |
| Lint | `pnpm lint` |
| Lint, autofix | `pnpm lint:fix` |
| Format | `pnpm format` |
| Unit tests | `pnpm test:unit --run` |
| One spec | `pnpm test:unit --run packages/share/tests/utils/format.spec.js` |

> ⚠️ **`pnpm test:unit` needs `--run`.** The script is bare `vitest`, which defaults to watch mode
> and will hang a CI-style invocation. Always pass `--run`.

Both dev servers proxy `/api` and `/uploads` to `http://localhost:9080`.

---

## 5. Repository layout

```
inkwash/
├── inkwash-api/          Spring Boot 4 backend (modular monolith)
├── inkwash-cmd/          Picocli licence-signing CLI
├── inkwash-site/         pnpm workspace — Vue 3, 3 packages
├── reference/            system-design.md — the normative design document
└── .github/CODEOWNERS    review routing
```

Inside `inkwash-api`, the package split is not cosmetic — it is enforced by tests:

| Package | Role |
|---|---|
| `base` | shared primitives; **must not depend on `security`** |
| `security` | auth, licence gate |
| `system` | users, groups, roles, permissions, menus, preferences, announcements |
| `monitor` | login logs, operation audit, metrics, health probes, cache views |
| `cms` | **a reference business module** — articles, comments, interactions |

`cms` exists to demonstrate the shape of a business module. **You can delete the whole package and
the rest still builds.** If you are adding business features, add a package *beside* `cms`, not
inside `system`. The platform layer must stay ignorant of your domain.

More: [inkwash-api/README.md](inkwash-api/README.md) ·
[inkwash-cmd/README.md](inkwash-cmd/README.md) ·
[inkwash-site/README.md](inkwash-site/README.md)

---

## 6. Contribution workflow

1. **Open an issue first** for anything non-trivial. Design changes especially — see
   [§9](#9-design-constraints-you-must-not-casually-break).
2. **Fork** the repository.
3. **Branch** off `main`:
   ```bash
   git checkout -b feature/my-change
   ```
   Use `feature/`, `fix/`, `docs/`, or `refactor/` as the prefix.
4. **Make the change.** Keep it to one concern. Run the tests for every project you touched — see
   [§4](#4-commands) and [§10](#10-testing).
5. **Verify before you push.** Walk the [PR checklist](#11-pull-request-checklist).
6. **Open the pull request** against `main`. Describe what changed and why, and link the issue.
7. **Respond to review.** Expect one or two rounds. This is a small project; review is close to
   conversational.

**Report what you found, not what you assume.** For any bug report, include the Spring profile
(`dev` or `prod`), the browser, and the exact endpoint. A report with those three details is
actionable; without them it is a guess.

---

## 7. Commit messages

📋 **Convention** · ⏳ **Not yet set up** (no commitlint, no git hooks)

Use [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <subject>
```

**Types** — `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `build`, `ci`, `chore`

**Scopes** — the project, not the directory:

| Scope | Covers |
|---|---|
| `api` | `inkwash-api` |
| `cmd` | `inkwash-cmd` |
| `site` | `inkwash-admin`, `inkwash-web` |
| `share` | `@inkwash/share` |
| `deps` | dependency and toolchain bumps |
| `docs` | documentation only |
| `repo` | `.github/`, `.gitignore`, root config |

Examples:

```
feat(cms): add article retraction reason field
fix(security): reject expired licence before token refresh
docs(api): correct ArticleState transition table in README
refactor(share): extract common axios interceptor
test(api): cover SortValidator whitelist rejection
```

Subject in the imperative, lowercase, no trailing period. Keep the subject under 72 characters.

**Keep the history clean.** One logical change per commit. Do not mix a reformat with a behaviour
change. If a commit is noise on its own, `git rebase -i` before you push.

---

## 8. Code style

📋 **Convention** · 🔒 **Enforced** for the frontend (via `oxlint` and `oxfmt`)

### Both

- **UTF-8**, LF line endings. No trailing whitespace.
- Match the surrounding code. This is the tiebreaker when two styles appear in the same file.

### Backend — Java

> **Indent with TABS.** All 341 files under `inkwash-api/src/main` use tab indentation. The project
> README previously claimed 4 spaces; that was wrong. (Earlier revisions of this document repeated
> the same error — if you read a cached copy, trust this sentence.)

- `camelCase` for identifiers, `PascalCase` for types.
- **GPL header on every new source file.** Copy it verbatim from any file in the package:
  ```java
  /*
   * This file is part of Inkwash.
   * Copyright (C) 2026 ruilink team.
   *
   * This program is free software: you can redistribute it and/or modify
   * it under the terms of the GNU General Public License as published by
   * the Free Software Foundation, either version 3 of the License, or
   * (at your option) any later version.
   *
   * This program is distributed in the hope that it will be useful,
   * but WITHOUT ANY WARRANTY; without even the implied warranty of
   * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
   * GNU General Public License for more details.
   *
   * You should have received a copy of the GNU General Public License
   * along with this program.  If not, see <https://www.gnu.org/licenses/>.
   */
  ```
  It is present on essentially all main-source files and rare on test files — match the file you
  are editing.
- Javadoc `@author` and `@since` tags follow existing usage. Use your own name for `@author` and the
  version you are introducing the API in for `@since`.
- Chinese comments are normal and expected in this codebase. Write them in the language the
  surrounding comments use.

### Frontend — JavaScript (not TypeScript)

There are no `.d.ts` files and no type definitions in this workspace. Do not introduce TypeScript
into an existing package as part of an unrelated change.

Enforced by [oxfmt](inkwash-site/.oxfmtrc.json) — run `pnpm format` before you push:

- **2-space indent** (`.editorconfig`), `tabWidth: 2`
- **120-column** `printWidth`
- single quotes, semicolons, trailing commas (`"all"`), arrow parens always
- LF line endings, final newline

Tests live in a **parallel `tests/` tree** mirroring `src/`, not beside the code:

```
packages/share/src/utils/format.js
packages/share/tests/utils/format.spec.js   ← here
```

Vitest is configured with three projects — `share`, `admin`, `web` — filter with
`--project share`.

---

## 9. Design constraints you must not casually break

🔒 **Enforced** (architecture tests will fail your build)

The normative design document is [`reference/system-design.md`](reference/system-design.md) §10. It
lists twelve decisions (D1–D12) that are deliberately load-bearing. Each is backed by a comment in
the code or by a structural guarantee. If your change touches one, you are not making a small
refactor.

The four with the most expensive consequences:

| # | Constraint | If you change it |
|---|---|---|
| **D2** | Permission strings are `module:resource:action` | The whole permission system and every `@PreAuthorize` annotation must be rewritten |
| **D4** | Hand-written MyBatis annotated SQL — **not** MyBatis-Plus | Loses dialect branching; H2 and MySQL have different date-operator semantics |
| **D9** | H2 is a first-class development target | Tests start needing Testcontainers and Docker |
| **D12** | Events publish synchronously, inside the transaction | Switching to `AFTER_COMMIT` loses atomicity. **Not a behaviour-preserving change.** |

The full D1–D12 table, with the rationale and the cost of each, is in
[`reference/system-design.md` §10](reference/system-design.md#10-设计约束清单). Read it before
touching persistence, permissions, session handling, or the article state machine.

### Architecture guard tests

🔒 **Enforced** — these run under `./mvnw test` and will fail your build:

| Test | Fails when |
|---|---|
| `base/domain/BaseLayerBoundaryTest` | anything under `base` imports from `security` |
| `base/PackageLayoutTest` | the package-role separation in design doc §2.1 is violated |
| `base/ComponentWiringTest` | a package move compiles but breaks Spring context wiring |
| `base/SchemaParityTest` | H2 and MySQL schemas drift apart |
| `monitor/api/MethodSecurityPolicyTest` | a method-level security annotation is missing |
| `security/license/LicenseValidatorTest` | a `license.dat` appears on the classpath |

`BaseLayerBoundaryTest` tells you the fix directly: declare the abstraction in `base` and have
`security` wire it in, rather than importing upward.

---

## 10. Testing

🔒 **Enforced** for backend and frontend unit tests.

### Backend

```bash
cd inkwash-api && ./mvnw test
```

98 test source files. Frameworks: JUnit 5, Mockito, Spring Boot test starters, Spring REST Docs
MockMvc, and GreenMail (embedded SMTP — no external mail server needed). No Docker required.

The Surefire config widens its include patterns to `Test*.java`, `*Test.java`, `*Tests.java`,
`*TestCase.java` **and `*IT.java`**. Integration tests run during the normal `test` phase — there is
no separate `verify` step. A `*IT` file that fails will fail `./mvnw test`.

### CLI

```bash
cd inkwash-cmd && mvn test
```

### Frontend

```bash
cd inkwash-site && pnpm test:unit --run
```

24 spec files across the three projects. Vitest with jsdom, Vue Test Utils. Three named projects:
`share`, `admin`, `web`.

⏳ **Not yet set up** — `pnpm test:e2e` invokes Playwright, but the `e2e/` directory does not exist
yet, so there is nothing to run. `playwright.config.js` is in place for when those tests are
written.

### What to write

- A bug fix gets a test that fails without it.
- A new endpoint gets slice tests (`@WebMvcTest`) and, if it touches the database, a `@MybatisTest`.
- A new state transition in `cms` gets a domain-level test. State guards live in the domain object
  ([D11](#9-design-constraints-you-must-not-casually-break)), so that is where they are cheapest to
  verify.

---

## 11. Pull request checklist

Before you open the PR:

- [ ] `./mvnw test` passes — if you touched `inkwash-api`
- [ ] `mvn test` passes — if you touched `inkwash-cmd`
- [ ] `pnpm lint` and `pnpm format` clean — if you touched `inkwash-site`
- [ ] `pnpm test:unit --run` passes — if you touched `inkwash-site`
- [ ] New source files carry the GPL header
- [ ] Java indented with tabs; JS/Vue with 2 spaces
- [ ] No secrets, credentials, or real licence keys in the diff
- [ ] `docs/` **not** staged — it is private and gitignored; it must not appear in a public PR
- [ ] Commit messages follow [§7](#7-commit-messages)
- [ ] One logical change per PR, with a description of what and why
- [ ] Linked to an issue, if one exists

Before you push, confirm you did not stage anything private:

```bash
git add -A
git status --short        # read every line; confirm there is no docs/
git diff --cached --stat
```

Please do not touch any file under `.github`.

---

## 12. Security vulnerabilities

⏳ **Not yet set up** — a standalone `SECURITY.md` does not exist yet.

**Do not open a public issue for a security vulnerability.** Report it privately to
`dyllons@163.com` with: the affected version or commit, the component (`inkwash-api`,
`inkwash-cmd`, `inkwash-site`), reproduction steps, and the impact you observed.

Please give the maintainer reasonable time to ship a fix before disclosing publicly. Do not test
against other people's deployments, and do not access data you were not authorised to access.

---

## 13. What is not automated

📋 Honest inventory, so nothing below surprises you at review time. All ⏳.

- **No CI.** There are no GitHub Actions workflows. Nothing runs on your pull request automatically.
  Run the [checklist](#11-pull-request-checklist) locally — a maintainer will ask.
- **No commit lint.** Commit messages are reviewed by eye.
- **No git hooks.** No pre-commit formatting, no husky.
- **No pull request template.** No issue templates either.
- **`CODEOWNERS` does not block merges.** `.github/CODEOWNERS` routes review requests for `.github/`,
  Java, XML, YAML, and `.github/sponsor/*.png`. It only *requests* a reviewer, @rui-link to review it.

---

## 14. License

**GNU General Public License v3.0** — full text in [`LICENSE`](LICENSE).
Commercial terms for organisations that cannot meet GPL's copyleft obligations are in
[`COMMERCIAL-LICENSE.md`](COMMERCIAL-LICENSE.md).

Contributions are accepted under GPL-3.0, and the project may additionally offer them under the
commercial terms. See [§2](#2-before-you-start--licensing) before you contribute.

---

Questions that do not fit any section above? Open an issue.