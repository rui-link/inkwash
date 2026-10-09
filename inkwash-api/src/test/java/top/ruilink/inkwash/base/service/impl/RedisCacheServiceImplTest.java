package top.ruilink.inkwash.base.service.impl;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Redis cache service serialization unit tests.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@ExtendWith(MockitoExtension.class)
class RedisCacheServiceImplTest {

	@Mock
	private StringRedisTemplate redisTemplate;
	@Mock
	private JsonMapper objectMapper;
	@Mock
	private ValueOperations<String, String> ops;

	private RedisCacheServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new RedisCacheServiceImpl(redisTemplate, objectMapper);
	}

	private static JacksonException serializeFailure() {
		return JacksonException.wrapWithPath(new IllegalStateException("boom"), "value", "value");
	}

	@Test
	@DisplayName("序列化失败时 put 抛异常，不静默丢弃")
	void put_serializationFailure_throws() {
		when(objectMapper.writeValueAsString(any())).thenThrow(serializeFailure());

		assertThrows(IllegalStateException.class, () -> service.put("k", "v", Duration.ofSeconds(10)));
		verify(redisTemplate, never()).opsForValue();
	}

	@Test
	@DisplayName("无 TTL 的 put 序列化失败同样抛异常")
	void putNoTtl_serializationFailure_throws() {
		when(objectMapper.writeValueAsString(any())).thenThrow(serializeFailure());

		assertThrows(IllegalStateException.class, () -> service.put("k", "v"));
	}

	@Test
	@DisplayName("put 成功写入 Redis")
	void put_success_writesWithTtl() {
		when(objectMapper.writeValueAsString("v")).thenReturn("\"v\"");
		when(redisTemplate.opsForValue()).thenReturn(ops);

		service.put("k", "v", Duration.ofSeconds(10));

		verify(ops).set("k", "\"v\"", Duration.ofSeconds(10));
	}

	@Test
	@DisplayName("反序列化失败时读取返回 null（读缺失只记录日志）")
	void get_deserializationFailure_returnsNull() {
		when(redisTemplate.opsForValue()).thenReturn(ops);
		when(ops.get("k")).thenReturn("{bad json");
		when(objectMapper.readValue("{bad json", String.class)).thenThrow(serializeFailure());

		assertNull(service.get("k", String.class));
	}
}
