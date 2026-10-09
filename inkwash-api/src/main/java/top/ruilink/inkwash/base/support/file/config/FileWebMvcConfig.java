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
package top.ruilink.inkwash.base.support.file.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import top.ruilink.inkwash.base.support.file.enums.FileFolderType;

/**
 * Static resource mapping for uploaded files under /uploads/**.
 * <p>
 * Design note (M-BS-11): uploaded files stay publicly readable because images
 * on published articles must render for anonymous visitors. File names are
 * unguessable UUIDs, so public access carries no enumeration risk, and writes
 * are already API-authenticated and allowlist-validated at upload time. This
 * mapping hardens the read path: only allowlisted extensions are served, and
 * path traversal, directory listing, NUL bytes and drive-letter prefixes are
 * rejected with 404.
 * 
 * @author Dyllon
 * @since 0.5.1
 */
@Configuration
public class FileWebMvcConfig implements WebMvcConfigurer {

	private final String uploadPath;

	public FileWebMvcConfig(@Value("${file.storage.local.upload-path:./uploads}") String uploadPath) {
		this.uploadPath = uploadPath;
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		String absolutePath = Paths.get(uploadPath).toAbsolutePath().normalize().toUri().toString();
		if (!absolutePath.endsWith("/")) {
			absolutePath += "/";
		}
		registry.addResourceHandler("/uploads/**").addResourceLocations(absolutePath).resourceChain(false)
				.addResolver(new UploadsPathResourceResolver());
	}

	/**
	 * Single-file resolver enforcing the path safety rules and extension allowlist,
	 * never listing directories.
	 */
	static final class UploadsPathResourceResolver extends PathResourceResolver {

		private static final Set<String> ALLOWED_EXTENSIONS = Arrays.stream(FileFolderType.values())
				.flatMap(folder -> folder.getAllowedExtensions().stream()).map(ext -> ext.toLowerCase(Locale.ROOT))
				.collect(Collectors.toUnmodifiableSet());

		@Override
		protected Resource getResource(String resourcePath, Resource location) throws IOException {
			if (!isAllowedPath(resourcePath)) {
				return null;
			}
			Resource resource = super.getResource(resourcePath, location);
			if (resource == null) {
				return null;
			}
			File file = resource.getFile();
			if (file.isDirectory()) {
				return null;
			}
			return resource;
		}

		private boolean isAllowedPath(String resourcePath) {
			String normalized = resourcePath.replace('\\', '/');
			if (normalized.indexOf('\0') >= 0) {
				return false;
			}
			for (String segment : normalized.split("/")) {
				if (segment.isEmpty()) {
					continue;
				}
				if (segment.equals(".") || segment.equals("..")) {
					return false;
				}
				if (segment.matches("[A-Za-z]:.*")) {
					return false;
				}
			}
			String filename = normalized.substring(normalized.lastIndexOf('/') + 1);
			int dot = filename.lastIndexOf('.');
			if (dot <= 0) {
				return false;
			}
			String extension = filename.substring(dot + 1).toLowerCase(Locale.ROOT);
			return ALLOWED_EXTENSIONS.contains(extension);
		}
	}
}
