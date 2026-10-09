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
package top.ruilink.inkwash.security.api.param;

import java.time.LocalDate;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import top.ruilink.inkwash.base.enums.Gender;
import top.ruilink.inkwash.system.enums.IdType;
import top.ruilink.inkwash.system.enums.UserEducation;

/**
 * Partial update payload for the current user's profile.
 * <p>
 * Only editable profile fields are present; id and username come from the
 * current user and the password is handled by the change password endpoint.
 * Fields left null are not updated. Phone and email go through the dedicated
 * verification endpoints rather than being changed here directly.
 * 
 * @author Dyllon
 * @since 0.5.1
 */
@Data
public class ProfileParam {
	/**
	 * Sole validation group: this Param has no create/update differences (D-20).
	 *
	 * <p>
	 * Declared explicitly so that "no differences" becomes a stated fact rather
	 * than an ambiguous absence of grouping. Constraints on this class carry no
	 * {@code groups} attribute and therefore belong to
	 * {@code jakarta.validation.groups.Default}, of which this interface is a
	 * subtype, so both {@code @Valid} and
	 * {@code @Validated(ProfileParam.Default.class)} validate every constraint
	 * declared here.
	 *
	 * <p>
	 * The supertype is fully qualified because the simple name {@code Default}
	 * would shadow the {@code jakarta.validation.groups.Default} import inside this
	 * class body.
	 */
	public interface Default extends jakarta.validation.groups.Default {
	}

	@Pattern(regexp = "^[a-zA-Z0-9_\\u4e00-\\u9fa5]{2,50}$", message = "昵称长度必须在2-50之间且只能包含字母、数字、下划线和中文")
	private String nickname;

	@Size(max = 50, message = "真实姓名长度不能超过50")
	private String realname;

	private Gender gender;

	private String avatar;

	private IdType idtype;

	@Size(max = 50, message = "证件编号长度不能超过50")
	private String idcode;

	@Size(max = 200, message = "座右铭长度不能超过200")
	private String motto;

	private LocalDate birthDate;

	private UserEducation education;

	@Size(max = 100, message = "籍贯长度不能超过100")
	private String location;

	@Size(max = 2000, message = "个人介绍长度不能超过2000")
	private String biography;
}
