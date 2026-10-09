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
package top.ruilink.inkwash.system.service;

import java.util.List;

import top.ruilink.inkwash.base.domain.PageResult;
import top.ruilink.inkwash.base.enums.AuthType;
import top.ruilink.inkwash.base.enums.BaseStatus;
import top.ruilink.inkwash.system.api.param.UserParam;
import top.ruilink.inkwash.system.api.query.UserQuery;
import top.ruilink.inkwash.system.api.view.ResetPasswordView;
import top.ruilink.inkwash.system.api.view.UserView;
import top.ruilink.inkwash.system.domain.SysUser;

/**
 * User service interface.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public interface UserService {

	SysUser findByIdentityAndType(String identity, AuthType authType);

	SysUser getById(Long userId);

	SysUser getByPhone(String phone);

	/**
	 * Finds a user by email, used when OAuth2 binds automatically.
	 */
	SysUser getByEmail(String email);

	PageResult<UserView> pageSearch(UserQuery query);

	/**
	 * Counts the total number of users.
	 */
	long count();

	UserView getUserDetail(Long userId);

	UserView createUser(UserParam param);

	UserView updateUser(Long userId, UserParam param);

	void deleteUser(Long userId);

	void assignGroups(Long userId, List<Long> groupIds);

	/**
	 * Adds a user to the default group, used during registration. When the default
	 * group is unconfigured or missing it is skipped silently so registration is
	 * unaffected.
	 */
	void joinDefaultGroup(Long userId);

	ResetPasswordView resetPassword(Long userId);

	void updateStatus(Long userId, BaseStatus status);

	/**
	 * Creates a simple user, for internal flows such as automatic OAuth2
	 * registration.
	 */
	SysUser createSimpleUser(String nickname, String email);

	/**
	 * Creates a user, for internal flows such as registration.
	 */
	SysUser createRawUser(SysUser user);

	void updateRawUser(SysUser user);
}
