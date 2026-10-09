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

import top.ruilink.inkwash.system.SystemConsts;
import top.ruilink.inkwash.system.api.query.MenuQuery;
import top.ruilink.inkwash.system.domain.SysMenu;

/**
 * Menu persistence mapper.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Mapper
public interface MenuMapper {

	@Results(id = "baseResult", value = { @Result(property = "id", column = "id"),
			@Result(property = "parentId", column = "parent_id"), @Result(property = "name", column = "name"),
			@Result(property = "title", column = "title"), @Result(property = "type", column = "type"),
			@Result(property = "path", column = "path"), @Result(property = "component", column = "component"),
			@Result(property = "visible", column = "visible"), @Result(property = "sort", column = "sort"),
			@Result(property = "icon", column = "icon"), @Result(property = "redirect", column = "redirect"),
			@Result(property = "treePath", column = "tree_path"),
			@Result(property = "keepAlive", column = "keep_alive"), @Result(property = "status", column = "status"),
			@Result(property = "authority", column = "authority"), @Result(property = "remark", column = "remark"),
			@Result(property = "creator", column = "creator"), @Result(property = "updater", column = "updater"),
			@Result(property = "createTime", column = "create_time"),
			@Result(property = "updateTime", column = "update_time") })
	@Select("SELECT id, parent_id, name, title, type, path, component, visible, sort, icon, redirect, tree_path, keep_alive, status, authority, remark, creator, updater, create_time, update_time FROM sys_menu WHERE id = #{id}")
	SysMenu selectById(@Param("id") Long id);

	@Select("SELECT id, parent_id, name, title, type, path, component, visible, sort, icon, redirect, tree_path, keep_alive, status, authority, remark, creator, updater, create_time, update_time FROM sys_menu ORDER BY sort ASC, create_time DESC LIMIT #{limit} OFFSET #{offset}")
	@ResultMap("baseResult")
	List<SysMenu> selectPage(@Param("offset") int offset, @Param("limit") int limit);

	@Select("SELECT COUNT(*) FROM sys_menu")
	long count();

	@Select("SELECT id, parent_id, name, title, type, path, component, visible, sort, icon, redirect, tree_path, keep_alive, status, authority, remark, creator, updater, create_time, update_time FROM sys_menu WHERE status = "
			+ SystemConsts.MENU_STATUS_ACTIVE + " ORDER BY sort ASC, create_time ASC")
	@ResultMap("baseResult")
	List<SysMenu> selectAll();

	@Select("SELECT CONCAT(p.module, ':', p.resource, ':', p.action) FROM sys_permission p INNER JOIN sys_role_permission rp ON p.id = rp.permission_id INNER JOIN sys_group_role gr ON rp.role_id = gr.role_id INNER JOIN sys_user_group ug ON gr.group_id = ug.group_id WHERE ug.user_id = #{userId} AND p.status = "
			+ SystemConsts.PERMISSION_STATUS_ACTIVE)
	List<String> selectUserAuthorities(@Param("userId") Long userId);

	@Select("SELECT id, parent_id, name, title, type, path, component, visible, sort, icon, redirect, tree_path, keep_alive, status, authority, remark, creator, updater, create_time, update_time FROM sys_menu WHERE parent_id = #{parentId} ORDER BY sort ASC, create_time ASC")
	@ResultMap("baseResult")
	List<SysMenu> selectByParentId(@Param("parentId") Integer parentId);

	@Select("<script>" + "SELECT COUNT(1) FROM sys_menu " + "<where>"
			+ "  <if test='query.status != null'> AND status = #{query.status} </if>"
			+ "  <if test='query.type != null'> AND type = #{query.type} </if>"
			+ "  <if test='query.name != null and query.name != \"\"'> AND name LIKE CONCAT('%', #{query.name}, '%') </if>"
			+ "</where>" + "</script>")
	long countMenus(@Param("query") MenuQuery query);

	@ResultMap("baseResult")
	@Select("<script>"
			+ "SELECT id, parent_id, name, title, type, path, component, visible, sort, icon, redirect, tree_path, keep_alive, status, authority, remark, creator, updater, create_time, update_time "
			+ "FROM sys_menu " + "<where>" + "  <if test='query.status != null'> AND status = #{query.status} </if>"
			+ "  <if test='query.type != null'> AND type = #{query.type} </if>"
			+ "  <if test='query.name != null and query.name != \"\"'> AND name LIKE CONCAT('%', #{query.name}, '%') </if>"
			+ "</where>" + "<if test='sortSql != null'> ORDER BY ${sortSql} </if>" + "LIMIT #{limit} OFFSET #{offset}"
			+ "</script>")
	List<SysMenu> selectMenuList(@Param("query") MenuQuery query, @Param("offset") long offset,
			@Param("limit") int limit, @Param("sortSql") String sortSql);

	@Insert("INSERT INTO sys_menu (parent_id, name, title, type, path, component, visible, sort, icon, redirect, tree_path, keep_alive, status, authority, remark, creator, create_time, updater, update_time) VALUES (#{parentId}, #{name}, #{title}, #{type}, #{path}, #{component}, #{visible}, #{sort}, #{icon}, #{redirect}, #{treePath}, #{keepAlive}, #{status}, #{authority}, #{remark}, #{creator}, #{createTime}, #{updater}, #{updateTime})")
	@Options(useGeneratedKeys = true, keyProperty = "id")
	Long create(SysMenu menu);

	@Update("<script>UPDATE sys_menu <set><if test='parentId != null'>parent_id = #{parentId},</if><if test='name != null'>name = #{name},</if><if test='title != null'>title = #{title},</if><if test='type != null'>type = #{type},</if><if test='path != null'>path = #{path},</if><if test='component != null'>component = #{component},</if><if test='visible != null'>visible = #{visible},</if><if test='sort != null'>sort = #{sort},</if><if test='icon != null'>icon = #{icon},</if><if test='redirect != null'>redirect = #{redirect},</if><if test='treePath != null'>tree_path = #{treePath},</if><if test='keepAlive != null'>keep_alive = #{keepAlive},</if><if test='status != null'>status = #{status},</if><if test='authority != null'>authority = #{authority},</if><if test='remark != null'>remark = #{remark},</if><if test='updater != null'>updater = #{updater},</if>update_time = #{updateTime}</set> WHERE id = #{id}</script>")
	int update(SysMenu menu);

	@Delete("DELETE FROM sys_menu WHERE id = #{id}")
	int delete(@Param("id") Long id);

	@Delete("DELETE FROM sys_menu WHERE parent_id = #{parentId}")
	int deleteByParentId(@Param("parentId") Integer parentId);
}
