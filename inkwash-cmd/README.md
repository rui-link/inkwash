# inkwash-cmd

> Offline license tooling for Inkwash — a cross-platform Java CLI (Windows, Linux) that generates RSA key pairs, computes machine codes, signs `license.dat` files, and verifies them exactly as the backend does.

**English | [中文](README.zh-CN.md)**

## Runtime Stack

| Component | Version / Detail |
|-----------|------------------|
| Java | 25 |
| CLI framework | Picocli 4.7.7 |
| JSON | Jackson (tools.jackson) 3.x |
| Output | Jansi 2.4.3 (ANSI colors) |
| Logging | SLF4J (api + simple) |
| Build | Maven — Spring Boot parent 4.1.1 used only for the executable-jar packaging plugin |

This is a plain CLI application, **not** a Spring runtime application.

## Build

```bash
cd inkwash-cmd
mvn clean package -DskipTests
```

Produces the executable jar `target/inkwash-cmd-0.5.1.jar` (requires JDK 25 to run).

## Subcommands

```bash
java -jar inkwash-cmd-0.5.1.jar --help         # root usage
java -jar inkwash-cmd-0.5.1.jar keygen         # RSA key pair → PEM files
java -jar inkwash-cmd-0.5.1.jar machine-code   # this machine's 32-hex code (alias: info)
java -jar inkwash-cmd-0.5.1.jar generate       # sign a license.dat
java -jar inkwash-cmd-0.5.1.jar validate       # verify a license.dat
```

Each subcommand also accepts `--help`. Running the jar with no subcommand prints usage and exits non-zero.

## Workflow (Core Path)

### 1. Generate a key pair (first time only)

```bash
java -jar inkwash-cmd-0.5.1.jar keygen
```

Writes `inkwash-private-key.pem` and `inkwash-public-key.pem` into the working directory. The private
key goes to the file only — it is never printed. Existing files are **not overwritten** unless
`--force` is passed; POSIX permissions are tightened to owner-only automatically.

### 2. Get the machine code

The machine code is a continuous 32-hex SHA-256 fingerprint of the host (physical NIC MACs + OS +
hostname), computed by the same `HardwareFingerprinter` the backend uses, so generator and verifier
always match.

```bash
java -jar inkwash-cmd-0.5.1.jar machine-code   # run ON the target machine
```

Alternative ways to obtain it: read the code the backend prints at startup (`License machine code
(shared hardware fingerprint): …`), or have the target machine's operator run `machine-code` and
send you the output.

### 3. Sign a license

Editions: `personal` (30 users) / `professional` (100) / `enterprise` (unlimited) / `trial` (10).
`trial` defaults to 30 days; `-d 0` means permanent. Provide the private key either via
`--private-key-file <path>` or the `INKWASH_PRIVATE_KEY` environment variable — never as a raw CLI
argument.

One command per edition (same key-injection pattern):

```bash
java -jar inkwash-cmd-0.5.1.jar generate -o "Your Company" -e "you@example.com" --edition personal -d 0 --private-key-file inkwash-private-key.pem
java -jar inkwash-cmd-0.5.1.jar generate -o "Your Company" -e "you@example.com" --edition professional -d 0 --private-key-file inkwash-private-key.pem
java -jar inkwash-cmd-0.5.1.jar generate -o "Your Company" -e "you@example.com" --edition enterprise -d 0 --private-key-file inkwash-private-key.pem
java -jar inkwash-cmd-0.5.1.jar generate -o "Your Company" -e "you@example.com" --edition trial --private-key-file inkwash-private-key.pem
```

Machine binding (optional) — bind to a specific server so the file cannot be copied elsewhere:

```bash
java -jar inkwash-cmd-0.5.1.jar generate -o "Your Company" -e "you@example.com" \
  --edition professional -m <32-hex-machine-code> --private-key-file inkwash-private-key.pem
# or --machine-auto to bind the current machine
```

Use `--output license.dat` to write the envelope to a file instead of stdout.

### 4. Verify a license

```bash
java -jar inkwash-cmd-0.5.1.jar validate --license license.dat --public-key-file inkwash-public-key.pem
```

The public key may also come from `INKWASH_PUBLIC_KEY`. Verification mirrors the backend
(SHA256withRSA over the base64 content) and exits non-zero on any failure. Note: the backend does
not enforce expiry by design, and the CLI mirrors that behavior.

### 5. Install in the backend

- Set `LICENSE_PUBLIC_KEY` on the API host so it can verify your signature.
- Place the signed `license.dat` on the API classpath (`inkwash-api/src/main/resources/license.dat`).
  The repo ships a local development licence; replace it with your own signed file.
- **The backend starts fine without a valid licence.** `LicenseInterceptor` gates requests at
  runtime and returns `403` with a `requireLicense` body for `/api/system/**`, `/api/cms/**` and
  `/api/monitor/**`. Restart the API after replacing the file.
- Note that `/api/support/file/**`, `/api/profile/**` and `/api/preference/**` are not gated, and
  public reads such as `GET /api/cms/articles` are exempt.

## Security Notes

- The private key is what signs licenses — keep it offline, never commit it to the repository, and
  never paste it into a shell command. Use `--private-key-file` or `INKWASH_PRIVATE_KEY`.
- A machine code is a one-way hash; it cannot be reversed into hardware information.
- Neither this CLI nor the backend enforces the licence expiry date. Verification mirrors that
  behaviour on purpose, so a signed licence stays valid; use the edition's user cap and the
  signature itself as your enforcement levers.

## Development

```bash
mvn test
```

## Layout (Key Files)

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

## License

GPL-3.0 — see the repository root `LICENSE`.