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
package top.ruilink.inkwash.security.service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Verification code store shared by SMS and email.
 *
 * <p>
 * Thread safe. Codes are consumed once and removed on success, and invalidated
 * once the error count reaches its limit, which prevents brute force attempts.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Slf4j
@Component
public class VerifyCodeStore {

	private static final int MAX_ATTEMPTS = 5;

	private final Map<String, Entry> store = new ConcurrentHashMap<>();

	public VerifyCodeStore() {
		startCleanupTask();
	}

	/**
	 * Stores a verification code that expires after expireMinutes minutes.
	 */
	public void save(String key, String code, long expireMinutes) {
		store.put(key, new Entry(code, LocalDateTime.now().plusMinutes(expireMinutes)));
	}

	/**
	 * Validates a verification code, consuming and removing it on success and
	 * removing it once the error count reaches its limit.
	 */
	public boolean verify(String key, String code) {
		if (key == null || code == null) {
			return false;
		}
		Entry entry = store.get(key);
		if (entry == null || entry.isExpired()) {
			store.remove(key);
			return false;
		}
		if (entry.code.equals(code)) {
			store.remove(key);
			return true;
		}
		store.computeIfPresent(key,
				(_, e) -> e.attempts + 1 >= MAX_ATTEMPTS ? null : new Entry(e.code, e.expireAt, e.attempts + 1));
		return false;
	}

	public void clear(String key) {
		store.remove(key);
	}

	private void startCleanupTask() {
		ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
			Thread thread = new Thread(r, "verification-code-cleanup");
			thread.setDaemon(true);
			return thread;
		});
		scheduler.scheduleAtFixedRate(() -> {
			try {
				store.entrySet().removeIf(e -> e.getValue().isExpired());
			} catch (Exception e) {
				log.warn("验证码清理任务执行失败", e);
			}
		}, 1, 1, TimeUnit.MINUTES);
	}

	private record Entry(String code, LocalDateTime expireAt, int attempts) {

		Entry(String code, LocalDateTime expireAt) {
			this(code, expireAt, 0);
		}

		boolean isExpired() {
			return expireAt.isBefore(LocalDateTime.now());
		}
	}
}
