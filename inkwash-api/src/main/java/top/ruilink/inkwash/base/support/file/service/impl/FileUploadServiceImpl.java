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

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;
import top.ruilink.inkwash.base.domain.PageResult;
import top.ruilink.inkwash.base.exception.BusinessException;
import top.ruilink.inkwash.base.support.file.api.view.FileView;
import top.ruilink.inkwash.base.support.file.domain.MediaFile;
import top.ruilink.inkwash.base.support.file.enums.FileFolderType;
import top.ruilink.inkwash.base.support.file.mapper.FileMapper;
import top.ruilink.inkwash.base.support.file.api.query.FileQuery;
import top.ruilink.inkwash.base.support.file.service.FileStorageService;
import top.ruilink.inkwash.base.support.file.service.FileUploadService;
import top.ruilink.inkwash.base.util.CurrentUserProvider;
import top.ruilink.inkwash.base.support.file.util.MimeTypeValidator;
import top.ruilink.inkwash.base.util.PageUtil;

/**
 * File upload, query and deletion service implementation.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Slf4j
@Service
public class FileUploadServiceImpl implements FileUploadService {

	private final FileStorageService fileStorageService;
	private final FileMapper fileMapper;

	public FileUploadServiceImpl(FileStorageService fileStorageService, FileMapper fileMapper) {
		this.fileStorageService = fileStorageService;
		this.fileMapper = fileMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public FileView upload(MultipartFile file, Integer folderType) {
		FileFolderType folder = (folderType != null) ? FileFolderType.fromCode(folderType) : FileFolderType.PUBLIC;

		String originalName = file.getOriginalFilename();
		String ext = "";
		if (originalName != null && originalName.contains(".")) {
			ext = originalName.substring(originalName.lastIndexOf(".") + 1).toLowerCase();
		}
		if (!ext.isEmpty() && !folder.isExtensionAllowed(ext)) {
			throw new BusinessException("error.file.type_not_allowed");
		}
		if (file.getSize() > folder.getMaxFileSize()) {
			throw new BusinessException("error.file.size_exceeded");
		}
		MimeTypeValidator.validate(ext, file.getContentType());

		String path = folder.getFolderName();
		FileStorageService.UploadResult uploadResult = fileStorageService.upload(file, path);

		MediaFile entity = new MediaFile();
		entity.setFolderType(folder.getCode());
		entity.setOriginalName(originalName);
		entity.setFileKey(uploadResult.fileKey());
		entity.setFileSize(uploadResult.size());
		entity.setMimeType(uploadResult.mimeType());
		Long userId = null;
		if (CurrentUserProvider.isAuthenticated()) {
			try {
				userId = CurrentUserProvider.getCurrentUserIdOrNull();
			} catch (Exception e) {
				log.debug("无法获取用户ID", e);
			}
		}
		entity.setCreator(userId);
		entity.setCreateTime(LocalDateTime.now());
		fileMapper.insert(entity);

		return toView(entity);
	}

	public FileView getById(Long id) {
		MediaFile entity = fileMapper.selectById(id);
		if (entity == null)
			throw new BusinessException("error.file.not_found");
		return toView(entity);
	}

	public String getUrl(String fileKey) {
		return fileStorageService.getFileUrl(fileKey);
	}

	public List<FileView> getFileList(List<String> fileKeys) {
		if (fileKeys == null || fileKeys.isEmpty())
			return Collections.emptyList();
		List<MediaFile> entities = fileMapper.selectByKeys(fileKeys);
		return entities.stream().map(this::toView).collect(Collectors.toList());
	}

	public PageResult<FileView> queryPage(FileQuery param) {
		int offset = (param.getPage() - 1) * param.getSize();
		List<MediaFile> list = fileMapper.selectPage(offset, param.getSize());
		long total = fileMapper.count();
		List<FileView> views = list.stream().map(this::toView).collect(Collectors.toList());
		Page<FileView> page = PageUtil.toPage(views, param.getPage(), param.getSize(), total);
		return PageResult.of(page);
	}

	@Transactional(rollbackFor = Exception.class)
	public void delete(Long id) {
		MediaFile entity = fileMapper.selectById(id);
		if (entity == null)
			return;
		fileStorageService.delete(entity.getFileKey());
		fileMapper.deleteById(id);
		log.info("文件已删除, id={}, fileKey={}", id, entity.getFileKey());
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteByKey(String fileKey) {
		if (fileKey == null || fileKey.isEmpty())
			return;
		fileStorageService.delete(fileKey);
		fileMapper.deleteByFileKey(fileKey);
		log.info("按fileKey删除文件, fileKey={}", fileKey);
	}

	private FileView toView(MediaFile entity) {
		FileView view = new FileView();
		view.setId(entity.getId());
		view.setFolderType(entity.getFolderType());
		view.setOriginalName(entity.getOriginalName());
		view.setFileKey(entity.getFileKey());
		view.setFileSize(entity.getFileSize());
		view.setMimeType(entity.getMimeType());
		view.setUrl(fileStorageService.getFileUrl(entity.getFileKey()));
		view.setCreateTime(entity.getCreateTime());
		return view;
	}
}
