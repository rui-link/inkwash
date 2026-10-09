package top.ruilink.inkwash.cms.convert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import top.ruilink.inkwash.cms.enums.ArticleStatus;

/**
 * 查询参数枚举绑定回归测试。
 *
 * <p>
 * 缺陷背景：枚举通过 {@code @JsonValue} 以整型 code 序列化，前端因此按数字发送
 * {@code ?status=2}；而查询字符串绑定走 {@code Enum.valueOf(name)}， 只接受
 * {@code ?status=PENDING}，导致管理端按状态筛选静默失效。
 */
@DisplayName("查询参数 ArticleStatus 转换器")
class StringToArticleStatusConverterTest {

	private final StringToArticleStatusConverter converter = new StringToArticleStatusConverter();

	@Test
	@DisplayName("应接受整型 code —— 前端实际发送的形式")
	void shouldBindNumericCode() {
		assertEquals(ArticleStatus.DRAFT, converter.convert("1"));
		assertEquals(ArticleStatus.PENDING, converter.convert("2"));
		assertEquals(ArticleStatus.REJECTED, converter.convert("4"));
		assertEquals(ArticleStatus.APPROVED, converter.convert("3"));
		assertEquals(ArticleStatus.PUBLISHED, converter.convert("5"));
		assertEquals(ArticleStatus.RETRACTED, converter.convert("6"));
	}

	@Test
	@DisplayName("应兼容枚举名，且大小写不敏感")
	void shouldBindEnumName() {
		assertEquals(ArticleStatus.PENDING, converter.convert("PENDING"));
		assertEquals(ArticleStatus.PENDING, converter.convert("pending"));
		assertEquals(ArticleStatus.PUBLISHED, converter.convert("Published"));
	}

	@Test
	@DisplayName("空白输入应返回 null 表示不过滤")
	void shouldReturnNullForBlank() {
		assertNull(converter.convert(null));
		assertNull(converter.convert(""));
		assertNull(converter.convert("   "));
	}

	@Test
	@DisplayName("无法识别的取值应抛 IllegalArgumentException")
	void shouldThrowForUnknownValue() {
		assertThrows(IllegalArgumentException.class, () -> converter.convert("99"));
		assertThrows(IllegalArgumentException.class, () -> converter.convert("NOT_A_STATUS"));
	}
}