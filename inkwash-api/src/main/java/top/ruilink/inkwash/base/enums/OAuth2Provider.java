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
package top.ruilink.inkwash.base.enums;

/**
 * Third-party OAuth2 provider enumeration.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public enum OAuth2Provider implements BaseEnum {
	ALIPAY(1, "支付宝"), WECHAT(2, "微信"), GITHUB(3, "Github"), GOOGLE(4, "谷歌"), APPLE(5, "苹果");

	private final int code;
	private final String name;

	OAuth2Provider(int code, String name) {
		this.code = code;
		this.name = name;
	}

	@Override
	public int getCode() {
		return code;
	}

	public String getName() {
		return name;
	}

	public static OAuth2Provider fromCode(int code) {
		return BaseEnum.fromCode(OAuth2Provider.class, code);
	}
}
