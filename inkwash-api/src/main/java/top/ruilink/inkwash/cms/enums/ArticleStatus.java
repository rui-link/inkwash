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
package top.ruilink.inkwash.cms.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import top.ruilink.inkwash.base.enums.BaseEnum;

/**
 * Article status enumeration.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public enum ArticleStatus implements BaseEnum {
	DRAFT(1, "草稿"), PENDING(2, "待审核"), APPROVED(3, "通过"), REJECTED(4, "驳回"), PUBLISHED(5, "已发布"), RETRACTED(6, "已撤回");

	private final int code;
	private final String name;

	ArticleStatus(int code, String name) {
		this.code = code;
		this.name = name;
	}

	@JsonValue
	public int getCode() {
		return code;
	}

	public String getName() {
		return name;
	}

	@JsonCreator
	public static ArticleStatus fromCode(int code) {
		// Delegates to BaseEnum.fromCode (ISS-038) so every enum reports an unknown
		// code the
		// same way: "无效的枚举编码：<code>，类型：<EnumName>". The previous hand-rolled loop
		// threw "Invalid code: <code>" with no type, which made an unknown status
		// impossible
		// to trace back to its source.
		return BaseEnum.fromCode(ArticleStatus.class, code);
	}
}
