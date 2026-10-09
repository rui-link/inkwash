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
package top.ruilink.inkwash.cmd.license;

import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.util.Base64;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * License Generator Service Generates signed license files using RSA private
 * key
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Slf4j
public class LicenseGenerator {

	private final LicenseKeyManager keyManager;
	private final ObjectMapper objectMapper;

	public LicenseGenerator(LicenseKeyManager keyManager) {
		this.keyManager = keyManager;
		this.objectMapper = JsonMapper.builder().build();
	}

	public String generateLicense(LicenseData data) {
		try {
			String contentJson = objectMapper.writeValueAsString(data);
			String contentBase64 = Base64.getEncoder().encodeToString(contentJson.getBytes(StandardCharsets.UTF_8));
			String signature = sign(contentBase64);

			Map<String, String> licenseMap = Map.of("content", contentBase64, "signature", signature);
			String licenseJson = objectMapper.writeValueAsString(licenseMap);
			return Base64.getEncoder().encodeToString(licenseJson.getBytes(StandardCharsets.UTF_8));

		} catch (Exception e) {
			throw new RuntimeException("Failed to generate license", e);
		}
	}

	private String sign(String data) {
		try {
			Signature signature = Signature.getInstance("SHA256withRSA");
			signature.initSign(keyManager.getPrivateKey());
			signature.update(data.getBytes(StandardCharsets.UTF_8));
			byte[] signatureBytes = signature.sign();
			return Base64.getEncoder().encodeToString(signatureBytes);
		} catch (Exception e) {
			throw new RuntimeException("Failed to sign license", e);
		}
	}
}
