# inkwash-cmd

> Inkwash 的离线授权工具 —— 一个跨平台 Java CLI（Windows、Linux），用于生成 RSA 密钥对、计算机器码、签署 `license.dat` 文件，并像后端一样精确地校验它们。

**[English](README.md) | 中文**

## 运行时技术栈

| 组件 | 版本 / 说明 |
|-----------|------------------|
| Java | 25 |
| CLI 框架 | Picocli 4.7.7 |
| JSON | Jackson（tools.jackson）3.x |
| 输出 | Jansi 2.4.3（ANSI 颜色） |
| 日志 | SLF4J（api + simple） |
| 构建 | Maven —— 仅使用 Spring Boot parent 4.1.1 作为可执行 JAR 打包插件 |

这是一个纯 CLI 应用，**不是** Spring 运行时应用。

## 构建

```bash
cd inkwash-cmd
mvn clean package -DskipTests
```

生成可执行 JAR `target/inkwash-cmd-0.5.1.jar`（需要 JDK 25 才能运行）。

## 子命令

```bash
java -jar inkwash-cmd-0.5.1.jar --help         # root usage
java -jar inkwash-cmd-0.5.1.jar keygen         # RSA key pair → PEM files
java -jar inkwash-cmd-0.5.1.jar machine-code   # this machine's 32-hex code (alias: info)
java -jar inkwash-cmd-0.5.1.jar generate       # sign a license.dat
java -jar inkwash-cmd-0.5.1.jar validate       # verify a license.dat
```

每个子命令也接受 `--help`。不带子命令直接运行 JAR 会打印用法并以非零码退出。

## 工作流（核心路径）

### 1. 生成密钥对（仅在首次使用时）

```bash
java -jar inkwash-cmd-0.5.1.jar keygen
```

将 `inkwash-private-key.pem` 和 `inkwash-public-key.pem` 写入当前工作目录。私钥只写入文件 —— 它从不会被
打印出来。除非传入 `--force`，已有文件**不会被覆盖**；POSIX 权限会自动收紧为仅所有者可访问。

### 2. 获取机器码

机器码是主机的连续 32 位十六进制 SHA-256 指纹（物理网卡 MAC + 操作系统 + 主机名），由后端使用的同一个
`HardwareFingerprinter` 计算，因此生成方与校验方始终一致。

```bash
java -jar inkwash-cmd-0.5.1.jar machine-code   # run ON the target machine
```

其它获取方式：读取后端启动时打印的编码（`License machine code (shared hardware fingerprint): …`），
或让目标机器的运维人员运行 `machine-code` 并将输出发送给你。

### 3. 签署授权

版本（editions）：`personal`（30 用户）/ `professional`（100）/ `enterprise`（不限）/ `trial`（10）。
`trial` 默认 30 天；`-d 0` 表示永久。通过 `--private-key-file <path>` 或 `INKWASH_PRIVATE_KEY`
环境变量提供私钥 —— 绝不要以裸 CLI 参数的形式传入。

每种版本一条命令（相同的注入私钥方式）：

```bash
java -jar inkwash-cmd-0.5.1.jar generate -o "Your Company" -e "you@example.com" --edition personal -d 0 --private-key-file inkwash-private-key.pem
java -jar inkwash-cmd-0.5.1.jar generate -o "Your Company" -e "you@example.com" --edition professional -d 0 --private-key-file inkwash-private-key.pem
java -jar inkwash-cmd-0.5.1.jar generate -o "Your Company" -e "you@example.com" --edition enterprise -d 0 --private-key-file inkwash-private-key.pem
java -jar inkwash-cmd-0.5.1.jar generate -o "Your Company" -e "you@example.com" --edition trial --private-key-file inkwash-private-key.pem
```

机器绑定（可选）—— 绑定到指定服务器，使文件无法被复制到其它位置：

```bash
java -jar inkwash-cmd-0.5.1.jar generate -o "Your Company" -e "you@example.com" \
  --edition professional -m <32-hex-machine-code> --private-key-file inkwash-private-key.pem
# or --machine-auto to bind the current machine
```

使用 `--output license.dat` 将信封写入文件而非标准输出。

### 4. 校验授权

```bash
java -jar inkwash-cmd-0.5.1.jar validate --license license.dat --public-key-file inkwash-public-key.pem
```

公钥也可以来自 `INKWASH_PUBLIC_KEY`。校验过程与后端保持一致（对 base64 内容做 SHA256withRSA），任何
失败都以非零码退出。注意：后端在设计上不强制校验有效期，CLI 也复现了这一行为。

### 5. 在后端安装

- 在 API 主机上设置 `LICENSE_PUBLIC_KEY`，使其能够校验你的签名。
- 将已签署的 `license.dat` 放到 API 类路径上（`inkwash-api/src/main/resources/license.dat`）。
  仓库内置了一份本地开发授权；请用你自己签署的文件替换它。
- **没有有效授权时后端依然可以正常启动。** `LicenseInterceptor` 在运行时拦截请求，
  对 `/api/system/**`、`/api/cms/**` 和 `/api/monitor/**` 返回带 `requireLicense` 字段的 `403`。
  替换文件后重启 API 即可。
- 注意 `/api/support/file/**`、`/api/profile/**` 和 `/api/preference/**` 不在门禁范围内，
  而 `GET /api/cms/articles` 这类公开读取请求也予以豁免。

## 安全注意事项

- 私钥是签署授权的唯一凭据 —— 请保持离线，切勿提交到仓库，也切勿粘贴到 shell 命令中。请使用
  `--private-key-file` 或 `INKWASH_PRIVATE_KEY`。
- 机器码是单向哈希；无法反推出硬件信息。
- CLI 与后端都**不会**强制校验授权的到期时间。校验刻意保持这一行为，因此已签署的授权始终有效；
  请把档位的用户数上限和签名本身作为你的约束手段。

## 开发

```bash
mvn test
```

## 目录结构（关键文件）

```
src/main/java/top/ruilink/inkwash/cmd/
├── LicenseCommand.java      # picocli root entry point
├── KeyGenCommand.java       # keygen
├── GenerateCommand.java     # generate
├── ValidateCommand.java     # validate
├── MachineCodeCommand.java  # machine-code (alias: info)
└── license/                 # LicenseData, LicenseGenerator, LicenseVerifier,
                             # LicenseKeyManager, LicenseConstants, HardwareFingerprinter
```

## 许可证

GPL-3.0 —— 参见仓库根目录 `LICENSE`。