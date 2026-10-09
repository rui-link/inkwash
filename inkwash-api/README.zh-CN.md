# inkwash-api

> Inkwash CMS 的后端 —— 一个模块化单体 Spring Boot 服务，提供认证、RBAC、内容管理、审计、监控以及内置的 License 授权门禁。

**[English](README.md) | 中文**

## 技术栈

| 组件 | 版本 / 说明 |
|-----------|------------------|
| Java | 25 |
| Spring Boot | 4.1.1 |
| MyBatis | mybatis-spring-boot-starter 4.1.0（注解驱动 SQL，**非** MyBatis-Plus） |
| JSON | Jackson 3（`tools.jackson`） |
| 数据库 | MySQL（生产、默认）/ H2 MySQL 模式（开发）/ PostgreSQL 驱动在依赖里但未提供建表脚本 |
| 缓存 | Caffeine（开发）/ Redis（生产） |
| JWT | jjwt 0.13.0 —— **HS384**，密钥长度写死要求 ≥ 48 字节 |
| 构建 | Maven（wrapper：`mvnw`） |

构件坐标：`top.ruilink:inkwash:0.5.1`。版本：`0.5.1`。

## 模块架构

inkwash-api 是**单个 Maven 构件**（`top.ruilink:inkwash`），以模块化单体方式构建。
下面的五个"模块"是 `top.ruilink.inkwash` 下的 Java 包，通过接口解耦：
如果你做的不是 CMS，保留 `base` + `security` + `system` + `monitor`，把 `cms` 删掉即可。

| 模块 | 职责 |
|--------|----------------|
| **base** | 共享实体、枚举、异常、工具类、文件存储、全局异常处理 |
| **security** | JWT 认证、多策略登录（密码+验证码 / 二维码 / 短信 / OAuth2 GitHub）、XSS 防护、License 强制实施、用户资料 |
| **system** | 用户、账号、用户组、角色、权限、菜单、通知，以及基于方法级 `@PreAuthorize` 的身份管理 |
| **monitor** | 登录审计、操作审计、系统指标、健康检查、License 状态 |
| **cms** | 文章、分类、标签、评论、互动、敏感词过滤、基于角色的工作流（作者 / 编辑 / 管理员） |

## 功能特性

- **登录流程**：密码 + 验证码、二维码扫码、短信验证码、OAuth2（GitHub）；一个用户可绑定多个账号
- **会话**：JWT 访问令牌（1 天）+ 刷新令牌（7 天），采用 **HS384** 签名；另有一套基于 Origin 校验
  取代 CSRF token 的 `HttpOnly` Cookie 会话。刷新令牌在同一家族 ID 下轮换；重放已消费的令牌会吊销
  整个家族。登出按剩余有效期写入令牌黑名单
- **RBAC**：`User → UserGroup → GroupRole → RolePermission → Permission`，通过
  `@PreAuthorize("hasAuthority('module:resource:action')")` 强制实施
- **CMS**：草稿 / 提交 / 审核 / 发布 / 撤回工作流，分类、标签、评论、敏感词词典
- **审计与监控**：登录日志、AOP 操作追踪（自动脱敏敏感参数）、Hikari + JMX 指标
- 通知、按角色划分的仪表盘、文件存储（可插拔接口的本地后端）、i18n（`zh-CN` / `en`）、SSE
- **License 门禁**：对管理端 API 实施签名 + 机器绑定 + 档位/模块校验（见下文）

## 运行要求

- JDK 25+
- Maven 3.9+（或使用内置的 `./mvnw` / `.\mvnw.cmd` 包装脚本）

## 配置

没有任何环境变量会阻止应用启动，但下面这些必须在真实部署前设置：

| 变量 | 用途 |
|----------|---------|
| `AUTH_JWT_SECRET` | HS384 签名密钥。必须 ≥ 48 字节，且不能是黑名单里的占位值 —— 否则 `JwtTokenConfig` 拒绝启动 |
| `MYSQL_PASSWORD` / `REDIS_PASSWORD` | 生产数据源与生产缓存 |

其它凭据在 `src/main/resources/application-dev.yaml` 中提供**仅用于开发的默认值**
（`AUTH_JWT_SECRET`、GitHub OAuth 占位符）。
dev 的默认 JWT 密钥是公开信息 —— 任何可达的环境都不要用 `dev` 配置启动。
`application-prod.yaml` 里还提供了注释掉的 PostgreSQL 数据源与 SMTP 模板。

以真实 HTTPS提供服务时请设置 `auth.cookie.secure: true`（prod 配置目前仍默认为 `false`，
文件中已用 `TODO` 标注）。

## 运行 —— 开发模式（H2 内存数据库）

```bash
cd inkwash-api
./mvnw spring-boot:run        # Windows: .\mvnw.cmd spring-boot:run
```

- 默认配置为 `dev`：使用基于内存的 H2 数据库，启动时从 `database/` 应用表结构与种子数据。
- API 地址：`http://localhost:9080`
- H2 控制台：`http://localhost:9080/h2-console`
- Actuator：`http://localhost:9080/actuator/health`、`http://localhost:9080/actuator/info`

## 运行 —— 生产模式（MySQL）

```bash
cd inkwash-api
./mvnw clean package -DskipTests
java -jar target/inkwash-0.5.1.jar --spring.profiles.active=prod
```

## 数据库初始化

`dev` 配置启动时会执行 `base/DatabaseInitializer.java`，依次应用
`database/h2-schema.sql` 与 `database/init-data.sql`。生产配置设置了 `spring.sql.init.mode: never`，
需要你自己导入：

```bash
mysql -u root -p inkwash < database/mysql-schema.sql
mysql -u root -p inkwash < database/init-data.sql
# 可选的演示数据（孔子 / 孟子 / 李白 / 苏轼 …）
mysql -u root -p inkwash < database/demo-data.sql
```

23 张表分属四个 schema 分组：`sys_*`（RBAC、通知、偏好）、`mon_*`（登录信息、操作日志）、
`media_file`，以及 `cms_*`（文章、分类标签、评论、互动、敏感词）。

`init-data.sql` 初始化的数据：

- 角色：`ROLE_SYSTEM`、`ROLE_ADMIN`、`ROLE_EDITOR`、`ROLE_USER`（共 92 条权限）
- 用户组：`Systems`、`Admins`、`Editors`、`Users`
- 账号：`admin` 与 `system`，密码均为 `Super99*`

> **安全提示：** 这些是公开的、仅用于开发的默认值 —— 任何生产环境使用前，**请在首次登录时修改密码**。

## 测试与打包

```bash
./mvnw test
./mvnw clean package -DskipTests
```

74 个测试源文件（JUnit 5 + Mockito，基于 H2 MySQL 模式的 `@MybatisTest` 切片，
另有 `*IT` 集成测试和用于邮件链路的 GreenMail）。不依赖 Testcontainers 或 Docker。

## License 集成

- 后端从**类路径**读取 `license.dat`（`src/main/resources/license.dat`）。仓库内置了一份本地开发
  授权；部署前请用 CLI 生成你自己的（参见 `inkwash-cmd/README.md`）。
- **没有有效授权时应用依然可以启动。** `LicenseInterceptor` 在运行时拦截请求并返回带
  `requireLicense` 字段的 `403` 响应 —— 它不是启动期检查。
- `LICENSE_PUBLIC_KEY` 是信任锚点：用 `inkwash-cmd keygen` 生成自己的密钥对，配置公钥，
  然后用针对你的档位签名的 `license.dat` 替换它。
- 校验流程：对 Base64 信封做 `SHA256withRSA`，再做机器绑定校验。到期时间会被记录但
  **刻意不做强制校验** —— 参见 `LicenseValidator` 的 javadoc。
- 后端启动时会打印机器码。它由 `HardwareFingerprinter` 计算，与 `inkwash-cmd` 中的那份
  逐字节相同，因此同一台主机上 `inkwash-cmd machine-code` 会输出同样的值。
  虚拟网卡（docker、vbox、vmnet、WSL、Hyper-V 等）会被排除。
- 档位上限按实时用户数限制模块：超出后相应收窄 `/api/system/**`、`/api/cms/**`、
  `/api/monitor/**`（`personal`/`trial` 只保留 `system`）。
- 受门禁的路径：`/api/system/**`、`/api/cms/**`、`/api/monitor/**`（`/api/license/**` 除外）。
  `GET /api/cms/articles` 这类公开读取请求豁免。注意 `/api/support/file/**`、`/api/profile/**`
  和 `/api/preference/**` **未**纳入授权门禁。

## 许可证

GPL-3.0 —— 参见仓库根目录 `LICENSE`。
