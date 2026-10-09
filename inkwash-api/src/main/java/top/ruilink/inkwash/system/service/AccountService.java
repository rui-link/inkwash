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

import top.ruilink.inkwash.base.enums.AuthType;
import top.ruilink.inkwash.system.domain.SysAccount;

/**
 * Login account service interface.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public interface AccountService {
	SysAccount findByIdentityAndType(String identity, AuthType authType);

	SysAccount findByProviderAndOpenId(String provider, String openId);

	Long create(SysAccount account);

	void update(SysAccount account);

	SysAccount getById(Long id);

	List<SysAccount> listByUserId(Long userId);

	int countByUserId(Long userId);

	void deleteById(Long id);
}
