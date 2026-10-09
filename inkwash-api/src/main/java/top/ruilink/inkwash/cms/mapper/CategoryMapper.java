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

import top.ruilink.inkwash.cms.api.query.CategoryQuery;
import top.ruilink.inkwash.cms.domain.Category;

/**
 * Category persistence mapper.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Mapper
public interface CategoryMapper {
	/**
	 * Columns selected by every {@code cms_category} read.
	 *
	 * <p>
	 * Centralised so that {@code level} (added by ISS-032) and any later column has
	 * one place to change instead of seven inline literals.
	 */
	String COLUMNS = "id, name, slug, parent_id, level, status, remark, creator, create_time, updater, update_time";

	@Insert("INSERT INTO cms_category (name, slug, parent_id, level, remark) "
			+ "VALUES (#{name}, #{slug}, #{parentId}, #{level}, #{remark})")
	@Options(useGeneratedKeys = true, keyProperty = "id")
	Long insert(Category category);

	@Update("UPDATE cms_category SET name=#{name}, slug=#{slug}, parent_id=#{parentId}, "
			+ "level=#{level}, remark=#{remark} WHERE id=#{id}")
	void update(Category category);

	@Delete("DELETE FROM cms_category WHERE id = #{id}")
	void deleteById(@Param("id") Long id);

	@Select("SELECT " + CategoryMapper.COLUMNS + " FROM cms_category WHERE id = #{id}")
	@Results(id = "categoryResult", value = { @Result(property = "id", column = "id"),
			@Result(property = "name", column = "name"), @Result(property = "slug", column = "slug"),
			@Result(property = "parentId", column = "parent_id"), @Result(property = "level", column = "level"),
			@Result(property = "status", column = "status"), @Result(property = "remark", column = "remark"),
			@Result(property = "creator", column = "creator"), @Result(property = "createTime", column = "create_time"),
			@Result(property = "updater", column = "updater"),
			@Result(property = "updateTime", column = "update_time") })
	Category selectById(@Param("id") Long id);

	@Select("<script>SELECT " + CategoryMapper.COLUMNS + " FROM cms_category WHERE id IN "
			+ "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
	@ResultMap("categoryResult")
	List<Category> selectByIds(@Param("ids") List<Long> ids);

	@Select("SELECT " + CategoryMapper.COLUMNS + " FROM cms_category ORDER BY id ASC")
	@ResultMap("categoryResult")
	List<Category> selectAll();

	@Select("SELECT " + CategoryMapper.COLUMNS + " FROM cms_category WHERE parent_id = #{parentId} "
			+ "OR (parent_id IS NULL AND #{parentId} IS NULL) ORDER BY id ASC")
	@ResultMap("categoryResult")
	List<Category> selectByParentId(@Param("parentId") Long parentId);

	@Select("SELECT " + CategoryMapper.COLUMNS + " FROM cms_category ORDER BY id ASC LIMIT #{limit} OFFSET #{offset}")
	@ResultMap("categoryResult")
	List<Category> selectPage(@Param("offset") int offset, @Param("limit") int limit);

	@Select("SELECT COUNT(*) FROM cms_category")
	long count();

	@Select("SELECT COUNT(*) FROM cms_category WHERE parent_id = #{parentId}")
	long countByParentId(@Param("parentId") Long parentId);

	@Select("<script>" + "SELECT COUNT(1) FROM cms_category " + "<where>"
			+ "  <if test='query.status != null'> AND status = #{query.status} </if>"
			+ "  <if test='query.name != null and query.name != \"\"'> AND name LIKE CONCAT('%', #{query.name}, '%') </if>"
			+ "</where>" + "</script>")
	long countCategories(@Param("query") CategoryQuery query);

	@ResultMap("categoryResult")
	@Select("<script>" + "SELECT " + CategoryMapper.COLUMNS + " FROM cms_category " + "<where>"
			+ "  <if test='query.status != null'> AND status = #{query.status} </if>"
			+ "  <if test='query.name != null and query.name != \"\"'> AND name LIKE CONCAT('%', #{query.name}, '%') </if>"
			+ "</where>" + "<if test='sortSql != null'> ORDER BY ${sortSql} </if>" + "LIMIT #{limit} OFFSET #{offset}"
			+ "</script>")
	List<Category> selectCategoryList(@Param("query") CategoryQuery query, @Param("offset") long offset,
			@Param("limit") int limit, @Param("sortSql") String sortSql);
}
