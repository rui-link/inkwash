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

import top.ruilink.inkwash.security.api.view.AccountView;

/**
 * Token service responsible for generating, refreshing and revoking JWT tokens.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public interface TokenService {

	/**
	 * Generates an access token and a refresh token.
	 * 
	 * @param userId   the user ID
	 * @param identity the login identity, such as username or phone number
	 * @param authType the authentication type
	 * @return an AccountView carrying the tokens
	 */
	AccountView generateTokens(Long userId, String identity, Integer authType);

	/**
	 * Refreshes the access token.
	 * 
	 * @param refreshToken the refresh token
	 * @return the new token pair
	 */
	AccountView refreshTokens(String refreshToken);

	/**
	 * Revokes a token on logout.
	 * 
	 * @param accessToken the access token
	 */
	void revokeToken(String accessToken);

	/**
	 * Checks whether a token has been revoked.
	 * 
	 * @param token the token
	 * @return true when revoked, false when valid
	 */
	boolean isTokenRevoked(String token);

	/**
	 * Extracts the user ID from a token.
	 * 
	 * @param token the access token
	 * @return the user ID
	 */
	Long extractUserId(String token);
}
