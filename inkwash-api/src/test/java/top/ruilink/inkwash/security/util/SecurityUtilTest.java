package top.ruilink.inkwash.security.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import top.ruilink.inkwash.base.enums.AuthType;
import top.ruilink.inkwash.base.exception.BusinessException;
import top.ruilink.inkwash.security.adapter.SysUserDetails;
import top.ruilink.inkwash.system.domain.SysAccount;
import top.ruilink.inkwash.system.domain.SysUser;

/**
 * Security context principal, admin role, and authentication state unit tests.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@ExtendWith(MockitoExtension.class)
class SecurityUtilTest {

	@Mock
	private SecurityContext securityContext;
	@Mock
	private Authentication authentication;

	@BeforeEach
	void setUp() {
		SecurityContextHolder.setContext(securityContext);
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	private SysUserDetails createUserDetails(Long userId, String nickname, String identity) {
		SysUser user = new SysUser();
		user.setId(userId);
		user.setNickname(nickname);
		SysAccount account = new SysAccount();
		account.setAuthType(AuthType.PASSWORD);
		account.setIdentity(identity);
		return new SysUserDetails(user, account, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
	}

	@Nested
	@DisplayName("getCurrentUserId tests")
	class GetCurrentUserIdTests {

		@Test
		@DisplayName("From SysUserDetails returns userId")
		void getCurrentUserId_FromSysUserDetails_ReturnsUserId() {
			SysUserDetails userDetails = createUserDetails(1L, "admin", "admin");
			when(securityContext.getAuthentication()).thenReturn(authentication);
			when(authentication.getPrincipal()).thenReturn(userDetails);

			Long result = SecurityUtil.getCurrentUserId();

			assertEquals(1L, result);
		}

		@Test
		@DisplayName("From numeric name returns parsed id")
		void getCurrentUserId_FromNumericName_ReturnsParsedId() {
			when(securityContext.getAuthentication()).thenReturn(authentication);
			when(authentication.getPrincipal()).thenReturn("anonymous");
			when(authentication.getName()).thenReturn("42");

			Long result = SecurityUtil.getCurrentUserId();

			assertEquals(42L, result);
		}

		@Test
		@DisplayName("From details map returns userId")
		void getCurrentUserId_FromDetailsMap_ReturnsUserId() {
			when(securityContext.getAuthentication()).thenReturn(authentication);
			when(authentication.getPrincipal()).thenReturn("anonymous");
			when(authentication.getName()).thenReturn("not-a-number");
			when(authentication.getDetails()).thenReturn(Map.of("userId", 99L));

			Long result = SecurityUtil.getCurrentUserId();

			assertEquals(99L, result);
		}

		@Test
		@DisplayName("No auth throws BusinessException")
		void getCurrentUserId_NoAuth_Throws() {
			when(securityContext.getAuthentication()).thenReturn(null);

			assertThrows(BusinessException.class, () -> SecurityUtil.getCurrentUserId());
		}
	}

	@Nested
	@DisplayName("getCurrentUser tests")
	class GetCurrentUserTests {

		@Test
		@DisplayName("Returns SysUser")
		void getCurrentUser_ReturnsSysUser() {
			SysUserDetails userDetails = createUserDetails(1L, "admin", "admin");
			when(securityContext.getAuthentication()).thenReturn(authentication);
			when(authentication.getPrincipal()).thenReturn(userDetails);

			SysUser result = SecurityUtil.getCurrentUser();

			assertEquals("admin", result.getNickname());
		}

		@Test
		@DisplayName("No auth throws BusinessException")
		void getCurrentUser_NoAuth_Throws() {
			when(securityContext.getAuthentication()).thenReturn(null);

			assertThrows(BusinessException.class, () -> SecurityUtil.getCurrentUser());
		}
	}

	@Nested
	@DisplayName("getCurrentUsername tests")
	class GetCurrentUsernameTests {

		@Test
		@DisplayName("Returns name")
		void getCurrentUsername_ReturnsName() {
			when(securityContext.getAuthentication()).thenReturn(authentication);
			when(authentication.getName()).thenReturn("testuser");

			String result = SecurityUtil.getCurrentUsername();

			assertEquals("testuser", result);
		}

		@Test
		@DisplayName("No auth throws BusinessException")
		void getCurrentUsername_NoAuth_Throws() {
			when(securityContext.getAuthentication()).thenReturn(null);

			assertThrows(BusinessException.class, () -> SecurityUtil.getCurrentUsername());
		}
	}

	@Nested
	@DisplayName("isAdmin tests")
	class IsAdminTests {

		@Test
		@DisplayName("With admin role returns true")
		void isAdmin_WithAdminRole_ReturnsTrue() {
			when(securityContext.getAuthentication()).thenReturn(authentication);
			doReturn(List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(authentication).getAuthorities();

			boolean result = SecurityUtil.isAdmin();

			assertTrue(result);
		}

		@Test
		@DisplayName("Without admin role returns false")
		void isAdmin_WithoutAdminRole_ReturnsFalse() {
			when(securityContext.getAuthentication()).thenReturn(authentication);
			doReturn(List.of(new SimpleGrantedAuthority("ROLE_USER"))).when(authentication).getAuthorities();

			boolean result = SecurityUtil.isAdmin();

			assertFalse(result);
		}

		@Test
		@DisplayName("No auth returns false")
		void isAdmin_NoAuth_ReturnsFalse() {
			when(securityContext.getAuthentication()).thenReturn(null);

			assertFalse(SecurityUtil.isAdmin());
		}

		@Test
		@DisplayName("ROLE_SYSTEM 视为管理员 —— ISS-020 / D-03")
		void isAdmin_WithSystemRole_ReturnsTrue() {
			when(securityContext.getAuthentication()).thenReturn(authentication);
			doReturn(List.of(new SimpleGrantedAuthority("ROLE_SYSTEM"))).when(authentication).getAuthorities();

			assertTrue(SecurityUtil.isAdmin(), "ROLE_SYSTEM 是权限最高的角色（拥有系统所有权限），必须判定为管理员");
		}

		@Test
		@DisplayName("管理员集合覆盖全部已播种角色，且只含 ROLE_SYSTEM / ROLE_ADMIN —— D-03")
		void isAdmin_MatchesSeededRoles() {
			Map<String, Boolean> expected = Map.of("ROLE_SYSTEM", true, "ROLE_ADMIN", true, "ROLE_EDITOR", false,
					"ROLE_USER", false);

			expected.forEach((role, isAdmin) -> {
				when(securityContext.getAuthentication()).thenReturn(authentication);
				doReturn(List.of(new SimpleGrantedAuthority(role))).when(authentication).getAuthorities();
				assertEquals(isAdmin, SecurityUtil.isAdmin(), "角色 " + role + " 的管理员判定必须与播种角色清单一致");
			});

			assertEquals(
					Set.of("ROLE_SYSTEM", "ROLE_ADMIN"), expected.entrySet().stream().filter(Map.Entry::getValue)
							.map(Map.Entry::getKey).collect(Collectors.toSet()),
					"管理员角色集合应为 {ROLE_SYSTEM, ROLE_ADMIN}；ROLE_SUPER 未播种，属死分支");
		}

		@Test
		@DisplayName("编辑器不是管理员 —— 防止误把 ROLE_EDITOR 纳入")
		void isAdmin_WithEditorRole_ReturnsFalse() {
			when(securityContext.getAuthentication()).thenReturn(authentication);
			doReturn(List.of(new SimpleGrantedAuthority("ROLE_EDITOR"))).when(authentication).getAuthorities();

			assertFalse(SecurityUtil.isAdmin());
		}
	}

	@Nested
	@DisplayName("isAuthenticated tests")
	class IsAuthenticatedTests {

		@Test
		@DisplayName("With valid auth returns true")
		void isAuthenticated_WithValidAuth_ReturnsTrue() {
			when(securityContext.getAuthentication()).thenReturn(authentication);
			when(authentication.isAuthenticated()).thenReturn(true);

			boolean result = SecurityUtil.isAuthenticated();

			assertTrue(result);
		}

		@Test
		@DisplayName("No auth returns false")
		void isAuthenticated_NoAuth_ReturnsFalse() {
			when(securityContext.getAuthentication()).thenReturn(null);

			assertFalse(SecurityUtil.isAuthenticated());
		}
	}
}
