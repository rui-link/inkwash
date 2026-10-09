# inkwash-site

> Frontend of the Inkwash CMS — a pnpm monorepo with two Vue 3 apps (admin dashboard + public portal) sharing one common library.

**English | [中文](README.zh-CN.md)**

## Tech Stack

| Component       | Version                                             |
| --------------- | --------------------------------------------------- |
| Vue             | 3.5                                                 |
| Vite            | 8.x                                                 |
| Pinia           | 4.x                                                 |
| Vue Router      | 5.x                                                 |
| Element Plus    | 2.14                                                |
| Tailwind CSS    | 4.x                                                 |
| ECharts         | 6.x (admin)                                         |
| Testing         | Vitest 4 + @vue/test-utils (unit), Playwright (E2E) |
| Lint / Format   | Oxlint, Oxfmt                                       |
| Package manager | pnpm 9 (Node ≥ 22.12)                               |

## Packages

### inkwash-admin — admin dashboard (dev port 9090, hash routing)

- **Role:** system administrators, content editors, ops staff.
- **Stack:** Vue 3.5, Element Plus 2.14, Tailwind CSS 4, Pinia 4, Vue Router 5, ECharts 6 + vue-echarts, Vite 8.
- **Highlights:** server-driven navigation (the route tree is generated from `GET /system/menus/user`,
  so granting a permission makes the page appear); two layouts (`layout-left` / `layout-top`);
  breadcrumb; multi-tab navigation with a right-click context menu and `keep-alive` caching;
  RBAC-driven pages (users / groups / roles / permissions / menus); CMS workflow
  (list / edit / review / publish, categories, tags, comments, sensitive words); audit & monitor
  views with live Actuator metrics; per-role dashboard; `v-hasPerm` / `v-hasRole` button-level
  control; Markdown editor (md-editor-v3) with image upload; notice bell; five themes
  (`chinese-red`, `sky-blue`, `natural-green`, `harvest-yellow`, `ink-dark`); i18n (`zh-CN` / `en`).

### inkwash-web — public portal (dev port 9089, HTML5 routing)

- **Role:** content readers, registered authors. No login needed to browse.
- **Stack:** Vue 3.5, Element Plus 2.14, Tailwind CSS 4, Pinia 4, Vue Router 5, Vite 8. No ECharts.
- **Highlights:** responsive layout that collapses to a slide-down mobile nav below 768 px and turns
  the personal-space sidebar into an off-canvas drawer; public article list and detail pages rendered
  with `markdown-it` + DOMPurify; 4 login methods (password+captcha / QR / SMS / GitHub OAuth2);
  interactions (comment / agree / favourite / share) on both list and detail pages; creation centre
  (md-editor-v3, drafts, commit for review); personal space (my articles / favourites / agrees /
  comments, each opening a filtered list); profile, account security, preferences and notices.
  Markdown rendering is plain `markdown-it` with DOMPurify sanitising — there is no syntax
  highlighting, TOC, KaTeX or Mermaid plugin.

### @inkwash/share — shared package

- **Role:** the internal shared library consumed by `inkwash-admin` and `inkwash-web` via the
  `workspace:*` protocol.
- **Contents:** the axios client (with 401 single-flight refresh and licence-error events), 15 API
  modules, 5 Pinia stores (auth / app / licence / settings / site), utility modules, 2 shared
  components (`UserAvatar`, `AvatarUploader`), the single `vue-i18n` instance with both locale
  dictionaries, and a Vite config factory.
- **JavaScript, not TypeScript.** There are no `.d.ts` files or type definitions anywhere in this
  workspace; editor aliasing is handled by `jsconfig.json` and the Vite `@` alias.
- **Unified account:** one JWT across admin and web; profiles, preferences and notifications sync
  across both sides; role differences apply per side (e.g. USER sees only a limited dashboard in
  admin, but has full creation rights on the web side).

## Getting Started

Prerequisites: Node ≥ 22.12, pnpm 9, and the backend (`inkwash-api`) running on `http://localhost:9080`.

```bash
cd inkwash-site
pnpm install
pnpm dev:admin    # admin dashboard → http://localhost:9090
pnpm dev:web      # public portal   → http://localhost:9089
```

## Configuration

There are **no `.env` files** in this repository. Exactly one variable is read:

| Env var        | Default                                           | Purpose                                                            |
| -------------- | ------------------------------------------------- | ------------------------------------------------------------------ |
| `VITE_API_URL` | `/api` (dev proxy target `http://localhost:9080`) | Backend base URL for axios, the SSE stream, and the Vite dev proxy |

The application title comes from the backend (`GET /system/meta`), not from an env var. The GitHub
OAuth2 entry point is the relative path `/oauth2/authorization/github`, which the dev proxy forwards
to the backend — so the login tab needs no frontend configuration at all.

The dev proxy in `packages/share/vite-config-factory.js` forwards `/api`, `/uploads` (changeOrigin
`true`) and `/oauth2` + `/.well-known` (changeOrigin `false`).

## Authentication model

The JWT is **never** stored in JS. On a browser login the backend returns tokens only as `HttpOnly`
cookies (`secure_access`, `secure_refresh`), so the token is unreachable from script. The axios
instance sets `withCredentials: true` and sends an `X-Requested-With` header; the backend treats that
combination as a browser request and returns the token in the body only for non-browser clients.

On a `401`, a single-flight refresher calls `POST /auth/token/refresh` once even when several
requests fail concurrently, replays the original request, and falls back to a forced logout
(`inkwash:session-ended` event → login redirect with a `?redirect=` return path).

On a licence `403`, the interceptor dispatches an `inkwash:license-error` event that opens the
licence dialog instead of bouncing to the 403 page.

## Build / Lint / Test

```bash
pnpm build:admin
pnpm build:web
pnpm build:share
pnpm lint        # oxlint
pnpm format      # oxfmt
pnpm test:unit --run    # Vitest — 24 spec files across the 3 projects
pnpm test:e2e           # Playwright (config present; the e2e/ suite is not written yet)
```

## Deployment (Nginx)

The configured ports (9090 / 9089) are development defaults. In production, serve each package's
built `dist/` output behind Nginx. Note that the admin app uses **hash history** and the web app
uses **HTML5 history**, so only the latter needs the SPA fallback:

- `inkwash-web`: `location / { try_files $uri $uri/ /index.html; }`
- `inkwash-admin`: no history fallback required (`/#/…` routes resolve client-side)
- Backend: `location /api/ { proxy_pass http://localhost:9080; }`
- Uploads: `location /uploads/ { ... }` pointing at the backend's file storage
- WebSocket-free, but the QR-login **SSE** stream (`/api/auth/qr/sse`) needs
  `proxy_buffering off;` and a long `proxy_read_timeout`

## Related Modules

- Backend API: `../inkwash-api`
- License CLI: `../inkwash-cmd`

## License

GPL-3.0 — see the repository root `LICENSE`.
