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
package top.ruilink.inkwash.system.api;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;
import lombok.extern.slf4j.Slf4j;
import top.ruilink.inkwash.security.license.HardwareFingerprinter;
import top.ruilink.inkwash.security.license.LicenseConfig;
import top.ruilink.inkwash.security.license.LicenseData;
import top.ruilink.inkwash.security.license.LicenseValidator;
import top.ruilink.inkwash.security.license.LicenseValidator.LicenseVerifyResult;
import top.ruilink.inkwash.security.license.TierService;
import top.ruilink.inkwash.system.api.view.LicenseInfoView;

/**
 * License Controller Provides license verification API endpoints
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Slf4j
@RestController
@RequestMapping("/api/license")
@Validated
public class LicenseController {
	private final LicenseValidator licenseValidator;
	private final TierService tierService;
	private final LicenseConfig licenseConfig;
	/**
	 * Shared hardware fingerprint, the same value {@code LicenseInterceptor} gates
	 * requests with. A machine-bound license must be checked against it, otherwise
	 * every non-TRIAL license is reported as {@code MACHINE_MISMATCH}.
	 */
	private final String machineCode = HardwareFingerprinter.machineCode();
	private final Object verifyLock = new Object();
	private volatile LicenseVerifyResult cachedVerifyResult;
	private volatile long cachedVerifyAt;

	public LicenseController(LicenseValidator licenseValidator, TierService tierService, LicenseConfig licenseConfig) {
		this.licenseValidator = licenseValidator;
		this.tierService = tierService;
		this.licenseConfig = licenseConfig;
	}

	/**
	 * Verify the deployed license signature at most once per configured cache TTL.
	 * Successful results are cached for {@code license.cache-ttl-minutes}; failed
	 * verifications are never cached so a repaired license is picked up quickly.
	 */
	private LicenseVerifyResult verifySysLicense(String licenseString) {
		long ttlMs = licenseConfig.getCacheTtlMinutes() * 60 * 1000L;
		LicenseVerifyResult cached = cachedVerifyResult;
		if (cached != null && System.currentTimeMillis() - cachedVerifyAt < ttlMs) {
			return cached;
		}
		synchronized (verifyLock) {
			cached = cachedVerifyResult;
			if (cached != null && System.currentTimeMillis() - cachedVerifyAt < ttlMs) {
				return cached;
			}
			LicenseVerifyResult result = licenseValidator.verify(licenseString, machineCode);
			if (result.isSuccess()) {
				cachedVerifyResult = result;
				cachedVerifyAt = System.currentTimeMillis();
			}
			return result;
		}
	}

	/**
	 * Get public key for client-side license generation This endpoint is public and
	 * doesn't require authentication
	 */
	@GetMapping("/public-key")
	public ResponseEntity<Map<String, String>> getPublicKey() {
		String publicKey = licenseValidator.getPublicKeyBase64();
		return ResponseEntity.ok(Map.of("publicKey", publicKey, "algorithm", "RSA-SHA256"));
	}

	/**
	 * Verify a license string This endpoint is public and doesn't require
	 * authentication
	 */
	@PostMapping("/verify")
	@PreAuthorize("hasAuthority('system:license:verify')")
	public ResponseEntity<LicenseVerifyResult> verifyLicense(@RequestBody Map<String, String> request) {
		String licenseString = request.get("license");
		String machineCode = request.get("machineCode");
		log.info("License verification request received");
		LicenseVerifyResult result = licenseValidator.verify(licenseString, machineCode);
		if (result.isSuccess()) {
			return ResponseEntity.ok(result);
		} else {
			return ResponseEntity.badRequest().body(result);
		}
	}

	/**
	 * Simple license status check - returns basic validity This can be used for
	 * health checks
	 */
	@GetMapping("/status")
	@PreAuthorize("hasAuthority('system:license:status')")
	public ResponseEntity<Map<String, Object>> getLicenseStatus(@RequestParam(required = false) String license,
			@RequestParam(required = false) String machineCode) {
		if (license == null || license.trim().isEmpty()) {
			return ResponseEntity
					.ok(Map.of("licensed", false, "status", "NO_LICENSE", "message", "No license provided"));
		}
		LicenseVerifyResult result = licenseValidator.verify(license, machineCode);
		return ResponseEntity.ok(Map.of("licensed", result.isSuccess(), "status", result.getStatus().name(), "message",
				result.getMessage()));
	}

	/**
	 * Health check endpoint - always returns OK if service is running
	 */
	@GetMapping("/health")
	public ResponseEntity<Map<String, String>> health() {
		return ResponseEntity.ok(Map.of("service", "license", "status", "UP"));
	}

	/**
	 * Whether the caller presented valid credentials.
	 *
	 * <p>
	 * Spring Security installs an {@link AnonymousAuthenticationToken} for
	 * unauthenticated requests, and that token reports itself as authenticated — so
	 * it has to be excluded explicitly, otherwise every anonymous caller would look
	 * signed in.
	 * 
	 * @return {@code true} only for a real, authenticated principal
	 */
	private boolean isAuthenticatedCaller() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication != null && authentication.isAuthenticated()
				&& !(authentication instanceof AnonymousAuthenticationToken);
	}

	/**
	 * Report status of the deployed license.dat Returns 200 with a structured body
	 * regardless of license validity
	 *
	 * <p>
	 * Reachable without authentication, because the admin frontend calls it during
	 * bootstrap — before anyone has signed in — to decide whether to show a licence
	 * banner. Everything the banner needs (licensed, edition, maxUsers,
	 * tierExceeded, allowedModules) is returned to everyone; the licence holder's
	 * contact email is withheld unless the caller is authenticated.
	 */
	@GetMapping("/info")
	public ResponseEntity<LicenseInfoView> getLicenseInfo() {
		boolean anonymous = !isAuthenticatedCaller();
		String licenseString = licenseValidator.getConfiguredLicense();
		if (licenseString == null || licenseString.trim().isEmpty()) {
			return ResponseEntity.ok(new LicenseInfoView(false, "NO_LICENSE", "No license deployed",
					LicenseValidator.getWarningMessage("NO_LICENSE", ""), null, null, 0, null, false, null));
		}
		LicenseVerifyResult result = verifySysLicense(licenseString);
		LicenseInfoView.LicenseDataView dataView = null;
		String edition = null;
		Integer currentUserCount = 0;
		Integer maxUsers = null;
		boolean tierExceeded = false;
		java.util.List<String> allowedModules = null;
		if (result.getLicenseData() != null) {
			LicenseData ld = result.getLicenseData();
			dataView = new LicenseInfoView.LicenseDataView(ld.getSubject(), ld.getHolder(),
					anonymous ? null : ld.getEmail(), ld.getType() == null ? null : ld.getType().name(),
					ld.getIssuedDate(), ld.getExpirationDate(), ld.getMaxUsers(), ld.getModules());
			// Run tier check
			TierService.TierCheckResult tierResult = tierService.checkAccess(ld, null);
			edition = tierResult.edition();
			currentUserCount = tierResult.currentUserCount();
			maxUsers = tierResult.maxUsers();
			tierExceeded = tierResult.userLimitExceeded();
			allowedModules = tierResult.allowedModules();
		}
		String warning = result.isSuccess() ? ""
				: LicenseValidator.getWarningMessage(result.getStatus().name(), result.getMessage());
		return ResponseEntity.ok(new LicenseInfoView(result.isSuccess(), result.getStatus().name(), result.getMessage(),
				warning, dataView, edition, currentUserCount, maxUsers, tierExceeded, allowedModules));
	}
}
