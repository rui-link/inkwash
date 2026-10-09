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
package top.ruilink.inkwash.monitor.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import top.ruilink.inkwash.monitor.domain.Journal;

/**
 * Operation journal persistence mapper.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Mapper
public interface JournalMapper {

	@Insert("INSERT INTO mon_journal (user_id, user_name, module, operation, url, method, "
			+ "param, result, fail_reason, ip, duration, creator, create_time) "
			+ "VALUES (#{userId}, #{userName}, #{module}, #{operation}, #{url}, #{method}, "
			+ "#{param}, #{result}, #{failReason}, #{ip}, #{duration}, #{creator}, #{createTime})")
	@Options(useGeneratedKeys = true, keyProperty = "id")
	Long insert(Journal entity);

	@Select("<script>" + "SELECT id, user_id, user_name, module, operation, url, method, param, "
			+ "result, fail_reason, ip, duration, creator, create_time FROM mon_journal " + "<where>"
			+ "<if test='username != null and username != \"\"'>AND user_name LIKE CONCAT('%', #{username}, '%')</if>"
			+ "<if test='module != null and module != \"\"'>AND module LIKE CONCAT('%', #{module}, '%')</if>"
			+ "<if test='operation != null and operation != \"\"'>AND operation LIKE CONCAT('%', #{operation}, '%')</if>"
			+ "<if test='result != null'>AND result = #{result}</if>"
			+ "<if test='startTime != null'>AND create_time &gt;= #{startTime}</if>"
			+ "<if test='endTime != null'>AND create_time &lt;= #{endTime}</if>" + "</where>"
			+ "ORDER BY create_time DESC LIMIT #{limit} OFFSET #{offset}" + "</script>")
	@Results(id = "journalResult", value = { @Result(property = "id", column = "id"),
			@Result(property = "userId", column = "user_id"), @Result(property = "userName", column = "user_name"),
			@Result(property = "module", column = "module"), @Result(property = "operation", column = "operation"),
			@Result(property = "url", column = "url"), @Result(property = "method", column = "method"),
			@Result(property = "param", column = "param"), @Result(property = "result", column = "result"),
			@Result(property = "failReason", column = "fail_reason"), @Result(property = "ip", column = "ip"),
			@Result(property = "duration", column = "duration"), @Result(property = "creator", column = "creator"),
			@Result(property = "createTime", column = "create_time") })
	List<Journal> selectPage(@Param("username") String username, @Param("module") String module,
			@Param("operation") String operation, @Param("result") Integer result,
			@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime,
			@Param("offset") int offset, @Param("limit") int limit);

	@Select("<script>" + "SELECT COUNT(*) FROM mon_journal " + "<where>"
			+ "<if test='username != null and username != \"\"'>AND user_name LIKE CONCAT('%', #{username}, '%')</if>"
			+ "<if test='module != null and module != \"\"'>AND module LIKE CONCAT('%', #{module}, '%')</if>"
			+ "<if test='operation != null and operation != \"\"'>AND operation LIKE CONCAT('%', #{operation}, '%')</if>"
			+ "<if test='result != null'>AND result = #{result}</if>"
			+ "<if test='startTime != null'>AND create_time &gt;= #{startTime}</if>"
			+ "<if test='endTime != null'>AND create_time &lt;= #{endTime}</if>" + "</where>" + "</script>")
	long count(@Param("username") String username, @Param("module") String module, @Param("operation") String operation,
			@Param("result") Integer result, @Param("startTime") LocalDateTime startTime,
			@Param("endTime") LocalDateTime endTime);

	@Select("SELECT id, user_id, user_name, module, operation, url, method, param, "
			+ "result, fail_reason, ip, duration, creator, create_time FROM mon_journal ORDER BY create_time DESC LIMIT #{limit}")
	@ResultMap("journalResult")
	List<Journal> selectRecent(@Param("limit") int limit);
}
