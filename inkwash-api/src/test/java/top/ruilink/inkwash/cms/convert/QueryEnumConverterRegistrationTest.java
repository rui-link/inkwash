package top.ruilink.inkwash.cms.convert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.convert.ConversionService;

import top.ruilink.inkwash.base.enums.BaseStatus;
import top.ruilink.inkwash.cms.enums.ArticleStatus;
import top.ruilink.inkwash.system.enums.UserStatus;

/**
 * 查询参数枚举绑定的<b>端到端</b>验证。
 *
 * <p>
 * 缺陷背景：枚举通过 {@code @JsonValue} 以整型 code 序列化，前端因此按数字发送 {@code ?status=2}；而
 * Spring MVC 默认的 {@code StringToEnumConverterFactory} 走
 * {@code Enum.valueOf(name)}，报 {@code No enum constant ...ArticleStatus.2}，
 * 导致管理端按状态筛选静默失效。
 *
 * <p>
 * 只测转换器类不足以证明缺陷已修复——真正决定行为的是 MVC 的 {@code ConversionService}
 * 是否真的用上了它，因此本测试启动完整上下文， 经 {@code mvcConversionService} 断言真实链路。
 *
 * <p>
 * 注意：不要用 {@code canConvert(String, XxxStatus.class)} 断言注册情况。 Spring 默认就注册了
 * {@code StringToEnumConverterFactory}，该断言即使在修复前 也为 true，属于无效断言。判据只能是「整型 code
 * 能否转换成功」。
 */
@SpringBootTest
@DisplayName("MVC 转换服务中的查询参数枚举")
class QueryEnumConverterRegistrationTest {

	@Autowired
	@Qualifier("mvcConversionService")
	private ConversionService conversionService;

	@Test
	@DisplayName("整型 code 应能绑定 —— ISS-008 的核心回归")
	void shouldConvertNumericCode() {
		assertEquals(ArticleStatus.PENDING, conversionService.convert("2", ArticleStatus.class));
		assertEquals(BaseStatus.ENABLE, conversionService.convert("1", BaseStatus.class));
		assertEquals(UserStatus.ENABLE, conversionService.convert("1", UserStatus.class));
	}

	@Test
	@DisplayName("code 为 0 的状态不应被当作空值")
	void shouldConvertZeroCode() {
		assertEquals(BaseStatus.DISABLE, conversionService.convert("0", BaseStatus.class));
		assertEquals(UserStatus.DISABLE, conversionService.convert("0", UserStatus.class));
	}

	@Test
	@DisplayName("枚举名应继续可用 —— 防止修复收窄既有能力")
	void shouldNotBreakEnumNameBinding() {
		assertEquals(ArticleStatus.PUBLISHED, conversionService.convert("PUBLISHED", ArticleStatus.class));
		assertEquals(BaseStatus.DISABLE, conversionService.convert("DISABLE", BaseStatus.class));
		assertEquals(UserStatus.LOCKED, conversionService.convert("LOCKED", UserStatus.class));
	}

	@Test
	@DisplayName("非法取值应失败而非静默降级为不过滤")
	void shouldFailForUnknownValue() {
		assertThrows(Exception.class, () -> conversionService.convert("99", ArticleStatus.class));
	}
}