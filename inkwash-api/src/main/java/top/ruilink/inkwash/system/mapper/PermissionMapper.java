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
import java.util.Set;

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

import top.ruilink.inkwash.system.api.query.PermissionQuery;
import top.ruilink.inkwash.system.domain.SysPermission;

/**
 * Permission persistence mapper.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Mapper
public interface PermissionMapper {

	@Results(id = "baseResult", value = { @Result(property = "id", column = "id"),
			@Result(property = "name", column = "name"), @Result(property = "type", column = "type"),
			@Result(property = "module", column = "module"), @Result(property = "resource", column = "resource"),
			@Result(property = "action", column = "action"), @Result(property = "status", column = "status"),
			@Result(property = "remark", column = "remark"), @Result(property = "creator", column = "creator"),
			@Result(property = "updater", column = "updater"), @Result(property = "createTime", column = "create_time"),
			@Result(property = "updateTime", column = "update_time") })
	@Select("SELECT id, name, type, module, resource, action, status, remark, creator, updater, create_time, update_time FROM sys_permission WHERE id = #{id}")
	SysPermission selectById(@Param("id") Integer id);

	@Select("SELECT id, name, type, module, resource, action, status, remark, creator, updater, create_time, update_time FROM sys_permission ORDER BY module, create_time DESC LIMIT #{limit} OFFSET #{offset}")
	@ResultMap("baseResult")
	List<SysPermission> selectPage(@Param("offset") int offset, @Param("limit") int limit);

	@Select("SELECT COUNT(*) FROM sys_permission")
	long count();

	@Select("SELECT id, name, type, module, resource, action, status, remark, creator, updater, create_time, update_time FROM sys_permission WHERE module = #{module} AND resource = #{resource} AND action = #{action}")
	@ResultMap("baseResult")
	SysPermission selectByCode(@Param("module") String module, @Param("resource") String resource,
			@Param("action") String action);

	@Select("SELECT p.id, p.name, p.type, p.module, p.resource, p.action, p.status, p.remark, p.creator, p.updater, p.create_time, p.update_time FROM sys_permission p INNER JOIN sys_role_permission rp ON p.id = rp.permission_id WHERE rp.role_id = #{roleId}")
	@ResultMap("baseResult")
	Set<SysPermission> selectByRoleId(@Param("roleId") Integer roleId);

	@Select("SELECT DISTINCT p.id, p.name, p.type, p.module, p.resource, p.action, p.status, p.remark, p.creator, p.updater, p.create_time, p.update_time FROM sys_permission p WHERE p.status = 1 AND EXISTS (SELECT 1 FROM sys_role_permission rp INNER JOIN sys_group_role gr ON rp.role_id = gr.role_id INNER JOIN sys_user_group ug ON gr.group_id = ug.group_id WHERE ug.user_id = #{userId} AND rp.permission_id = p.id)")
	@ResultMap("baseResult")
	Set<SysPermission> selectByUserId(@Param("userId") Long userId);

	@Insert("INSERT INTO sys_permission (name, type, module, resource, action, status, remark, creator, create_time, updater, update_time) VALUES (#{name}, #{type}, #{module}, #{resource}, #{action}, #{status}, #{remark}, #{creator}, #{createTime}, #{updater}, #{updateTime})")
	@Options(useGeneratedKeys = true, keyProperty = "id")
	Integer create(SysPermission permission);

	@Update("<script>UPDATE sys_permission <set><if test='name != null'>name = #{name},</if><if test='type != null'>type = #{type},</if><if test='module != null'>module = #{module},</if><if test='resource != null'>resource = #{resource},</if><if test='action != null'>action = #{action},</if><if test='status != null'>status = #{status},</if><if test='remark != null'>remark = #{remark},</if><if test='updater != null'>updater = #{updater},</if>update_time = #{updateTime}</set> WHERE id = #{id}</script>")
	int update(SysPermission permission);

	@Delete("DELETE FROM sys_permission WHERE id = #{id}")
	int delete(@Param("id") Integer id);

	@Select("SELECT DISTINCT r.code FROM sys_role r INNER JOIN sys_group_role gr ON r.id = gr.role_id INNER JOIN sys_user_group ug ON gr.group_id = ug.group_id WHERE ug.user_id = #{userId} AND r.status = 1")
	List<String> selectRoleCodesByUserId(@Param("userId") Long userId);

	@Select("<script>" + "SELECT COUNT(1) FROM sys_permission " + "<where>"
			+ "  <if test='query.status != null'> AND status = #{query.status} </if>"
			+ "  <if test='query.module != null and query.module != \"\"'> AND module = #{query.module} </if>"
			+ "  <if test='query.resource != null and query.resource != \"\"'> AND resource = #{query.resource} </if>"
			+ "</where>" + "</script>")
	long countPermissions(@Param("query") PermissionQuery query);

	@ResultMap("baseResult")
	@Select("<script>"
			+ "SELECT id, name, type, module, resource, action, status, remark, creator, updater, create_time, update_time "
			+ "FROM sys_permission " + "<where>"
			+ "  <if test='query.status != null'> AND status = #{query.status} </if>"
			+ "  <if test='query.module != null and query.module != \"\"'> AND module = #{query.module} </if>"
			+ "  <if test='query.resource != null and query.resource != \"\"'> AND resource = #{query.resource} </if>"
			+ "</where>" + "<if test='sortSql != null'> ORDER BY ${sortSql} </if>" + "LIMIT #{limit} OFFSET #{offset}"
			+ "</script>")
	List<SysPermission> selectPermissionList(@Param("query") PermissionQuery query, @Param("offset") long offset,
			@Param("limit") int limit, @Param("sortSql") String sortSql);
}
