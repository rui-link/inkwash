package top.ruilink.inkwash.system.convert;

import java.util.Locale;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import top.ruilink.inkwash.base.enums.BaseEnum;
import top.ruilink.inkwash.system.enums.UserStatus;

/**
 * 查询参数 {@code UserStatus} 转换器。
 *
 * <p>
 * 与 {@code StringToBaseStatusConverter} 同理：同时接受整型 code 与枚举名， 避免管理端按数字发送
 * {@code ?status=1} 时绑定失败。
 */
@Component
public class StringToUserStatusConverter implements Converter<String, UserStatus> {

	@Override
	public UserStatus convert(String source) {
		String value = source == null ? null : source.trim();
		if (value == null || value.isEmpty()) {
			return null;
		}
		try {
			return UserStatus.fromCode(Integer.parseInt(value));
		} catch (NumberFormatException ignored) {
			// 不是纯数字，按枚举名处理
		}
		return UserStatus.valueOf(value.toUpperCase(Locale.ROOT));
	}
}