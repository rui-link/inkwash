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

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import top.ruilink.inkwash.base.enums.AuthType;
import top.ruilink.inkwash.base.enums.IdentityType;
import top.ruilink.inkwash.base.enums.IdentityVerifier;
import top.ruilink.inkwash.base.enums.OAuth2Provider;
import top.ruilink.inkwash.base.exception.BusinessException;
import top.ruilink.inkwash.security.service.OAuth2LoginService;
import top.ruilink.inkwash.security.service.login.LoginResult;
import top.ruilink.inkwash.system.domain.SysAccount;
import top.ruilink.inkwash.system.domain.SysIdentity;
import top.ruilink.inkwash.system.domain.SysUser;
import top.ruilink.inkwash.system.domain.credential.OAuth2Credential;
import top.ruilink.inkwash.system.enums.UserStatus;
import top.ruilink.inkwash.system.service.AccountService;
import top.ruilink.inkwash.system.service.IdentityService;
import top.ruilink.inkwash.system.service.UserService;

/**
 * Core OAuth2 login service handling OIDC claim reuse, email binding, new user
 * creation and OAuth2 credential maintenance in one place.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2LoginServiceImpl implements OAuth2LoginService {

	private final IdentityService identityService;
	private final UserService userService;
	private final AccountService accountService;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public LoginResult login(String provider, String openId, String nickname, String verifiedEmail, String accessToken,
			String refreshToken) {
		String p = StringUtils.hasText(provider) ? provider.toLowerCase() : "";
		if (!StringUtils.hasText(p) || !StringUtils.hasText(openId)) {
			throw new BusinessException("error.oauth2.invalid_params");
		}
		try {
			SysIdentity oidc = identityService.findByTypeProviderValue(IdentityType.OIDC_SUB, p, openId);
			if (oidc != null) {
				SysUser user = requireEnabled(oidc.getUserId());
				userService.joinDefaultGroup(user.getId());
				upsertOAuth2Account(user.getId(), p, openId, accessToken, refreshToken);
				oidc.setLoginTime(LocalDateTime.now());
				identityService.update(oidc);
				log.info("OAuth2 复用已有用户, userId={}, provider={}", user.getId(), p);
				return new LoginResult(user.getId(), p + ":" + openId, AuthType.OAUTH2);
			}

			if (StringUtils.hasText(verifiedEmail)) {
				SysIdentity emailClaim = identityService.findByTypeValue(IdentityType.EMAIL, verifiedEmail);
				if (emailClaim != null) {
					SysUser user = requireEnabled(emailClaim.getUserId());
					bindOidcTo(user.getId(), p, openId, verifiedEmail, accessToken, refreshToken);
					log.info("OAuth2 经邮箱绑定已有用户, userId={}, provider={}", user.getId(), p);
					return new LoginResult(user.getId(), p + ":" + openId, AuthType.OAUTH2);
				}
			}

			// A brand new user
			String effectiveNickname = StringUtils.hasText(nickname) ? nickname
					: p + "_" + openId.substring(Math.max(0, openId.length() - 8));
			SysUser user = userService.createSimpleUser(effectiveNickname, verifiedEmail);
			userService.joinDefaultGroup(user.getId());
			bindOidcTo(user.getId(), p, openId, verifiedEmail, accessToken, refreshToken);
			log.info("OAuth2 新用户自动创建, userId={}, provider={}", user.getId(), p);
			return new LoginResult(user.getId(), p + ":" + openId, AuthType.OAUTH2);
		} catch (BusinessException e) {
			throw e;
		} catch (Exception e) {
			log.error("oauth2 login failed: provider={}", p, e);
			throw new BusinessException("error.oauth2.failed");
		}
	}

	private SysUser requireEnabled(Long userId) {
		SysUser user = userService.getById(userId);
		if (user == null || !UserStatus.ENABLE.equals(user.getStatus())) {
			throw new BusinessException("error.user.not_found");
		}
		return user;
	}

	private void bindOidcTo(Long userId, String provider, String openId, String email, String accessToken,
			String refreshToken) {
		identityService.findOrCreateVerified(IdentityType.OIDC_SUB, openId, provider, userId, IdentityVerifier.OAUTH2);
		upsertOAuth2Account(userId, provider, openId, accessToken, refreshToken);
	}

	private void upsertOAuth2Account(Long userId, String provider, String openId, String accessToken,
			String refreshToken) {
		OAuth2Provider providerEnum = resolveProvider(provider);
		OAuth2Credential cred = new OAuth2Credential(providerEnum, openId, accessToken, refreshToken,
				accessToken != null ? LocalDateTime.now().plusHours(1) : null);
		SysAccount account = accountService.findByProviderAndOpenId(provider, openId);
		if (account == null) {
			account = new SysAccount();
			account.setUserId(userId);
			account.setIdentity(provider + ":" + openId);
			account.setAuthType(AuthType.OAUTH2);
			account.setCredential(cred);
			accountService.create(account);
		} else {
			account.setCredential(cred);
			accountService.update(account);
		}
	}

	private static OAuth2Provider resolveProvider(String provider) {
		return switch (provider.toLowerCase()) {
		case "github" -> OAuth2Provider.GITHUB;
		case "alipay" -> OAuth2Provider.ALIPAY;
		case "wechat" -> OAuth2Provider.WECHAT;
		case "google" -> OAuth2Provider.GOOGLE;
		case "apple" -> OAuth2Provider.APPLE;
		default -> OAuth2Provider.GITHUB;
		};
	}
}
