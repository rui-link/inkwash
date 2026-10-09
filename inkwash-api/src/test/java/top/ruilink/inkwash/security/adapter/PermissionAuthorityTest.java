package top.ruilink.inkwash.security.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import top.ruilink.inkwash.system.domain.SysPermission;

/**
 * Permission authority adapter unit tests.
 *
 * @author Dyllon
 * @since 0.5.1
 */
class PermissionAuthorityTest {

	@Test
	@DisplayName("getAuthority returns permission authority string")
	void getAuthority_ReturnsPermissionAuthority() {
		SysPermission permission = new SysPermission();
		permission.setAuthority("system:user:update");
		PermissionAuthority authority = new PermissionAuthority(permission);
		assertEquals("system:user:update", authority.getAuthority());
	}

	@Test
	@DisplayName("getPermission returns the original permission object")
	void getPermission_ReturnsOriginalPermission() {
		SysPermission permission = new SysPermission();
		permission.setAuthority("system:user:update");
		PermissionAuthority authority = new PermissionAuthority(permission);
		assertSame(permission, authority.getPermission());
	}
}
