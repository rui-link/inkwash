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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * License signature and machine-binding validator; the expiration date is
 * recorded but never enforced.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Slf4j
@Component
public class LicenseValidator {

	@Value("${license.public-key:}")
	private String publicKeyBase64;

	private PublicKey publicKey;
	private final JsonMapper objectMapper;

	public LicenseValidator() {
		this.objectMapper = JsonMapper.builder().build();
	}

	@PostConstruct
	public void init() {
		if (publicKeyBase64 != null && !publicKeyBase64.isEmpty()) {
			try {
				byte[] keyBytes = Base64.getDecoder().decode(publicKeyBase64);
				X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
				KeyFactory keyFactory = KeyFactory.getInstance("RSA");
				this.publicKey = keyFactory.generatePublic(keySpec);
				log.info("License public key loaded from configuration");
			} catch (Exception e) {
				log.error("Failed to load license public key", e);
			}
		} else {
			log.warn("No license public key configured - license validation will fail");
		}
	}

	/**
	 * Verify a license string
	 */
	public LicenseVerifyResult verify(String licenseString, String machineCode) {
		if (licenseString == null || licenseString.trim().isEmpty()) {
			return LicenseVerifyResult.error("License string is empty", VerifyStatus.EMPTY);
		}

		if (publicKey == null) {
			return LicenseVerifyResult.error("License public key not configured", VerifyStatus.ERROR);
		}

		try {
			// Step 1: Decode Base64
			byte[] licenseBytes = Base64.getDecoder().decode(licenseString.trim());
			String licenseJson = new String(licenseBytes, StandardCharsets.UTF_8);

			// Step 2: Parse JSON to get content and signature
			LicenseFile licenseFile = objectMapper.readValue(licenseJson, LicenseFile.class);

			if (licenseFile.getContent() == null || licenseFile.getSignature() == null) {
				return LicenseVerifyResult.error("Invalid license file structure", VerifyStatus.INVALID_FORMAT);
			}

			// Step 3: Verify signature
			boolean signatureValid = verifySignature(licenseFile.getContent(), licenseFile.getSignature());
			if (!signatureValid) {
				log.warn("License signature verification failed");
				return LicenseVerifyResult.error("License signature verification failed",
						VerifyStatus.INVALID_SIGNATURE);
			}

// Step 4: Decode and parse content
			byte[] contentBytes = Base64.getDecoder().decode(licenseFile.getContent());
			String contentJson = new String(contentBytes, StandardCharsets.UTF_8);

			LicenseData licenseData = parseLicenseData(contentJson);

			// Step 5: Verify machine code binding, required for every non-TRIAL license
			if (licenseData.isMachineBound()) {
				if (machineCode == null) {
					log.warn("License requires machine code but none provided");
					return LicenseVerifyResult.error("License requires machine code binding",
							VerifyStatus.MACHINE_MISMATCH);
				}
				if (!licenseData.verifyMachineCode(machineCode)) {
					log.warn("Machine code mismatch");
					return LicenseVerifyResult.error("Machine code mismatch - license bound to different machine",
							VerifyStatus.MACHINE_MISMATCH);
				}
			} else if (licenseData.getType() != LicenseData.LicenseType.TRIAL) {
				log.warn("License carries no machine code but its type is {}", licenseData.getType());
				return LicenseVerifyResult.error("License must be bound to a machine code",
						VerifyStatus.MACHINE_MISMATCH);
			}

			// All checks passed
			log.info("License verified successfully for: {}", licenseData.getHolder());
			return LicenseVerifyResult.builder().success(true).status(VerifyStatus.VALID).message("License is valid")
					.licenseData(licenseData).build();

		} catch (IllegalArgumentException e) {
			log.error("Invalid Base64 encoding in license", e);
			return LicenseVerifyResult.error("Invalid license encoding", VerifyStatus.INVALID_FORMAT);
		} catch (JacksonException e) {
			log.error("Failed to parse license JSON", e);
			return LicenseVerifyResult.error("Invalid license JSON format", VerifyStatus.INVALID_FORMAT);
		} catch (Exception e) {
			log.error("Unexpected error during license verification", e);
			return LicenseVerifyResult.error("License verification failed: " + e.getMessage(), VerifyStatus.ERROR);
		}
	}

	/**
	 * Verify a license without supplying a machine code, which is only accepted for
	 * TRIAL licenses
	 */
	public LicenseVerifyResult verify(String licenseString) {
		return verify(licenseString, null);
	}

	/**
	 * Verify RSA signature
	 */
	private boolean verifySignature(String content, String signatureBase64) {
		try {
			Signature signature = Signature.getInstance("SHA256withRSA");
			signature.initVerify(publicKey);
			signature.update(content.getBytes(StandardCharsets.UTF_8));
			byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);
			return signature.verify(signatureBytes);
		} catch (Exception e) {
			log.error("Signature verification error", e);
			return false;
		}
	}

	/**
	 * Parse license data from JSON content
	 */
	private LicenseData parseLicenseData(String contentJson) throws JacksonException {
		LicenseData licenseData = objectMapper.readValue(contentJson, LicenseData.class);

		// Validate required fields
		if (licenseData.getHolder() == null || licenseData.getIssuedDate() == null) {
			throw new IllegalArgumentException("Invalid license: missing required fields (holder, issuedDate)");
		}

		// Extract modules from extra if not directly present
		if (licenseData.getModules() == null && licenseData.getExtra() != null) {
			Object modulesObj = licenseData.getExtra().get("modules");
			if (modulesObj instanceof List) {
				@SuppressWarnings("unchecked")
				List<String> modules = (List<String>) modulesObj;
				licenseData.setModules(modules);
			}
		}

		return licenseData;
	}

	/**
	 * Get public key for client embedding
	 */
	public String getPublicKeyBase64() {
		return publicKeyBase64;
	}

	/**
	 * Read the deployed license.dat from the classpath
	 */
	public String getConfiguredLicense() {
		try {
			var resource = getClass().getClassLoader().getResource("license.dat");
			if (resource != null) {
				var path = Path.of(resource.toURI());
				return Files.readString(path).trim();
			}
		} catch (Exception e) {
			log.warn("Failed to read license.dat file: {}", e.getMessage());
		}
		return null;
	}

	/**
	 * User-friendly warning message for frontend display
	 */
	public static String getWarningMessage(String status, String originalMessage) {
		return switch (status) {
		case "NO_LICENSE" -> "管理系统未授权使用，请联系管理员获取有效License";
		case "INVALID_SIGNATURE" -> "License验证失败，License可能已被篡改";
		case "MACHINE_MISMATCH" -> "License与当前机器不匹配，请联系管理员";
		default -> "License验证失败: " + originalMessage;
		};
	}

	/**
	 * License file structure
	 */
	@Data
	public static class LicenseFile {
		private String content;
		private String signature;
	}

	/**
	 * Verification status enum
	 */
	public enum VerifyStatus {
		VALID, INVALID_SIGNATURE, MACHINE_MISMATCH, INVALID_FORMAT, EMPTY, ERROR
	}

	/**
	 * Verification result DTO
	 */
	@Data
	@Builder
	public static class LicenseVerifyResult {
		private boolean success;
		private VerifyStatus status;
		private String message;
		private LicenseData licenseData;

		public static LicenseVerifyResult error(String message, VerifyStatus status) {
			return LicenseVerifyResult.builder().success(false).status(status).message(message).build();
		}
	}
}
