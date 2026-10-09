package top.ruilink.inkwash.base.convert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import top.ruilink.inkwash.base.enums.BaseStatus;

/**
 * {@code BaseStatus} 查询参数绑定回归测试。
 *
 * <p>
 * 覆盖 6 个使用 {@code BaseStatus} 的查询 DTO（category / term / group / menu /
 * permission / role），确认管理端按整型 code 发送时能正确绑定。
 */
@DisplayName("查询参数 BaseStatus 转换器")
class StringToBaseStatusConverterTest {

	private final StringToBaseStatusConverter converter = new StringToBaseStatusConverter();

	@Test
	@DisplayName("应接受整型 code")
	void shouldBindNumericCode() {
		assertEquals(BaseStatus.ENABLE, converter.convert("1"));
		assertEquals(BaseStatus.DISABLE, converter.convert("0"));
	}

	@Test
	@DisplayName("应兼容枚举名，且大小写不敏感")
	void shouldBindEnumName() {
		assertEquals(BaseStatus.ENABLE, converter.convert("ENABLE"));
		assertEquals(BaseStatus.ENABLE, converter.convert("enable"));
	}

	@Test
	@DisplayName("空白输入应返回 null 表示不过滤")
	void shouldReturnNullForBlank() {
		assertNull(converter.convert(null));
		assertNull(converter.convert("  "));
	}

	@Test
	@DisplayName("无法识别的取值应抛 IllegalArgumentException")
	void shouldThrowForUnknownValue() {
		assertThrows(IllegalArgumentException.class, () -> converter.convert("7"));
	}
}