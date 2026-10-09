/*
 * This file is part of Inkwash.
 * Copyright (C) 2026 ruilink team.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package top.ruilink.inkwash.security.license;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import top.ruilink.inkwash.security.license.LicenseValidator.LicenseVerifyResult;

/**
 * Verifies the bundled trial license against the public key that
 * {@code application.yaml} actually embeds.
 *
 * <p>
 * The CLI has its own verifier, so a green CLI run proves nothing about the
 * running application: it could be validating against a different key, or the
 * key in {@code license.public-key} could have drifted from the one that signed
 * the file. This test closes that gap by using the application's own
 * {@link LicenseValidator}, wired from the real configuration.
 *
 * <p>
 * The trial license is read from the classpath, because the repository ships
 * one: a fresh clone must run the complete system without a manual
 * licence-issuance step. Reading it here rather than from {@code docs/} also
 * means this test genuinely executes in a clean checkout instead of skipping,
 * so a key that drifts out of sync with the shipped file fails the build.
 */
@SpringBootTest
@DisplayName("随包附带的 trial license 可被应用校验")
class TrialLicenseVerificationTest {

	private static final String EXPECTED_EMAIL = "trial@ruilink.top";

	@Autowired
	private LicenseValidator validator;

	private String trialLicense;

	@BeforeEach
	void loadLicense() throws Exception {
		try (InputStream in = TrialLicenseVerificationTest.class.getClassLoader().getResourceAsStream("license.dat")) {
			assertNotNull(in, "仓库应随包附带试用授权，使新用户 clone 后无需手工步骤即可体验完整功能");
			trialLicense = new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
		}
	}

	@Test
	@DisplayName("配置中的公钥能验证 trial license 签名")
	void signatureVerifiesAgainstConfiguredPublicKey() {
		LicenseVerifyResult result = validator.verify(trialLicense, null);

		assertTrue(result.isSuccess(), "校验应通过，实际状态：" + result.getStatus() + " / " + result.getMessage());
		assertEquals(LicenseValidator.VerifyStatus.VALID, result.getStatus());
		assertEquals(EXPECTED_EMAIL, result.getLicenseData().getEmail());
	}

	@Test
	@DisplayName("license 不绑定机器码 —— 在任意机器上均可校验")
	void licenseIsNotMachineBound() {
		LicenseVerifyResult result = validator.verify(trialLicense, null);

		assertTrue(result.isSuccess());
		assertNull(result.getLicenseData().getMachineCode(), "trial license 不应携带机器码");
		assertFalse(result.getLicenseData().isMachineBound(), "trial license 不应是机器绑定的");
	}

	@Test
	@DisplayName("类型为 TRIAL，档位与用户上限符合试用约定")
	void licenseIsTrialTiered() {
		LicenseData data = validator.verify(trialLicense, null).getLicenseData();

		assertEquals(LicenseData.LicenseType.TRIAL, data.getType());
		assertEquals("trial", data.getEdition());
		assertEquals(Integer.valueOf(10), data.getMaxUsers());
	}

	@Test
	@DisplayName("即使传入任意机器码也照样通过（证明未绑定，而非恰好匹配）")
	void anyMachineCodeStillPasses() {
		LicenseVerifyResult withGarbage = validator.verify(trialLicense, "deadbeefdeadbeefdeadbeefdeadbeef");

		assertTrue(withGarbage.isSuccess(), "未绑定的 license 不应因机器码不匹配而失败：" + withGarbage.getStatus());
	}

	@Test
	@DisplayName("应用确实读到了非空的公钥配置")
	void publicKeyIsConfigured() throws Exception {
		Field field = LicenseValidator.class.getDeclaredField("publicKeyBase64");
		field.setAccessible(true);
		String configured = (String) field.get(validator);

		assertTrue(configured != null && !configured.isBlank(), "license.public-key 未配置，validate 会返回 ERROR 而非 VALID");
	}
}