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
package top.ruilink.inkwash.security.jwt;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import lombok.Data;

/**
 * JWT configuration properties bound to the jwt section of application.yml.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Data
@Component
@ConfigurationProperties(prefix = "auth.jwt")
public class JwtTokenConfig {
	/**
	 * The pinned signing algorithm. HS384 requires a key of at least 384 bits (48
	 * bytes).
	 */
	public static final String JWT_ALGORITHM = "HmacSHA384";
	/**
	 * Minimum key length in bytes for {@link #JWT_ALGORITHM}.
	 */
	public static final int MIN_SECRET_BYTES = 48;
	/**
	 * Secrets that must never be used to sign real tokens. The value shipped as a
	 * default in earlier revisions is included because it is public knowledge:
	 * anyone who can read the repository can forge an admin token with it. The
	 * dev-profile value is deliberately NOT listed here, since application-dev.yaml
	 * relies on it and it is unreachable from the prod profile.
	 */
	private static final Set<String> FORBIDDEN_SECRETS = Set.of("strive-continuously-to-strengthen-oneself-48char",
			"your-jwt-secret-here", "changeit", "secret", "jwt-secret", "your-encrypt-key");

	/**
	 * JWT signing key.
	 */
	private String secret;
	/**
	 * Access token expiry in milliseconds.
	 */
	private Long accessExpireTime = 86400000L;
	/**
	 * Refresh token expiry in milliseconds.
	 */
	private Long refreshExpireTime = 604800000L;
	/**
	 * Token prefix, such as Bearer.
	 */
	private String tokenPrefix;
	/**
	 * Request header carrying the token.
	 */
	private String header;
	/**
	 * Token issuer.
	 */
	private String issuer;

	/**
	 * Validates the signing key at startup so a missing or weak secret fails
	 * immediately with an actionable message instead of a placeholder-resolution
	 * error or a silent algorithm downgrade on the first login.
	 *
	 * <p>
	 * The leading line of every message is deliberately ASCII-only: consoles on
	 * Windows frequently mangle non-ASCII output, and a startup error nobody can
	 * read is as bad as no error at all.
	 */
	@PostConstruct
	public void validate() {
		if (!StringUtils.hasText(secret) || isUnresolvedPlaceholder(secret)) {
			throw new IllegalStateException(
					"""
							[Inkwash] JWT signing key is missing (env AUTH_JWT_SECRET is not set). Refusing to start.

							生成一个密钥 / generate a key:
							  openssl rand -base64 48

							Linux / macOS:
							  export AUTH_JWT_SECRET="$(openssl rand -base64 48)"
							Windows PowerShell:
							  $env:AUTH_JWT_SECRET = [Convert]::ToBase64String(1..48 | ForEach-Object { Get-Random -Maximum 256 })

							Docker: 把 AUTH_JWT_SECRET 传给容器，例如
							  docker run --env-file .env ... 或 -e AUTH_JWT_SECRET="$(openssl rand -base64 48)"
							注意环境变量名是 AUTH_JWT_SECRET，不是 JWT_SECRET。
							本地开发请使用 dev profile（application-dev.yaml 内置仅开发用密钥）。""");
		}
		int length = secret.getBytes(StandardCharsets.UTF_8).length;
		if (length < MIN_SECRET_BYTES) {
			throw new IllegalStateException("""
					[Inkwash] JWT signing key is too short: %d bytes, minimum is %d bytes.

					The signing algorithm is pinned to %s, so a short key weakens or breaks signing.
					请重新生成 / regenerate: openssl rand -base64 48""".formatted(length, MIN_SECRET_BYTES, JWT_ALGORITHM));
		}
		if (FORBIDDEN_SECRETS.contains(secret)) {
			throw new IllegalStateException(
					"""
							[Inkwash] JWT signing key is a publicly known example value; anyone can forge an admin token with it.

							请通过 AUTH_JWT_SECRET 设置自己的密钥 / set your own: openssl rand -base64 48""");
		}
	}

	/**
	 * Detects a value that is still a literal <code>${...}</code> placeholder.
	 *
	 * <p>
	 * A <code>String</code>-typed configuration property silently keeps the raw
	 * placeholder when the referenced environment variable is absent, so
	 * <code>${AUTH_JWT_SECRET}</code> can reach this class as an 18-character
	 * "secret". Treat that as missing rather than than letting it be counted as a
	 * (very short) key.
	 */
	private boolean isUnresolvedPlaceholder(String value) {
		return value.startsWith("${") && value.endsWith("}");
	}
}
