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

import top.ruilink.inkwash.cms.api.query.TermQuery;
import top.ruilink.inkwash.cms.domain.Term;

/**
 * Article tag persistence mapper.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Mapper
public interface TermMapper {

	@Insert("INSERT INTO cms_term (name, slug, status, remark) VALUES (#{name}, #{slug}, #{status}, #{remark})")
	@Options(useGeneratedKeys = true, keyProperty = "id")
	Long insert(Term term);

	@Update("UPDATE cms_term SET name=#{name}, slug=#{slug}, status=#{status}, remark=#{remark} WHERE id=#{id}")
	void update(Term term);

	@Delete("DELETE FROM cms_term WHERE id = #{id}")
	void deleteById(@Param("id") Long id);

	@Select("SELECT * FROM cms_term WHERE id = #{id}")
	@Results(id = "termResult", value = { @Result(property = "id", column = "id"),
			@Result(property = "name", column = "name"), @Result(property = "slug", column = "slug"),
			@Result(property = "status", column = "status"), @Result(property = "remark", column = "remark"),
			@Result(property = "createTime", column = "create_time"),
			@Result(property = "updateTime", column = "update_time") })
	Term selectById(@Param("id") Long id);

	@Select("<script>SELECT * FROM cms_term WHERE id IN "
			+ "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
	@ResultMap("termResult")
	List<Term> selectByIds(@Param("ids") List<Integer> ids);

	@Select("SELECT * FROM cms_term ORDER BY id DESC")
	@ResultMap("termResult")
	List<Term> selectAll();

	@Select("SELECT * FROM cms_term ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}")
	@ResultMap("termResult")
	List<Term> selectPage(@Param("offset") int offset, @Param("limit") int limit);

	@Select("SELECT COUNT(*) FROM cms_term")
	long count();

	@Select("<script>" + "SELECT COUNT(1) FROM cms_term " + "<where>"
			+ "  <if test='query.status != null'> AND status = #{query.status} </if>"
			+ "  <if test='query.name != null and query.name != \"\"'> AND name LIKE CONCAT('%', #{query.name}, '%') </if>"
			+ "</where>" + "</script>")
	long countTerms(@Param("query") TermQuery query);

	@ResultMap("termResult")
	@Select("<script>" + "SELECT * FROM cms_term " + "<where>"
			+ "  <if test='query.status != null'> AND status = #{query.status} </if>"
			+ "  <if test='query.name != null and query.name != \"\"'> AND name LIKE CONCAT('%', #{query.name}, '%') </if>"
			+ "</where>" + "<if test='sortSql != null'> ORDER BY ${sortSql} </if>" + "LIMIT #{limit} OFFSET #{offset}"
			+ "</script>")
	List<Term> selectTermList(@Param("query") TermQuery query, @Param("offset") long offset, @Param("limit") int limit,
			@Param("sortSql") String sortSql);
}
