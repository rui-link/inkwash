package top.ruilink.inkwash.cms.convert;

import java.util.Locale;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import top.ruilink.inkwash.cms.enums.ArticleStatus;

/**
 * 查询参数 {@code ArticleStatus} 转换器。
 *
 * <p>
 * 与 {@code StringToBaseStatusConverter} 同理：枚举以整型 code 序列化，
 * 而查询字符串默认按枚举名绑定。此处同时接受 code 与枚举名， 使管理端的 {@code ?status=2} 与
 * {@code ?status=PENDING} 都能正确绑定。
 */
@Component
public class StringToArticleStatusConverter implements Converter<String, ArticleStatus> {

	@Override
	public ArticleStatus convert(String source) {
		String value = source == null ? null : source.trim();
		if (value == null || value.isEmpty()) {
			return null;
		}
		try {
			return ArticleStatus.fromCode(Integer.parseInt(value));
		} catch (NumberFormatException ignored) {
			// 不是纯数字，按枚举名处理
		}
		return ArticleStatus.valueOf(value.toUpperCase(Locale.ROOT));
	}
}