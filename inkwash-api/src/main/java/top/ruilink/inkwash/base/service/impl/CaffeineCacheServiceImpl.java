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
package top.ruilink.inkwash.base.service.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;

import lombok.extern.slf4j.Slf4j;
import top.ruilink.inkwash.base.service.CacheService;

/**
 * In-memory Caffeine cache service implementation, selected by the dev profile
 * setting {@code cache.type=caffeine} and used by default when unset.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "cache.type", havingValue = "caffeine", matchIfMissing = true)
public class CaffeineCacheServiceImpl implements CacheService {

	private static final String GENERAL_CACHE = "general";

	private static final String GENERAL_TTL_CACHE = "generalTtl";

	static final Duration DEFAULT_TTL = Duration.ofMinutes(10);

	private final CacheManager cacheManager;

	private final ConcurrentMap<String, TtlEntry> ttlEntries = new ConcurrentHashMap<>();

	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
		Thread thread = new Thread(r, "caffeine-cache-ttl-applier");
		thread.setDaemon(true);
		return thread;
	});

	public CaffeineCacheServiceImpl(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}

	private record TtlEntry(long epoch, Instant deadline, ScheduledFuture<?> future) {
	}

	@Override
	public void put(String key, Object value, Duration ttl) {
		getTtlCache().put(key, value);
		scheduleEviction(key, normalizeTtl(ttl));
	}

	@Override
	public void put(String key, Object value) {
		cancelTtlEntry(key);
		getCache().put(key, value);
	}

	private static Duration normalizeTtl(Duration ttl) {
		if (ttl == null || ttl.isZero() || ttl.isNegative()) {
			return DEFAULT_TTL;
		}
		return ttl;
	}

	private void scheduleEviction(String key, Duration ttl) {
		ttlEntries.compute(key, (k, existing) -> {
			long epoch = (existing == null) ? 1L : existing.epoch() + 1;
			if (existing != null) {
				existing.future().cancel(false);
			}
			ScheduledFuture<?> future = scheduler.schedule(() -> evictIfEpochCurrent(key, epoch), ttl.toNanos(),
					TimeUnit.NANOSECONDS);
			return new TtlEntry(epoch, Instant.now().plus(ttl), future);
		});
	}

	private void evictIfEpochCurrent(String key, long epoch) {
		TtlEntry current = ttlEntries.get(key);
		if (current != null && current.epoch() == epoch) {
			evict(key);
		}
	}

	private void cancelTtlEntry(String key) {
		TtlEntry previous = ttlEntries.remove(key);
		if (previous != null) {
			previous.future().cancel(false);
		}
		Cache ttlCache = cacheManager.getCache(GENERAL_TTL_CACHE);
		if (ttlCache != null) {
			ttlCache.evict(key);
		}
	}

	@Override
	public <T> T get(String key, Class<T> type) {
		Object value = doGet(key);
		if (value != null && type.isInstance(value)) {
			return type.cast(value);
		}
		return null;
	}

	@Override
	public Object get(String key) {
		return doGet(key);
	}

	private Object doGet(String key) {
		Cache.ValueWrapper wrapper = getCache().get(key);
		if (wrapper != null) {
			return wrapper.get();
		}
		if (isTtlEntryLive(key)) {
			wrapper = getTtlCache().get(key);
			if (wrapper != null) {
				return wrapper.get();
			}
		}
		return null;
	}

	private boolean isTtlEntryLive(String key) {
		TtlEntry entry = ttlEntries.get(key);
		return entry != null && entry.deadline().isAfter(Instant.now());
	}

	@Override
	public void evict(String key) {
		TtlEntry previous = ttlEntries.remove(key);
		if (previous != null) {
			previous.future().cancel(false);
		}
		getCache().evict(key);
		Cache ttlCache = cacheManager.getCache(GENERAL_TTL_CACHE);
		if (ttlCache != null) {
			ttlCache.evict(key);
		}
	}

	@Override
	public boolean hasKey(String key) {
		if (existsIn(getCache(), key)) {
			return true;
		}
		return isTtlEntryLive(key) && existsIn(getTtlCache(), key);
	}

	private boolean existsIn(Cache cache, String key) {
		Object nativeCache = cache.getNativeCache();
		if (nativeCache instanceof com.github.benmanes.caffeine.cache.Cache) {
			@SuppressWarnings("unchecked")
			com.github.benmanes.caffeine.cache.Cache<String, Object> caffeineCache = (com.github.benmanes.caffeine.cache.Cache<String, Object>) nativeCache;
			return caffeineCache.getIfPresent(key) != null;
		}
		return cache.get(key) != null;
	}

	private Cache getCache() {
		Cache cache = cacheManager.getCache(GENERAL_CACHE);
		if (cache == null) {
			throw new IllegalStateException("Cache '" + GENERAL_CACHE + "' not found");
		}
		return cache;
	}

	private Cache getTtlCache() {
		Cache cache = cacheManager.getCache(GENERAL_TTL_CACHE);
		if (cache == null) {
			throw new IllegalStateException("Cache '" + GENERAL_TTL_CACHE + "' not found");
		}
		return cache;
	}

	@Override
	public void increment(String key, long delta) {
		nativeConcurrentMap(getCache()).compute(key, (_, existing) -> {
			if (existing == null)
				return delta;
			if (existing instanceof Long l)
				return l + delta;
			if (existing instanceof Integer i)
				return i + (int) delta;
			return delta;
		});
	}

	/**
	 * Returns a {@link ConcurrentMap} view of a cache, for atomic read-modify-write
	 * operations.
	 *
	 * <p>
	 * {@code getNativeCache()} must never be cast straight to
	 * {@code ConcurrentMap}: under Caffeine it returns a {@code BoundedLocalCache},
	 * which is not a {@code ConcurrentMap} and fails at runtime with a
	 * {@link ClassCastException}. Only the {@code asMap()} view is a real
	 * {@code ConcurrentMap}. Other providers (Redis in prod) already expose one.
	 */
	private ConcurrentMap<String, Object> nativeConcurrentMap(Cache cache) {
		Object nativeCache = cache.getNativeCache();
		if (nativeCache instanceof com.github.benmanes.caffeine.cache.Cache<?, ?> caffeine) {
			ConcurrentMap<?, ?> asMap = caffeine.asMap();
			@SuppressWarnings("unchecked")
			ConcurrentMap<String, Object> typed = (ConcurrentMap<String, Object>) asMap;
			return typed;
		}
		@SuppressWarnings("unchecked")
		ConcurrentMap<String, Object> map = (ConcurrentMap<String, Object>) nativeCache;
		return map;
	}

	@Override
	public Long getIncrement(String key) {
		// Cache.get(String) yields a ValueWrapper, not the value; unwrap before the
		// type checks.
		Cache.ValueWrapper wrapper = getCache().get(key);
		if (wrapper == null) {
			return null;
		}
		Object value = wrapper.get();
		if (value instanceof Long l)
			return l;
		if (value instanceof Integer i)
			return i.longValue();
		return null;
	}

	Instant deadlineOf(String key) {
		TtlEntry entry = ttlEntries.get(key);
		return entry == null ? null : entry.deadline();
	}

	@PreDestroy
	public void shutdown() {
		scheduler.shutdownNow();
	}
}
