package top.ruilink.inkwash.security.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import top.ruilink.inkwash.base.enums.AuthType;
import top.ruilink.inkwash.base.enums.IdentityType;
import top.ruilink.inkwash.base.enums.IdentityVerifier;
import top.ruilink.inkwash.base.exception.BusinessException;
import top.ruilink.inkwash.security.api.param.ChangePasswordParam;
import top.ruilink.inkwash.security.api.param.ProfileParam;
import top.ruilink.inkwash.security.api.param.SetPasswordParam;
import top.ruilink.inkwash.security.api.param.VerifyEmailParam;
import top.ruilink.inkwash.security.api.param.VerifyPhoneParam;
import top.ruilink.inkwash.security.service.CaptchaService;
import top.ruilink.inkwash.security.service.EmailService;
import top.ruilink.inkwash.security.service.LoginAttemptService;
import top.ruilink.inkwash.security.service.PasswordPolicyService;
import top.ruilink.inkwash.security.util.CryptoUtil;
import top.ruilink.inkwash.system.domain.SysAccount;
import top.ruilink.inkwash.system.domain.SysIdentity;
import top.ruilink.inkwash.system.domain.SysUser;
import top.ruilink.inkwash.system.service.AccountService;
import top.ruilink.inkwash.system.service.IdentityService;
import top.ruilink.inkwash.system.service.PermissionService;
import top.ruilink.inkwash.system.service.UserService;

/**
 * ProfileServiceImpl unit tests for partial profile updates.
 * <p>
 * Only the fields present in the request may be changed; id/username/password
 * must never be part of a profile update (password lives in the change-password
 * tab, username/id come from the logged-in user).
 *
 * @author Dyllon
 * @since 0.5.1
 */
@ExtendWith(MockitoExtension.class)
class ProfileServiceImplTest {

	@Mock
	private UserService userService;
	@Mock
	private PermissionService permissionService;
	@Mock
	private AccountService accountService;
	@Mock
	private IdentityService identityService;
	@Mock
	private CaptchaService captchaService;
	@Mock
	private LoginAttemptService loginAttemptService;
	@Mock
	private EmailService emailService;
	@Mock
	private PasswordPolicyService passwordPolicyService;

	@InjectMocks
	private ProfileServiceImpl profileService;

	@BeforeEach
	void setUp() {
		try {
			var field = CryptoUtil.class.getDeclaredField("passwordEncoder");
			field.setAccessible(true);
			field.set(null, new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("1", null));
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("only provided fields are updated, others stay untouched")
	void updateProfile_appliesOnlyProvidedFields() {
		SysUser user = new SysUser();
		user.setId(1L);
		user.setNickname("old");
		user.setEmail("old@example.com");
		user.setPhone("13800000000");
		user.setAvatar(null);
		when(userService.getById(1L)).thenReturn(user);

		ProfileParam param = new ProfileParam();
		param.setAvatar("/uploads/avatars/new.png");

		profileService.updateProfile(param);

		assertEquals("/uploads/avatars/new.png", user.getAvatar());
		assertEquals("old", user.getNickname());
		assertEquals("old@example.com", user.getEmail());
		assertEquals("13800000000", user.getPhone());
		verify(userService).updateRawUser(user);
	}

	@Test
	@DisplayName("null profile field is treated as not-provided")
	void updateProfile_nullFieldsAreSkipped() {
		SysUser user = new SysUser();
		user.setId(1L);
		user.setNickname("张三");
		user.setEmail(null);
		user.setPhone(null);
		user.setAvatar("/uploads/avatars/old.png");
		when(userService.getById(1L)).thenReturn(user);

		ProfileParam param = new ProfileParam();
		param.setNickname("新名字");

		profileService.updateProfile(param);

		assertEquals("新名字", user.getNickname());
		assertNull(user.getEmail());
		assertNull(user.getPhone());
		assertEquals("/uploads/avatars/old.png", user.getAvatar());
	}

	@Test
	@DisplayName("无密码账号时修改密码抛 no_password")
	void updatePassword_NoPasswordAccount_Throws() {
		when(accountService.listByUserId(1L)).thenReturn(List.of());

		BusinessException ex = assertThrows(BusinessException.class,
				() -> profileService.updatePassword(new ChangePasswordParam("old", "newpass", "newpass")));

		assertEquals("error.account.no_password", ex.getMessageKey());
	}

	@Test
	@DisplayName("设置密码前必须存在已验证邮箱")
	void setPassword_NoVerifiedEmail_Throws() {
		when(identityService.listByUserId(1L)).thenReturn(List.of());

		BusinessException ex = assertThrows(BusinessException.class,
				() -> profileService.setPassword(new SetPasswordParam("tom", "123456", "newpass", "newpass")));

		assertEquals("error.email.not_verified", ex.getMessageKey());
	}

	@Test
	@DisplayName("邮箱验证码错误时抛 code_error")
	void setPassword_WrongEmailCode_Throws() {
		SysIdentity claim = new SysIdentity();
		claim.setIdentityType(IdentityType.EMAIL);
		claim.setIdentityValue("a@b.com");
		claim.setVerified(1);
		when(identityService.listByUserId(1L)).thenReturn(List.of(claim));
		when(emailService.verifyCode("a@b.com", "bad")).thenReturn(false);

		BusinessException ex = assertThrows(BusinessException.class,
				() -> profileService.setPassword(new SetPasswordParam("tom", "bad", "newpass", "newpass")));

		assertEquals("error.email.code_error", ex.getMessageKey());
	}

	@Test
	@DisplayName("用户名已存在时抛 username_exists")
	void setPassword_UsernameTaken_Throws() {
		SysIdentity claim = new SysIdentity();
		claim.setIdentityType(IdentityType.EMAIL);
		claim.setIdentityValue("a@b.com");
		claim.setVerified(1);
		when(identityService.listByUserId(1L)).thenReturn(List.of(claim));
		when(emailService.verifyCode("a@b.com", "123456")).thenReturn(true);
		SysAccount taken = new SysAccount();
		when(accountService.findByIdentityAndType("tom", AuthType.PASSWORD)).thenReturn(taken);

		BusinessException ex = assertThrows(BusinessException.class,
				() -> profileService.setPassword(new SetPasswordParam("tom", "123456", "newpass", "newpass")));

		assertEquals("error.user.username_exists", ex.getMessageKey());
	}

	@Test
	@DisplayName("已有密码时抛 password_exists")
	void setPassword_HasPassword_Throws() {
		SysIdentity claim = new SysIdentity();
		claim.setIdentityType(IdentityType.EMAIL);
		claim.setIdentityValue("a@b.com");
		claim.setVerified(1);
		when(identityService.listByUserId(1L)).thenReturn(List.of(claim));
		when(emailService.verifyCode("a@b.com", "123456")).thenReturn(true);
		when(accountService.findByIdentityAndType("tom", AuthType.PASSWORD)).thenReturn(null);
		SysAccount existing = new SysAccount();
		existing.setAuthType(AuthType.PASSWORD);
		when(accountService.listByUserId(1L)).thenReturn(List.of(existing));

		BusinessException ex = assertThrows(BusinessException.class,
				() -> profileService.setPassword(new SetPasswordParam("tom", "123456", "newpass", "newpass")));

		assertEquals("error.account.password_exists", ex.getMessageKey());
	}

	@Test
	@DisplayName("设置密码成功时创建密码账号")
	void setPassword_Success_CreatesAccount() {
		SysIdentity claim = new SysIdentity();
		claim.setIdentityType(IdentityType.EMAIL);
		claim.setIdentityValue("a@b.com");
		claim.setVerified(1);
		when(identityService.listByUserId(1L)).thenReturn(List.of(claim));
		when(emailService.verifyCode("a@b.com", "123456")).thenReturn(true);
		when(accountService.findByIdentityAndType("tom", AuthType.PASSWORD)).thenReturn(null);
		when(accountService.listByUserId(1L)).thenReturn(List.of());

		profileService.setPassword(new SetPasswordParam("tom", "123456", "newpass", "newpass"));

		verify(passwordPolicyService).validateStrength("newpass");
		verify(accountService).create(org.mockito.ArgumentMatchers.any(SysAccount.class));
	}

	@Test
	@DisplayName("验证手机验证码错误时抛 sms_code_error")
	void verifyPhone_WrongCode_Throws() {
		when(captchaService.verifySmsCode("13800138000", "bad")).thenReturn(false);

		BusinessException ex = assertThrows(BusinessException.class,
				() -> profileService.verifyPhone(new VerifyPhoneParam("13800138000", "bad")));

		assertEquals("error.auth.sms_code_error", ex.getMessageKey());
	}

	@Test
	@DisplayName("验证手机成功后同步 user.phone")
	void verifyPhone_Success_SyncsUserPhone() {
		when(captchaService.verifySmsCode("13800138000", "123456")).thenReturn(true);
		SysIdentity claim = new SysIdentity();
		claim.setUserId(1L);
		claim.setVerified(1);
		when(identityService.findOrCreateVerified(IdentityType.PHONE, "13800138000", null, 1L,
				IdentityVerifier.SMS_CODE)).thenReturn(claim);
		SysUser user = new SysUser();
		user.setId(1L);
		user.setPhone("13900000000");
		when(userService.getById(1L)).thenReturn(user);

		profileService.verifyPhone(new VerifyPhoneParam("13800138000", "123456"));

		assertEquals("13800138000", user.getPhone());
		verify(userService).updateRawUser(user);
	}

	@Test
	@DisplayName("验证邮箱验证码错误时抛 code_error")
	void verifyEmail_WrongCode_Throws() {
		when(emailService.verifyCode("a@b.com", "bad")).thenReturn(false);

		BusinessException ex = assertThrows(BusinessException.class,
				() -> profileService.verifyEmail(new VerifyEmailParam("a@b.com", "bad")));

		assertEquals("error.email.code_error", ex.getMessageKey());
	}

	@Test
	@DisplayName("验证邮箱成功后同步 user.email")
	void verifyEmail_Success_SyncsUserEmail() {
		when(emailService.verifyCode("a@b.com", "123456")).thenReturn(true);
		SysIdentity claim = new SysIdentity();
		claim.setUserId(1L);
		claim.setVerified(1);
		when(identityService.findOrCreateVerified(IdentityType.EMAIL, "a@b.com", null, 1L, IdentityVerifier.USER))
				.thenReturn(claim);
		SysUser user = new SysUser();
		user.setId(1L);
		user.setEmail("old@example.com");
		when(userService.getById(1L)).thenReturn(user);

		profileService.verifyEmail(new VerifyEmailParam("a@b.com", "123456"));

		assertEquals("a@b.com", user.getEmail());
		verify(userService).updateRawUser(user);
	}

	@Test
	@DisplayName("仅剩一种登录方式时禁止解绑")
	void unbindAccount_LastCredential_Throws() {
		SysAccount account = new SysAccount();
		account.setId(1L);
		account.setUserId(1L);
		account.setAuthType(AuthType.PASSWORD);
		when(accountService.listByUserId(1L)).thenReturn(List.of(account));
		when(identityService.listByUserId(1L)).thenReturn(List.of());

		BusinessException ex = assertThrows(BusinessException.class, () -> profileService.unbindAccount(1L, "ACCOUNT"));

		assertEquals("error.account.at_least_one", ex.getMessageKey());
	}
}
