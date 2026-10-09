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
package top.ruilink.inkwash.security.util;

import java.util.Map;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import top.ruilink.inkwash.base.exception.BusinessException;
import top.ruilink.inkwash.security.adapter.SysUserDetails;
import top.ruilink.inkwash.system.domain.SysUser;

/**
 * Current authentication context access helper.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public final class SecurityUtil {

	private SecurityUtil() {
	}

	public static Long getCurrentUserId() {
		SysUserDetails userDetails = getCurrentUserDetails();
		if (userDetails != null) {
			return userDetails.getUserId();
		}
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null) {
			throw new BusinessException("error.auth.current_user_unavailable");
		}
		String name = authentication.getName();
		if (name != null && name.matches("\\d+")) {
			return Long.parseLong(name);
		}
		Object details = authentication.getDetails();
		if (details instanceof Map) {
			Object uid = ((Map<?, ?>) details).get("userId");
			if (uid instanceof String && ((String) uid).matches("\\d+")) {
				return Long.parseLong((String) uid);
			}
			if (uid instanceof Number) {
				return ((Number) uid).longValue();
			}
		}
		throw new BusinessException("error.auth.current_user_unavailable");
	}

	public static SysUser getCurrentUser() {
		SysUserDetails userDetails = getCurrentUserDetails();
		if (userDetails != null) {
			return userDetails.getUser();
		}
		throw new BusinessException("error.auth.current_user_unavailable");
	}

	public static String getCurrentUsername() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null) {
			throw new BusinessException("error.auth.current_user_unavailable");
		}
		return authentication.getName();
	}

	/**
	 * Gets the current authenticated user ID, returning null without throwing when
	 * there is no authentication context or parsing fails.
	 */
	public static Long getCurrentUserIdOrNull() {
		SysUserDetails userDetails = getCurrentUserDetails();
		if (userDetails != null) {
			return userDetails.getUserId();
		}
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			return null;
		}
		String name = authentication.getName();
		if (name != null && name.matches("\\d+")) {
			return Long.parseLong(name);
		}
		return null;
	}

	/**
	 * 管理员角色的 authority 集合。
	 *
	 * <p>
	 * {@code ROLE_SYSTEM} 是系统中权限最高的角色（{@code init-data.sql} 中其 remark 为
	 * 「拥有系统所有权限」），因此必须被视为管理员。
	 *
	 * <p>
	 * 仪表盘侧已不再持有角色白名单：接口授权统一由 {@code DashboardController} 上的 {@code @PreAuthorize} 与
	 * {@code init-data.sql} 的权限播种表达（admin-only 的 {@code cms:dashboard:user-stats}
	 * 只授予 ROLE_SYSTEM / ROLE_ADMIN）。因此本集合是 管理员判定的唯一来源；调整管理员角色时须同步核对
	 * {@code PermissionSeedIT} 的播种断言。
	 *
	 * <p>
	 * 注意：{@code ROLE_SUPER} 不在种子数据中（实际角色只有 ROLE_SYSTEM / ROLE_ADMIN / ROLE_EDITOR /
	 * ROLE_USER），任何对它的判定都永不成立，故不在此列。
	 */
	private static final Set<String> ADMIN_AUTHORITIES = Set.of("ROLE_SYSTEM", "ROLE_ADMIN");

	/**
	 * 判定当前认证主体是否具备管理员身份。
	 *
	 * @return authority 命中 {@link #ADMIN_AUTHORITIES} 中任一项时返回 {@code true}
	 */
	public static boolean isAdmin() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null) {
			return false;
		}
		return authentication.getAuthorities().stream().anyMatch(a -> ADMIN_AUTHORITIES.contains(a.getAuthority()));
	}

	public static boolean isEditor() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null) {
			return false;
		}
		return authentication.getAuthorities().stream().anyMatch(a -> "ROLE_EDITOR".equals(a.getAuthority()));
	}

	public static boolean hasAuthority(String permission) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null) {
			return false;
		}
		return authentication.getAuthorities().stream().anyMatch(a -> permission.equals(a.getAuthority()));
	}

	public static boolean isAuthenticated() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication != null && authentication.isAuthenticated();
	}

	private static SysUserDetails getCurrentUserDetails() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null) {
			return null;
		}
		if (authentication.getPrincipal() instanceof SysUserDetails userDetails) {
			return userDetails;
		}
		return null;
	}
}
