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
package top.ruilink.inkwash.base.config;

import static top.ruilink.inkwash.base.CacheConsts.MENU_TREE_CACHE;
import static top.ruilink.inkwash.base.CacheConsts.ROLE_PERMISSIONS_CACHE;
import static top.ruilink.inkwash.base.CacheConsts.SENSITIVE_WORDS_CACHE;
import static top.ruilink.inkwash.base.CacheConsts.USER_PERMISSIONS_CACHE;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Application cache manager configuration.
 *
 * <p>
 * Lives in {@code base} because it is application-wide infrastructure with no
 * domain knowledge: it enables caching for every module and registers caches on
 * behalf of {@code system} ({@code userPermissions}, {@code rolePermissions},
 * {@code menuTree}) and {@code cms} ({@code sensitiveWords}). It previously sat
 * in {@code monitor/config}, where it registered nothing for its own module —
 * {@code monitor} legitimately owns cache <em>views</em> ({@code CacheView}),
 * not cache wiring.
 *
 * <p>
 * The per-cache names all come from
 * {@link top.ruilink.inkwash.base.CacheConsts} so the Caffeine and Redis
 * branches cannot drift apart: they are declared side by side in the same
 * block, and a name used as a bare literal in only one branch would make
 * {@code @Cacheable} resolve against a differently-named cache depending on
 * {@code cache.type}.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Configuration
@EnableCaching
public class CacheConfig {

	@Bean
	@ConditionalOnProperty(name = "cache.type", havingValue = "caffeine", matchIfMissing = true)
	public CacheManager caffeineCacheManager() {
		CaffeineCacheManager cacheManager = new CaffeineCacheManager();
		cacheManager.registerCustomCache(USER_PERMISSIONS_CACHE,
				Caffeine.newBuilder().maximumSize(1000).expireAfterWrite(15, TimeUnit.MINUTES).recordStats().build());
		cacheManager.registerCustomCache(ROLE_PERMISSIONS_CACHE,
				Caffeine.newBuilder().maximumSize(500).expireAfterWrite(30, TimeUnit.MINUTES).recordStats().build());
		cacheManager.registerCustomCache(MENU_TREE_CACHE,
				Caffeine.newBuilder().maximumSize(500).expireAfterWrite(30, TimeUnit.MINUTES).recordStats().build());
		// Registered explicitly (ISS-016): a dynamically created Caffeine cache has NO
		// expiry,
		// while Redis falls back to cacheDefaults (30 min). Registering both keeps the
		// effective TTL identical in development and production.
		cacheManager.registerCustomCache(SENSITIVE_WORDS_CACHE,
				Caffeine.newBuilder().maximumSize(500).expireAfterWrite(15, TimeUnit.MINUTES).recordStats().build());
		return cacheManager;
	}

	@Bean
	@Primary
	@ConditionalOnProperty(name = "cache.type", havingValue = "redis")
	public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
		RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(30))
				.serializeKeysWith(
						RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
				.serializeValuesWith(RedisSerializationContext.SerializationPair
						.fromSerializer(GenericJacksonJsonRedisSerializer.create(_ -> {
						})))
				.disableCachingNullValues();

		return RedisCacheManager.builder(connectionFactory).cacheDefaults(config)
				.withCacheConfiguration(USER_PERMISSIONS_CACHE,
						RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(15)))
				.withCacheConfiguration(ROLE_PERMISSIONS_CACHE,
						RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(30)))
				.withCacheConfiguration(MENU_TREE_CACHE, config.entryTtl(Duration.ofMinutes(30)))
				.withCacheConfiguration(SENSITIVE_WORDS_CACHE,
						RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(15)))
				.transactionAware().build();
	}
}
