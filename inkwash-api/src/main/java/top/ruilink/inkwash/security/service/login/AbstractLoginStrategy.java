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

import java.time.LocalDateTime;

import top.ruilink.inkwash.base.enums.AuthType;
import top.ruilink.inkwash.base.exception.BusinessException;
import top.ruilink.inkwash.system.domain.SysAccount;
import top.ruilink.inkwash.system.domain.SysIdentity;
import top.ruilink.inkwash.system.domain.SysUser;
import top.ruilink.inkwash.system.enums.UserStatus;
import top.ruilink.inkwash.system.service.AccountService;
import top.ruilink.inkwash.system.service.IdentityService;
import top.ruilink.inkwash.system.service.UserService;

/**
 * Base login strategy centralising cross-cutting logic such as the enabled user
 * check and last login time update.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public abstract class AbstractLoginStrategy implements LoginStrategy {

	protected final UserService userService;
	protected final AccountService accountService;
	protected final IdentityService identityService;

	protected AbstractLoginStrategy(UserService userService, AccountService accountService,
			IdentityService identityService) {
		this.userService = userService;
		this.accountService = accountService;
		this.identityService = identityService;
	}

	/**
	 * Constructor for identity claim based strategies that need no AccountService.
	 */
	protected AbstractLoginStrategy(UserService userService, IdentityService identityService) {
		this(userService, null, identityService);
	}

	protected SysUser requireEnabledUser(Long userId) {
		var user = userService.getById(userId);
		if (user == null || !UserStatus.ENABLE.equals(user.getStatus())) {
			throw new BusinessException("error.user.not_found");
		}
		return user;
	}

	/**
	 * Shared success path for credential logins through sys_account: validates the
	 * user and updates the last login time.
	 */
	protected LoginResult accountSuccess(SysAccount account) {
		requireEnabledUser(account.getUserId());
		account.recordLastLogin();
		accountService.update(account);
		return new LoginResult(account.getUserId(), account.getIdentity(), account.getAuthType());
	}

	/**
	 * Shared success path for identity claim logins through sys_identity: validates
	 * the user and claim and updates the claim login time.
	 */
	protected LoginResult claimSuccess(SysIdentity claim, AuthType authType) {
		requireEnabledUser(claim.getUserId());
		if (!claim.identityVerified()) {
			throw new BusinessException("error.user.not_found");
		}
		claim.setLoginTime(LocalDateTime.now());
		identityService.update(claim);
		return new LoginResult(claim.getUserId(), claim.getIdentityValue(), authType);
	}
}
