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
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import top.ruilink.inkwash.security.adapter.SysUserDetails;

/**
 * JWT token utility for generating, parsing, validating and refreshing tokens.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Slf4j
@Component
public class JwtTokenProvider {
	private final JwtTokenConfig jwtConfig;

	public JwtTokenProvider(JwtTokenConfig jwtConfig) {
		this.jwtConfig = jwtConfig;
	}

	private volatile SecretKey cachedSecretKey;

	/**
	 * Builds the signing key.
	 *
	 * The algorithm is pinned to HS384 explicitly. Previously the key was built via
	 * {@code Keys.hmacShaKeyFor(bytes)} and passed to {@code signWith(key)} without
	 * an algorithm, which made JJWT derive the algorithm from the key length: 64+
	 * bytes became HS512, 32-63 bytes became HS384/HS256. A short secret therefore
	 * silently downgraded the signature strength. Pinning it makes a too-short key
	 * fail loudly instead.
	 *
	 * @return the HS384 signing key
	 */
	private SecretKey getSecretKey() {
		if (cachedSecretKey == null) {
			synchronized (this) {
				if (cachedSecretKey == null) {
					byte[] keyBytes = jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8);
					cachedSecretKey = new SecretKeySpec(keyBytes, JwtTokenConfig.JWT_ALGORITHM);
				}
			}
		}
		return cachedSecretKey;
	}

	/**
	 * Generates an access token from user details.
	 * 
	 * @param userDetails Spring Security user details
	 * @return a JWT access token
	 */
	public String generateAccessToken(SysUserDetails userDetails) {
		// Build the custom claims: user ID, username and permission set
		Map<String, Object> claims = Map.of("userId", userDetails.getUser().getId(), "username",
				userDetails.getUsername(), "authorities",
				userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()));
		return generateToken(claims, userDetails.getUsername(), jwtConfig.getAccessExpireTime());
	}

	/**
	 * Generates a refresh token carrying only the username, used to refresh the
	 * access token.
	 * 
	 * @param username the username
	 * @return a JWT refresh token
	 */
	public String generateRefreshToken(String username) {
		return generateRefreshToken(username, UUID.randomUUID().toString(), UUID.randomUUID().toString());
	}

	public String generateRefreshToken(String username, String jti, String fam) {
		Map<String, Object> claims = Map.of("jti", jti, "fam", fam);
		return generateToken(claims, username, jwtConfig.getRefreshExpireTime());
	}

	/**
	 * Core token generation method.
	 * 
	 * @param claims     the custom claims
	 * @param subject    the subject, which is the username
	 * @param expireTime expiry in milliseconds
	 * @return a JWT token
	 */
	private String generateToken(Map<String, Object> claims, String subject, Long expireTime) {
		long currentTime = System.currentTimeMillis();
		return Jwts.builder().signWith(getSecretKey(), Jwts.SIG.HS384) // 固定使用 HS384 算法签名
				.claims(claims) // 自定义载荷
				.subject(subject) // 主题：唯一标识（用户名）
				.issuer(jwtConfig.getIssuer()) // 签发者
				.issuedAt(Date.from(Instant.ofEpochMilli(currentTime))) // 签发时间
				.expiration(Date.from(Instant.ofEpochMilli(currentTime + expireTime))) // 过期时间
				.compact(); // 生成令牌
	}

	/**
	 * Extracts the JWT token from the request header.
	 * 
	 * @param request HttpServletRequest
	 * @return the bare token string with the prefix removed, or null when absent
	 */
	public String extractTokenFromRequest(HttpServletRequest request) {
		try {
			String header = request.getHeader(jwtConfig.getHeader());
			if (StringUtils.hasText(header) && header.startsWith(jwtConfig.getTokenPrefix() + " ")) {
				return header.substring(jwtConfig.getTokenPrefix().length() + 1);
			}
		} catch (Exception e) {
			// Tomcat header handling bug, so the exception is ignored and null returned
			log.debug("Failed to extract token from request header: {}", e.getMessage());
		}
		return null;
	}

	/**
	 * Parses the token to obtain its claims.
	 *
	 * The verification algorithm is derived from the key itself, which
	 * {@link #getSecretKey()} pins to {@link JwtTokenConfig#JWT_ALGORITHM}. JJWT
	 * exposes no {@code verifyWith(key, alg)} overload, so the pinned key is what
	 * enforces HS384 here.
	 * 
	 * @param token the JWT token
	 * @return the claims
	 * @throws ExpiredJwtException      when the token has expired
	 * @throws MalformedJwtException    when the token is malformed
	 * @throws IllegalArgumentException when the token is null
	 */
	public Claims parseToken(String token) {
		return Jwts.parser().verifyWith(getSecretKey()).build().parseSignedClaims(token).getPayload();
	}

	/**
	 * Validates a token as neither expired nor badly signed.
	 * 
	 * @param token the JWT token
	 * @return true when valid, false otherwise
	 */
	public boolean validateToken(String token) {
		try {
			parseToken(token);
			return true;
		} catch (ExpiredJwtException e) {
			log.debug("JWT令牌已过期：{}", e.getMessage());
		} catch (MalformedJwtException e) {
			log.debug("JWT令牌格式错误：{}", e.getMessage());
		} catch (IllegalArgumentException e) {
			log.debug("JWT令牌为空或无效：{}", e.getMessage());
		} catch (Exception e) {
			log.warn("JWT令牌验证失败：{}", e.getMessage());
		}
		return false;
	}

	/**
	 * Extracts the username from a token.
	 * 
	 * @param token the JWT token
	 * @return the username
	 */
	public String extractUsername(String token) {
		return parseToken(token).getSubject();
	}

	/**
	 * Extracts the user ID from a token.
	 * 
	 * @param token the JWT token
	 * @return the user ID
	 */
	public Long extractUserId(String token) {
		return parseToken(token).get("userId", Long.class);
	}

	public String getTokenPrefix() {
		return this.jwtConfig.getTokenPrefix();
	}

	public Long getAccessExpireTime() {
		return this.jwtConfig.getAccessExpireTime();
	}
}
