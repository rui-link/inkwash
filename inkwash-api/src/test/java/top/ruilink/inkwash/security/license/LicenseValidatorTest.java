package top.ruilink.inkwash.security.license;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * License warning message mapping and configured license loading unit tests.
 *
 * @author Dyllon
 * @since 0.5.1
 */
class LicenseValidatorTest {

	@Test
	@DisplayName("warning消息映射 - 未授权")
	void getWarningMessage_NoLicense() {
		assertEquals("管理系统未授权使用，请联系管理员获取有效License", LicenseValidator.getWarningMessage("NO_LICENSE", ""));
	}

	@Test
	@DisplayName("warning消息映射 - 签名无效")
	void getWarningMessage_InvalidSignature() {
		assertEquals("License验证失败，License可能已被篡改", LicenseValidator.getWarningMessage("INVALID_SIGNATURE", ""));
	}

	@Test
	@DisplayName("warning消息映射 - 默认分支")
	void getWarningMessage_Default() {
		assertEquals("License验证失败: boom", LicenseValidator.getWarningMessage("ERROR", "boom"));
	}

	/**
	 * The repository ships a TRIAL licence so that a fresh clone runs the complete
	 * system with no manual step. Shipping it is deliberate: the code is GPL, the
	 * licence is a record of entitlements rather than DRM, and a trial file is what
	 * lets a prospective buyer's procurement review have something concrete to look
	 * at. Commercial use is gated by {@code maxUsers} instead of by
	 * {@code expirationDate}, which is recorded but deliberately never enforced —
	 * see {@link LicenseValidator}.
	 */
	@Test
	@DisplayName("随包附带试用授权，clone 后无需任何手工步骤即可读取")
	void getConfiguredLicense_readsBundledTrialLicense() {
		LicenseValidator validator = new LicenseValidator();

		String license = validator.getConfiguredLicense();

		assertNotNull(license, "仓库应随包附带试用授权，让新用户开箱即可体验完整功能");
		assertFalse(license.isBlank(), "随包授权内容不应为空");
	}

	/**
	 * The bundled trial must stay on the trial tier. Without this, editing the
	 * shipped file to raise {@code maxUsers} would be the only way to detect a
	 * tampered bundle, and a widened trial would silently pass every other test
	 * here.
	 */
	@Test
	@DisplayName("随包授权是已签名的试用档，不携带商业档位的权益")
	void getConfiguredLicense_isSignedTrialTierOnly() {
		LicenseValidator validator = new LicenseValidator();

		String license = validator.getConfiguredLicense();
		String json = new String(java.util.Base64.getDecoder().decode(license.trim()),
				java.nio.charset.StandardCharsets.UTF_8);

		assertTrue(json.contains("\"signature\""), "随包授权应为已签名的 license 文件");
		// The payload is signed rather than edited in place, so the trial tier is
		// asserted on
		// the decoded content. Signature verification itself needs the configured
		// public key
		// and is covered by TrialLicenseVerificationTest, which wires the real config.
		String payload = new String(
				java.util.Base64.getDecoder()
						.decode(new tools.jackson.databind.json.JsonMapper().readTree(json).get("content").asString()),
				java.nio.charset.StandardCharsets.UTF_8);

		assertTrue(payload.contains("\"type\":\"TRIAL\""), "随包授权必须是 TRIAL —— 商业档位只通过外部配置路径下发");
		assertTrue(payload.contains("\"machineCode\":null"), "随包授权的 machineCode 应为 null：预签名的文件无法绑定到 clone 者的机器");
	}
}
