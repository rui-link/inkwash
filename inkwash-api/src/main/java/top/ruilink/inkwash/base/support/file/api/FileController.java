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
package top.ruilink.inkwash.base.support.file.api;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import jakarta.validation.Valid;
import top.ruilink.inkwash.base.domain.PageResult;
import top.ruilink.inkwash.base.support.file.api.view.FileView;
import top.ruilink.inkwash.base.support.file.api.query.FileQuery;
import top.ruilink.inkwash.base.support.file.service.FileStorageService;
import top.ruilink.inkwash.base.support.file.service.FileUploadService;

/**
 * File upload, download, query and deletion REST endpoints.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Validated
@RestController
@RequestMapping("/api/support/file")
public class FileController {

	private final FileUploadService fileUploadService;
	private final FileStorageService fileStorageService;

	public FileController(FileUploadService fileUploadService, FileStorageService fileStorageService) {
		this.fileUploadService = fileUploadService;
		this.fileStorageService = fileStorageService;
	}

	@PostMapping("/upload")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<FileView> upload(@RequestParam MultipartFile file,
			@RequestParam(required = false, defaultValue = "1") Integer folderType) {
		return ResponseEntity.ok(fileUploadService.upload(file, folderType));
	}

	@GetMapping("/{id}")
	public ResponseEntity<FileView> getById(@PathVariable Long id) {
		return ResponseEntity.ok(fileUploadService.getById(id));
	}

	@GetMapping("/{id}/download")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<StreamingResponseBody> download(@PathVariable Long id) {
		FileView view = fileUploadService.getById(id);
		StreamingResponseBody stream = outputStream -> {
			try (InputStream is = fileStorageService.downloadStream(view.getFileKey())) {
				is.transferTo(outputStream);
			}
		};
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + view.getOriginalName() + "\"")
				.contentType(MediaType.APPLICATION_OCTET_STREAM).body(stream);
	}

	@GetMapping("/page")
	public ResponseEntity<PageResult<FileView>> queryPage(@Valid FileQuery param) {
		return ResponseEntity.ok(fileUploadService.queryPage(param));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasAuthority('system:file:delete')")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		fileUploadService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
