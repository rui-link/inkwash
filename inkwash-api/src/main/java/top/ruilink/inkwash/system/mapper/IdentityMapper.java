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

import top.ruilink.inkwash.base.enums.IdentityType;
import top.ruilink.inkwash.system.domain.SysIdentity;

/**
 * Identity claim persistence mapper.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Mapper
public interface IdentityMapper {

	@Results(id = "identityBaseResult", value = { @Result(property = "id", column = "id", id = true),
			@Result(property = "userId", column = "user_id"),
			@Result(property = "identityType", column = "identity_type"),
			@Result(property = "identityValue", column = "identity_value"),
			@Result(property = "provider", column = "provider"), @Result(property = "verified", column = "verified"),
			@Result(property = "verifyTime", column = "verified_at"),
			@Result(property = "verifier", column = "verified_by"), @Result(property = "status", column = "status"),
			@Result(property = "loginTime", column = "login_time"),
			@Result(property = "createTime", column = "create_time"),
			@Result(property = "updateTime", column = "update_time") })
	@Select("SELECT * FROM sys_identity WHERE identity_type = #{identityType} AND identity_value = #{identityValue}")
	SysIdentity selectByTypeValue(@Param("identityType") IdentityType identityType,
			@Param("identityValue") String identityValue);

	@Select("SELECT * FROM sys_identity WHERE identity_type = #{identityType} AND provider = #{provider} AND identity_value = #{identityValue}")
	@ResultMap("identityBaseResult")
	SysIdentity selectByTypeProviderValue(@Param("identityType") IdentityType identityType,
			@Param("provider") String provider, @Param("identityValue") String identityValue);

	@Select("SELECT * FROM sys_identity WHERE user_id = #{userId}")
	@ResultMap("identityBaseResult")
	List<SysIdentity> selectByUserId(@Param("userId") Long userId);

	@Insert("INSERT INTO sys_identity (user_id, identity_type, identity_value, provider, verified, verified_at, verified_by, status, login_time, create_time, update_time) VALUES (#{userId}, #{identityType}, #{identityValue}, #{provider}, #{verified}, #{verifyTime}, #{verifier}, #{status}, #{loginTime}, #{createTime}, #{updateTime})")
	@Options(useGeneratedKeys = true, keyProperty = "id")
	int create(SysIdentity identity);

	@Update("UPDATE sys_identity SET user_id = #{userId}, identity_type = #{identityType}, identity_value = #{identityValue}, provider = #{provider}, verified = #{verified}, verified_at = #{verifyTime}, verified_by = #{verifier}, status = #{status}, login_time = #{loginTime}, update_time = #{updateTime} WHERE id = #{id}")
	int update(SysIdentity identity);

	@Delete("DELETE FROM sys_identity WHERE id = #{id}")
	int deleteById(@Param("id") Long id);
}
