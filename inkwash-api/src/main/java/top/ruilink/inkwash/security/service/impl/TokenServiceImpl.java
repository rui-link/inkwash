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
package top.ruilink.inkwash.security.service.impl;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import top.ruilink.inkwash.base.CharConsts;
import top.ruilink.inkwash.base.exception.BusinessException;
import top.ruilink.inkwash.base.service.CacheService;
import top.ruilink.inkwash.security.adapter.SysUserDetails;
import top.ruilink.inkwash.security.adapter.SysUserDetailsService;
import top.ruilink.inkwash.security.jwt.JwtTokenConfig;
import top.ruilink.inkwash.security.jwt.JwtTokenProvider;
import top.ruilink.inkwash.security.service.TokenService;
import top.ruilink.inkwash.security.api.view.AccountView;

/**
 * Token service implementation generating JWTs with JwtTokenProvider and
 * managing the token revocation list.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Slf4j
@Service
public class TokenServiceImpl implements TokenService {

	private final JwtTokenProvider jwtTokenProvider;
	private final JwtTokenConfig jwtConfig;
	private final SysUserDetailsService userDetailsService;
	private final CacheService cacheService;

	public TokenServiceImpl(JwtTokenProvider jwtTokenProvider, JwtTokenConfig jwtConfig,
			SysUserDetailsService userDetailsService, CacheService cacheService) {
		this.jwtTokenProvider = jwtTokenProvider;
		this.jwtConfig = jwtConfig;
		this.userDetailsService = userDetailsService;
		this.cacheService = cacheService;
	}

	/**
	 * Generates an access token and a refresh token.
	 */
	@Override
	public AccountView generateTokens(Long userId, String identity, Integer authType) {
		log.info("generateTokens called: userId={}, identity={}, authType={}", userId, identity, authType);

		// Build the full username as authType:identity
		String username = authType + ":" + identity;
		log.info("username constructed: {}", username);

		// Load the user details
		SysUserDetails userDetails = (SysUserDetails) userDetailsService.loadUserByUsername(username);
		log.info("userDetails loaded: {}", userDetails.getUsername());

		// Generate the tokens
		String accessToken = jwtTokenProvider.generateAccessToken(userDetails);
		String jti = UUID.randomUUID().toString();
		String refreshToken = jwtTokenProvider.generateRefreshToken(username, jti, jti);
		log.info("tokens generated");

		// Build the return value using the real JWT expiry
		LocalDateTime expireTime = Instant.now().plusMillis(jwtConfig.getAccessExpireTime())
				.atZone(ZoneId.systemDefault()).toLocalDateTime();
		AccountView accountView = new AccountView();
		accountView.setUserId(userId);
		accountView.setIdentity(username);
		accountView.setCredential(accessToken + CharConsts.COLON + refreshToken);
		accountView.setExpireTime(expireTime);
		log.info("AccountView created with expireTime={}", expireTime);

		return accountView;
	}

	/**
	 * Refreshes the access token.
	 */
	@Override
	public AccountView refreshTokens(String refreshToken) {
		if (!jwtTokenProvider.validateToken(refreshToken)) {
			throw new BusinessException("error.auth.refresh_invalid");
		}

		Claims claims = jwtTokenProvider.parseToken(refreshToken);
		String jti = claims.get("jti", String.class);
		String fam = claims.get("fam", String.class);

		if (jti == null || fam == null) {
			throw new BusinessException("error.auth.refresh_malformed");
		}

		if (cacheService.hasKey("jwt:revoked:" + fam)) {
			throw new BusinessException("error.auth.token_expired");
		}

		if (cacheService.hasKey("jwt:consumed:" + jti)) {
			cacheService.put("jwt:revoked:" + fam, true, refreshTokenTtl());
			cacheService.evict("jwt:consumed:" + jti);
			log.warn("检测到刷新令牌重用，已撤销令牌家族 fam={}", fam);
			throw new BusinessException("error.auth.token_reuse_detected");
		}

		cacheService.put("jwt:consumed:" + jti, fam, refreshTokenTtl());

		String username = claims.getSubject();
		SysUserDetails userDetails = (SysUserDetails) userDetailsService.loadUserByUsername(username);

		String newJti = UUID.randomUUID().toString();
		String newAccessToken = jwtTokenProvider.generateAccessToken(userDetails);
		String newRefreshToken = jwtTokenProvider.generateRefreshToken(username, newJti, fam);

		LocalDateTime expireTime = Instant.now().plusMillis(jwtConfig.getAccessExpireTime())
				.atZone(ZoneId.systemDefault()).toLocalDateTime();
		AccountView accountView = new AccountView();
		accountView.setUserId(userDetails.getUser().getId());
		accountView.setIdentity(username);
		accountView.setCredential(newAccessToken + CharConsts.COLON + newRefreshToken);
		accountView.setExpireTime(expireTime);

		log.debug("刷新令牌成功, username={}", username);
		return accountView;
	}

	/**
	 * Revokes a token by adding it to the blacklist.
	 */
	@Override
	public void revokeToken(String accessToken) {
		if (accessToken == null)
			return;
		try {
			Long userId = jwtTokenProvider.extractUserId(accessToken);
			long expiration = jwtTokenProvider.parseToken(accessToken).getExpiration().getTime();
			long ttlMs = expiration - System.currentTimeMillis();
			if (ttlMs > 0) {
				cacheService.put("jwt:blacklist:" + accessToken, expiration, Duration.ofMillis(ttlMs));
			}
			log.info("令牌已撤销, userId={}", userId);
		} catch (Exception e) {
			log.warn("撤销令牌失败: {}", e.getMessage(), e);
		}
	}

	/**
	 * 重放检测标记的存活时长。
	 *
	 * <p>
	 * 该 TTL 必须不短于 refresh 令牌自身的有效期，否则重放检测窗口会在令牌 仍然有效时提前关闭——一枚被盗的 refresh
	 * 令牌可在检测失效后无限重放。 因此这里从 {@code auth.jwt.refresh-expire-time} 推导，而不是使用常量。
	 *
	 * @return 保留期，至少为 1 小时，防止配置被误设为极小值
	 */
	private Duration refreshTokenTtl() {
		long configured = jwtConfig.getRefreshExpireTime() == null ? 0L : jwtConfig.getRefreshExpireTime();
		return Duration.ofMillis(Math.max(configured, Duration.ofHours(1).toMillis()));
	}

	/**
	 * Checks whether a token has been revoked.
	 */
	@Override
	public boolean isTokenRevoked(String token) {
		if (token == null)
			return false;
		return cacheService.hasKey("jwt:blacklist:" + token);
	}

	/**
	 * Extracts the user ID from a token.
	 */
	@Override
	public Long extractUserId(String token) {
		try {
			return jwtTokenProvider.extractUserId(token);
		} catch (Exception e) {
			log.warn("提取用户ID失败: {}", e.getMessage());
			return null;
		}
	}

}
