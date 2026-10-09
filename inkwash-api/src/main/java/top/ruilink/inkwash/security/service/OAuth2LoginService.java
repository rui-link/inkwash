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
package top.ruilink.inkwash.security.service;

import top.ruilink.inkwash.security.service.login.LoginResult;

/**
 * Core OAuth2 login service.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public interface OAuth2LoginService {

	/**
	 * OAuth2 login reusing an existing user by OIDC claims or verified email,
	 * otherwise creating a new user and binding it.
	 *
	 * @param provider      the third party provider identifier, such as github or
	 *                      google, case insensitive
	 * @param openId        the unique third party user identifier
	 * @param nickname      the nickname, possibly null and generated automatically
	 *                      for new users
	 * @param verifiedEmail the email verified by the provider, possibly null
	 * @param accessToken   the access token, possibly null
	 * @param refreshToken  the refresh token, possibly null
	 */
	LoginResult login(String provider, String openId, String nickname, String verifiedEmail, String accessToken,
			String refreshToken);
}
