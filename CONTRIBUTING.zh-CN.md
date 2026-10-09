# 参与 Inkwash 贡献

[English](CONTRIBUTING.md)

感谢你考虑为 Inkwash 贡献代码。本项目是一个模块化单体 —— 后端 Spring Boot 4，前端 Vue 3 ——
其设计包含若干刻意为之、且代价高昂难以回退的决策。

**本文件是一份规范性（normative）文档**，描述项目「应当如何被修改」。若当前代码与之存在偏差，
那是代码的缺陷，而不是继续复制偏差的许可。

## 如何阅读本文件

下面每条规则都带一个标记。它们告诉你你的 PR 实际上会发生什么 —— 这比我们「希望」发生什么更重要：

| 标记 | 含义 |
|---|---|
| 🔒 **强制** | 有工具或测试会拦下你的改动。不修就不会合并。 |
| 📋 **约定** | 无机器校验，但评审会要求你遵守。 |
| ⏳ **尚未建立** | 计划中，但当前无任何自动化。没有任何东西会拦住你。 |

如果你发现自己想违反其中一条，请在动手写代码**之前**开 issue。带着讨论而来的设计变更受欢迎的；
作为 diff 里一个意外出现的变更则不是。

## 目录

1. [行为准则](#1-行为准则)
2. [动手之前 —— 授权](#2-动手之前--授权)
3. [环境准备](#3-环境准备)
4. [命令](#4-命令)
5. [仓库结构](#5-仓库结构)
6. [贡献流程](#6-贡献流程)
7. [提交信息](#7-提交信息)
8. [代码风格](#8-代码风格)
9. [不可随手破坏的设计约束](#9-不可随手破坏的设计约束)
10. [测试](#10-测试)
11. [PR 检查清单](#11-pr-检查清单)
12. [安全漏洞](#12-安全漏洞)
13. [尚未自动化的部分](#13-尚未自动化的部分)
14. [许可证](#14-许可证)

---

## 1. 行为准则

⏳ **尚未建立** —— 目前还没有独立的 `CODE_OF_CONDUCT.md`。在项目活跃度足以支撑之前，下列条款直接
生效；届时会迁出为独立文件。

Inkwash 采纳[贡献者公约 v2.1](https://www.contributor-covenant.org/version/2/1/code_of_conduct/code_of_conduct.md)。
概括而言：彼此尊重；预设善意；只评论代码，不针对人；禁止骚扰与人身攻击；接受维护者拒绝你的
贡献是可能的。

举报不当行为请联系 `dyllons@163.com`，举报内容仅维护者可见。我们会为举报人保密。

---

## 2. 动手之前 —— 授权

📋 **约定** · ⏳ **尚未建立**（无 CLA bot）

本项目为**双许可证**：对所有人适用 [GPL-3.0](LICENSE)，同时为无法承担 GPL 传染性义务的机构提供
[商业授权](COMMERCIAL-LICENSE.md)。

你的贡献即视为：

- 以 **GPL-3.0** 授权，且
- 项目可另行按 `COMMERCIAL-LICENSE.md` 中的商业条款提供该贡献。

**动手前请先与你的雇主确认。** 部分雇主要求事先书面批准开源贡献，或要求你保留某些权利。若你的
雇主有此要求，请在写代码**之前**在 issue 中提出。提前理清，远比事后拆解一个已合并的提交容易。

你保留自己贡献的著作权。本项目目前**不要求**签署 CLA。⏳ 计划接入 CLA bot（可能是
`cla-assistant`），因为双许可证结构当前建立在「维护者是唯一版权人」这一假设上 —— 而外部代码一旦
合入，该假设即不再成立。若日后引入 CLA，会提前公告，且不会追溯适用于已合并的工作。

---

## 3. 环境准备

🔒 **强制**（缺少这些将构建失败）

| 工具 | 版本 | 用途 |
|---|---|---|
| JDK | 25 | `inkwash-api`、`inkwash-cmd` |
| Node.js | ≥ 22.12 | `inkwash-site` |
| pnpm | 9.0.0（由 `packageManager` 字段锁定） | `inkwash-site` |
| Maven | 3.9.16，经 `./mvnw` 调用 | `inkwash-api` |
| MySQL / Redis | **不需要** | `dev` 环境使用内存 H2 + Caffeine |

`dev` 环境无需外部数据库。H2 是**一等开发目标**，而不仅仅图个方便（见
[D9](#9-不可随手破坏的设计约束)）。不要引入任何让测试依赖 Docker 或 Testcontainers 的方案。

> **注意** `inkwash-api` 自带 Maven wrapper，**`inkwash-cmd` 没有** —— 那里请用系统的 `mvn`。

完整快速上手见 [README §5](README.zh-CN.md#5-快速开始)。

---

## 4. 命令

构建、测试、lint 为🔒 **强制**。以下就是项目实际运行的命令，请勿替换成你自己的方式。

### 后端 —— `inkwash-api`（Maven）

| 用途 | 命令 |
|---|---|
| 运行（dev，H2） | `./mvnw spring-boot:run`  ·  Windows：`.\mvnw.cmd spring-boot:run` |
| 构建 | `./mvnw clean package -DskipTests` |
| 测试 | `./mvnw test` |
| 跑单个测试类 | `./mvnw test -Dtest=ArticleServiceTest` |
| 跑单个测试方法 | `./mvnw test -Dtest=ArticleDomainTest#someMethodName` |

API 位于 `http://localhost:9080`。H2 控制台在 `/h2-console`。Actuator 健康检查在
`/actuator/health`。

项目未配置 Java linter 或 formatter。请勿在无关的改动中顺手引入一个。

### 命令行工具 —— `inkwash-cmd`（Maven，无 wrapper）

| 用途 | 命令 |
|---|---|
| 构建 | `mvn clean package -DskipTests` |
| 测试 | `mvn test` |
| 运行 | `java -jar target/inkwash-cmd-0.5.1.jar --help` |

### 前端 —— `inkwash-site`（pnpm workspace）

请在 `inkwash-site/` 下执行，不要进入单个 package。

| 用途 | 命令 |
|---|---|
| 安装 | `pnpm install` |
| 开发 —— 后台 | `pnpm dev:admin` → `http://localhost:9090` |
| 开发 —— 门户 | `pnpm dev:web` → `http://localhost:9089` |
| 构建 | 先 `pnpm build:share`，再 `pnpm build:admin` / `pnpm build:web` |
| Lint | `pnpm lint` |
| Lint 自动修复 | `pnpm lint:fix` |
| 格式化 | `pnpm format` |
| 单元测试 | `pnpm test:unit --run` |
| 跑单个 spec | `pnpm test:unit --run packages/share/tests/utils/format.spec.js` |

> ⚠️ **`pnpm test:unit` 需要 `--run`。** 该脚本是裸 `vitest`，默认进入 watch 模式，会让 CI 式调用
> 一直挂住。请始终传 `--run`。

两个开发服务器都会把 `/api` 与 `/uploads` 代理到 `http://localhost:9080`。

---

## 5. 仓库结构

```
inkwash/
├── inkwash-api/          Spring Boot 4 后端（模块化单体）
├── inkwash-cmd/          Picocli 授权签发 CLI
├── inkwash-site/         pnpm workspace —— Vue 3，3 个 package
├── reference/            system-design.md —— 规范性设计文档
└── .github/CODEOWNERS    评审路由
```

`inkwash-api` 内部的包划分不是装饰，它由测试强制：

| 包 | 职责 |
|---|---|
| `base` | 共享基础设施；**不得依赖 `security`** |
| `security` | 认证、授权门禁 |
| `system` | 用户、用户组、角色、权限、菜单、偏好、公告 |
| `monitor` | 登录日志、操作审计、指标采集、健康探针、缓存视图 |
| `cms` | **一个参考业务模块** —— 文章、评论、互动 |

`cms` 的存在是为了演示业务模块应有的形态。**你可以整体删除这个包，其余部分依然能构建。**
若要添加业务功能，请在 `cms` **旁边**新建包，而不是伸进 `system`。平台层必须始终对你的业务无知。

更多：[inkwash-api/README.md](inkwash-api/README.md) ·
[inkwash-cmd/README.md](inkwash-cmd/README.md) ·
[inkwash-site/README.md](inkwash-site/README.md)

---

## 6. 贡献流程

1. **任何非小事，先开 issue。** 设计变更尤其如此 —— 见[§9](#9-不可随手破坏的设计约束)。
2. **Fork** 本仓库。
3. 从 `main` 切出**分支**：
   ```bash
   git checkout -b feature/my-change
   ```
   前缀使用 `feature/`、`fix/`、`docs/` 或 `refactor/`。
4. **完成改动。** 一次只做一件事。对你触及的每个项目都跑测试 —— 见
   [§4](#4-命令) 与[§10](#10-测试)。
5. **推送前自查。** 走一遍 [PR 检查清单](#11-pr-检查清单)。
6. **发起 PR**，目标分支 `main`。说明改了什么、为什么改，并关联 issue。
7. **响应评审。** 预计一到两轮往返。本项目规模不大，评审基本是对话式的。

**报告你发现的问题，而不是你假设的问题。** 提交缺陷报告时，请附上 Spring profile（`dev` 或
`prod`）、浏览器，以及准确的接口路径。带齐这三样的报告是可处理的；缺了就是猜测。

---

## 7. 提交信息

📋 **约定** · ⏳ **尚未建立**（无 commitlint，无 git hooks）

使用 [Conventional Commits](https://www.conventionalcommits.org/) 规范：

```
<type>(<scope>): <subject>
```

**type** —— `feat`、`fix`、`docs`、`style`、`refactor`、`perf`、`test`、`build`、`ci`、`chore`

**scope** —— 按项目划分，而非按目录：

| scope | 涵盖 |
|---|---|
| `api` | `inkwash-api` |
| `cmd` | `inkwash-cmd` |
| `site` | `inkwash-admin`、`inkwash-web` |
| `share` | `@inkwash/share` |
| `deps` | 依赖与工具链升级 |
| `docs` | 仅文档 |
| `repo` | `.github/`、`.gitignore`、根配置 |

示例：

```
feat(cms): add article retraction reason field
fix(security): reject expired licence before token refresh
docs(api): correct ArticleState transition table in README
refactor(share): extract common axios interceptor
test(api): cover SortValidator whitelist rejection
```

subject 用祈使句、小写、不加句号，长度控制在 72 字符以内。

**保持提交历史干净。** 一个提交只做一件逻辑上的事。不要把格式化与行为变更混在一起。若某个提交单独看
毫无意义，推送前用 `git rebase -i` 整理掉。

---

## 8. 代码风格

📋 **约定** · 前端部分🔒 **强制**（由 `oxlint` 与 `oxfmt` 保障）

### 通用

- **UTF-8**，LF 换行。行尾不留空白字符。
- 遵循周边代码风格。当同一文件中出现两种风格时，以此为裁决依据。

### 后端 —— Java

> **使用 TAB 缩进。** `inkwash-api/src/main` 下全部 341 个文件均使用 TAB 缩进。项目 README 曾声称
> 4 空格，那是错的。（本文件的早期版本同样重复了这个错误 —— 若你读到的是缓存副本，请以本句为准。）

- 标识符用 `camelCase`，类型用 `PascalCase`。
- **每个新增源文件都要带 GPL 头部。** 从同包下任意文件原样复制：
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
  主源码中几乎每个文件都有，测试文件则少见 —— 与你正在编辑的文件保持一致。
- Javadoc 的 `@author` 与 `@since` 沿用现有写法。`@author` 填你自己的名字，`@since` 填你引入该 API
  时的版本号。
- 中文注释在本代码库中是正常且被预期的。请用周边注释所用的语言来写。

### 前端 —— JavaScript（非 TypeScript）

本 workspace 中没有 `.d.ts` 文件，也没有任何类型定义。请勿在无关的改动中把 TypeScript 引入既有
package。

由 [oxfmt](inkwash-site/.oxfmtrc.json) 🔒 **强制** —— 推送前请执行 `pnpm format`：

- **2 空格缩进**（`.editorconfig`），`tabWidth: 2`
- **120 列** `printWidth`
- 单引号、分号、尾随逗号（`"all"`）、箭头函数括号总是保留
- LF 换行，文件末尾保留换行

测试位于**与 `src/` 平行的 `tests/` 目录树**，而不是与源码放在一起：

```
packages/share/src/utils/format.js
packages/share/tests/utils/format.spec.js   ← 在这里
```

Vitest 配置了三个 project —— `share`、`admin`、`web`，用 `--project share` 过滤。

---

## 9. 不可随手破坏的设计约束

🔒 **强制**（架构测试会让你的构建失败）

规范性设计文档是 [`reference/system-design.md`](reference/system-design.md) 的 §10，其中列出了十二项
刻意承载压力的决策（D1–D12）。每一条在代码中都有对应注释或结构性保障。若你的改动触及其中之一，
那你做的就不是一次小重构。

代价最高的四条：

| # | 约束 | 若你改动它 |
|---|---|---|
| **D2** | 权限串为 `module:resource:action` | 整个权限体系与全部 `@PreAuthorize` 注解需重写 |
| **D4** | 手写 MyBatis 注解 SQL —— **不用** MyBatis-Plus | 失去方言分支能力；H2 与 MySQL 的日期算子语义不同 |
| **D9** | H2 是一等开发目标 | 测试将开始依赖 Testcontainers 与 Docker |
| **D12** | 事件在事务内同步发布 | 改为 `AFTER_COMMIT` 会失去原子性，**这不是行为等价改动** |

完整的 D1–D12 表格及其理由与改动代价，见
[`reference/system-design.md` §10](reference/system-design.md#10-设计约束清单)。在触碰持久化、权限、
会话处理或文章状态机之前，请先读它。

### 架构守卫测试

🔒 **强制** —— 它们在 `./mvnw test` 下运行，会让你的构建失败：

| 测试 | 何时失败 |
|---|---|
| `base/domain/BaseLayerBoundaryTest` | `base` 下任何文件 import 了 `security` |
| `base/PackageLayoutTest` | 违反设计文档 §2.1 的包职责分离 |
| `base/ComponentWiringTest` | 包迁移能编译通过但破坏了 Spring 上下文装配 |
| `base/SchemaParityTest` | H2 与 MySQL 的 schema 发生漂移 |
| `monitor/api/MethodSecurityPolicyTest` | 缺少方法级安全注解 |
| `security/license/LicenseValidatorTest` | classpath 上出现了 `license.dat` |

`BaseLayerBoundaryTest` 会直接告诉你怎么改：在 `base` 侧声明抽象，由 `security` 反向装配，
而不是向上 import。

---

## 10. 测试

后端与前端单元测试均为🔒 **强制**。

### 后端

```bash
cd inkwash-api && ./mvnw test
```

98 个测试源文件。框架：JUnit 5、Mockito、Spring Boot test starters、Spring REST Docs MockMvc，
以及 GreenMail（内嵌 SMTP —— 不需要外部邮件服务器）。无需 Docker。

Surefire 配置把匹配模式扩展为 `Test*.java`、`*Test.java`、`*Tests.java`、`*TestCase.java`
**以及 `*IT.java`**。集成测试会在正常的 `test` 阶段运行 —— 没有单独的 `verify` 步骤。某个 `*IT`
文件失败就会让 `./mvnw test` 失败。

### 命令行工具

```bash
cd inkwash-cmd && mvn test
```

### 前端

```bash
cd inkwash-site && pnpm test:unit --run
```

三个 package 共 24 个 spec 文件。Vitest + jsdom + Vue Test Utils。三个具名 project：`share`、
`admin`、`web`。

⏳ **尚未建立** —— `pnpm test:e2e` 会调用 Playwright，但 `e2e/` 目录还不存在，因此没有测试可跑。
`playwright.config.js` 已就位，等待这些测试被写出来。

### 该写什么测试

- 修缺陷要补一个「不打这个补丁就会失败」的测试。
- 新增接口要补 slice 测试（`@WebMvcTest`）；若涉及数据库，再补 `@MybatisTest`。
- `cms` 中新增的状态流转要补领域层测试。状态守卫位于领域对象中
  （[D11](#9-不可随手破坏的设计约束)），因此在那里验证成本最低。

---

## 11. PR 检查清单

发起 PR 之前：

- [ ] `./mvnw test` 通过 —— 若改动了 `inkwash-api`
- [ ] `mvn test` 通过 —— 若改动了 `inkwash-cmd`
- [ ] `pnpm lint` 与 `pnpm format` 干净 —— 若改动了 `inkwash-site`
- [ ] `pnpm test:unit --run` 通过 —— 若改动了 `inkwash-site`
- [ ] 新增源文件都带了 GPL 头部
- [ ] Java 用 TAB 缩进；JS/Vue 用 2 空格
- [ ] diff 中没有密钥、凭据或真实授权文件
- [ ] **未**暂存 `docs/` —— 它是私有且被 gitignore 的，绝不能出现在公开 PR 中
- [ ] 提交信息符合 [§7](#7-提交信息)
- [ ] 一个 PR 一件事，并说明改了什么、为什么改
- [ ] 如有相关 issue，已关联

推送前，确认你没有暂存任何私有内容：

```bash
git add -A
git status --short        # 逐行审阅，确认没有 docs/
git diff --cached --stat
```

请不要改动 `.github` 目录下的文件

---

## 12. 安全漏洞

⏳ **尚未建立** —— 目前还没有独立的 `SECURITY.md`。

**请不要为安全漏洞开公开 issue。** 请私下联系 `dyllons@163.com`，并附上：受影响的版本或 commit、
所在组件（`inkwash-api`、`inkwash-cmd`、`inkwash-site`）、复现步骤，以及你观察到的实际影响。

请给维护者留出合理的修复发布时间，再进行公开披露。请勿针对他人部署进行测试，也请勿访问你无权
访问的数据。

---

## 13. 尚未自动化的部分

📋 一份诚实的清单，以免评审时有任何一项让你措手不及。全部为 ⏳。

- **没有 CI。** 仓库中没有任何 GitHub Actions workflow。不会有任何东西在你的 PR 上自动运行。
  请本地执行[检查清单](#11-pr-检查清单) —— 维护者会来问。
- **没有 commit lint。** 提交信息靠人工评审。
- **没有 git hooks。** 没有 pre-commit 格式化，没有 husky。
- **没有 PR 模板。** 也没有 issue 模板。
- **`CODEOWNERS` 并不阻止合并。** `.github/CODEOWNERS` 会为 `.github/`、Java、XML、YAML 与
  `.github/sponsor/*.png` 路由评审请求，它只是*发起*评审，@rui-link用户进行评审。

---

## 14. 许可证

**GNU General Public License v3.0** —— 完整文本见 [`LICENSE`](LICENSE)。
面向无法承担 GPL 传染性义务的机构的商业条款见
[`COMMERCIAL-LICENSE.md`](COMMERCIAL-LICENSE.md)。

贡献内容以 GPL-3.0 接受，项目可另行按商业条款提供。贡献前请先阅读
[§2](#2-动手之前--授权)。

---

以上任何章节都不涵盖的问题？开 issue 即可。