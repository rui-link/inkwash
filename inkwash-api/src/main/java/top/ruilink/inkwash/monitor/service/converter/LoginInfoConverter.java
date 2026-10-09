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
package top.ruilink.inkwash.monitor.service.converter;

import top.ruilink.inkwash.monitor.api.view.LoginInfoView;
import top.ruilink.inkwash.monitor.domain.LoginInfo;

/**
 * Login log entity to response view converter.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public final class LoginInfoConverter {
	private LoginInfoConverter() {
	}

	public static LoginInfoView toView(LoginInfo entity) {
		if (entity == null)
			return null;
		LoginInfoView view = new LoginInfoView();
		view.setId(entity.getId());
		view.setUserId(entity.getUserId());
		view.setIdentity(entity.getIdentity());
		view.setLoginType(entity.getLoginType());
		view.setAddress(entity.getAddress());
		view.setDevice(entity.getDevice());
		view.setBrowser(entity.getBrowser());
		view.setOstype(entity.getOstype());
		view.setStatus(entity.getStatus());
		view.setMessage(entity.getMessage());
		view.setLoginTime(entity.getLoginTime());
		view.setCreateTime(entity.getCreateTime());
		view.setCreator(entity.getCreator());
		view.setUpdater(entity.getUpdater());
		return view;
	}
}
