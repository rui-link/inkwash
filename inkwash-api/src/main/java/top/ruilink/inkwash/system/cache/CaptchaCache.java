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
package top.ruilink.inkwash.system.cache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;
import top.ruilink.inkwash.security.AuthConsts;

/**
 * In-memory captcha cache storing captcha information in a ConcurrentHashMap.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Slf4j
@Repository
public class CaptchaCache {

	// Stores captcha information keyed by captcha key
	private final ConcurrentHashMap<String, CaptchaInfo> captchaCache = new ConcurrentHashMap<>();

	// Stores SMS captcha information keyed by phone number
	private final ConcurrentHashMap<String, CaptchaInfo> smsCache = new ConcurrentHashMap<>();

	// Periodically purges expired captchas
	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
		Thread t = new Thread(r, "captcha-cache-cleanup");
		t.setDaemon(true);
		return t;
	});

	public CaptchaCache() {
		// Purge expired captchas once a minute
		scheduler.scheduleAtFixedRate(this::cleanupExpiredData, 1, 1, TimeUnit.MINUTES);
	}

	@jakarta.annotation.PreDestroy
	public void destroy() {
		scheduler.shutdownNow();
	}

	/**
	 * Captcha information.
	 */
	private static class CaptchaInfo {
		String code;
		long expireTime;

		CaptchaInfo(String code, long expireTime) {
			this.code = code;
			this.expireTime = expireTime;
		}

		boolean isExpired() {
			return System.currentTimeMillis() > expireTime;
		}
	}

	/**
	 * Purges expired captchas.
	 */
	private void cleanupExpiredData() {
		// Purge expired image captchas
		captchaCache.entrySet().removeIf(entry -> entry.getValue().isExpired());

		// Purge expired SMS captchas
		smsCache.entrySet().removeIf(entry -> entry.getValue().isExpired());

		log.debug("清理过期的验证码, captchaCache size={}, smsCache size={}", captchaCache.size(), smsCache.size());
	}

	public void saveCaptcha(String key, String code, long expireSeconds) {
		String cacheKey = AuthConsts.CAPTCHA_PREFIX + key;
		long expireTime = System.currentTimeMillis() + expireSeconds * 1000;
		captchaCache.put(cacheKey, new CaptchaInfo(code, expireTime));
		log.debug("保存图形验证码, key={}", key);
	}

	public boolean validateCaptcha(String key, String code) {
		if (!StringUtils.hasText(key) || !StringUtils.hasText(code)) {
			return false;
		}
		String cacheKey = AuthConsts.CAPTCHA_PREFIX + key;
		CaptchaInfo info = captchaCache.get(cacheKey);

		if (info == null || info.isExpired()) {
			return false;
		}

		if (code.equalsIgnoreCase(info.code)) {
			captchaCache.remove(cacheKey); // 校验成功删除，防止重复使用
			return true;
		}
		return false;
	}

	public void saveSmsCode(String phone, String code, long expireMinutes) {
		String cacheKey = AuthConsts.SMS_CODE_PREFIX + phone;
		long expireTime = System.currentTimeMillis() + expireMinutes * 60 * 1000;
		smsCache.put(cacheKey, new CaptchaInfo(code, expireTime));
		log.debug("保存短信验证码, phone={}", phone);
	}

	public boolean validateSmsCode(String phone, String code) {
		if (!StringUtils.hasText(phone) || !StringUtils.hasText(code)) {
			return false;
		}
		String cacheKey = AuthConsts.SMS_CODE_PREFIX + phone;
		CaptchaInfo info = smsCache.get(cacheKey);

		if (info == null || info.isExpired()) {
			return false;
		}

		if (code.equals(info.code)) {
			smsCache.remove(cacheKey); // 校验成功删除，防止重复使用
			return true;
		}
		return false;
	}
}
