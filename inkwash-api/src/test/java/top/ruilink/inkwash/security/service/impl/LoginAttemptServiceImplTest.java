package top.ruilink.inkwash.security.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import top.ruilink.inkwash.base.service.CacheService;
import top.ruilink.inkwash.security.config.CredentialConfig;

/**
 * Login failure counting, account lockout, and concurrent attempt unit tests.
 *
 * @author Dyllon
 * @since 0.5.1
 */
class LoginAttemptServiceImplTest {

	private static final int MAX_ERROR_TIMES = 3;
	private static final int LOCK_MINUTES = 10;

	private CredentialConfig credentialConfig;
	private FakeCacheService cacheService;
	private LoginAttemptServiceImpl loginAttemptService;

	@BeforeEach
	void setUp() {
		credentialConfig = new CredentialConfig();
		credentialConfig.getPassword().setMaxErrorTimes(MAX_ERROR_TIMES);
		credentialConfig.getPassword().setLockMinutes(LOCK_MINUTES);
		cacheService = new FakeCacheService();
		loginAttemptService = new LoginAttemptServiceImpl(credentialConfig, cacheService);
	}

	@Test
	@DisplayName("达到最大错误次数后锁定账号")
	void recordLoginFailure_ReachesMax_LocksAccount() {
		loginAttemptService.recordLoginFailure("alice");
		assertFalse(loginAttemptService.isLocked("alice"));
		assertEquals(MAX_ERROR_TIMES - 1, loginAttemptService.getRemainingAttempts("alice"));

		loginAttemptService.recordLoginFailure("alice");
		assertFalse(loginAttemptService.isLocked("alice"));
		assertEquals(MAX_ERROR_TIMES - 2, loginAttemptService.getRemainingAttempts("alice"));

		loginAttemptService.recordLoginFailure("alice");

		assertTrue(loginAttemptService.isLocked("alice"));
		assertEquals(-1, loginAttemptService.getRemainingAttempts("alice"));
		assertNotNull(loginAttemptService.getLockoutExpiration("alice"));
	}

	@Test
	@DisplayName("登录成功清除失败记录")
	void recordLoginSuccess_ClearsFailures() {
		loginAttemptService.recordLoginFailure("alice");

		loginAttemptService.recordLoginSuccess("alice");

		assertFalse(loginAttemptService.isLocked("alice"));
		assertEquals(MAX_ERROR_TIMES, loginAttemptService.getRemainingAttempts("alice"));
	}

	@Test
	@DisplayName("unlock 清除锁定和失败记录")
	void unlock_ClearsLockAndAttempts() {
		loginAttemptService.recordLoginFailure("alice");
		loginAttemptService.recordLoginFailure("alice");
		loginAttemptService.recordLoginFailure("alice");
		assertTrue(loginAttemptService.isLocked("alice"));

		loginAttemptService.unlock("alice");

		assertFalse(loginAttemptService.isLocked("alice"));
		assertEquals(MAX_ERROR_TIMES, loginAttemptService.getRemainingAttempts("alice"));
		assertNull(loginAttemptService.getLockoutExpiration("alice"));
	}

	@Test
	@DisplayName("并发登录失败不会绕过锁定")
	void concurrentFailures_DoNotBypassLock() throws Exception {
		int threadCount = 20;
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch ready = new CountDownLatch(threadCount);
		CountDownLatch start = new CountDownLatch(1);

		for (int i = 0; i < threadCount; i++) {
			executor.submit(() -> {
				ready.countDown();
				try {
					start.await();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				loginAttemptService.recordLoginFailure("alice");
			});
		}
		ready.await();
		start.countDown();
		executor.shutdown();
		assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

		assertTrue(loginAttemptService.isLocked("alice"));
		assertEquals(-1, loginAttemptService.getRemainingAttempts("alice"));
	}

	/**
	 * CacheService stub backed by an in-memory Map, whose increment uses atomic
	 * operations to mimic Caffeine and Redis behaviour
	 */
	private static final class FakeCacheService implements CacheService {

		private final Map<String, Object> store = new ConcurrentHashMap<>();
		private final Map<String, Long> counters = new ConcurrentHashMap<>();

		@Override
		public void put(String key, Object value, Duration ttl) {
			store.put(key, value);
		}

		@Override
		public void put(String key, Object value) {
			store.put(key, value);
		}

		@Override
		public <T> T get(String key, Class<T> type) {
			Object value = store.get(key);
			return value == null ? null : type.cast(value);
		}

		@Override
		public Object get(String key) {
			return store.get(key);
		}

		@Override
		public void evict(String key) {
			store.remove(key);
			counters.remove(key);
		}

		@Override
		public boolean hasKey(String key) {
			return store.containsKey(key);
		}

		@Override
		public void increment(String key, long delta) {
			counters.compute(key, (k, v) -> v == null ? delta : v + delta);
		}

		@Override
		public Long getIncrement(String key) {
			return counters.get(key);
		}
	}
}