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
package top.ruilink.inkwash.cms.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import top.ruilink.inkwash.cms.api.query.SensitiveQuery;
import top.ruilink.inkwash.cms.domain.Sensitive;

/**
 * Sensitive word persistence mapper.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Mapper
public interface SensitiveMapper {

	/**
	 * Enabled status.
	 */
	int STATUS_ACTIVE = 1;

	@Insert("INSERT INTO cms_sensitive (word, type, status, remark, creator, updater, create_time, update_time) "
			+ "VALUES (#{word}, #{type}, #{status}, #{remark}, #{creator}, #{updater}, NOW(), NOW())")
	@Options(useGeneratedKeys = true, keyProperty = "id")
	Long insert(Sensitive sensitive);

	@Update("UPDATE cms_sensitive SET word = #{word}, type = #{type}, status = #{status}, remark = #{remark}, "
			+ "updater = #{updater}, update_time = NOW() WHERE id = #{id}")
	void update(Sensitive sensitive);

	@Delete("DELETE FROM cms_sensitive WHERE id = #{id}")
	void deleteById(@Param("id") Long id);

	@Select("SELECT id, word, type, status, remark, creator, updater, create_time, update_time "
			+ "FROM cms_sensitive WHERE id = #{id}")
	@Results(id = "sensitiveResult", value = { @Result(property = "id", column = "id"),
			@Result(property = "word", column = "word"), @Result(property = "type", column = "type"),
			@Result(property = "status", column = "status"), @Result(property = "remark", column = "remark"),
			@Result(property = "creator", column = "creator"), @Result(property = "updater", column = "updater"),
			@Result(property = "createTime", column = "create_time"),
			@Result(property = "updateTime", column = "update_time") })
	Sensitive selectById(@Param("id") Long id);

	@Select("SELECT id, word, type, status, remark, creator, updater, create_time, update_time "
			+ "FROM cms_sensitive ORDER BY id DESC")
	@ResultMap("sensitiveResult")
	List<Sensitive> selectAll();

	@Select("SELECT id, word, type, status, remark, creator, updater, create_time, update_time "
			+ "FROM cms_sensitive WHERE status = " + SensitiveMapper.STATUS_ACTIVE + " ORDER BY id DESC")
	@ResultMap("sensitiveResult")
	List<Sensitive> selectAllActive();

	@Select("SELECT id, word, type, status, remark, creator, updater, create_time, update_time "
			+ "FROM cms_sensitive ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}")
	@ResultMap("sensitiveResult")
	List<Sensitive> selectPage(@Param("offset") int offset, @Param("limit") int limit);

	@Select("SELECT COUNT(*) FROM cms_sensitive")
	long count();

	@Select("<script>" + "SELECT COUNT(1) FROM cms_sensitive " + "<where>"
			+ "  <if test='query.type != null'> AND type = #{query.type} </if>"
			+ "  <if test='query.status != null'> AND status = #{query.status} </if>" + "</where>" + "</script>")
	long countSensitives(@Param("query") SensitiveQuery query);

	@ResultMap("sensitiveResult")
	@Select("<script>" + "SELECT id, word, type, status, remark, creator, updater, create_time, update_time "
			+ "FROM cms_sensitive " + "<where>" + "  <if test='query.type != null'> AND type = #{query.type} </if>"
			+ "  <if test='query.status != null'> AND status = #{query.status} </if>" + "</where>"
			+ "<if test='sortSql != null'> ORDER BY ${sortSql} </if>" + "LIMIT #{limit} OFFSET #{offset}" + "</script>")
	List<Sensitive> selectSensitiveList(@Param("query") SensitiveQuery query, @Param("offset") long offset,
			@Param("limit") int limit, @Param("sortSql") String sortSql);
}
