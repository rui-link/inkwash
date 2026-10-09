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
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Standalone license verifier mirroring the inkwash-api backend logic
 * (top.ruilink.inkwash.security.license.LicenseValidator): Base64 envelope
 * {content, signature}, SHA256withRSA over the UTF-8 bytes of the base64
 * content, then content parsing and the machine-code binding check, which every
 * non-TRIAL license must satisfy.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public class LicenseVerifier {

	/** Immutable verification outcome. */
	public record Result(boolean valid, String message, LicenseData data) {
		public static Result error(String message) {
			return new Result(false, message, null);
		}

		public static Result success(String message, LicenseData data) {
			return new Result(true, message, data);
		}
	}

	private final PublicKey publicKey;
	private final JsonMapper objectMapper;

	public LicenseVerifier(PublicKey publicKey) {
		this.publicKey = publicKey;
		this.objectMapper = JsonMapper.builder().build();
	}

	/**
	 * Verify a license string. A machine-bound license is rejected when
	 * {@code machineCode} is absent, and a non-TRIAL license without a bound
	 * machine code is always rejected, exactly as the backend does.
	 */
	public Result verify(String licenseString, String machineCode) {
		if (licenseString == null || licenseString.isBlank()) {
			return Result.error("License string is empty");
		}
		if (publicKey == null) {
			return Result.error("License public key not configured");
		}
		try {
			byte[] licenseBytes = Base64.getDecoder().decode(licenseString.trim());
			String licenseJson = new String(licenseBytes, StandardCharsets.UTF_8);

			LicenseFile licenseFile = objectMapper.readValue(licenseJson, LicenseFile.class);
			if (licenseFile.content() == null || licenseFile.signature() == null) {
				return Result.error("Invalid license file structure");
			}

			if (!verifySignature(licenseFile.content(), licenseFile.signature())) {
				return Result.error("License signature verification failed");
			}

			byte[] contentBytes = Base64.getDecoder().decode(licenseFile.content());
			String contentJson = new String(contentBytes, StandardCharsets.UTF_8);
			LicenseData licenseData = objectMapper.readValue(contentJson, LicenseData.class);

			if (licenseData == null || licenseData.holder() == null || licenseData.issuedDate() == null) {
				return Result.error("Invalid license: missing required fields (holder, issuedDate)");
			}

			boolean machineCodePresent = licenseData.machineCode() != null && !licenseData.machineCode().isEmpty();
			if (!machineCodePresent) {
				if (licenseData.type() != LicenseData.LicenseType.TRIAL) {
					return Result.error("License must be bound to a machine code");
				}
			} else {
				if (machineCode == null || machineCode.isEmpty()) {
					return Result.error("License requires machine code binding");
				}
				if (!licenseData.machineCode().equals(machineCode)) {
					return Result.error("Machine code mismatch - license bound to different machine");
				}
			}

			return Result.success("License is valid", licenseData);
		} catch (IllegalArgumentException e) {
			return Result.error("Invalid license encoding: " + e.getMessage());
		} catch (JacksonException e) {
			return Result.error("Invalid license JSON format: " + e.getMessage());
		} catch (Exception e) {
			return Result.error("License verification failed: " + e.getMessage());
		}
	}

	private boolean verifySignature(String content, String signatureBase64) {
		try {
			Signature signature = Signature.getInstance("SHA256withRSA");
			signature.initVerify(publicKey);
			signature.update(content.getBytes(StandardCharsets.UTF_8));
			byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);
			return signature.verify(signatureBytes);
		} catch (Exception e) {
			return false;
		}
	}

	/** Envelope structure {content, signature}, same field names as the backend. */
	private record LicenseFile(String content, String signature) {
	}
}
