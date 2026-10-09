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
package top.ruilink.inkwash.system.service.converter;

import java.time.LocalDateTime;
import java.util.List;

import top.ruilink.inkwash.system.api.param.UserParam;
import top.ruilink.inkwash.security.api.view.AccountView;
import top.ruilink.inkwash.system.api.view.UserView;
import top.ruilink.inkwash.system.domain.SysAccount;
import top.ruilink.inkwash.system.domain.SysUser;
import top.ruilink.inkwash.system.enums.UserStatus;

/**
 * User and account entities to views, user param to entity converter.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public final class UserConverter {
	private UserConverter() {
	}

	public static UserView toUserView(SysUser user) {
		if (user == null)
			return null;
		UserView view = new UserView();
		view.setId(user.getId());
		view.setNickname(user.getNickname());
		view.setRealname(user.getRealname());
		view.setGender(user.getGender());
		view.setAvatar(user.getAvatar());
		view.setPhone(user.getPhone());
		view.setEmail(user.getEmail());
		view.setStatus(user.getStatus());
		view.setIdtype(user.getIdtype());
		view.setIdcode(user.getIdcode());
		view.setCreateTime(user.getCreateTime());
		view.setUpdateTime(user.getUpdateTime());
		return view;
	}

	public static UserView toUserDetailView(SysUser user) {
		if (user == null)
			return null;
		UserView view = new UserView();
		view.setId(user.getId());
		view.setNickname(user.getNickname());
		view.setRealname(user.getRealname());
		view.setGender(user.getGender());
		view.setAvatar(user.getAvatar());
		view.setPhone(user.getPhone());
		view.setEmail(user.getEmail());
		view.setStatus(user.getStatus());
		view.setIdtype(user.getIdtype());
		view.setIdcode(user.getIdcode());
		view.setMotto(user.getMotto());
		view.setBirthDate(user.getBirthDate());
		view.setEducation(user.getEducation());
		view.setLocation(user.getLocation());
		view.setBiography(user.getBiography());
		// groups and roles need a separate query, so they are left unset
		view.setCreateTime(user.getCreateTime());
		view.setUpdateTime(user.getUpdateTime());
		return view;
	}

	public static List<AccountView> toAccountViews(List<SysAccount> accounts) {
		if (accounts == null)
			return List.of();
		return accounts.stream().map(a -> {
			AccountView v = new AccountView();
			v.setIdentity(a.getIdentity());
			v.setAuthType(a.getAuthType());
			v.setStatus(a.getLoginTime() != null ? "ENABLE" : a.getStatus());
			v.setLoginTime(a.getLoginTime());
			return v;
		}).toList();
	}

	public static SysUser toUserEntity(UserParam param) {
		if (param == null)
			return null;
		SysUser user = new SysUser();
		user.setNickname(param.getNickname());
		user.setRealname(param.getRealname());
		user.setGender(param.getGender());
		user.setPhone(param.getPhone());
		user.setEmail(param.getEmail());
		user.setAvatar(param.getAvatar());
		user.setIdtype(param.getIdtype());
		user.setIdcode(param.getIdcode());
		user.setMotto(param.getMotto());
		user.setBirthDate(param.getBirthDate());
		user.setEducation(param.getEducation());
		user.setLocation(param.getLocation());
		user.setBiography(param.getBiography());
		user.setStatus(UserStatus.ENABLE);
		user.setCreateTime(LocalDateTime.now());
		user.setUpdateTime(LocalDateTime.now());
		return user;
	}

	public static void updateUserEntity(SysUser user, UserParam param) {
		if (user == null || param == null)
			return;
		if (param.getNickname() != null)
			user.setNickname(param.getNickname());
		if (param.getRealname() != null)
			user.setRealname(param.getRealname());
		if (param.getGender() != null)
			user.setGender(param.getGender());
		if (param.getPhone() != null)
			user.setPhone(param.getPhone());
		if (param.getEmail() != null)
			user.setEmail(param.getEmail());
		if (param.getAvatar() != null)
			user.setAvatar(param.getAvatar());
		if (param.getIdtype() != null)
			user.setIdtype(param.getIdtype());
		if (param.getIdcode() != null)
			user.setIdcode(param.getIdcode());
		if (param.getMotto() != null)
			user.setMotto(param.getMotto());
		if (param.getBirthDate() != null)
			user.setBirthDate(param.getBirthDate());
		if (param.getEducation() != null)
			user.setEducation(param.getEducation());
		if (param.getLocation() != null)
			user.setLocation(param.getLocation());
		if (param.getBiography() != null)
			user.setBiography(param.getBiography());
		user.setUpdateTime(LocalDateTime.now());
	}
}
