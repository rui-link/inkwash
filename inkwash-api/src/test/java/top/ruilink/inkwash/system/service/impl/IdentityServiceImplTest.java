package top.ruilink.inkwash.system.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
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
import org.springframework.dao.DuplicateKeyException;

import top.ruilink.inkwash.base.enums.IdentityType;
import top.ruilink.inkwash.base.enums.IdentityVerifier;
import top.ruilink.inkwash.base.exception.BusinessException;
import top.ruilink.inkwash.system.domain.SysIdentity;
import top.ruilink.inkwash.system.mapper.IdentityMapper;

/**
 * IdentityServiceImpl unit tests.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@ExtendWith(MockitoExtension.class)
class IdentityServiceImplTest {

	@Mock
	private IdentityMapper identityMapper;

	private IdentityServiceImpl service() {
		return new IdentityServiceImpl(identityMapper);
	}

	@Test
	@DisplayName("同用户已验证声明直接返回，不更新")
	void findOrCreateVerified_SameUserVerified_AsIs() {
		SysIdentity existing = new SysIdentity();
		existing.setUserId(1L);
		existing.setVerified(1);
		when(identityMapper.selectByTypeValue(IdentityType.PHONE, "13800000000")).thenReturn(existing);

		SysIdentity result = service().findOrCreateVerified(IdentityType.PHONE, "13800000000", null, 1L,
				IdentityVerifier.SMS_CODE);

		assertSame(existing, result);
		verify(identityMapper, never()).update(any());
		verify(identityMapper, never()).create(any());
	}

	@Test
	@DisplayName("同用户未验证声明被标记为已验证并更新")
	void findOrCreateVerified_SameUserUnverified_MarksVerified() {
		SysIdentity existing = new SysIdentity();
		existing.setUserId(1L);
		existing.setVerified(0);
		when(identityMapper.selectByTypeValue(IdentityType.PHONE, "13800000000")).thenReturn(existing);

		SysIdentity result = service().findOrCreateVerified(IdentityType.PHONE, "13800000000", null, 1L,
				IdentityVerifier.SMS_CODE);

		assertTrue(result.identityVerified());
		verify(identityMapper).update(existing);
		verify(identityMapper, never()).create(any());
	}

	@Test
	@DisplayName("手机号被他人占用抛 phone_occupied")
	void findOrCreateVerified_OtherUserPhone_ThrowsOccupied() {
		SysIdentity existing = new SysIdentity();
		existing.setUserId(2L);
		existing.setVerified(1);
		when(identityMapper.selectByTypeValue(IdentityType.PHONE, "13800000000")).thenReturn(existing);

		BusinessException ex = assertThrows(BusinessException.class, () -> service()
				.findOrCreateVerified(IdentityType.PHONE, "13800000000", null, 1L, IdentityVerifier.SMS_CODE));

		assertEquals("error.identity.phone_occupied", ex.getMessageKey());
	}

	@Test
	@DisplayName("邮箱被他人占用抛 email_occupied")
	void findOrCreateVerified_OtherUserEmail_ThrowsOccupied() {
		SysIdentity existing = new SysIdentity();
		existing.setUserId(2L);
		existing.setVerified(1);
		when(identityMapper.selectByTypeValue(IdentityType.EMAIL, "a@b.com")).thenReturn(existing);

		BusinessException ex = assertThrows(BusinessException.class,
				() -> service().findOrCreateVerified(IdentityType.EMAIL, "a@b.com", null, 1L, IdentityVerifier.USER));

		assertEquals("error.identity.email_occupied", ex.getMessageKey());
	}

	@Test
	@DisplayName("不存在时创建新声明")
	void findOrCreateVerified_NotFound_Creates() {
		when(identityMapper.selectByTypeValue(IdentityType.PHONE, "13800000000")).thenReturn(null);

		SysIdentity result = service().findOrCreateVerified(IdentityType.PHONE, "13800000000", null, 1L,
				IdentityVerifier.SMS_CODE);

		assertTrue(result.identityVerified());
		assertEquals(1L, result.getUserId());
		verify(identityMapper).create(result);
	}

	@Test
	@DisplayName("带 provider 时按 provider+value 查询")
	void findOrCreateVerified_WithProvider_QueriesByProvider() {
		when(identityMapper.selectByTypeProviderValue(IdentityType.OIDC_SUB, "github", "open-1")).thenReturn(null);

		service().findOrCreateVerified(IdentityType.OIDC_SUB, "open-1", "github", 3L, IdentityVerifier.OAUTH2);

		verify(identityMapper, never()).selectByTypeValue(any(), any());
		verify(identityMapper).create(any(SysIdentity.class));
	}

	@Test
	@DisplayName("并发创建唯一键冲突时重查并返回已存在行")
	void findOrCreateVerified_DuplicateKey_RequeryReturnsExisting() {
		SysIdentity existing = new SysIdentity();
		existing.setUserId(1L);
		existing.setVerified(1);
		when(identityMapper.selectByTypeValue(IdentityType.PHONE, "13800000000")).thenReturn(null, existing);
		when(identityMapper.selectByTypeProviderValue(eq(IdentityType.PHONE), isNull(), eq("13800000000")))
				.thenReturn(null);
		when(identityMapper.create(any(SysIdentity.class))).thenThrow(new DuplicateKeyException("dup"));

		SysIdentity result = service().findOrCreateVerified(IdentityType.PHONE, "13800000000", null, 1L,
				IdentityVerifier.SMS_CODE);

		assertSame(existing, result);
		verify(identityMapper, times(2)).selectByTypeValue(IdentityType.PHONE, "13800000000");
	}
}
