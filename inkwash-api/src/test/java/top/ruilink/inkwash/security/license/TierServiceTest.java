package top.ruilink.inkwash.security.license;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import top.ruilink.inkwash.system.mapper.UserMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * License edition user limits and module access gating unit tests.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@ExtendWith(MockitoExtension.class)
class TierServiceTest {

	@Mock
	private UserMapper userMapper;

	@InjectMocks
	private TierService tierService;

	private LicenseData baseLicense;

	@BeforeEach
	void setUp() {
		baseLicense = new LicenseData();
		baseLicense.setSubject("Inkwash");
		baseLicense.setHolder("Test");
		baseLicense.setEdition("professional");
	}

	@Test
	@DisplayName("专业版 - 用户未超限 - 所有模块可访问")
	void checkAccess_Proessional_UnderLimit_AllModulesAllowed() {
		baseLicense.setEdition("professional");
		when(userMapper.count()).thenReturn(50L);

		TierService.TierCheckResult result = tierService.checkAccess(baseLicense, "cms");

		assertTrue(result.allowed());
		assertFalse(result.userLimitExceeded());
		assertEquals(50, result.currentUserCount());
		assertEquals(100, result.maxUsers());
		assertEquals(List.of("system", "cms", "monitor"), result.allowedModules());
	}

	@Test
	@DisplayName("专业版 - 用户超限 - cms模块仍可访问")
	void checkAccess_Proessional_Exceeded_CmsAllowed() {
		baseLicense.setEdition("professional");
		when(userMapper.count()).thenReturn(101L);

		TierService.TierCheckResult result = tierService.checkAccess(baseLicense, "cms");

		assertTrue(result.allowed());
		assertTrue(result.userLimitExceeded());
		assertEquals(101, result.currentUserCount());
		assertEquals(List.of("system", "cms"), result.allowedModules());
	}

	@Test
	@DisplayName("专业版 - 用户超限 - system模块仍可访问")
	void checkAccess_Proessional_Exceeded_SystemAllowed() {
		baseLicense.setEdition("professional");
		when(userMapper.count()).thenReturn(101L);

		TierService.TierCheckResult result = tierService.checkAccess(baseLicense, "system");

		assertTrue(result.allowed());
		assertTrue(result.userLimitExceeded());
	}

	@Test
	@DisplayName("个人版 - 用户未超限 - 所有模块可访问")
	void checkAccess_Personal_UnderLimit_AllModulesAllowed() {
		baseLicense.setEdition("personal");
		when(userMapper.count()).thenReturn(20L);

		TierService.TierCheckResult result = tierService.checkAccess(baseLicense, "monitor");

		assertTrue(result.allowed());
		assertFalse(result.userLimitExceeded());
	}

	@Test
	@DisplayName("个人版 - 用户超限 - 只有system模块可访问")
	void checkAccess_Personal_Exceeded_OnlySystemAllowed() {
		baseLicense.setEdition("personal");
		when(userMapper.count()).thenReturn(31L);

		TierService.TierCheckResult result = tierService.checkAccess(baseLicense, "cms");

		assertFalse(result.allowed());
		assertTrue(result.userLimitExceeded());
		assertEquals(List.of("system"), result.allowedModules());
	}

	@Test
	@DisplayName("企业版 - 无用户限制 - 所有模块可访问")
	void checkAccess_Enterprise_NoLimit_AllModulesAllowed() {
		baseLicense.setEdition("enterprise");
		when(userMapper.count()).thenReturn(9999L);

		TierService.TierCheckResult result = tierService.checkAccess(baseLicense, "monitor");

		assertTrue(result.allowed());
		assertFalse(result.userLimitExceeded());
		assertEquals(null, result.maxUsers());
	}

	@Test
	@DisplayName("未指定edition时默认为professional")
	void checkAccess_NullEdition_DefaultsToProfessional() {
		baseLicense.setEdition(null);
		when(userMapper.count()).thenReturn(50L);

		TierService.TierCheckResult result = tierService.checkAccess(baseLicense, "cms");

		assertTrue(result.allowed());
		assertEquals("professional", result.edition());
	}

	@Test
	@DisplayName("边界值 - 正好30个用户 - 个人版不超限")
	void checkAccess_Personal_Boundary_30Users_NotExceeded() {
		baseLicense.setEdition("personal");
		when(userMapper.count()).thenReturn(30L);

		TierService.TierCheckResult result = tierService.checkAccess(baseLicense, "cms");

		assertTrue(result.allowed());
		assertFalse(result.userLimitExceeded());
	}

	@Test
	@DisplayName("边界值 - 31个用户 - 个人版超限")
	void checkAccess_Personal_Boundary_31Users_Exceeded() {
		baseLicense.setEdition("personal");
		when(userMapper.count()).thenReturn(31L);

		TierService.TierCheckResult result = tierService.checkAccess(baseLicense, "cms");

		assertFalse(result.allowed());
		assertTrue(result.userLimitExceeded());
	}

	@Test
	@DisplayName("试用版 - 用户未超限 - 所有模块可访问")
	void trial_withinLimit_allModulesAllowed() {
		LicenseData license = new LicenseData();
		license.setEdition("trial");
		when(userMapper.count()).thenReturn(3L);
		TierService.TierCheckResult result = tierService.checkAccess(license, "cms");
		assertTrue(result.allowed());
		assertFalse(result.userLimitExceeded());
		assertEquals(3, result.currentUserCount());
		assertEquals(10, result.maxUsers());
		assertEquals(List.of("system", "cms", "monitor"), result.allowedModules());
	}

	@Test
	@DisplayName("试用版 - 用户超限 - 只有system模块可访问")
	void trial_exceeded_onlySystemModule() {
		LicenseData license = new LicenseData();
		license.setEdition("trial");
		when(userMapper.count()).thenReturn(11L);
		TierService.TierCheckResult result = tierService.checkAccess(license, "system");
		assertTrue(result.allowed());
		assertTrue(result.userLimitExceeded());
		assertEquals(List.of("system"), result.allowedModules());
	}

	@Test
	@DisplayName("null模块请求 - 始终允许")
	void checkAccess_NullModule_AlwaysAllowed() {
		baseLicense.setEdition("personal");
		when(userMapper.count()).thenReturn(31L);

		TierService.TierCheckResult result = tierService.checkAccess(baseLicense, null);

		assertTrue(result.allowed());
	}
}
