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
package top.ruilink.inkwash.base.support.file.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Select;

import top.ruilink.inkwash.base.support.file.domain.MediaFile;

/**
 * Uploaded file metadata persistence mapper.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Mapper
public interface FileMapper {

	@Insert("INSERT INTO media_file (folder_type, original_name, file_key, file_size, mime_type, creator, create_time) "
			+ "VALUES (#{folderType}, #{originalName}, #{fileKey}, #{fileSize}, #{mimeType}, #{creator}, #{createTime})")
	@Options(useGeneratedKeys = true, keyProperty = "id")
	Long insert(MediaFile entity);

	@Select("SELECT id, folder_type, original_name, file_key, file_size, mime_type, creator, create_time "
			+ "FROM media_file WHERE id = #{id}")
	@Result(property = "folderType", column = "folder_type")
	@Result(property = "originalName", column = "original_name")
	@Result(property = "fileKey", column = "file_key")
	@Result(property = "fileSize", column = "file_size")
	@Result(property = "mimeType", column = "mime_type")
	@Result(property = "createTime", column = "create_time")
	MediaFile selectById(@Param("id") Long id);

	@Select("<script>" + "SELECT id, folder_type, original_name, file_key, file_size, mime_type, creator, create_time "
			+ "FROM media_file WHERE file_key IN "
			+ "<foreach collection='keys' item='k' open='(' separator=',' close=')'>#{k}</foreach>" + "</script>")
	@Result(property = "folderType", column = "folder_type")
	@Result(property = "originalName", column = "original_name")
	@Result(property = "fileKey", column = "file_key")
	@Result(property = "fileSize", column = "file_size")
	@Result(property = "mimeType", column = "mime_type")
	@Result(property = "createTime", column = "create_time")
	List<MediaFile> selectByKeys(@Param("keys") List<String> keys);

	@Select("SELECT id, folder_type, original_name, file_key, file_size, mime_type, creator, create_time "
			+ "FROM media_file ORDER BY create_time DESC LIMIT #{limit} OFFSET #{offset}")
	@Result(property = "folderType", column = "folder_type")
	@Result(property = "originalName", column = "original_name")
	@Result(property = "fileKey", column = "file_key")
	@Result(property = "fileSize", column = "file_size")
	@Result(property = "mimeType", column = "mime_type")
	@Result(property = "createTime", column = "create_time")
	List<MediaFile> selectPage(@Param("offset") int offset, @Param("limit") int limit);

	@Select("SELECT COUNT(*) FROM media_file")
	long count();

	@Delete("DELETE FROM media_file WHERE id = #{id}")
	int deleteById(@Param("id") Long id);

	@Select("SELECT id FROM media_file WHERE file_key = #{fileKey}")
	Long selectIdByFileKey(@Param("fileKey") String fileKey);

	@Delete("DELETE FROM media_file WHERE file_key = #{fileKey}")
	int deleteByFileKey(@Param("fileKey") String fileKey);
}
