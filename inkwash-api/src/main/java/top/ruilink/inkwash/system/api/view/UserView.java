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
package top.ruilink.inkwash.system.api.view;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.Data;
import top.ruilink.inkwash.base.annotation.DataMask;
import top.ruilink.inkwash.base.annotation.DataMask.MaskType;
import top.ruilink.inkwash.base.domain.ResultView;
import top.ruilink.inkwash.base.enums.Gender;
import top.ruilink.inkwash.security.api.view.AccountView;
import top.ruilink.inkwash.system.enums.IdType;
import top.ruilink.inkwash.system.enums.UserEducation;
import top.ruilink.inkwash.system.enums.UserStatus;

/**
 * User view supporting the Basic and Detail display scenarios.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Data
public class UserView {

	// ========== Basic view fields (Basic Summary) ==========

	@JsonView(ResultView.Basic.class)
	private Long id;

	@JsonView(ResultView.Basic.class)
	private String nickname;

	@JsonView(ResultView.Basic.class)
	@DataMask(value = MaskType.NAME, exemptForSelf = true)
	private String realname;

	@JsonView(ResultView.Basic.class)
	private String avatar;

	@JsonView(ResultView.Basic.class)
	@DataMask(value = MaskType.PHONE, exemptForSelf = true)
	private String phone;

	@JsonView(ResultView.Basic.class)
	@DataMask(value = MaskType.EMAIL, exemptForSelf = true)
	private String email;

	@JsonView(ResultView.Basic.class)
	private Gender gender;

	@JsonView(ResultView.Basic.class)
	private UserStatus status;

	@JsonView(ResultView.Basic.class)
	private IdType idtype;

	@JsonView(ResultView.Basic.class)
	@DataMask(value = MaskType.ID_CARD, exemptForSelf = true)
	private String idcode;

	@JsonView(ResultView.Basic.class)
	private String motto;

	@JsonView(ResultView.Basic.class)
	private LocalDate birthDate;

	@JsonView(ResultView.Basic.class)
	private UserEducation education;

	@JsonView(ResultView.Basic.class)
	private String location;

	@JsonView(ResultView.Basic.class)
	private LocalDateTime lastLoginTime;

	@JsonView(ResultView.Detail.class)
	private String biography;

	@JsonView(ResultView.Detail.class)
	private Set<String> permissions;

	@JsonView(ResultView.Detail.class)
	private Set<GroupView> groups;

	@JsonView(ResultView.Detail.class)
	private List<String> roles;

	@JsonView(ResultView.Basic.class)
	private List<AccountView> accounts;

	@JsonView(ResultView.Basic.class)
	private LocalDateTime createTime;

	@JsonView(ResultView.Basic.class)
	private LocalDateTime updateTime;
}
