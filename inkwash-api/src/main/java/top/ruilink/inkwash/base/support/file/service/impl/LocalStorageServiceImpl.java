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
package top.ruilink.inkwash.base.support.file.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import top.ruilink.inkwash.base.support.file.service.FileStorageService;

/**
 * Local filesystem file storage service implementation.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Slf4j
@ConditionalOnProperty(name = "file.storage.mode", havingValue = "local", matchIfMissing = true)
@Service
public class LocalStorageServiceImpl implements FileStorageService {

	@Value("${file.storage.local.upload-path:./uploads}")
	private String uploadPath;

	@Value("${file.storage.local.url-prefix:}")
	private String urlPrefix;

	@PostConstruct
	public void init() {
		if (urlPrefix.isEmpty()) {
			urlPrefix = "/uploads/";
		}
		Path dir = Paths.get(uploadPath);
		if (!Files.exists(dir)) {
			try {
				Files.createDirectories(dir);
				log.info("文件上传目录已创建: {}", dir.toAbsolutePath());
			} catch (IOException e) {
				log.error("创建文件上传目录失败: {}", e.getMessage());
			}
		}
	}

	@Override
	public UploadResult upload(MultipartFile file, String path) {
		String originalName = file.getOriginalFilename();
		String ext = "";
		if (originalName != null && originalName.contains(".")) {
			ext = originalName.substring(originalName.lastIndexOf("."));
		}
		String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
		String uuid = UUID.randomUUID().toString().replace("-", "");
		String fileKey = datePath + "/" + (path != null ? path + "/" : "") + uuid + ext;

		Path targetPath = Paths.get(uploadPath, fileKey);
		try {
			Files.createDirectories(targetPath.getParent());
			try (InputStream is = file.getInputStream()) {
				Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
			}
			return new UploadResult(fileKey, file.getSize(), file.getContentType());
		} catch (IOException e) {
			throw new top.ruilink.inkwash.base.exception.BusinessException("error.file.upload_failed");
		}
	}

	@Override
	public String getFileUrl(String fileKey) {
		if (fileKey == null || fileKey.isEmpty())
			return null;
		return urlPrefix + fileKey;
	}

	@Override
	public byte[] download(String fileKey) {
		try {
			Path filePath = Paths.get(uploadPath, fileKey);
			return Files.readAllBytes(filePath);
		} catch (IOException e) {
			throw new top.ruilink.inkwash.base.exception.BusinessException("error.file.download_failed");
		}
	}

	@Override
	public InputStream downloadStream(String fileKey) throws IOException {
		Path filePath = Paths.get(uploadPath, fileKey);
		if (!Files.exists(filePath)) {
			throw new top.ruilink.inkwash.base.exception.BusinessException("error.file.not_found");
		}
		return Files.newInputStream(filePath);
	}

	@Override
	public void delete(String fileKey) {
		try {
			Path filePath = Paths.get(uploadPath, fileKey);
			Files.deleteIfExists(filePath);
			// Remove now empty parent directories (yyyy-MM/folderName)
			Path parent = filePath.getParent();
			while (parent != null && !parent.equals(Paths.get(uploadPath).toAbsolutePath())) {
				if (Files.isDirectory(parent) && isDirectoryEmpty(parent)) {
					Files.delete(parent);
					log.debug("已清理空目录: {}", parent);
				} else {
					break;
				}
				parent = parent.getParent();
			}
		} catch (IOException e) {
			log.warn("删除文件失败: {}", e.getMessage());
		}
	}

	private boolean isDirectoryEmpty(Path dir) throws IOException {
		try (var stream = Files.list(dir)) {
			return stream.findFirst().isEmpty();
		}
	}
}
