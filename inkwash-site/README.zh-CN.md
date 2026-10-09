# inkwash-site

> Inkwash CMS 的前端 —— 一个 pnpm monorepo，包含两个 Vue 3 应用（后台管理 + 内容门户），共享一个公共库。

**[English](README.md) | 中文**

## 技术栈

| 组件          | 版本                                                      |
| ------------- | --------------------------------------------------------- |
| Vue           | 3.5                                                       |
| Vite          | 8.x                                                       |
| Pinia         | 4.x                                                       |
| Vue Router    | 5.x                                                       |
| Element Plus  | 2.14                                                      |
| Tailwind CSS  | 4.x                                                       |
| ECharts       | 6.x（admin）                                              |
| 测试          | Vitest 4 + @vue/test-utils（单元测试）、Playwright（E2E） |
| Lint / 格式化 | Oxlint、Oxfmt                                             |
| 包管理器      | pnpm 9（Node ≥ 22.12）                                    |

## 软件包

### inkwash-admin —— 后台管理（dev 端口 9090，hash 路由）

- **角色：** 系统管理员、内容编辑、运维人员。
- **技术栈：** Vue 3.5、Element Plus 2.14、Tailwind CSS 4、Pinia 4、Vue Router 5、ECharts 6 + vue-echarts、Vite 8。
- **亮点：** 服务端驱动的导航（路由树由 `GET /system/menus/user` 生成 —— 给角色加一个权限，
  页面就出现了）；两种布局（`layout-left` / `layout-top`）；面包屑；多标签页导航（右键菜单 +
  `keep-alive` 缓存）；RBAC 驱动的页面（用户 / 用户组 / 角色 / 权限 / 菜单）；CMS 工作流
  （列表 / 编辑 / 审核 / 发布、分类、标签、评论、敏感词）；带实时 Actuator 指标的审计与监控视图；
  按角色区分的仪表盘；`v-hasPerm` / `v-hasRole` 按钮级权限控制；Markdown 编辑器（md-editor-v3）
  支持图片上传；通知铃铛；五套主题（`chinese-red`、`sky-blue`、`natural-green`、
  `harvest-yellow`、`ink-dark`）；i18n（`zh-CN` / `en`）。

### inkwash-web —— 内容门户（dev 端口 9089，HTML5 路由）

- **角色：** 内容读者、注册作者。浏览无需登录。
- **技术栈：** Vue 3.5、Element Plus 2.14、Tailwind CSS 4、Pinia 4、Vue Router 5、Vite 8。不含 ECharts。
- **亮点：** 响应式布局 —— 768 px 以下折叠为下拉式移动导航，「我的」区域侧栏变为抽屉；
  公开的文章列表与详情页，用 `markdown-it` + DOMPurify 渲染；4 种登录方式
  （密码+验证码 / 二维码 / 短信 / GitHub OAuth2）；列表页与详情页均可互动
  （评论 / 点赞 / 收藏 / 分享）；创作中心（md-editor-v3、草稿、提交审核）；
  个人空间（我的文章 / 收藏 / 点赞 / 评论，每一项都能点进对应的筛选列表）；
  资料、账号安全、偏好与通知。
  Markdown 渲染是 `markdown-it` 加 DOMPurify 消毒 —— **没有**代码高亮、目录、KaTeX 或 Mermaid 插件。

### @inkwash/share —— 共享软件包

- **角色：** 内部共享库，`inkwash-admin` 与 `inkwash-web` 通过 `workspace:*` 协议使用。
- **内容：** axios 实例（含 401 单飞刷新与授权错误事件）、15 个 API 模块、5 个 Pinia store
  （auth / app / license / settings / site）、工具模块、2 个共享组件
  （`UserAvatar`、`AvatarUploader`）、唯一的 `vue-i18n` 实例（含两套语言包），以及一个 Vite 配置工厂。
- **纯 JavaScript，不是 TypeScript。** 本 workspace 中没有任何 `.d.ts` 或类型定义文件；
  编辑器路径别名由 `jsconfig.json` 与 Vite 的 `@` 别名处理。
- **统一账号：** admin 与 web 共用同一个 JWT；个人资料、偏好与通知在两端同步；角色差异按端生效
  （例如 USER 在 admin 端只能看到受限的仪表盘，但在 web 端拥有完整创作权限）。

## 快速开始

前置条件：Node ≥ 22.12、pnpm 9，以及运行在 `http://localhost:9080` 的后端（`inkwash-api`）。

```bash
cd inkwash-site
pnpm install
pnpm dev:admin    # admin dashboard → http://localhost:9090
pnpm dev:web      # public portal   → http://localhost:9089
```

## 配置

本仓库中**没有任何 `.env` 文件**。实际只读取一个环境变量：

| 环境变量       | 默认值                                         | 用途                                                   |
| -------------- | ---------------------------------------------- | ------------------------------------------------------ |
| `VITE_API_URL` | `/api`（开发代理目标 `http://localhost:9080`） | axios 的后端基址、SSE 流地址，以及 Vite 开发代理的目标 |

应用标题来自后端（`GET /system/meta`），不是环境变量。GitHub OAuth2 的入口是相对路径
`/oauth2/authorization/github`，由开发代理转发到后端 —— 因此 GitHub 登录页签在前端侧无需任何配置。

`packages/share/vite-config-factory.js` 中的开发代理会转发 `/api`、`/uploads`
（`changeOrigin: true`）以及 `/oauth2` + `/.well-known`（`changeOrigin: false`）。

## 认证模型

JWT **从不**存放在 JS 里。浏览器登录时，后端只把令牌作为 `HttpOnly` Cookie
（`secure_access`、`secure_refresh`）返回，因此脚本无法触及令牌。
axios 实例设置 `withCredentials: true` 并发送 `X-Requested-With` 头；
后端把这一组合判定为浏览器请求，仅对非浏览器客户端才在响应体中返回令牌。

收到 `401` 时，单飞刷新器只调用一次 `POST /auth/token/refresh`（即使多个请求同时失败），
然后重放原请求；失败则强制登出（派发 `inkwash:session-ended` 事件 → 带 `?redirect=` 返回路径的登录跳转）。

收到授权 `403` 时，拦截器派发 `inkwash:license-error` 事件打开授权弹窗，而不是跳转到 403 页面。

## 构建 / Lint / 测试

```bash
pnpm build:admin
pnpm build:web
pnpm build:share
pnpm lint        # oxlint
pnpm format      # oxfmt
pnpm test:unit --run    # Vitest —— 3 个 project 共 24 个 spec 文件
pnpm test:e2e           # Playwright（配置已存在；e2e/ 用例尚未编写）
```

## 部署（Nginx）

配置的端口（9090 / 9089）是开发默认值。生产环境中，将各软件包构建出的 `dist/` 产物放到 Nginx
后面提供服务。注意管理端使用 **hash 模式**、用户端使用 **HTML5 模式**，因此只有后者需要 SPA 回退：

- `inkwash-web`：`location / { try_files $uri $uri/ /index.html; }`
- `inkwash-admin`：无需 history 回退（`/#/…` 路由在客户端解析）
- 后端：`location /api/ { proxy_pass http://localhost:9080; }`
- 上传：`location /uploads/ { ... }` 指向后端的文件存储
- 本项目不使用 WebSocket，但扫码登录的 **SSE** 流（`/api/auth/qr/sse`）需要
  `proxy_buffering off;` 以及足够长的 `proxy_read_timeout`

## 相关模块

- 后端 API：`../inkwash-api`
- License CLI：`../inkwash-cmd`

## 许可证

GPL-3.0 —— 参见仓库根目录 `LICENSE`。
