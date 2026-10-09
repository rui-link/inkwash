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

import java.util.Collection;
import java.util.Objects;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import top.ruilink.inkwash.base.CharConsts;
import top.ruilink.inkwash.base.enums.AuthType;
import top.ruilink.inkwash.system.domain.SysAccount;
import top.ruilink.inkwash.system.domain.SysUser;
import top.ruilink.inkwash.system.domain.credential.PasswordCredential;
import top.ruilink.inkwash.system.enums.UserStatus;

/**
 * User details adapter for Spring Security.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public class SysUserDetails implements UserDetails {
	private static final long serialVersionUID = 6417418708515123144L;

	private final SysUser user;
	private final AuthType authType;
	private final String identity;
	private final String passwordHash;
	private final Collection<? extends GrantedAuthority> authorities;

	public SysUserDetails(SysUser user, SysAccount account, Collection<? extends GrantedAuthority> authorities) {
		this(user, account != null ? account.getAuthType() : null, account != null ? account.getIdentity() : null,
				passwordHashOf(account), authorities);
	}

	/**
	 * Builds user details from identity claims, for login methods such as SMS that
	 * have no sys_account row.
	 */
	public SysUserDetails(SysUser user, AuthType authType, String identity, String passwordHash,
			Collection<? extends GrantedAuthority> authorities) {
		this.user = user;
		this.authType = authType;
		this.identity = identity;
		this.passwordHash = passwordHash;
		this.authorities = authorities;
	}

	private static String passwordHashOf(SysAccount account) {
		if (account != null && AuthType.PASSWORD.equals(account.getAuthType())
				&& account.getCredential() instanceof PasswordCredential cred) {
			return cred.passwordHash();
		}
		return null;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}

	// Returns the encoded password for password auth, null for every other method
	@Override
	public String getPassword() {
		return passwordHash;
	}

	public SysUser getUser() {
		return this.user;
	}

	public Long getUserId() {
		return user != null ? user.getId() : null;
	}

	/**
	 * Overrides the username using the format "authType:identity".
	 */
	@Override
	public String getUsername() {
		if (authType == null) {
			return identity != null ? identity : "";
		}
		return authType.getCode() + CharConsts.COLON + identity;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return user != null && UserStatus.ENABLE.equals(user.getStatus());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		SysUserDetails that = (SysUserDetails) obj;
		return user != null && Objects.equals(user.getId(), that.user.getId());
	}

	@Override
	public int hashCode() {
		return Objects.hash(user != null ? user.getId() : null);
	}
}
