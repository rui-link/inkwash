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

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import top.ruilink.inkwash.system.domain.SysPreference;

/**
 * User preference persistence mapper.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Mapper
public interface PreferenceMapper {

	@Results(id = "prefResult", value = { @Result(property = "userId", column = "user_id"),
			@Result(property = "theme", column = "theme"), @Result(property = "language", column = "language"),
			@Result(property = "menuStyle", column = "menu_style"), @Result(property = "options", column = "options"),
			@Result(property = "createTime", column = "create_time"),
			@Result(property = "updateTime", column = "update_time") })
	@Select("SELECT user_id, theme, language, menu_style, options, create_time, update_time FROM sys_preference WHERE user_id = #{userId}")
	SysPreference selectByUserId(@Param("userId") Long userId);

	@Insert("INSERT INTO sys_preference (user_id, theme, language, menu_style, options, create_time, update_time) VALUES (#{p.userId}, #{p.theme}, #{p.language}, #{p.menuStyle}, #{p.options}, #{p.createTime}, #{p.updateTime})")
	int insert(@Param("p") SysPreference preference);

	@Update("UPDATE sys_preference SET theme = #{p.theme}, language = #{p.language}, menu_style = #{p.menuStyle}, options = #{p.options}, update_time = #{p.updateTime} WHERE user_id = #{p.userId}")
	int update(@Param("p") SysPreference preference);
}
