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
package top.ruilink.inkwash.security.adapter;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;
import top.ruilink.inkwash.base.CharConsts;
import top.ruilink.inkwash.base.enums.AuthType;
import top.ruilink.inkwash.base.enums.IdentityType;
import top.ruilink.inkwash.system.domain.SysAccount;
import top.ruilink.inkwash.system.domain.SysIdentity;
import top.ruilink.inkwash.system.domain.SysPermission;
import top.ruilink.inkwash.system.domain.SysUser;
import top.ruilink.inkwash.system.enums.UserStatus;
import top.ruilink.inkwash.system.service.AccountService;
import top.ruilink.inkwash.system.service.IdentityService;
import top.ruilink.inkwash.system.service.PermissionService;
import top.ruilink.inkwash.system.service.UserService;

/**
 * Spring Security UserDetailsService living in the infrastructure layer and
 * depending on application services.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Slf4j
@Service
public class SysUserDetailsService implements UserDetailsService {
	private final UserService userService;
	private final AccountService accountService;
	private final PermissionService permissionService;
	private final IdentityService identityService;

	public SysUserDetailsService(UserService userService, AccountService accountService,
			PermissionService permissionService, IdentityService identityService) {
		this.userService = userService;
		this.accountService = accountService;
		this.permissionService = permissionService;
		this.identityService = identityService;
	}

	/**
	 * Loads user details with all permissions, where username is the
	 * authType:identity of a SysAccount.
	 */
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		if (!StringUtils.hasText(username) || !username.contains(CharConsts.COLON)) {
			log.error("用户名格式错误，必须为authType:identity，当前值：{}", username);
			throw new UsernameNotFoundException("用户名格式错误");
		}
		String[] usernameParts = username.split(CharConsts.COLON, 2);
		AuthType authType = AuthType.fromCode(Integer.parseInt(usernameParts[0]));
		String identity = usernameParts[1];

		SysAccount account = accountService.findByIdentityAndType(identity, authType);
		if (account != null) {
			SysUser user = userService.getById(account.getUserId());
			if (user == null) {
				log.error("用户不存在，userId={}", account.getUserId());
				throw new UsernameNotFoundException("用户不存在");
			}
			if (!UserStatus.ENABLE.equals(user.getStatus())) {
				log.error("用户状态不可用，userId={}, status={}", user.getId(), user.getStatus());
				throw new UsernameNotFoundException("账号不可用");
			}
			return new SysUserDetails(user, account, authoritiesOf(user));
		}

		// SMS_CODE has no sys_account row: fall back to the PHONE claim
		if (authType == AuthType.SMS_CODE) {
			SysIdentity claim = identityService.findByTypeValue(IdentityType.PHONE, identity);
			if (claim == null || !claim.identityVerified()) {
				log.error("短信登录手机号无有效声明，identity={}", identity);
				throw new UsernameNotFoundException("未找到账号");
			}
			SysUser user = userService.getById(claim.getUserId());
			if (user == null || !UserStatus.ENABLE.equals(user.getStatus())) {
				log.error("短信登录用户不可用，userId={}", claim.getUserId());
				throw new UsernameNotFoundException("账号不可用");
			}
			return new SysUserDetails(user, AuthType.SMS_CODE, claim.getIdentityValue(), null, authoritiesOf(user));
		}

		log.error("账号不存在，identity={}，loginType={}", identity, authType.getCode());
		throw new UsernameNotFoundException("账号不存在或登录方式错误");
	}

	private List<GrantedAuthority> authoritiesOf(SysUser user) {
		Set<SysPermission> permissions = permissionService.findByUser(user);
		return permissions.stream().map(PermissionAuthority::new).collect(Collectors.toList());
	}
}
