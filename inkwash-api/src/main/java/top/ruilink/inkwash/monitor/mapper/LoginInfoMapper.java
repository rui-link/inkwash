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

import top.ruilink.inkwash.monitor.domain.LoginInfo;

/**
 * Login log persistence mapper.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Mapper
public interface LoginInfoMapper {

	@Insert("INSERT INTO mon_login_info (user_id, identity, login_type, address, device, "
			+ "browser, ostype, status, message, login_time) "
			+ "VALUES (#{userId}, #{identity}, #{loginType}, #{address}, #{device}, "
			+ "#{browser}, #{ostype}, #{status}, #{message}, #{loginTime})")
	@Options(useGeneratedKeys = true, keyProperty = "id")
	Long create(LoginInfo loginInfo);

	@Select("SELECT id, user_id, identity, login_type, address, device, browser, ostype, "
			+ "status, message, login_time, logout_time, create_time FROM mon_login_info WHERE id = #{id}")
	@Results(id = "loginInfoResult", value = { @Result(property = "id", column = "id"),
			@Result(property = "userId", column = "user_id"), @Result(property = "identity", column = "identity"),
			@Result(property = "loginType", column = "login_type"), @Result(property = "address", column = "address"),
			@Result(property = "device", column = "device"), @Result(property = "browser", column = "browser"),
			@Result(property = "ostype", column = "ostype"), @Result(property = "status", column = "status"),
			@Result(property = "message", column = "message"), @Result(property = "loginTime", column = "login_time"),
			@Result(property = "logoutTime", column = "logout_time"),
			@Result(property = "createTime", column = "create_time") })
	LoginInfo selectById(@Param("id") Long id);

	/**
	 * Closes the most recent still-open login session of a user by stamping its
	 * logout time.
	 *
	 * <p>
	 * Inkwash issues one {@code HttpOnly} cookie pair per browser, so a user has at
	 * most one active session per browser and "the latest row without a logout
	 * time" identifies it. If concurrent multi-device sessions are ever supported,
	 * this must instead correlate on the refresh-token family id.
	 *
	 * @return number of rows closed (0 when no open session was found)
	 */
	@Update("UPDATE mon_login_info SET logout_time = #{logoutTime} "
			+ "WHERE user_id = #{userId} AND logout_time IS NULL " + "ORDER BY login_time DESC, id DESC LIMIT 1")
	int closeLatestOpenSession(@Param("userId") Long userId, @Param("logoutTime") LocalDateTime logoutTime);

	@Select("<script>SELECT id, user_id, identity, login_type, address, device, browser, ostype, "
			+ "status, message, login_time, logout_time, create_time FROM mon_login_info " + "<where>"
			+ "<if test='identity != null and identity != \"\"'>AND identity LIKE CONCAT('%', #{identity}, '%')</if>"
			+ "<if test='status != null and status != \"\"'>AND status = #{status}</if>"
			+ "<if test='startTime != null'>AND login_time &gt;= #{startTime}</if>"
			+ "<if test='endTime != null'>AND login_time &lt;= #{endTime}</if>" + "</where>"
			+ "ORDER BY login_time DESC</script>")
	@ResultMap("loginInfoResult")
	List<LoginInfo> selectList(@Param("identity") String identity, @Param("status") String status,
			@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

	@Select("<script>SELECT id, user_id, identity, login_type, address, device, browser, ostype, "
			+ "status, message, login_time, logout_time, create_time FROM mon_login_info " + "<where>"
			+ "<if test='identity != null and identity != \"\"'>AND identity LIKE CONCAT('%', #{identity}, '%')</if>"
			+ "<if test='status != null and status != \"\"'>AND status = #{status}</if>"
			+ "<if test='startTime != null'>AND login_time &gt;= #{startTime}</if>"
			+ "<if test='endTime != null'>AND login_time &lt;= #{endTime}</if>" + "</where>"
			+ "ORDER BY login_time DESC LIMIT #{limit} OFFSET #{offset}</script>")
	@ResultMap("loginInfoResult")
	List<LoginInfo> selectPage(@Param("identity") String identity, @Param("status") String status,
			@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime,
			@Param("offset") int offset, @Param("limit") int limit);

	@Select("<script>SELECT COUNT(*) FROM mon_login_info " + "<where>"
			+ "<if test='identity != null and identity != \"\"'>AND identity LIKE CONCAT('%', #{identity}, '%')</if>"
			+ "<if test='status != null and status != \"\"'>AND status = #{status}</if>"
			+ "<if test='startTime != null'>AND login_time &gt;= #{startTime}</if>"
			+ "<if test='endTime != null'>AND login_time &lt;= #{endTime}</if>" + "</where></script>")
	long count(@Param("identity") String identity, @Param("status") String status,
			@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

	@Delete("DELETE FROM mon_login_info WHERE id = #{id}")
	int deleteById(@Param("id") Long id);

	@Delete("<script>DELETE FROM mon_login_info WHERE id IN "
			+ "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
	int deleteByIds(@Param("ids") List<Long> ids);

	@Delete("DELETE FROM mon_login_info")
	void deleteAll();

	@Select("SELECT id, user_id, identity, login_type, address, device, browser, ostype, "
			+ "status, message, login_time, logout_time, create_time FROM mon_login_info ORDER BY login_time DESC LIMIT #{limit}")
	@ResultMap("loginInfoResult")
	List<LoginInfo> selectRecent(@Param("limit") int limit);
}
