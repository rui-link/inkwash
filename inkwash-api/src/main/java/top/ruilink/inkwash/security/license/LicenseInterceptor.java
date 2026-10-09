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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * License Interceptor Validates license on protected endpoints
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Slf4j
@Component
public class LicenseInterceptor implements HandlerInterceptor {

	private final LicenseValidator licenseValidator;
	private final TierService tierService;
	private final LicenseConfig licenseConfig;

	/**
	 * Machine code derived once from the shared hardware fingerprint
	 * ({@link HardwareFingerprinter}), the same value the cmd tool binds for
	 * machine-bound licenses. Computed once at bean construction.
	 */
	private final String machineCode = HardwareFingerprinter.machineCode();

	/**
	 * In-memory cache for license status Key format:
	 * "license_status:{machineCode}:{module}" for tier cache Key format:
	 * "license_sig:{machineCode}" for signature validation cache
	 */
	private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

	/**
	 * Paths that don't require license validation (public APIs) - loaded from
	 * config
	 */
	private List<String> EXCLUDED_PATHS;

	/**
	 * Public-read prefix mirroring SecurityConfig: {@code GET /api/cms/articles}
	 * and {@code GET /api/cms/articles/**} are permitAll for portal browsing, but
	 * every other method under it is authenticated admin CRUD and must therefore
	 * still pass license validation.
	 */
	private static final String CMS_ARTICLES_PREFIX = "/api/cms/articles";

	/**
	 * Paths that require license validation (admin APIs only)
	 */
	private static final String ADMIN_API_PREFIX = "/api/";

	/**
	 * Module to path mapping (module name -> path prefix)
	 */
	private static final Map<String, String> MODULE_PATHS = Map.of("system", "/api/system/", "cms", "/api/cms/",
			"monitor", "/api/monitor/");

	public LicenseInterceptor(LicenseValidator licenseValidator, TierService tierService, LicenseConfig licenseConfig) {
		this.licenseValidator = licenseValidator;
		this.tierService = tierService;
		this.licenseConfig = licenseConfig;
		// The fingerprint is a stable per-host identifier: useful when diagnosing a
		// licence
		// mismatch, but it is not an event worth recording on every boot, and shipping
		// it to
		// a log aggregator would persist a machine identity. DEBUG keeps it available
		// for
		// support without putting it in the default log stream.
		log.debug("License machine code (shared hardware fingerprint): {}", machineCode);
	}

	@PostConstruct
	public void init() {
		if (licenseConfig.getExcludedPaths() == null || licenseConfig.getExcludedPaths().isEmpty()) {
			// Default fallback
			this.EXCLUDED_PATHS = List.of("/api/license/", "/api/auth/", "/api/cms/articles", "/api/system/meta",
					"/health", "/error");
			log.warn("No license.excluded-paths configured, using defaults");
		} else {
			this.EXCLUDED_PATHS = licenseConfig.getExcludedPaths();
			log.info("Loaded {} license excluded paths from configuration", EXCLUDED_PATHS.size());
		}
	}

	private long getCacheTtlMs() {
		return licenseConfig.getCacheTtlMinutes() * 60 * 1000L;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		String path = request.getRequestURI();

		if (isExcluded(request)) {
			return true;
		}

		if (!path.startsWith(ADMIN_API_PREFIX)) {
			log.debug("Non-admin API path, skipping license check: {}", path);
			return true;
		}

		String module = getModuleFromPath(path);

		// Check signature cache first (per machineCode)
		String sigCacheKey = "license_sig:" + machineCode;
		CacheEntry sigCached = cache.get(sigCacheKey);
		if (sigCached != null && !sigCached.isExpired(getCacheTtlMs())) {
			if (!sigCached.isSuccess()) {
				log.warn("Cached license signature invalid for machineCode={}, rejecting request to: {}", machineCode,
						path);
				sendLicenseError(response, sigCached.getStatus(), sigCached.getMessage());
				return false;
			}
			// Signature valid — check tier cache (per machineCode + module)
			String tierCacheKey = "license_tier:" + machineCode + ":" + (module != null ? module : "global");
			CacheEntry tierCached = cache.get(tierCacheKey);
			if (tierCached != null && !tierCached.isExpired(getCacheTtlMs())) {
				if (!tierCached.isSuccess()) {
					sendTierExceededError(response,
							new TierService.TierCheckResult(false, true, tierCached.getCurrentUserCount(),
									tierCached.getMaxUsers(), tierCached.getEdition(), tierCached.getAllowedModules()));
					return false;
				}
				log.debug("License and tier validated from cache for machineCode={}, module={}, path={}", machineCode,
						module, path);
				return true;
			}
		}

		// Get and verify license (signature only, no expiration check)
		String licenseString = getLicenseFromConfig();
		if (licenseString == null || licenseString.trim().isEmpty()) {
			log.warn("No license found, rejecting request to: {}", path);
			CacheEntry entry = new CacheEntry(false, "NO_LICENSE", "系统未授权，请配置有效License", null, 0, null, null, null);
			cache.put(sigCacheKey, entry);
			sendLicenseError(response, "NO_LICENSE", "系统未授权，请配置有效License");
			return false;
		}

		LicenseValidator.LicenseVerifyResult result = licenseValidator.verify(licenseString, machineCode);

		if (!result.isSuccess()) {
			log.warn("License validation failed for machineCode={}, path={}: {} - {}", machineCode, path,
					result.getStatus(), result.getMessage());
			CacheEntry entry = new CacheEntry(false, result.getStatus().name(), result.getMessage(), null, 0, null,
					null, null);
			cache.put(sigCacheKey, entry);
			sendLicenseError(response, result.getStatus().name(), result.getMessage());
			return false;
		}

		// Cache the successful signature result
		cache.put(sigCacheKey, new CacheEntry(true, result.getStatus().name(), result.getMessage(),
				result.getLicenseData(), 0, null, null, null));

		// Tier check runs on every request (but we cache the result)
		if (!checkTierAccess(request, response, path, result.getLicenseData(), machineCode)) {
			return false;
		}

		log.debug("License and tier validated for machineCode={}, module={}, path={}", machineCode, module, path);
		return true;
	}

	/**
	 * Check tier/module access for the request path
	 */
	private boolean checkTierAccess(HttpServletRequest request, HttpServletResponse response, String path,
			LicenseData licenseData, String machineCode) throws Exception {
		String module = getModuleFromPath(path);
		TierService.TierCheckResult tierResult = tierService.checkAccess(licenseData, module);

		// Cache tier result
		String tierCacheKey = "license_tier:" + machineCode + ":" + (module != null ? module : "global");
		cache.put(tierCacheKey,
				new CacheEntry(tierResult.allowed(), tierResult.allowed() ? "ALLOWED" : "TIER_EXCEEDED",
						tierResult.allowed() ? "OK" : "Tier limit exceeded", licenseData, tierResult.currentUserCount(),
						tierResult.maxUsers(), tierResult.edition(), tierResult.allowedModules()));

		if (!tierResult.allowed()) {
			log.warn("Tier access denied: edition={}, module={}, exceeded={}", tierResult.edition(), module,
					tierResult.userLimitExceeded());
			sendTierExceededError(response, tierResult);
			return false;
		}
		return true;
	}

	/**
	 * Get module name from path
	 */
	private String getModuleFromPath(String path) {
		for (Map.Entry<String, String> entry : MODULE_PATHS.entrySet()) {
			if (path.startsWith(entry.getValue())) {
				return entry.getKey();
			}
		}
		return null;
	}

	/**
	 * Check if path is excluded from license validation. The CMS article prefix is
	 * narrowed to GET only (public portal browsing), mirroring SecurityConfig;
	 * licensed/admin CRUD operations under it still require a valid license.
	 */
	private boolean isExcluded(HttpServletRequest request) {
		String path = request.getRequestURI();
		boolean excluded = EXCLUDED_PATHS.stream().anyMatch(path::startsWith);
		if (excluded && path.startsWith(CMS_ARTICLES_PREFIX) && !HttpMethod.GET.matches(request.getMethod())) {
			return false;
		}
		return excluded;
	}

	/**
	 * Get license from configuration In production, this should read from a secure
	 * location
	 */
	private String getLicenseFromConfig() {
		return licenseValidator.getConfiguredLicense();
	}

	/**
	 * Send license error response with clear warning for admin pages
	 */
	private void sendLicenseError(HttpServletResponse response, String status, String message) throws Exception {
		response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		response.setContentType("application/json");

		String warningMessage = LicenseValidator.getWarningMessage(status, message);
		response.getWriter()
				.write("{\"code\":40003,\"status\":\"" + status + "\",\"message\":\""
						+ message.replace("\\", "\\\\").replace("\"", "\\\"") + "\",\"warning\":\""
						+ warningMessage.replace("\\", "\\\\").replace("\"", "\\\"") + "\",\"requireLicense\":true}");
	}

	/**
	 * Send tier exceeded error response with detailed info
	 */
	private void sendTierExceededError(HttpServletResponse response, TierService.TierCheckResult tierResult)
			throws Exception {
		response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		response.setContentType("application/json");

		String editionLabel = switch (tierResult.edition()) {
		case "personal" -> "个人版";
		case "professional" -> "专业版";
		case "enterprise" -> "企业版";
		case "trial" -> "试用版";
		default -> tierResult.edition();
		};

		response.getWriter()
				.write("{\"code\":40003," + "\"requireLicense\":true," + "\"tierExceeded\":true," + "\"edition\":\""
						+ tierResult.edition() + "\"," + "\"currentUserCount\":" + tierResult.currentUserCount() + ","
						+ "\"maxUsers\":" + (tierResult.maxUsers() == null ? "null" : tierResult.maxUsers()) + ","
						+ "\"allowedModules\":[\"" + String.join("\",\"", tierResult.allowedModules()) + "\"],"
						+ "\"message\":\"用户数已超过" + editionLabel + "限制\"," + "\"warning\":\"用户数已超过" + editionLabel
						+ "限制，部分模块已禁用\"}");
	}

	/**
	 * Cache entry for license status
	 */
	private static class CacheEntry {
		private final boolean success;
		private final String status;
		private final String message;
		// private final LicenseData licenseData;
		private final long timestamp;
		// Tier-specific fields
		private final int currentUserCount;
		private final Integer maxUsers;
		private final String edition;
		private final List<String> allowedModules;

		CacheEntry(boolean success, String status, String message, LicenseData licenseData, int currentUserCount,
				Integer maxUsers, String edition, List<String> allowedModules) {
			this.success = success;
			this.status = status;
			this.message = message;
			// this.licenseData = licenseData;
			this.timestamp = System.currentTimeMillis();
			this.currentUserCount = currentUserCount;
			this.maxUsers = maxUsers;
			this.edition = edition;
			this.allowedModules = allowedModules;
		}

		boolean isExpired(long ttlMs) {
			return System.currentTimeMillis() - timestamp >= ttlMs;
		}

		boolean isSuccess() {
			return success;
		}

		String getStatus() {
			return status;
		}

		String getMessage() {
			return message;
		}

		int getCurrentUserCount() {
			return currentUserCount;
		}

		Integer getMaxUsers() {
			return maxUsers;
		}

		String getEdition() {
			return edition;
		}

		List<String> getAllowedModules() {
			return allowedModules;
		}
	}
}
