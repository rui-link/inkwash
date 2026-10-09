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
package top.ruilink.inkwash.base.util;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

/**
 * Internationalized message resolution helper.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Component
public class LocaleUtil {
	// Static holder for the Spring MessageSource
	private static MessageSource messageSource;

	public LocaleUtil(MessageSource source) {
		LocaleUtil.messageSource = source;
	}

	// ===================== Public static methods =====================

	/**
	 * Returns the localised message for the current request locale, falling back to
	 * the key itself when no message is configured.
	 * 
	 * @param key the i18n message key
	 */
	public static String getValue(String key) {
		return getValue(key, new Object[] {}, key, LocaleContextHolder.getLocale());
	}

	/**
	 * Message with {0} and {1} placeholder arguments.
	 */
	public static String getValue(String key, Object... args) {
		return getValue(key, args, key, LocaleContextHolder.getLocale());
	}

	/**
	 * Message with a default value.
	 */
	public static String getValue(String key, String defaultValue) {
		return getValue(key, new Object[] {}, defaultValue, LocaleContextHolder.getLocale());
	}

	/**
	 * Message for an explicit locale.
	 */
	public static String getValue(String key, Locale locale) {
		return getValue(key, new Object[] {}, key, locale);
	}

	/**
	 * 
	 * @param key        the i18n message key
	 * @param defaultMsg value used when the key is missing
	 * @param locale     the requested locale
	 * @return the resolved i18n message
	 */
	public static String getValue(String key, Object[] args, String defaultMsg, Locale locale) {
		if (messageSource == null) {
			return defaultMsg;
		}
		try {
			return messageSource.getMessage(key, args, defaultMsg, locale);
		} catch (Exception e) {
			return defaultMsg;
		}
	}

	public String getLanguage() {
		return LocaleContextHolder.getLocale().getDisplayLanguage();
	}
}
