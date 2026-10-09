package top.ruilink.inkwash.security.service.login;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import top.ruilink.inkwash.security.api.param.SmsLoginParam;
import top.ruilink.inkwash.security.service.CaptchaService;
import top.ruilink.inkwash.security.service.LoginAttemptService;
import top.ruilink.inkwash.system.domain.SysIdentity;
import top.ruilink.inkwash.system.domain.SysUser;
import top.ruilink.inkwash.system.enums.UserStatus;
import top.ruilink.inkwash.system.service.IdentityService;
import top.ruilink.inkwash.system.service.UserService;

/**
 * SmsLoginStrategy unit tests.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@ExtendWith(MockitoExtension.class)
class SmsLoginStrategyTest {

	@Mock
	private CaptchaService captchaService;
	@Mock
	private LoginAttemptService loginAttemptService;
	@Mock
	private UserService userService;
	@Mock
	private IdentityService identityService;

	private static final String PHONE = "13800138000";
	private static final String CODE = "123456";

	private SmsLoginStrategy strategy() {
		return new SmsLoginStrategy(captchaService, loginAttemptService, userService, identityService, true);
	}

	private SmsLoginParam param() {
		return new SmsLoginParam(PHONE, CODE, AuthType.SMS_CODE);
	}

	@Test
	@DisplayName("已验证手机号直接登录，复用用户，不创建新用户")
	void authenticate_VerifiedClaim_ReusesUser() {
		SysIdentity claim = new SysIdentity();
		claim.setUserId(7L);
		claim.setIdentityValue(PHONE);
		claim.setVerified(1);
		when(captchaService.verifySmsCode(PHONE, CODE)).thenReturn(true);
		when(identityService.findByTypeValue(IdentityType.PHONE, PHONE)).thenReturn(claim);
		SysUser user = new SysUser();
		user.setId(7L);
		user.setStatus(UserStatus.ENABLE);
		when(userService.getById(7L)).thenReturn(user);

		LoginResult result = strategy().authenticate(param());

		assertEquals(7L, result.userId());
		assertEquals(PHONE, result.subject());
		assertEquals(AuthType.SMS_CODE, result.authType());
		verify(userService, never()).createRawUser(any());
		verify(userService, never()).joinDefaultGroup(any());
		verify(identityService, never()).findOrCreateVerified(any(), any(), any(), any(), any());
		verify(identityService).update(claim);
	}

	@Test
	@DisplayName("未验证声明在登录时被标记为已验证")
	void authenticate_UnverifiedClaim_MarksVerified() {
		SysIdentity claim = new SysIdentity();
		claim.setUserId(7L);
		claim.setIdentityValue(PHONE);
		claim.setVerified(0);
		when(captchaService.verifySmsCode(PHONE, CODE)).thenReturn(true);
		when(identityService.findByTypeValue(IdentityType.PHONE, PHONE)).thenReturn(claim);
		SysUser user = new SysUser();
		user.setId(7L);
		user.setStatus(UserStatus.ENABLE);
		when(userService.getById(7L)).thenReturn(user);

		strategy().authenticate(param());

		assertTrue(claim.identityVerified());
		verify(identityService, times(2)).update(claim);
	}

	@Test
	@DisplayName("无声明时创建用户并建 PHONE 声明")
	void authenticate_NoClaim_CreatesUserAndClaim() {
		SysUser user = new SysUser();
		user.setPhone(PHONE);
		when(captchaService.verifySmsCode(PHONE, CODE)).thenReturn(true);
		when(identityService.findByTypeValue(IdentityType.PHONE, PHONE)).thenReturn(null);
		when(userService.createRawUser(any())).thenAnswer(inv -> {
			SysUser u = inv.getArgument(0);
			u.setId(9L);
			return u;
		});
		SysIdentity claim = new SysIdentity();
		claim.setUserId(9L);
		claim.setVerified(1);
		when(identityService.findOrCreateVerified(eq(IdentityType.PHONE), eq(PHONE), isNull(), eq(9L),
				eq(IdentityVerifier.SMS_CODE))).thenReturn(claim);
		SysUser saved = new SysUser();
		saved.setId(9L);
		saved.setStatus(UserStatus.ENABLE);
		when(userService.getById(9L)).thenReturn(saved);

		LoginResult result = strategy().authenticate(param());

		assertEquals(9L, result.userId());
		verify(userService).createRawUser(any());
		verify(userService).joinDefaultGroup(9L);
		verify(identityService).findOrCreateVerified(eq(IdentityType.PHONE), eq(PHONE), isNull(), eq(9L),
				eq(IdentityVerifier.SMS_CODE));
	}

	@Test
	@DisplayName("并发冲突：findOrCreateVerified 返回他人声明时抛 phone_occupied")
	void authenticate_ClaimBelongsToOther_ThrowsOccupied() {
		SysUser user = new SysUser();
		user.setPhone(PHONE);
		when(captchaService.verifySmsCode(PHONE, CODE)).thenReturn(true);
		when(identityService.findByTypeValue(IdentityType.PHONE, PHONE)).thenReturn(null);
		when(userService.createRawUser(any())).thenAnswer(inv -> {
			SysUser u = inv.getArgument(0);
			u.setId(9L);
			return u;
		});
		SysIdentity other = new SysIdentity();
		other.setUserId(88L);
		other.setVerified(1);
		when(identityService.findOrCreateVerified(eq(IdentityType.PHONE), eq(PHONE), isNull(), eq(9L),
				eq(IdentityVerifier.SMS_CODE))).thenReturn(other);

		assertThrows(BusinessException.class, () -> strategy().authenticate(param()), "error.identity.phone_occupied");
	}

	@Test
	@DisplayName("验证码错误时记录失败并抛 sms_code_error")
	void authenticate_WrongCode_RecordsFailure() {
		when(captchaService.verifySmsCode(PHONE, CODE)).thenReturn(false);

		BusinessException ex = assertThrows(BusinessException.class, () -> strategy().authenticate(param()));

		assertEquals("error.auth.sms_code_error", ex.getMessageKey());
		verify(loginAttemptService).recordLoginFailure(PHONE);
		verify(identityService, never()).findByTypeValue(any(), any());
	}

	@Test
	@DisplayName("账号被锁定时直接拒绝，不再校验验证码")
	void authenticate_Locked_RejectsWithoutCodeCheck() {
		when(loginAttemptService.isLocked(PHONE)).thenReturn(true);

		assertThrows(BusinessException.class, () -> strategy().authenticate(param()));

		verify(captchaService, never()).verifySmsCode(any(), any());
	}

	@Test
	@DisplayName("用户被禁用时抛 user_not_found")
	void authenticate_DisabledUser_ThrowsNotFound() {
		SysIdentity claim = new SysIdentity();
		claim.setUserId(7L);
		claim.setVerified(1);
		when(captchaService.verifySmsCode(PHONE, CODE)).thenReturn(true);
		when(identityService.findByTypeValue(IdentityType.PHONE, PHONE)).thenReturn(claim);
		SysUser user = new SysUser();
		user.setId(7L);
		user.setStatus(UserStatus.DISABLE);
		when(userService.getById(7L)).thenReturn(user);

		BusinessException ex = assertThrows(BusinessException.class, () -> strategy().authenticate(param()));

		assertEquals("error.user.not_found", ex.getMessageKey());
	}

	@Test
	@DisplayName("supports 仅接受 SmsLoginParam")
	void supports_OnlySmsLoginParam() {
		SmsLoginStrategy s = strategy();
		assertTrue(s.supports(param()));
	}
}
