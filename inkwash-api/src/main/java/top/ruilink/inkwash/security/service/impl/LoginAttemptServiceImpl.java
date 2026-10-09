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
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import top.ruilink.inkwash.base.service.CacheService;
import top.ruilink.inkwash.security.AuthConsts;
import top.ruilink.inkwash.security.config.CredentialConfig;
import top.ruilink.inkwash.security.service.LoginAttemptService;

/**
 * Login attempt service implementation, in-memory. Implements the password
 * failure lockout: three consecutive failures lock the account for ten minutes.
 * Uses CacheService to hold the failure state.
 * 
 * @author Dyllon
 * @since 0.5.1
 */
@Slf4j
@Service
public class LoginAttemptServiceImpl implements LoginAttemptService {

	private final CredentialConfig credentialConfig;
	private final CacheService cacheService;

	public LoginAttemptServiceImpl(CredentialConfig credentialConfig, CacheService cacheService) {
		this.credentialConfig = credentialConfig;
		this.cacheService = cacheService;
	}

	/**
	 * Records a failed password login.
	 */
	@Override
	public void recordLoginFailure(String username) {
		String lockKey = AuthConsts.LOGIN_LOCK_PREFIX + username;
		if (cacheService.hasKey(lockKey))
			return;

		String attemptKey = AuthConsts.LOGIN_ATTEMPT_PREFIX + username;
		cacheService.increment(attemptKey, 1);
		Long count = cacheService.getIncrement(attemptKey);
		if (count == null)
			count = 1L;

		if (count >= credentialConfig.getPassword().getMaxErrorTimes()) {
			cacheService.put(lockKey, true, Duration.ofMinutes(credentialConfig.getPassword().getLockMinutes()));
			cacheService.put(AuthConsts.LOGIN_LOCKTIME_PREFIX + username,
					LocalDateTime.now().plusMinutes(credentialConfig.getPassword().getLockMinutes()).toString(),
					Duration.ofMinutes(credentialConfig.getPassword().getLockMinutes()));
			log.warn("账号登录失败次数过多，已锁定, username={}, lockMinutes={}", username,
					credentialConfig.getPassword().getLockMinutes());
		} else {
			log.debug("记录登录失败, username={}, failureCount={}", username, count);
		}
	}

	/**
	 * Records a successful password login and clears the failure record.
	 */
	@Override
	public void recordLoginSuccess(String username) {
		cacheService.evict(AuthConsts.LOGIN_LOCK_PREFIX + username);
		cacheService.evict(AuthConsts.LOGIN_ATTEMPT_PREFIX + username);
		cacheService.evict(AuthConsts.LOGIN_LOCKTIME_PREFIX + username);
		log.debug("登录成功，清除失败记录, username={}", username);
	}

	/**
	 * Checks whether the account is locked.
	 */
	@Override
	public boolean isLocked(String username) {
		return cacheService.hasKey(AuthConsts.LOGIN_LOCK_PREFIX + username);
	}

	/**
	 * Gets the number of remaining attempts for the account.
	 */
	@Override
	public int getRemainingAttempts(String username) {
		if (isLocked(username))
			return -1;
		Long count = cacheService.getIncrement(AuthConsts.LOGIN_ATTEMPT_PREFIX + username);
		int c = count == null ? 0 : count.intValue();
		return Math.max(0, credentialConfig.getPassword().getMaxErrorTimes() - c);
	}

	/**
	 * Unlocks the account.
	 */
	@Override
	public void unlock(String username) {
		cacheService.evict(AuthConsts.LOGIN_LOCK_PREFIX + username);
		cacheService.evict(AuthConsts.LOGIN_ATTEMPT_PREFIX + username);
		cacheService.evict(AuthConsts.LOGIN_LOCKTIME_PREFIX + username);
		log.info("账号已解锁, username={}", username);
	}

	/**
	 * Gets the lock expiry time.
	 */
	@Override
	public LocalDateTime getLockoutExpiration(String username) {
		Object val = cacheService.get(AuthConsts.LOGIN_LOCKTIME_PREFIX + username);
		if (val instanceof String s)
			return LocalDateTime.parse(s);
		return null;
	}
}
