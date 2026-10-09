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
package top.ruilink.inkwash.system.domain.credential;

import top.ruilink.inkwash.base.enums.AuthType;
import top.ruilink.inkwash.base.enums.OAuth2Provider;
import java.time.LocalDateTime;

/**
 * OAuth2 credential record holding provider tokens and expiry.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public record OAuth2Credential(OAuth2Provider provider, String openId, String accessToken, String refreshToken,
		LocalDateTime expireTime) implements Credential {
	@Override
	public AuthType getType() {
		return AuthType.OAUTH2;
	}

	public boolean isTokenExpired() {
		return expireTime == null || expireTime.isBefore(LocalDateTime.now());
	}

	/**
	 * Returns a new OAuth2Credential instance with refreshed tokens.
	 */
	public OAuth2Credential withTokens(String accessToken, String refreshToken, LocalDateTime expireTime) {
		return new OAuth2Credential(provider, openId, accessToken, refreshToken, expireTime);
	}

	/**
	 * Returns a new OAuth2Credential instance with cleared tokens.
	 */
	public OAuth2Credential withoutTokens() {
		return new OAuth2Credential(provider, openId, null, null, null);
	}
}
