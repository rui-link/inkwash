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

import java.time.LocalDateTime;
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

import top.ruilink.inkwash.base.enums.AuthType;
import top.ruilink.inkwash.base.enums.StatisticUnit;
import top.ruilink.inkwash.base.domain.StatisticEntry;
import top.ruilink.inkwash.system.SystemConsts;
import top.ruilink.inkwash.system.api.query.UserQuery;
import top.ruilink.inkwash.system.domain.SysGroup;
import top.ruilink.inkwash.system.domain.SysUser;

/**
 * User persistence mapper.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Mapper
public interface UserMapper {

	/**
	 * Columns selected by every {@code sys_user} read, kept in one place so adding
	 * a column does not require finding all seven call sites (ISS-044).
	 *
	 * <p>
	 * The joined variant in {@link #selectByIdentity} repeats the same list with a
	 * {@code u.} prefix; keep the two in sync when changing this constant.
	 */
	String USER_COLUMNS = "id, nickname, realname, gender, avatar, phone, email, idtype, idcode, status, motto, location, birthDate, education, biography, creator, updater, create_time, update_time";
	String USER_COLUMNS_JOINED = "u.id, u.nickname, u.realname, u.gender, u.avatar, u.phone, u.email, u.idtype, u.idcode, u.status, u.motto, u.location, u.birthDate, u.education, u.biography, u.creator, u.updater, u.create_time, u.update_time";

	@Results(id = "baseResult", value = { @Result(property = "id", column = "id"),
			@Result(property = "nickname", column = "nickname"), @Result(property = "realname", column = "realname"),
			@Result(property = "gender", column = "gender"), @Result(property = "avatar", column = "avatar"),
			@Result(property = "phone", column = "phone"), @Result(property = "email", column = "email"),
			@Result(property = "idtype", column = "idtype"), @Result(property = "idcode", column = "idcode"),
			@Result(property = "motto", column = "motto"), @Result(property = "birthDate", column = "birthDate"),
			@Result(property = "location", column = "location"), @Result(property = "education", column = "education"),
			@Result(property = "status", column = "status"), @Result(property = "biography", column = "biography"),
			@Result(property = "creator", column = "creator"), @Result(property = "updater", column = "updater"),
			@Result(property = "createTime", column = "create_time"),
			@Result(property = "updateTime", column = "update_time") })
	@Select("SELECT " + UserMapper.USER_COLUMNS + " FROM sys_user WHERE id = #{id}")
	SysUser selectById(@Param("id") Long id);

	@Select("<script>SELECT " + UserMapper.USER_COLUMNS + " FROM sys_user WHERE id IN "
			+ "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
	@ResultMap("baseResult")
	List<SysUser> selectByIds(@Param("ids") List<Long> ids);

	@Select("SELECT " + UserMapper.USER_COLUMNS
			+ " FROM sys_user ORDER BY create_time DESC LIMIT #{limit} OFFSET #{offset}")
	@ResultMap("baseResult")
	List<SysUser> selectPage(@Param("offset") int offset, @Param("limit") int limit);

	@Select("SELECT COUNT(*) FROM sys_user WHERE status != " + SystemConsts.USER_STATUS_DELETED)
	long count();

	@Select("SELECT " + UserMapper.USER_COLUMNS_JOINED
			+ " FROM sys_user u INNER JOIN sys_account a ON u.id = a.user_id WHERE a.identity = #{identity} AND a.auth_type = #{authType}")
	@ResultMap("baseResult")
	SysUser selectByIdentityAndType(@Param("identity") String identity, @Param("authType") AuthType authType);

	@Select("SELECT " + UserMapper.USER_COLUMNS + " FROM sys_user WHERE nickname = #{nickname}")
	@ResultMap("baseResult")
	SysUser selectByNickname(@Param("nickname") String nickname);

	@Select("SELECT " + UserMapper.USER_COLUMNS + " FROM sys_user WHERE email = #{email}")
	@ResultMap("baseResult")
	SysUser selectByEmail(@Param("email") String email);

	@Select("SELECT " + UserMapper.USER_COLUMNS + " FROM sys_user WHERE phone = #{phone}")
	@ResultMap("baseResult")
	SysUser selectByPhone(@Param("phone") String phone);

	@Select("<script>" + "SELECT COUNT(1) FROM sys_user " + "<where>" + "  AND status != "
			+ SystemConsts.USER_STATUS_DELETED + " "
			+ "  <if test='query.status != null'> AND status = #{query.status} </if>"
			+ "  <if test='query.nickname != null and query.nickname != \"\"'> AND nickname LIKE CONCAT('%', #{query.nickname}, '%') </if>"
			+ "  <if test='query.phone != null and query.phone != \"\"'> AND phone LIKE CONCAT('%', #{query.phone}, '%') </if>"
			+ "</where>" + "</script>")
	long countUser(@Param("query") UserQuery query);

	@ResultMap("baseResult")
	@Select("<script>" + "SELECT " + UserMapper.USER_COLUMNS + " " + "FROM sys_user " + "<where>" + "  AND status != "
			+ SystemConsts.USER_STATUS_DELETED + " "
			+ "  <if test='query.status != null'> AND status = #{query.status} </if>"
			+ "  <if test='query.nickname != null and query.nickname != \"\"'> AND nickname LIKE CONCAT('%', #{query.nickname}, '%') </if>"
			+ "  <if test='query.phone != null and query.phone != \"\"'> AND phone LIKE CONCAT('%', #{query.phone}, '%') </if>"
			+ "</where>" + "<if test='sortSql != null'> ORDER BY ${sortSql} </if>" + "LIMIT #{limit} OFFSET #{offset}"
			+ "</script>")
	List<SysUser> selectUserList(@Param("query") UserQuery query, @Param("offset") long offset,
			@Param("limit") int limit, @Param("sortSql") String sortSql);

	@Results(id = "groupResult", value = { @Result(property = "id", column = "id"),
			@Result(property = "name", column = "name"), @Result(property = "parentId", column = "parent_id"),
			@Result(property = "level", column = "level"), @Result(property = "status", column = "status"),
			@Result(property = "remark", column = "remark"), @Result(property = "creator", column = "creator"),
			@Result(property = "updater", column = "updater"), @Result(property = "createTime", column = "create_time"),
			@Result(property = "updateTime", column = "update_time") })
	@Select("SELECT g.id, g.name, g.parent_id, g.level, g.status, g.remark, g.creator, g.updater, g.create_time, g.update_time FROM sys_group g INNER JOIN sys_user_group ug ON g.id = ug.group_id WHERE ug.user_id = #{userId}")
	List<SysGroup> selectGroupsByUserId(@Param("userId") Long userId);

	@Insert("INSERT INTO sys_user (nickname, realname, gender, avatar, phone, email, idtype, idcode, motto, birthDate, education, location, biography, status, creator, create_time, updater, update_time) VALUES (#{user.nickname}, #{user.realname}, #{user.gender}, #{user.avatar}, #{user.phone}, #{user.email}, #{user.idtype}, #{user.idcode}, #{user.motto}, #{user.birthDate}, #{user.education}, #{user.location}, #{user.biography}, #{user.status}, #{user.creator}, #{user.createTime}, #{user.updater}, #{user.updateTime})")
	@Options(useGeneratedKeys = true, keyProperty = "user.id")
	Long create(@Param("user") SysUser user);

	@Update("<script>UPDATE sys_user <set><if test='user.nickname != null'>nickname = #{user.nickname},</if><if test='user.realname != null'>realname = #{user.realname},</if><if test='user.gender != null'>gender = #{user.gender},</if><if test='user.avatar != null'>avatar = #{user.avatar},</if><if test='user.phone != null'>phone = #{user.phone},</if><if test='user.email != null'>email = #{user.email},</if><if test='user.idtype != null'>idtype = #{user.idtype},</if><if test='user.idcode != null'>idcode = #{user.idcode},</if><if test='user.motto != null'>motto = #{user.motto},</if><if test='user.birthDate != null'>birthDate = #{user.birthDate},</if><if test='user.education != null'>education = #{user.education},</if><if test='user.location != null'>location = #{user.location},</if><if test='user.biography != null'>biography = #{user.biography},</if><if test='user.status != null'>status = #{user.status},</if><if test='user.updater != null'>updater = #{user.updater},</if>update_time = #{user.updateTime}</set> WHERE id = #{user.id}</script>")
	int update(@Param("user") SysUser user);

	@Update("UPDATE sys_user SET status = #{status}, update_time = #{updateTime} WHERE id = #{id}")
	int updateStatus(@Param("id") Long id, @Param("status") int status, @Param("updateTime") LocalDateTime updateTime);

	@Update("UPDATE sys_user SET update_time = #{loginTime} WHERE id = #{id}")
	int updateLoginTime(@Param("id") Long id, @Param("loginTime") LocalDateTime loginTime);

	@Delete("DELETE FROM sys_user WHERE id = #{id}")
	int remove(@Param("id") Long id);

	@Insert("INSERT INTO sys_user_group (user_id, group_id) VALUES (#{userId}, #{groupId})")
	void assignGroup(@Param("userId") Long userId, @Param("groupId") Integer groupId);

	@Insert("<script>INSERT INTO sys_user_group (user_id, group_id) VALUES "
			+ "<foreach collection='groupIds' item='gid' separator=','>" + "(#{userId}, #{gid})</foreach></script>")
	void assignGroupsBatch(@Param("userId") Long userId, @Param("groupIds") List<Long> groupIds);

	@Delete("DELETE FROM sys_user_group WHERE user_id = #{userId}")
	void removeGroups(@Param("userId") Long userId);

	@Select("SELECT DISTINCT ug.user_id FROM sys_role r INNER JOIN sys_group_role gr ON r.id = gr.role_id INNER JOIN sys_user_group ug ON gr.group_id = ug.group_id WHERE r.code = #{roleCode}")
	List<Long> selectUserIdsByRoleCode(@Param("roleCode") String roleCode);

	@Select("<script>" + "SELECT " + "<choose>"
			+ "  <when test='g.name() == \"DAY\" or g.name() == \"WEEK\"'>DATE(create_time) AS stat_day</when>"
			+ "  <otherwise>YEAR(create_time)*100+MONTH(create_time) AS yearMonth</otherwise>"
			+ "</choose>, COUNT(*) AS total " + "FROM sys_user WHERE create_time &gt;= #{start} AND status != "
			+ SystemConsts.USER_STATUS_DELETED + " " + "GROUP BY " + "<choose>"
			+ "  <when test='g.name() == \"DAY\" or g.name() == \"WEEK\"'>DATE(create_time)</when>"
			+ "  <otherwise>YEAR(create_time)*100+MONTH(create_time)</otherwise>" + "</choose>" + "</script>")
	List<StatisticEntry> countUsersByRange(@Param("g") StatisticUnit g, @Param("start") LocalDateTime start);
}
