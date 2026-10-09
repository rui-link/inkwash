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
package top.ruilink.inkwash.system.mapper;

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

import top.ruilink.inkwash.system.api.query.GroupQuery;
import top.ruilink.inkwash.system.domain.SysGroup;

/**
 * User group persistence mapper.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Mapper
public interface GroupMapper {

	@Results(id = "baseResult", value = { @Result(property = "id", column = "id"),
			@Result(property = "name", column = "name"), @Result(property = "parentId", column = "parent_id"),
			@Result(property = "level", column = "level"), @Result(property = "status", column = "status"),
			@Result(property = "remark", column = "remark"), @Result(property = "creator", column = "creator"),
			@Result(property = "updater", column = "updater"), @Result(property = "createTime", column = "create_time"),
			@Result(property = "updateTime", column = "update_time") })
	@Select("SELECT id, name, parent_id, level, status, remark, creator, updater, create_time, update_time FROM sys_group WHERE id = #{id}")
	SysGroup selectById(@Param("id") Integer id);

	@Select("SELECT id, name, parent_id, level, status, remark, creator, updater, create_time, update_time FROM sys_group ORDER BY create_time DESC LIMIT #{limit} OFFSET #{offset}")
	@ResultMap("baseResult")
	List<SysGroup> selectPage(@Param("offset") int offset, @Param("limit") int limit);

	@Select("SELECT COUNT(*) FROM sys_group")
	long count();

	@Select("<script>" + "SELECT COUNT(1) FROM sys_group " + "<where>"
			+ "  <if test='query.status != null'> AND status = #{query.status} </if>"
			+ "  <if test='query.name != null and query.name != \"\"'> AND name LIKE CONCAT('%', #{query.name}, '%') </if>"
			+ "</where>" + "</script>")
	long countGroups(@Param("query") GroupQuery query);

	@ResultMap("baseResult")
	@Select("<script>"
			+ "SELECT id, name, parent_id, level, status, remark, creator, updater, create_time, update_time "
			+ "FROM sys_group " + "<where>" + "  <if test='query.status != null'> AND status = #{query.status} </if>"
			+ "  <if test='query.name != null and query.name != \"\"'> AND name LIKE CONCAT('%', #{query.name}, '%') </if>"
			+ "</where>" + "<if test='sortSql != null'> ORDER BY ${sortSql} </if>" + "LIMIT #{limit} OFFSET #{offset}"
			+ "</script>")
	List<SysGroup> selectGroupList(@Param("query") GroupQuery query, @Param("offset") long offset,
			@Param("limit") int limit, @Param("sortSql") String sortSql);

	@Select("SELECT id, name, parent_id, level, status, remark, creator, updater, create_time, update_time FROM sys_group WHERE status = 1 ORDER BY level ASC, create_time ASC")
	@ResultMap("baseResult")
	List<SysGroup> selectAll();

	@Select("SELECT id, name, parent_id, level, status, remark, creator, updater, create_time, update_time FROM sys_group WHERE parent_id = #{parentId} ORDER BY create_time ASC")
	@ResultMap("baseResult")
	List<SysGroup> selectChildren(@Param("parentId") Integer parentId);

	@Select("SELECT id, name, parent_id, level, status, remark, creator, updater, create_time, update_time FROM sys_group WHERE name = #{name} AND status = 1 ORDER BY create_time ASC LIMIT 1")
	@ResultMap("baseResult")
	SysGroup selectByName(@Param("name") String name);

	@Select("SELECT g.id, g.name, g.parent_id, g.level, g.status, g.remark, g.creator, g.updater, g.create_time, g.update_time FROM sys_group g INNER JOIN sys_user_group ug ON g.id = ug.group_id WHERE ug.user_id = #{userId}")
	@ResultMap("baseResult")
	List<SysGroup> selectByUserId(@Param("userId") Long userId);

	@Select("SELECT g.id, g.name, g.parent_id, g.level, g.status, g.remark, g.creator, g.updater, g.create_time, g.update_time FROM sys_group g INNER JOIN sys_group_role gr ON g.id = gr.group_id WHERE gr.role_id = #{roleId}")
	@ResultMap("baseResult")
	List<SysGroup> selectByRoleId(@Param("roleId") Integer roleId);

	@Insert("INSERT INTO sys_group (name, parent_id, level, status, remark, creator, create_time, updater, update_time) VALUES (#{name}, #{parentId}, #{level}, #{status}, #{remark}, #{creator}, #{createTime}, #{updater}, #{updateTime})")
	@Options(useGeneratedKeys = true, keyProperty = "id")
	Integer create(SysGroup group);

	@Update("<script>UPDATE sys_group <set><if test='name != null'>name = #{name},</if><if test='parentId != null'>parent_id = #{parentId},</if><if test='level != null'>level = #{level},</if><if test='status != null'>status = #{status},</if><if test='remark != null'>remark = #{remark},</if><if test='updater != null'>updater = #{updater},</if>update_time = #{updateTime}</set> WHERE id = #{id}</script>")
	int update(SysGroup group);

	@Delete("DELETE FROM sys_group WHERE id = #{id}")
	int delete(@Param("id") Integer id);

	@Delete("DELETE FROM sys_group WHERE parent_id = #{parentId}")
	int deleteByParentId(@Param("parentId") Integer parentId);

	@Insert("INSERT INTO sys_group_role (group_id, role_id) VALUES (#{groupId}, #{roleId})")
	void assignRole(@Param("groupId") Integer groupId, @Param("roleId") Integer roleId);

	@Insert("<script>INSERT INTO sys_group_role (group_id, role_id) VALUES "
			+ "<foreach collection='roleIds' item='rid' separator=','>" + "(#{groupId}, #{rid})</foreach></script>")
	void assignRolesBatch(@Param("groupId") Integer groupId, @Param("roleIds") List<Integer> roleIds);

	@Delete("DELETE FROM sys_group_role WHERE group_id = #{groupId}")
	void removeRoles(@Param("groupId") Integer groupId);
}
