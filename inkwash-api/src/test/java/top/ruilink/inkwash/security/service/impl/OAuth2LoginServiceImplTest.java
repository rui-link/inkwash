package top.ruilink.inkwash.security.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import top.ruilink.inkwash.base.enums.AuthType;
import top.ruilink.inkwash.base.enums.IdentityType;
import top.ruilink.inkwash.base.enums.IdentityVerifier;
import top.ruilink.inkwash.base.exception.BusinessException;
import top.ruilink.inkwash.security.service.login.LoginResult;
import top.ruilink.inkwash.system.domain.SysAccount;
import top.ruilink.inkwash.system.domain.SysIdentity;
import top.ruilink.inkwash.system.domain.SysUser;
import top.ruilink.inkwash.system.enums.UserStatus;
import top.ruilink.inkwash.system.service.AccountService;
import top.ruilink.inkwash.system.service.IdentityService;
import top.ruilink.inkwash.system.service.UserService;

/**
 * OAuth2LoginServiceImpl unit tests.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@ExtendWith(MockitoExtension.class)
class OAuth2LoginServiceImplTest {

	@Mock
	private IdentityService identityService;
	@Mock
	private UserService userService;
	@Mock
	private AccountService accountService;

	private OAuth2LoginServiceImpl service() {
		return new OAuth2LoginServiceImpl(identityService, userService, accountService);
	}

	private SysUser enabledUser(Long id) {
		SysUser user = new SysUser();
		user.setId(id);
		user.setStatus(UserStatus.ENABLE);
		return user;
	}

	@Test
	@DisplayName("OIDC 声明命中且已有账号时复用用户并更新凭证")
	void login_OidcHitWithAccount_UpdatesAccount() {
		SysIdentity oidc = new SysIdentity();
		oidc.setUserId(5L);
		oidc.setVerified(1);
		when(identityService.findByTypeProviderValue(eq(IdentityType.OIDC_SUB), eq("github"), eq("open-1")))
				.thenReturn(oidc);
		when(userService.getById(5L)).thenReturn(enabledUser(5L));
		SysAccount account = new SysAccount();
		when(accountService.findByProviderAndOpenId("github", "open-1")).thenReturn(account);

		LoginResult result = service().login("GITHUB", "open-1", "nick", null, "at", "rt");

		assertEquals(5L, result.userId());
		assertEquals("github:open-1", result.subject());
		assertEquals(AuthType.OAUTH2, result.authType());
		verify(accountService).update(account);
		verify(accountService, never()).create(any());
		verify(identityService, never()).findByTypeValue(any(), any());
		verify(identityService).update(oidc);
		verify(userService).joinDefaultGroup(5L);
	}

	@Test
	@DisplayName("OIDC 声明命中但无账号时创建账号")
	void login_OidcHitWithoutAccount_CreatesAccount() {
		SysIdentity oidc = new SysIdentity();
		oidc.setUserId(5L);
		oidc.setVerified(1);
		when(identityService.findByTypeProviderValue(eq(IdentityType.OIDC_SUB), eq("github"), eq("open-1")))
				.thenReturn(oidc);
		when(userService.getById(5L)).thenReturn(enabledUser(5L));
		when(accountService.findByProviderAndOpenId("github", "open-1")).thenReturn(null);

		service().login("github", "open-1", "nick", null, "at", "rt");

		verify(accountService).create(any(SysAccount.class));
		verify(accountService, never()).update(any());
		verify(userService).joinDefaultGroup(5L);
	}

	@Test
	@DisplayName("OIDC 未命中但邮箱声明命中时绑定 OIDC 到已有用户")
	void login_NoOidcEmailHit_BindsToExistingUser() {
		when(identityService.findByTypeProviderValue(eq(IdentityType.OIDC_SUB), eq("github"), eq("open-1")))
				.thenReturn(null);
		SysIdentity emailClaim = new SysIdentity();
		emailClaim.setUserId(6L);
		emailClaim.setVerified(1);
		when(identityService.findByTypeValue(IdentityType.EMAIL, "a@b.com")).thenReturn(emailClaim);
		when(userService.getById(6L)).thenReturn(enabledUser(6L));

		LoginResult result = service().login("github", "open-1", "nick", "a@b.com", "at", "rt");

		assertEquals(6L, result.userId());
		verify(identityService).findOrCreateVerified(eq(IdentityType.OIDC_SUB), eq("open-1"), eq("github"), eq(6L),
				eq(IdentityVerifier.OAUTH2));
		verify(userService, never()).createSimpleUser(any(), any());
	}

	@Test
	@DisplayName("OIDC 与邮箱均未命中时创建新用户")
	void login_NoClaims_CreatesSimpleUser() {
		when(identityService.findByTypeProviderValue(eq(IdentityType.OIDC_SUB), eq("github"), eq("open-1")))
				.thenReturn(null);
		when(identityService.findByTypeValue(IdentityType.EMAIL, "a@b.com")).thenReturn(null);
		SysUser created = new SysUser();
		created.setId(8L);
		when(userService.createSimpleUser("nick", "a@b.com")).thenReturn(created);
		when(accountService.findByProviderAndOpenId("github", "open-1")).thenReturn(null);

		LoginResult result = service().login("github", "open-1", "nick", "a@b.com", "at", "rt");

		assertEquals(8L, result.userId());
		verify(userService).createSimpleUser("nick", "a@b.com");
		verify(userService).joinDefaultGroup(8L);
		verify(identityService).findOrCreateVerified(eq(IdentityType.OIDC_SUB), eq("open-1"), eq("github"), eq(8L),
				eq(IdentityVerifier.OAUTH2));
	}

	@Test
	@DisplayName("邮箱为空时新用户使用默认昵称")
	void login_NoClaimsNoEmail_UsesDefaultNickname() {
		when(identityService.findByTypeProviderValue(eq(IdentityType.OIDC_SUB), eq("google"), eq("open-123")))
				.thenReturn(null);
		SysUser created = new SysUser();
		created.setId(8L);
		when(userService.createSimpleUser(eq("google_open-123"), isNull())).thenReturn(created);
		when(accountService.findByProviderAndOpenId("google", "open-123")).thenReturn(null);

		service().login("google", "open-123", null, null, "at", "rt");

		verify(userService).createSimpleUser("google_open-123", null);
		verify(userService).joinDefaultGroup(8L);
	}

	@Test
	@DisplayName("已禁用用户抛 user_not_found")
	void login_DisabledUser_ThrowsNotFound() {
		SysIdentity oidc = new SysIdentity();
		oidc.setUserId(5L);
		oidc.setVerified(1);
		when(identityService.findByTypeProviderValue(eq(IdentityType.OIDC_SUB), eq("github"), eq("open-1")))
				.thenReturn(oidc);
		SysUser user = new SysUser();
		user.setId(5L);
		user.setStatus(UserStatus.DISABLE);
		when(userService.getById(5L)).thenReturn(user);

		BusinessException ex = assertThrows(BusinessException.class,
				() -> service().login("github", "open-1", "nick", null, "at", "rt"));

		assertEquals("error.user.not_found", ex.getMessageKey());
	}

	@Test
	@DisplayName("provider/openId 缺失抛 invalid_params")
	void login_MissingParams_ThrowsInvalidParams() {
		BusinessException ex = assertThrows(BusinessException.class,
				() -> service().login("", "open-1", "nick", null, "at", "rt"));
		assertEquals("error.oauth2.invalid_params", ex.getMessageKey());

		BusinessException ex2 = assertThrows(BusinessException.class,
				() -> service().login("github", " ", "nick", null, "at", "rt"));
		assertEquals("error.oauth2.invalid_params", ex2.getMessageKey());
	}

	@Test
	@DisplayName("provider 大小写被归一化")
	void login_ProviderNormalizedToLowercase() {
		SysIdentity oidc = new SysIdentity();
		oidc.setUserId(5L);
		oidc.setVerified(1);
		when(identityService.findByTypeProviderValue(eq(IdentityType.OIDC_SUB), eq("wechat"), eq("open-1")))
				.thenReturn(oidc);
		when(userService.getById(5L)).thenReturn(enabledUser(5L));
		when(accountService.findByProviderAndOpenId("wechat", "open-1")).thenReturn(null);

		LoginResult result = service().login("WeChat", "open-1", "nick", null, "at", "rt");

		assertEquals("wechat:open-1", result.subject());
		verify(accountService).create(any(SysAccount.class));
	}

	@Test
	@DisplayName("意外异常包装为 oauth2.failed")
	void login_UnexpectedException_WrapsAsFailed() {
		when(identityService.findByTypeProviderValue(eq(IdentityType.OIDC_SUB), eq("github"), eq("open-1")))
				.thenThrow(new IllegalStateException("boom"));

		BusinessException ex = assertThrows(BusinessException.class,
				() -> service().login("github", "open-1", "nick", null, "at", "rt"));

		assertEquals("error.oauth2.failed", ex.getMessageKey());
	}

	@Test
	@DisplayName("新用户走邮箱命中分支时 bindOidcTo 不重复创建用户")
	void login_EmailHit_DoesNotCreateUserTwice() {
		when(identityService.findByTypeProviderValue(eq(IdentityType.OIDC_SUB), eq("github"), eq("open-1")))
				.thenReturn(null);
		SysIdentity emailClaim = new SysIdentity();
		emailClaim.setUserId(6L);
		emailClaim.setVerified(1);
		when(identityService.findByTypeValue(IdentityType.EMAIL, "a@b.com")).thenReturn(emailClaim);
		when(userService.getById(6L)).thenReturn(enabledUser(6L));
		when(accountService.findByProviderAndOpenId("github", "open-1")).thenReturn(null);

		service().login("github", "open-1", "nick", "a@b.com", "at", "rt");

		verify(userService, times(0)).createSimpleUser(any(), any());
		verify(accountService).create(any(SysAccount.class));
	}
}
