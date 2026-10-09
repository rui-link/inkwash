package top.ruilink.inkwash.system.convert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import top.ruilink.inkwash.system.enums.UserStatus;

/**
 * {@code UserStatus} 查询参数绑定回归测试。
 *
 * <p>
 * 与 {@code StringToBaseStatusConverterTest} 同理，覆盖用户管理端按整型 code 发送
 * {@code ?status=1} 的场景。
 */
@DisplayName("查询参数 UserStatus 转换器")
class StringToUserStatusConverterTest {

	private final StringToUserStatusConverter converter = new StringToUserStatusConverter();

	@Test
	@DisplayName("应接受整型 code —— 含 code 为 0 的 DISABLE")
	void shouldBindNumericCode() {
		assertEquals(UserStatus.DISABLE, converter.convert("0"));
		assertEquals(UserStatus.ENABLE, converter.convert("1"));
		assertEquals(UserStatus.LOCKED, converter.convert("2"));
		assertEquals(UserStatus.PENDING, converter.convert("4"));
	}

	@Test
	@DisplayName("应兼容枚举名，且大小写不敏感")
	void shouldBindEnumName() {
		assertEquals(UserStatus.LOCKED, converter.convert("LOCKED"));
		assertEquals(UserStatus.LOCKED, converter.convert("locked"));
	}

	@Test
	@DisplayName("空白输入应返回 null 表示不过滤")
	void shouldReturnNullForBlank() {
		assertNull(converter.convert(null));
		assertNull(converter.convert(""));
		assertNull(converter.convert("  "));
	}

	@Test
	@DisplayName("无法识别的取值应抛 IllegalArgumentException")
	void shouldThrowForUnknownValue() {
		assertThrows(IllegalArgumentException.class, () -> converter.convert("99"));
	}
}