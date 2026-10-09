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
package top.ruilink.inkwash.base.support.file.enums;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import top.ruilink.inkwash.base.enums.BaseEnum;

/**
 * Upload folder type enumeration defining allowed extensions and size limits.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public enum FileFolderType implements BaseEnum {

	PUBLIC(1, "公共文件", "public",
			Set.of("jpg", "jpeg", "png", "gif", "bmp", "webp", "doc", "docx", "xls", "xlsx", "pdf", "txt", "zip",
					"rar"),
			10 * 1024 * 1024),
	AVATAR(2, "头像", "avatar", Set.of("jpg", "jpeg", "png", "gif", "webp"), 2 * 1024 * 1024),
	ARTICLE(3, "文章附件", "media",
			Set.of("jpg", "jpeg", "png", "gif", "webp", "mp3", "wav", "aac", "ogg", "flac", "mp4", "avi", "mov", "mkv",
					"webm", "pdf"),
			50 * 1024 * 1024),
	NOTICE(4, "公告附件", "notice", Set.of("jpg", "png", "pdf", "doc", "docx"), 10 * 1024 * 1024),
	COVER(5, "文章封面", "cover", Set.of("jpg", "jpeg", "png", "gif", "webp"), 10 * 1024 * 1024);

	private final int code;
	private final String name;
	private final String folderName;
	private final Set<String> allowedExtensions;
	private final long maxFileSize;

	FileFolderType(int code, String name, String folderName, Set<String> allowedExtensions, long maxFileSize) {
		this.code = code;
		this.name = name;
		this.folderName = folderName;
		this.allowedExtensions = allowedExtensions;
		this.maxFileSize = maxFileSize;
	}

	@Override
	public int getCode() {
		return code;
	}

	public String getName() {
		return name;
	}

	public String getFolderName() {
		return folderName;
	}

	public Set<String> getAllowedExtensions() {
		return allowedExtensions;
	}

	public long getMaxFileSize() {
		return maxFileSize;
	}

	public boolean isExtensionAllowed(String ext) {
		return allowedExtensions.stream().anyMatch(e -> e.equalsIgnoreCase(ext));
	}

	@JsonValue
	public int getJsonValue() {
		return code;
	}

	@JsonCreator
	public static FileFolderType fromCode(int code) {
		return BaseEnum.fromCode(FileFolderType.class, code);
	}
}
