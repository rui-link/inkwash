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
package top.ruilink.inkwash.base.support.file.util;

import java.util.Map;
import java.util.Set;

import top.ruilink.inkwash.base.exception.BusinessException;

/**
 * Upload file declared MIME type versus extension validation helper.
 *
 * The MIME type is taken from the client-supplied {@code Content-Type} header
 * and is therefore not trustworthy on its own: it validates that a client
 * declares a consistent type, not that the bytes match. Unknown extensions and
 * blank MIME types are accepted without inspection.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public final class MimeTypeValidator {

	private MimeTypeValidator() {
	}

	private static final Map<String, Set<String>> EXTENSION_TO_MIME = Map.ofEntries(
			Map.entry("jpg", Set.of("image/jpeg")), Map.entry("jpeg", Set.of("image/jpeg")),
			Map.entry("png", Set.of("image/png")), Map.entry("gif", Set.of("image/gif")),
			Map.entry("bmp", Set.of("image/bmp", "image/x-ms-bmp")), Map.entry("webp", Set.of("image/webp")),
			Map.entry("pdf", Set.of("application/pdf")), Map.entry("doc", Set.of("application/msword")),
			Map.entry("docx", Set.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document")),
			Map.entry("xls", Set.of("application/vnd.ms-excel")),
			Map.entry("xlsx", Set.of("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")),
			Map.entry("txt", Set.of("text/plain")), Map.entry("zip", Set.of("application/zip")),
			Map.entry("rar", Set.of("application/vnd.rar", "application/x-rar-compressed")),
			Map.entry("mp3", Set.of("audio/mpeg", "audio/mp3")),
			Map.entry("wav", Set.of("audio/wav", "audio/x-wav", "audio/wave", "audio/vnd.wave")),
			Map.entry("aac", Set.of("audio/aac")), Map.entry("ogg", Set.of("audio/ogg", "application/ogg")),
			Map.entry("flac", Set.of("audio/flac", "audio/x-flac")),
			Map.entry("mp4", Set.of("video/mp4", "application/mp4")),
			Map.entry("avi", Set.of("video/x-msvideo", "video/avi")), Map.entry("mov", Set.of("video/quicktime")),
			Map.entry("mkv", Set.of("video/x-matroska", "video/matroska")), Map.entry("webm", Set.of("video/webm")));

	/**
	 * Reject an upload whose declared MIME type does not match its extension.
	 *
	 * @param extension        file extension, with or without a leading dot
	 * @param declaredMimeType MIME type declared by the client, may carry
	 *                         parameters such as {@code ; charset=UTF-8}
	 * @throws BusinessException if the declared type contradicts the extension
	 */
	public static void validate(String extension, String declaredMimeType) {
		if (extension == null || extension.isEmpty())
			return;
		String ext = extension.toLowerCase().replaceFirst("^\\.", "");

		Set<String> expected = EXTENSION_TO_MIME.get(ext);
		if (expected == null)
			return;
		if (declaredMimeType == null || declaredMimeType.isBlank())
			return;

		String actual = declaredMimeType.trim().toLowerCase();
		int parameterStart = actual.indexOf(';');
		if (parameterStart >= 0) {
			actual = actual.substring(0, parameterStart).trim();
		}

		if (expected.contains(actual))
			return;
		throw new BusinessException("error.file.type_mismatch");
	}
}
