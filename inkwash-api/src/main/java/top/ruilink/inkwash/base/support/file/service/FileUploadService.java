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
package top.ruilink.inkwash.base.support.file.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import top.ruilink.inkwash.base.domain.PageResult;
import top.ruilink.inkwash.base.support.file.api.view.FileView;
import top.ruilink.inkwash.base.support.file.api.query.FileQuery;

/**
 * File upload, query and deletion service interface.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public interface FileUploadService {
	FileView upload(MultipartFile file, Integer folderType);

	FileView getById(Long id);

	String getUrl(String fileKey);

	List<FileView> getFileList(List<String> fileKeys);

	PageResult<FileView> queryPage(FileQuery param);

	void delete(Long id);

	void deleteByKey(String fileKey);
}
