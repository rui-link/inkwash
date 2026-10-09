package top.ruilink.inkwash.base.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Caffeine cache service expiry and lookup unit tests.
 *
 * @author Dyllon
 * @since 0.5.1
 */
class CaffeineCacheServiceImplTest {

	private CaffeineCacheManager cacheManager;
	private CaffeineCacheServiceImpl service;

	@BeforeEach
	void setUp() {
		cacheManager = new CaffeineCacheManager();
		cacheManager.registerCustomCache("general", Caffeine.newBuilder().maximumSize(10000).recordStats().build());
		cacheManager.registerCustomCache("generalTtl", Caffeine.newBuilder().maximumSize(10000).recordStats().build());
		service = new CaffeineCacheServiceImpl(cacheManager);
	}

	@Test
	void hasKey_OnPlainCache_WhenPresent_ReturnsTrue() {
		service.put("plain-key", "value");

		assertTrue(service.hasKey("plain-key"));
	}

	@Test
	void hasKey_OnTtlCache_WhenPresent_ReturnsTrue() {
		service.put("ttl-key", "value", Duration.ofMinutes(1));

		assertTrue(service.hasKey("ttl-key"));
	}

	@Test
	void hasKey_WhenAbsent_ReturnsFalse() {
		assertFalse(service.hasKey("missing-key"));
	}

	@Test
	void hasKey_ChecksWithoutMutatingOrCreatingEntries() {
		service.put("ttl-key", "value", Duration.ofMinutes(1));

		assertTrue(service.hasKey("ttl-key"));
		assertFalse(service.hasKey("missing-key"));
		assertEquals(0, nativeCacheSize("general"));
		assertEquals(1, nativeCacheSize("generalTtl"));
		assertEquals("value", service.get("ttl-key"));
	}

	@Test
	void putWithTtl_valueExpiresAfterTtl() throws Exception {
		service.put("expiring-key", "value", Duration.ofMillis(200));

		assertTrue(service.hasKey("expiring-key"));
		assertEquals("value", service.get("expiring-key"));

		Thread.sleep(700);

		assertFalse(service.hasKey("expiring-key"));
		assertNull(service.get("expiring-key"));
	}

	@Test
	void putWithTtl_physicallyEvictsEntryAfterTtl() throws Exception {
		service.put("expiring-key", "value", Duration.ofMillis(200));

		awaitSize("generalTtl", 1, 1000);
		awaitSize("generalTtl", 0, 3000);
	}

	@Test
	void putWithoutTtl_usesGeneralCacheWideDefault() throws Exception {
		service.put("plain-key", "value");

		Thread.sleep(300);

		assertTrue(service.hasKey("plain-key"));
		assertEquals("value", service.get("plain-key"));
		assertEquals(1, nativeCacheSize("general"));
		assertEquals(0, nativeCacheSize("generalTtl"));
	}

	@Test
	void putWithNullTtl_appliesCacheWideDefaultTtl() {
		service.put("default-key", "value", null);

		Instant deadline = service.deadlineOf("default-key");
		assertNotNull(deadline);
		long remainingMs = Duration.between(Instant.now(), deadline).toMillis();
		assertTrue(remainingMs <= CaffeineCacheServiceImpl.DEFAULT_TTL.toMillis(),
				"remaining " + remainingMs + "ms must not exceed cache default");
		assertTrue(remainingMs > CaffeineCacheServiceImpl.DEFAULT_TTL.toMillis() - 1000,
				"remaining " + remainingMs + "ms must stay close to cache default");
	}

	@Test
	void putWithLongerTtl_supersedesEarlierShorterTtl() throws Exception {
		service.put("key", "value", Duration.ofMillis(100));
		service.put("key", "value2", Duration.ofMinutes(5));

		Thread.sleep(400);

		assertTrue(service.hasKey("key"));
		assertEquals("value2", service.get("key"));
	}

	@Test
	void increment_accumulatesAcrossCalls() {
		service.increment("counter", 2L);
		service.increment("counter", 3L);

		assertEquals(5L, service.getIncrement("counter"));
	}

	@Test
	void increment_addsToExistingIntegerValue() {
		service.put("counter", 7);

		service.increment("counter", 5L);

		assertEquals(12L, service.getIncrement("counter"));
	}

	@Test
	void increment_replacesNonNumericValue() {
		service.put("counter", "not-a-number");

		service.increment("counter", 4L);

		assertEquals(4L, service.getIncrement("counter"));
	}

	private void awaitSize(String cacheName, long expected, long timeoutMillis) throws InterruptedException {
		long deadline = System.currentTimeMillis() + timeoutMillis;
		while (nativeCacheSize(cacheName) != expected && System.currentTimeMillis() < deadline) {
			Thread.sleep(20);
		}
		assertEquals(expected, nativeCacheSize(cacheName));
	}

	private long nativeCacheSize(String cacheName) {
		com.github.benmanes.caffeine.cache.Cache<?, ?> cache = (com.github.benmanes.caffeine.cache.Cache<?, ?>) cacheManager
				.getCache(cacheName).getNativeCache();
		return cache.estimatedSize();
	}
}