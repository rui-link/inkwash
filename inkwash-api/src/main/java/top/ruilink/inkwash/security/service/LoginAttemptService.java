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

import java.time.LocalDateTime;

/**
 * Login attempt service recording failed login counts to implement account
 * locking.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public interface LoginAttemptService {

	/**
	 * Records a failed password login.
	 * 
	 * @param username the username or account
	 */
	void recordLoginFailure(String username);

	/**
	 * Records a successful password login and clears the failure record.
	 * 
	 * @param username the username or account
	 */
	void recordLoginSuccess(String username);

	/**
	 * Checks whether the account is locked.
	 * 
	 * @param username the username or account
	 * @return true when locked, false otherwise
	 */
	boolean isLocked(String username);

	/**
	 * Gets the number of remaining attempts for the account.
	 * 
	 * @param username the username or account
	 * @return the remaining attempts, or -1 when locked
	 */
	int getRemainingAttempts(String username);

	/**
	 * Unlocks the account.
	 * 
	 * @param username the username or account
	 */
	void unlock(String username);

	/**
	 * Gets the lock expiry time.
	 * 
	 * @param username the username or account
	 * @return the lock expiry time, or null when the account is not locked
	 */
	LocalDateTime getLockoutExpiration(String username);
}
