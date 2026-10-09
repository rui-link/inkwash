package top.ruilink.inkwash.base.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static top.ruilink.inkwash.base.CacheConsts.MENU_TREE_CACHE;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.transaction.TransactionAwareCacheDecorator;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import top.ruilink.inkwash.system.api.view.MenuTreeView;

/**
 * Cache configuration tests verifying exclusive backend selection and menu tree
 * serialization.
 *
 * @author Dyllon
 * @since 0.5.1
 */
class CacheConfigTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withUserConfiguration(CacheConfig.class);

	@Test
	void caffeineIsTheOnlyBackendWhenCacheTypeIsCaffeine() {
		runner.withPropertyValues("cache.type=caffeine").run(context -> {
			assertEquals(CaffeineCacheManager.class, context.getBean(CacheManager.class).getClass());
			assertThrows(NoSuchBeanDefinitionException.class, () -> context.getBean("redisCacheManager"));
		});
	}

	@Test
	void caffeineIsTheOnlyBackendWhenCacheTypeIsMissing() {
		runner.run(context -> {
			assertEquals(CaffeineCacheManager.class, context.getBean(CacheManager.class).getClass());
			assertThrows(NoSuchBeanDefinitionException.class, () -> context.getBean("redisCacheManager"));
		});
	}

	@Test
	void redisIsTheOnlyPrimaryBackendWhenCacheTypeIsRedis() {
		runner.withBean(RedisConnectionFactory.class, () -> mock(RedisConnectionFactory.class))
				.withPropertyValues("cache.type=redis").run(context -> {
					assertEquals(RedisCacheManager.class, context.getBean(CacheManager.class).getClass());
					assertThrows(NoSuchBeanDefinitionException.class, () -> context.getBean("caffeineCacheManager"));
					Object definition = context.getBeanFactory().getBeanDefinition("redisCacheManager");
					assertTrue(((AbstractBeanDefinition) definition).isPrimary(), "redisCacheManager must be @Primary");
				});
	}

	@Test
	void redisMenuTreeCacheCanSerializeMenuTreeView() {
		runner.withBean(RedisConnectionFactory.class, () -> mock(RedisConnectionFactory.class))
				.withPropertyValues("cache.type=redis").run(context -> {
					RedisCacheManager manager = context.getBean(RedisCacheManager.class);
					RedisCacheConfiguration menuTreeConfig = redisCacheConfig(manager, MENU_TREE_CACHE);
					assertTrue(menuTreeConfig != null, "menuTree cache must be registered on redis cache manager");
					assertDoesNotThrow(
							() -> menuTreeConfig.getValueSerializationPair().getWriter().write(new MenuTreeView()),
							"menuTree cache must not use JDK serialization (MenuTreeView is not Serializable)");
				});
	}

	private static RedisCacheConfiguration redisCacheConfig(RedisCacheManager manager, String name) {
		Cache cache = manager.getCache(name);
		if (cache instanceof TransactionAwareCacheDecorator decorator) {
			cache = decorator.getTargetCache();
		}
		if (cache instanceof RedisCache redisCache) {
			return redisCache.getCacheConfiguration();
		}
		return null;
	}
}