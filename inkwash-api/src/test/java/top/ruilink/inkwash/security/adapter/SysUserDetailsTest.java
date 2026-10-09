package top.ruilink.inkwash.security.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import top.ruilink.inkwash.base.enums.AuthType;
import top.ruilink.inkwash.system.domain.SysAccount;
import top.ruilink.inkwash.system.domain.SysUser;
import top.ruilink.inkwash.system.domain.credential.PasswordCredential;
import top.ruilink.inkwash.system.enums.UserStatus;

/**
 * SysUserDetails security principal adapter unit tests.
 *
 * @author Dyllon
 * @since 0.5.1
 */
class SysUserDetailsTest {

	@Test
	@DisplayName("claim-based constructor builds username and null password")
	void claimBasedConstructor_Works() {
		SysUser user = new SysUser();
		SysUserDetails details = new SysUserDetails(user, AuthType.SMS_CODE, "13800138000", null, List.of());
		assertEquals("2:13800138000", details.getUsername());
		assertNull(details.getPassword());
		assertEquals(user, details.getUser());
	}

	@Test
	@DisplayName("getUsername returns authType code + colon + identity")
	void getUsername_ReturnsAuthTypeAndIdentity() {
		SysUser user = new SysUser();
		SysAccount account = new SysAccount();
		account.setAuthType(AuthType.SMS_CODE);
		account.setIdentity("13800138000");
		SysUserDetails details = new SysUserDetails(user, account, List.of());
		assertEquals("2:13800138000", details.getUsername());
	}

	@Test
	@DisplayName("getPassword with PASSWORD auth returns password hash")
	void getPassword_WithPasswordAuth_ReturnsHash() {
		SysUser user = new SysUser();
		SysAccount account = new SysAccount();
		account.setAuthType(AuthType.PASSWORD);
		account.setCredential(new PasswordCredential("$2a$10$hash"));
		SysUserDetails details = new SysUserDetails(user, account, List.of());
		assertEquals("$2a$10$hash", details.getPassword());
	}

	@Test
	@DisplayName("getPassword with non-PASSWORD auth returns null")
	void getPassword_WithoutPasswordAuth_ReturnsNull() {
		SysUser user = new SysUser();
		SysAccount account = new SysAccount();
		account.setAuthType(AuthType.OAUTH2);
		SysUserDetails details = new SysUserDetails(user, account, List.of());
		assertNull(details.getPassword());
	}

	@Test
	@DisplayName("getPassword with null credential returns null")
	void getPassword_NullCredential_ReturnsNull() {
		SysUser user = new SysUser();
		SysAccount account = new SysAccount();
		account.setAuthType(AuthType.PASSWORD);
		account.setCredential(null);
		SysUserDetails details = new SysUserDetails(user, account, List.of());
		assertNull(details.getPassword());
	}

	@Test
	@DisplayName("getUserId returns user id")
	void getUserId_ReturnsUserId() {
		SysUser user = new SysUser();
		user.setId(42L);
		SysAccount account = new SysAccount();
		SysUserDetails details = new SysUserDetails(user, account, List.of());
		assertEquals(42L, details.getUserId());
	}

	@Test
	@DisplayName("getUserId with null user returns null")
	void getUserId_NullUser_ReturnsNull() {
		SysAccount account = new SysAccount();
		SysUserDetails details = new SysUserDetails(null, account, List.of());
		assertNull(details.getUserId());
	}

	@Test
	@DisplayName("getUser returns the user object")
	void getUser_ReturnsUser() {
		SysUser user = new SysUser();
		user.setNickname("admin");
		SysAccount account = new SysAccount();
		SysUserDetails details = new SysUserDetails(user, account, List.of());
		assertEquals("admin", details.getUser().getNickname());
	}

	@Test
	@DisplayName("getAuthorities returns the authorities collection")
	void getAuthorities_ReturnsAuthorities() {
		SysUser user = new SysUser();
		SysAccount account = new SysAccount();
		Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
		SysUserDetails details = new SysUserDetails(user, account, authorities);
		assertEquals(1, details.getAuthorities().size());
	}

	@Test
	@DisplayName("isEnabled with ENABLE status returns true")
	void isEnabled_WithEnableStatus_ReturnsTrue() {
		SysUser user = new SysUser();
		user.setStatus(UserStatus.ENABLE);
		SysAccount account = new SysAccount();
		SysUserDetails details = new SysUserDetails(user, account, List.of());
		assertTrue(details.isEnabled());
	}

	@Test
	@DisplayName("isEnabled with DISABLE status returns false")
	void isEnabled_WithDisableStatus_ReturnsFalse() {
		SysUser user = new SysUser();
		user.setStatus(UserStatus.DISABLE);
		SysAccount account = new SysAccount();
		SysUserDetails details = new SysUserDetails(user, account, List.of());
		assertFalse(details.isEnabled());
	}

	@Test
	@DisplayName("isAccountNonExpired always returns true")
	void isAccountNonExpired_ReturnsTrue() {
		SysUser user = new SysUser();
		SysAccount account = new SysAccount();
		SysUserDetails details = new SysUserDetails(user, account, List.of());
		assertTrue(details.isAccountNonExpired());
	}

	@Test
	@DisplayName("isAccountNonLocked always returns true")
	void isAccountNonLocked_ReturnsTrue() {
		SysUser user = new SysUser();
		SysAccount account = new SysAccount();
		SysUserDetails details = new SysUserDetails(user, account, List.of());
		assertTrue(details.isAccountNonLocked());
	}

	@Test
	@DisplayName("isCredentialsNonExpired always returns true")
	void isCredentialsNonExpired_ReturnsTrue() {
		SysUser user = new SysUser();
		SysAccount account = new SysAccount();
		SysUserDetails details = new SysUserDetails(user, account, List.of());
		assertTrue(details.isCredentialsNonExpired());
	}

	@Test
	@DisplayName("equals with same user id returns true")
	void equals_SameUserId_ReturnsTrue() {
		SysUser user1 = new SysUser();
		user1.setId(1L);
		SysUser user2 = new SysUser();
		user2.setId(1L);
		SysAccount account = new SysAccount();
		SysUserDetails details1 = new SysUserDetails(user1, account, List.of());
		SysUserDetails details2 = new SysUserDetails(user2, account, List.of());
		assertEquals(details1, details2);
	}

	@Test
	@DisplayName("equals with different user id returns false")
	void equals_DifferentUserId_ReturnsFalse() {
		SysUser user1 = new SysUser();
		user1.setId(1L);
		SysUser user2 = new SysUser();
		user2.setId(2L);
		SysAccount account = new SysAccount();
		SysUserDetails details1 = new SysUserDetails(user1, account, List.of());
		SysUserDetails details2 = new SysUserDetails(user2, account, List.of());
		assertFalse(details1.equals(details2));
	}

	@Test
	@DisplayName("hashCode is consistent with user id")
	void hashCode_ConsistentWithUserId() {
		SysUser user1 = new SysUser();
		user1.setId(1L);
		SysUser user2 = new SysUser();
		user2.setId(1L);
		SysAccount account = new SysAccount();
		SysUserDetails details1 = new SysUserDetails(user1, account, List.of());
		SysUserDetails details2 = new SysUserDetails(user2, account, List.of());
		assertEquals(details1.hashCode(), details2.hashCode());
	}
}
