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

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import lombok.extern.slf4j.Slf4j;
import top.ruilink.inkwash.base.service.CacheService;

/**
 * Redis-backed cache service implementation, selected by the prod profile
 * setting {@code cache.type=redis}.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "cache.type", havingValue = "redis")
public class RedisCacheServiceImpl implements CacheService {

	private final StringRedisTemplate redisTemplate;
	private final JsonMapper objectMapper;

	public RedisCacheServiceImpl(StringRedisTemplate redisTemplate, JsonMapper objectMapper) {
		this.redisTemplate = redisTemplate;
		this.objectMapper = objectMapper;
	}

	@Override
	public void put(String key, Object value, Duration ttl) {
		try {
			String json = objectMapper.writeValueAsString(value);
			redisTemplate.opsForValue().set(key, json, ttl);
		} catch (JacksonException e) {
			log.error("缓存序列化失败, key={}", key, e);
			throw new IllegalStateException("缓存序列化失败, key=" + key, e);
		}
	}

	@Override
	public void put(String key, Object value) {
		try {
			String json = objectMapper.writeValueAsString(value);
			redisTemplate.opsForValue().set(key, json);
		} catch (JacksonException e) {
			log.error("缓存序列化失败, key={}", key, e);
			throw new IllegalStateException("缓存序列化失败, key=" + key, e);
		}
	}

	@Override
	public <T> T get(String key, Class<T> type) {
		String json = redisTemplate.opsForValue().get(key);
		if (json == null)
			return null;
		try {
			return objectMapper.readValue(json, type);
		} catch (JacksonException e) {
			log.error("缓存反序列化失败, key={}", key, e);
			return null;
		}
	}

	@Override
	public Object get(String key) {
		return redisTemplate.opsForValue().get(key);
	}

	@Override
	public void evict(String key) {
		redisTemplate.delete(key);
	}

	@Override
	public boolean hasKey(String key) {
		return Boolean.TRUE.equals(redisTemplate.hasKey(key));
	}

	@Override
	public void increment(String key, long delta) {
		redisTemplate.opsForValue().increment(key, delta);
	}

	@Override
	public Long getIncrement(String key) {
		String val = redisTemplate.opsForValue().get(key);
		if (val == null)
			return null;
		try {
			return Long.parseLong(val);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
