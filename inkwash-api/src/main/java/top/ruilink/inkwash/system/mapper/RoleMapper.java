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

import top.ruilink.inkwash.system.api.query.RoleQuery;
import top.ruilink.inkwash.system.domain.SysRole;

/**
 * Role persistence mapper.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Mapper
public interface RoleMapper {

	@Select("SELECT DISTINCT r.code FROM sys_role r INNER JOIN sys_group_role gr ON r.id = gr.role_id INNER JOIN sys_user_group ug ON gr.group_id = ug.group_id WHERE ug.user_id = #{userId}")
	List<String> selectCodesByUserId(@Param("userId") Long userId);

	@Results(id = "baseResult", value = { @Result(property = "id", column = "id"),
			@Result(property = "name", column = "name"), @Result(property = "code", column = "code"),
			@Result(property = "status", column = "status"), @Result(property = "remark", column = "remark"),
			@Result(property = "groupId", column = "group_id"), @Result(property = "creator", column = "creator"),
			@Result(property = "updater", column = "updater"), @Result(property = "createTime", column = "create_time"),
			@Result(property = "updateTime", column = "update_time") })
	@Select("SELECT id, name, code, status, remark, creator, updater, create_time, update_time FROM sys_role WHERE id = #{roleId}")
	SysRole selectById(@Param("roleId") Integer roleId);

	@Select("<script>" + "SELECT COUNT(1) FROM sys_role " + "<where>"
			+ "  <if test='query.name != null and query.name != \"\"'> AND name LIKE CONCAT('%', #{query.name}, '%') </if>"
			+ "  <if test='query.status != null'> AND status = #{query.status} </if>"
			+ "  <if test='query.code != null and query.code != \"\"'> AND code = #{query.code} </if>" + "</where>"
			+ "</script>")
	long countRoles(@Param("query") RoleQuery query);

	@ResultMap("baseResult")
	@Select("<script>" + "SELECT id, name, code, status, remark, creator, updater, create_time, update_time "
			+ "FROM sys_role " + "<where>"
			+ "  <if test='query.name != null and query.name != \"\"'> AND name LIKE CONCAT('%', #{query.name}, '%') </if>"
			+ "  <if test='query.status != null'> AND status = #{query.status} </if>"
			+ "  <if test='query.code != null and query.code != \"\"'> AND code = #{query.code} </if>" + "</where>"
			+ "<if test='sortSql != null'> ORDER BY ${sortSql} </if>" + "LIMIT #{limit} OFFSET #{offset}" + "</script>")
	List<SysRole> selectRoleList(@Param("query") RoleQuery query, @Param("offset") long offset,
			@Param("limit") int limit, @Param("sortSql") String sortSql);

	@Select("SELECT id, name, code, status, remark, creator, updater, create_time, update_time FROM sys_role ORDER BY create_time DESC LIMIT #{limit} OFFSET #{offset}")
	@ResultMap("baseResult")
	List<SysRole> selectPage(@Param("offset") int offset, @Param("limit") int limit);

	@Select("SELECT COUNT(*) FROM sys_role")
	long count();

	@Select("SELECT id, name, code, status, remark, creator, updater, create_time, update_time FROM sys_role WHERE code = #{code}")
	@ResultMap("baseResult")
	SysRole selectByCode(@Param("code") String code);

	@Select("SELECT id, name, code, status, remark, creator, updater, create_time, update_time FROM sys_role WHERE id = #{roleId}")
	@ResultMap("baseResult")
	SysRole selectByIdNoPermissions(@Param("roleId") Integer roleId);

	@Select("SELECT DISTINCT r.id, r.name, r.code, r.status, r.remark, r.creator, r.updater, r.create_time, r.update_time FROM sys_role r INNER JOIN sys_group_role gr ON r.id = gr.role_id INNER JOIN sys_user_group ug ON gr.group_id = ug.group_id WHERE ug.user_id = #{userId}")
	@ResultMap("baseResult")
	List<SysRole> selectByUserId(@Param("userId") Long userId);

	@Select("SELECT DISTINCT r.id, r.name, r.code, r.status, r.remark, r.creator, r.updater, r.create_time, r.update_time FROM sys_role r INNER JOIN sys_group_role gr ON r.id = gr.role_id WHERE gr.group_id = #{groupId}")
	@ResultMap("baseResult")
	List<SysRole> selectByGroupId(@Param("groupId") Integer groupId);

	@Select("<script>SELECT DISTINCT r.id, r.name, r.code, r.status, r.remark, r.creator, r.updater, r.create_time, r.update_time, gr.group_id FROM sys_role r INNER JOIN sys_group_role gr ON r.id = gr.role_id WHERE gr.group_id IN <foreach collection='groupIds' item='gid' open='(' separator=',' close=')'>#{gid}</foreach></script>")
	@ResultMap("baseResult")
	List<SysRole> selectByGroupIds(@Param("groupIds") List<Integer> groupIds);

	@Insert("INSERT INTO sys_role (name, code, status, remark, creator, create_time, updater, update_time) VALUES (#{name}, #{code}, #{status}, #{remark}, #{creator}, #{createTime}, #{updater}, #{updateTime})")
	@Options(useGeneratedKeys = true, keyProperty = "id")
	Integer create(SysRole role);

	@Update("<script>UPDATE sys_role <set><if test='name != null'>name = #{name},</if><if test='code != null'>code = #{code},</if><if test='status != null'>status = #{status},</if><if test='remark != null'>remark = #{remark},</if><if test='updater != null'>updater = #{updater},</if>update_time = #{updateTime}</set> WHERE id = #{id}</script>")
	int update(SysRole role);

	@Delete("DELETE FROM sys_role WHERE id = #{id}")
	int delete(@Param("id") Integer id);

	@Delete("DELETE FROM sys_role_permission WHERE role_id = #{roleId}")
	void removePermissions(@Param("roleId") Integer roleId);

	@Insert("INSERT INTO sys_role_permission (role_id, permission_id) VALUES (#{roleId}, #{permissionId})")
	void assignPermission(@Param("roleId") Integer roleId, @Param("permissionId") Integer permissionId);

	@Insert("<script>INSERT INTO sys_role_permission (role_id, permission_id) VALUES "
			+ "<foreach collection='permissionIds' item='pid' separator=','>"
			+ "(#{roleId}, #{pid})</foreach></script>")
	void assignPermissions(@Param("roleId") Integer roleId, @Param("permissionIds") List<Long> permissionIds);

	@Insert("INSERT INTO sys_group_role (group_id, role_id) VALUES (#{groupId}, #{roleId})")
	void assignGroupRole(@Param("groupId") Integer groupId, @Param("roleId") Integer roleId);

	@Insert("<script>INSERT INTO sys_group_role (group_id, role_id) VALUES "
			+ "<foreach collection='roleIds' item='rid' separator=','>" + "(#{groupId}, #{rid})</foreach></script>")
	void assignGroupRolesBatch(@Param("groupId") Integer groupId, @Param("roleIds") List<Integer> roleIds);

	@Delete("DELETE FROM sys_group_role WHERE group_id = #{groupId}")
	void removeGroupRoles(@Param("groupId") Integer groupId);
}
