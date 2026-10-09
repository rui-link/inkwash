package top.ruilink.inkwash.system.api;

import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import top.ruilink.inkwash.security.license.LicenseConfig;
import top.ruilink.inkwash.security.license.LicenseData;
import top.ruilink.inkwash.security.license.LicenseValidator;
import top.ruilink.inkwash.security.license.HardwareFingerprinter;
import top.ruilink.inkwash.security.license.LicenseValidator.LicenseVerifyResult;
import top.ruilink.inkwash.security.license.LicenseValidator.VerifyStatus;
import top.ruilink.inkwash.security.license.TierService;
import top.ruilink.inkwash.system.api.view.LicenseInfoView;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * License information endpoint unit tests covering verification caching and
 * tier limits.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@ExtendWith(MockitoExtension.class)
class LicenseControllerTest {

	@Mock
	private LicenseValidator licenseValidator;

	@Mock
	private TierService tierService;

	@Mock
	private LicenseConfig licenseConfig;

	@InjectMocks
	private LicenseController licenseController;

	/**
	 * The controller verifies against the host fingerprint rather than
	 * {@code null}; stubbing {@code null} would mask the machine-binding bug this
	 * test now guards.
	 */
	private static final String MACHINE_CODE = HardwareFingerprinter.machineCode();

	@Nested
	@DisplayName("License信息接口测试")
	class LicenseInfoTests {

		private LicenseData licenseData;

		@BeforeEach
		void setUp() {
			licenseData = new LicenseData();
			licenseData.setSubject("Inkwash");
			licenseData.setHolder("测试单位");
			licenseData.setEmail("a@b.com");
			licenseData.setType(LicenseData.LicenseType.PERMANENT);
			licenseData.setExpirationDate(LocalDateTime.of(2030, 1, 1, 0, 0));
		}

		@Test
		@DisplayName("未部署License时返回未授权")
		void getLicenseInfo_NoLicense_NotLicensed() {
			when(licenseValidator.getConfiguredLicense()).thenReturn(null);

			ResponseEntity<LicenseInfoView> response = licenseController.getLicenseInfo();

			assertEquals(200, response.getStatusCode().value());
			assertNotNull(response.getBody());
			assertFalse(response.getBody().licensed());
			assertEquals("NO_LICENSE", response.getBody().status());
			assertNull(response.getBody().licenseData());
			assertNull(response.getBody().edition());
			assertFalse(response.getBody().tierExceeded());
		}

		@Test
		@DisplayName("License有效时返回已授权及详细信息")
		void getLicenseInfo_Valid_Licensed() {
			when(licenseValidator.getConfiguredLicense()).thenReturn("LICENSE_STRING");
			LicenseVerifyResult result = LicenseVerifyResult.builder().success(true).status(VerifyStatus.VALID)
					.message("License is valid").licenseData(licenseData).build();
			when(licenseValidator.verify("LICENSE_STRING", MACHINE_CODE)).thenReturn(result);

			TierService.TierCheckResult tierResult = new TierService.TierCheckResult(true, false, 50, 100,
					"professional", List.of("system", "cms", "monitor"));
			when(tierService.checkAccess(licenseData, null)).thenReturn(tierResult);

			ResponseEntity<LicenseInfoView> response = licenseController.getLicenseInfo();

			assertEquals(200, response.getStatusCode().value());
			LicenseInfoView body = response.getBody();
			assertTrue(body.licensed());
			assertEquals("VALID", body.status());
			assertEquals("", body.warning());
			assertNotNull(body.licenseData());
			assertEquals("测试单位", body.licenseData().holder());
			assertEquals("PERMANENT", body.licenseData().type());
			assertEquals("professional", body.edition());
			assertEquals(50, body.currentUserCount());
			assertEquals(100, body.maxUsers());
			assertFalse(body.tierExceeded());
		}

		@Test
		@DisplayName("License签名无效时返回未授权及warning")
		void getLicenseInfo_InvalidSignature_NotLicensed() {
			when(licenseValidator.getConfiguredLicense()).thenReturn("LICENSE_STRING");
			LicenseVerifyResult result = LicenseVerifyResult.builder().success(false)
					.status(VerifyStatus.INVALID_SIGNATURE).message("License signature verification failed")
					.licenseData(licenseData).build();
			when(licenseValidator.verify("LICENSE_STRING", MACHINE_CODE)).thenReturn(result);

			TierService.TierCheckResult tierResult = new TierService.TierCheckResult(false, false, 0, 100,
					"professional", List.of("system", "cms", "monitor"));
			when(tierService.checkAccess(licenseData, null)).thenReturn(tierResult);

			ResponseEntity<LicenseInfoView> response = licenseController.getLicenseInfo();

			LicenseInfoView body = response.getBody();
			assertNotNull(body);
			assertFalse(body.licensed());
			assertEquals("INVALID_SIGNATURE", body.status());
			assertEquals("License验证失败，License可能已被篡改", body.warning());
			assertNotNull(body.licenseData());
		}

		@Test
		@DisplayName("有效License在缓存周期内重复请求不重复签名校验")
		void getLicenseInfo_Valid_CachedWithinTtl() {
			when(licenseConfig.getCacheTtlMinutes()).thenReturn(720L);
			when(licenseValidator.getConfiguredLicense()).thenReturn("LICENSE_STRING");
			LicenseVerifyResult result = LicenseVerifyResult.builder().success(true).status(VerifyStatus.VALID)
					.message("License is valid").licenseData(licenseData).build();
			when(licenseValidator.verify("LICENSE_STRING", MACHINE_CODE)).thenReturn(result);

			TierService.TierCheckResult tierResult = new TierService.TierCheckResult(true, false, 50, 100,
					"professional", List.of("system", "cms", "monitor"));
			when(tierService.checkAccess(licenseData, null)).thenReturn(tierResult);

			ResponseEntity<LicenseInfoView> first = licenseController.getLicenseInfo();
			ResponseEntity<LicenseInfoView> second = licenseController.getLicenseInfo();

			verify(licenseValidator, times(1)).verify("LICENSE_STRING", MACHINE_CODE);
			assertTrue(first.getBody().licensed());
			assertTrue(second.getBody().licensed());
		}

		@Test
		@DisplayName("缓存周期过后会重新执行签名校验")
		void getLicenseInfo_Valid_ReverifyAfterTtl() {
			when(licenseConfig.getCacheTtlMinutes()).thenReturn(0L);
			when(licenseValidator.getConfiguredLicense()).thenReturn("LICENSE_STRING");
			LicenseVerifyResult result = LicenseVerifyResult.builder().success(true).status(VerifyStatus.VALID)
					.message("License is valid").licenseData(licenseData).build();
			when(licenseValidator.verify("LICENSE_STRING", MACHINE_CODE)).thenReturn(result);

			TierService.TierCheckResult tierResult = new TierService.TierCheckResult(true, false, 50, 100,
					"professional", List.of("system", "cms", "monitor"));
			when(tierService.checkAccess(licenseData, null)).thenReturn(tierResult);

			licenseController.getLicenseInfo();
			licenseController.getLicenseInfo();

			verify(licenseValidator, times(2)).verify("LICENSE_STRING", MACHINE_CODE);
		}

		@Test
		@DisplayName("校验失败不缓存，每次请求都重新校验以便快速恢复")
		void getLicenseInfo_Failed_NotCached() {
			when(licenseConfig.getCacheTtlMinutes()).thenReturn(720L);
			when(licenseValidator.getConfiguredLicense()).thenReturn("LICENSE_STRING");
			LicenseVerifyResult result = LicenseVerifyResult.builder().success(false)
					.status(VerifyStatus.INVALID_SIGNATURE).message("License signature verification failed")
					.licenseData(licenseData).build();
			when(licenseValidator.verify("LICENSE_STRING", MACHINE_CODE)).thenReturn(result);

			TierService.TierCheckResult tierResult = new TierService.TierCheckResult(false, false, 0, 100,
					"professional", List.of("system", "cms", "monitor"));
			when(tierService.checkAccess(licenseData, null)).thenReturn(tierResult);

			assertFalse(licenseController.getLicenseInfo().getBody().licensed());
			assertFalse(licenseController.getLicenseInfo().getBody().licensed());

			verify(licenseValidator, times(2)).verify("LICENSE_STRING", MACHINE_CODE);
		}
	}

	/**
	 * {@code /api/license/info} is reachable without authentication: the admin
	 * frontend calls it during bootstrap, before anyone has signed in, so the
	 * console can decide whether to show a licence banner. The endpoint's purpose
	 * is that status readout — not the licence holder's identity — so the contact
	 * email is withheld from anonymous callers.
	 */
	@Nested
	@DisplayName("未认证调用的授权信息脱敏")
	class AnonymousRedactionTests {

		private LicenseData licenseData;

		@BeforeEach
		void setUp() {
			licenseData = new LicenseData();
			licenseData.setSubject("Inkwash");
			licenseData.setHolder("测试单位");
			licenseData.setEmail("holder@example.com");
			licenseData.setType(LicenseData.LicenseType.TRIAL);
			licenseData.setExpirationDate(LocalDateTime.of(2030, 1, 1, 0, 0));

			when(licenseValidator.getConfiguredLicense()).thenReturn("LICENSE_STRING");
			when(licenseValidator.verify("LICENSE_STRING", MACHINE_CODE))
					.thenReturn(LicenseVerifyResult.builder().success(true).status(VerifyStatus.VALID)
							.message("License is valid").licenseData(licenseData).build());
			when(tierService.checkAccess(licenseData, null)).thenReturn(
					new TierService.TierCheckResult(true, false, 2, 10, "trial", List.of("system", "cms", "monitor")));
		}

		@AfterEach
		void clearSecurityContext() {
			SecurityContextHolder.clearContext();
		}

		@Test
		@DisplayName("无认证上下文时不返回License邮箱")
		void getLicenseInfo_NoAuthentication_OmitsEmail() {
			ResponseEntity<LicenseInfoView> response = licenseController.getLicenseInfo();

			LicenseInfoView body = response.getBody();
			assertNotNull(body);
			assertTrue(body.licensed());
			assertNotNull(body.licenseData());
			assertNull(body.licenseData().email());
		}

		@Test
		@DisplayName("匿名认证令牌不视为已登录，仍不返回邮箱")
		void getLicenseInfo_AnonymousToken_OmitsEmail() {
			SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken("key",
					"anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

			ResponseEntity<LicenseInfoView> response = licenseController.getLicenseInfo();

			LicenseInfoView body = response.getBody();
			assertNotNull(body);
			assertNotNull(body.licenseData());
			assertNull(body.licenseData().email());
		}

		@Test
		@DisplayName("匿名调用仍可读取前端展示所需的授权状态字段")
		void getLicenseInfo_Anonymous_StillExposesStatusFields() {
			ResponseEntity<LicenseInfoView> response = licenseController.getLicenseInfo();

			LicenseInfoView body = response.getBody();
			assertNotNull(body);
			assertTrue(body.licensed());
			assertEquals("VALID", body.status());
			assertEquals("trial", body.edition());
			assertEquals(10, body.maxUsers());
			assertEquals(2, body.currentUserCount());
			assertFalse(body.tierExceeded());
			assertEquals(List.of("system", "cms", "monitor"), body.allowedModules());
		}

		@Test
		@DisplayName("已登录调用返回完整License信息含邮箱")
		void getLicenseInfo_Authenticated_IncludesEmail() {
			SecurityContextHolder.getContext()
					.setAuthentication(new UsernamePasswordAuthenticationToken("1:admin", null, List.of()));

			ResponseEntity<LicenseInfoView> response = licenseController.getLicenseInfo();

			LicenseInfoView body = response.getBody();
			assertNotNull(body);
			assertNotNull(body.licenseData());
			assertEquals("holder@example.com", body.licenseData().email());
		}
	}
}
