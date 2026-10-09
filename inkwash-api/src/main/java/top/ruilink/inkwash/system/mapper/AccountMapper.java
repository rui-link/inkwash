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

import top.ruilink.inkwash.base.enums.AuthType;
import top.ruilink.inkwash.system.handler.CredentialTypeHandler;
import top.ruilink.inkwash.system.domain.SysAccount;

/**
 * Login account persistence mapper.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Mapper
public interface AccountMapper {

	@Results(id = "baseResult", value = { @Result(property = "id", column = "id"),
			@Result(property = "userId", column = "user_id"), @Result(property = "identity", column = "identity"),
			@Result(property = "authType", column = "auth_type"),
			@Result(property = "credential", column = "credential", typeHandler = CredentialTypeHandler.class),
			@Result(property = "status", column = "status"), @Result(property = "expiration", column = "expiration"),
			@Result(property = "loginTime", column = "login_time"),
			@Result(property = "createTime", column = "create_time"),
			@Result(property = "updateTime", column = "update_time") })
	@Select("SELECT * FROM sys_account WHERE id = #{id}")
	SysAccount selectById(Long id);

	@Select("SELECT * FROM sys_account WHERE identity = #{identity} AND auth_type = #{authType}")
	@ResultMap("baseResult")
	SysAccount selectByIdentityAndType(@Param("identity") String identity, @Param("authType") AuthType authType);

	@Insert("INSERT INTO sys_account (user_id, identity, auth_type, credential, login_time, create_time, update_time) VALUES (#{userId}, #{identity}, #{authType}, #{credential,typeHandler=top.ruilink.inkwash.system.handler.CredentialTypeHandler}, #{loginTime}, #{createTime}, #{updateTime})")
	@Options(useGeneratedKeys = true, keyProperty = "id")
	Long create(SysAccount account);

	@Update("UPDATE sys_account SET status = #{status} WHERE id = #{accountId}")
	void updateAccountStatus(@Param("accountId") Long accountId, @Param("status") Integer status);

	@Update("UPDATE sys_account SET user_id = #{userId}, identity = #{identity}, auth_type = #{authType}, credential = #{credential,typeHandler=top.ruilink.inkwash.system.handler.CredentialTypeHandler}, status = #{status}, expiration = #{expiration}, login_time = #{loginTime}, update_time = #{updateTime} WHERE id = #{id}")
	int update(SysAccount account);

	@Select("SELECT * FROM sys_account WHERE user_id = #{userId}")
	@ResultMap("baseResult")
	List<SysAccount> selectByUserId(@Param("userId") Long userId);

	@Select("<script>SELECT * FROM sys_account WHERE user_id IN "
			+ "<foreach collection='userIds' item='id' open='(' separator=',' close=')'>" + "#{id}" + "</foreach>"
			+ "</script>")
	@ResultMap("baseResult")
	List<SysAccount> selectByUserIds(@Param("userIds") List<Long> userIds);

	@Select("SELECT COUNT(*) FROM sys_account WHERE user_id = #{userId}")
	int countByUserId(@Param("userId") Long userId);

	@Delete("DELETE FROM sys_account WHERE id = #{id}")
	int deleteById(@Param("id") Long id);
}
