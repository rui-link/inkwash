package top.ruilink.inkwash.base.convert;

import java.util.Locale;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import top.ruilink.inkwash.base.enums.BaseStatus;

/**
 * 查询参数 {@code BaseStatus} 转换器。
 *
 * <p>
 * 枚举在 JSON 上以整型 code 序列化（{@code @JsonValue}），前端因此按数字发送
 * {@code ?status=1}。但查询字符串绑定走的是 {@code StringToEnumConverterFactory}， 即
 * {@code Enum.valueOf(name)}，只接受 {@code ?status=ENABLE}。两者不一致会导致 管理端的状态筛选静默失效。
 *
 * <p>
 * 本转换器同时接受 code 与枚举名，使两种形式都可用。
 */
@Component
public class StringToBaseStatusConverter implements Converter<String, BaseStatus> {

	@Override
	public BaseStatus convert(String source) {
		String value = source == null ? null : source.trim();
		if (value == null || value.isEmpty()) {
			return null;
		}
		try {
			return BaseStatus.fromCode(Integer.parseInt(value));
		} catch (NumberFormatException ignored) {
			// 不是纯数字，按枚举名处理
		}
		return BaseStatus.valueOf(value.toUpperCase(Locale.ROOT));
	}
}