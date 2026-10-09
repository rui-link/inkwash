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
package top.ruilink.inkwash.security.service.login;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;
import top.ruilink.inkwash.base.exception.BusinessException;
import top.ruilink.inkwash.security.api.param.LoginParam;
import top.ruilink.inkwash.security.api.param.OAuth2LoginParam;
import top.ruilink.inkwash.security.service.OAuth2LoginService;

/**
 * Third-party OAuth2 login strategy.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Slf4j
@Component
public class OAuth2LoginStrategy implements LoginStrategy {

	private final OAuth2LoginService oauth2LoginService;

	public OAuth2LoginStrategy(OAuth2LoginService oauth2LoginService) {
		this.oauth2LoginService = oauth2LoginService;
	}

	@Override
	public boolean supports(LoginParam loginParam) {
		return loginParam instanceof OAuth2LoginParam;
	}

	@Override
	public LoginResult authenticate(LoginParam loginParam) {
		OAuth2LoginParam param = (OAuth2LoginParam) loginParam;
		String provider = param.inputOauth2Provider();
		String openId = param.inputOpenId();

		if (!StringUtils.hasText(provider) || !StringUtils.hasText(openId)) {
			throw new BusinessException("error.oauth2.invalid_params");
		}
		return oauth2LoginService.login(provider, openId, null, null, null, null);
	}
}
