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
package top.ruilink.inkwash.monitor.service;

import java.util.List;

import top.ruilink.inkwash.base.domain.PageResult;
import top.ruilink.inkwash.monitor.api.query.LoginInfoQuery;
import top.ruilink.inkwash.monitor.api.view.LoginInfoView;
import top.ruilink.inkwash.monitor.domain.LoginInfo;

/**
 * Login log service.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public interface LoginInfoService {

	/**
	 * Records a login log entry.
	 */
	Long recordLoginInfo(LoginInfo loginInfo);

	/**
	 * Looks up an entry by ID.
	 */
	LoginInfo getById(Long id);

	/**
	 * Queries a page of entries.
	 */
	PageResult<LoginInfoView> listLoginInfos(LoginInfoQuery query);

	/**
	 * Deletes an entry.
	 */
	void deleteById(Long id);

	/**
	 * Deletes entries in bulk.
	 */
	void deleteByIds(List<Long> ids);

	/**
	 * Removes every entry.
	 */
	void deleteAll();

	List<LoginInfo> listRecent(int limit);

	/**
	 * Closes the user's most recent still-open login session by stamping its logout
	 * time.
	 *
	 * <p>
	 * Authentication events include logout: without this, a login session is never
	 * closed in {@code mon_login_info} and the authentication trail cannot be
	 * reconstructed. The {@code @OperateTrace} journal deliberately does not cover
	 * this — see the division of responsibility between the two tables in the
	 * design document.
	 *
	 * @param userId user whose open session should be closed; {@code null} is
	 *               ignored
	 * @return number of sessions closed (0 when there was none, or the user was
	 *         anonymous)
	 */
	int recordLogout(Long userId);
}
