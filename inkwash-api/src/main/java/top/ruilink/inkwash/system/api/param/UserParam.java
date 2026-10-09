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
package top.ruilink.inkwash.system.api.param;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.Default;
import lombok.Data;
import top.ruilink.inkwash.base.enums.Gender;
import top.ruilink.inkwash.system.enums.IdType;
import top.ruilink.inkwash.system.enums.UserEducation;

/**
 * User request payload, a unified VO with validation groups.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Data
public class UserParam {

	public interface Create extends Default {
	}

	public interface Update extends Default {
	}

	@NotNull(message = "用户ID不能为空", groups = Update.class)
	private Long id;

	@NotBlank(message = "昵称不能为空")
	@Size(min = 2, max = 50, message = "昵称长度必须在2-50之间")
	@Pattern(regexp = "^[a-zA-Z0-9_\\u4e00-\\u9fa5]+$", message = "昵称只能包含字母、数字、下划线和中文")
	private String nickname;

	@NotBlank(message = "用户名不能为空", groups = Create.class)
	@Size(min = 3, max = 30, message = "用户名长度必须在3-30之间", groups = { Create.class, Update.class })
	private String username;

	@NotBlank(message = "密码不能为空", groups = Create.class)
	@Size(min = 6, max = 100, message = "密码长度必须在6-100之间", groups = { Create.class, Update.class })
	private String password;

	@Size(max = 50, message = "真实姓名长度不能超过50")
	private String realname;

	private Gender gender;

	@Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
	private String phone;

	@Email(message = "邮箱格式不正确")
	private String email;

	private String avatar;

	private IdType idtype;

	@Size(max = 50, message = "证件编号长度不能超过50")
	private String idcode;

	private String motto;

	private LocalDate birthDate;

	private UserEducation education;

	private String location;

	private String biography;
}
