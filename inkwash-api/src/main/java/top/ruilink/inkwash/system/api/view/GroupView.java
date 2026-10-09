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

import java.time.LocalDateTime;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.Data;
import top.ruilink.inkwash.base.domain.ResultView;
import top.ruilink.inkwash.base.enums.BaseStatus;

/**
 * User group view supporting the Basic and Detail display scenarios.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Data
public class GroupView {
	@JsonView(ResultView.Basic.class)
	private Integer id;

	@JsonView(ResultView.Basic.class)
	private String name;

	@JsonView(ResultView.Basic.class)
	private Integer parentId;

	@JsonView(ResultView.Basic.class)
	private Integer level;

	@JsonView(ResultView.Basic.class)
	private BaseStatus status;

	@JsonView(ResultView.Detail.class)
	private String remark;

	@JsonView(ResultView.Detail.class)
	private Set<RoleView> roles;

	@JsonView(ResultView.Detail.class)
	private String creator;

	@JsonView(ResultView.Detail.class)
	private String updater;

	@JsonView(ResultView.Detail.class)
	private LocalDateTime createTime;

	@JsonView(ResultView.Basic.class)
	private LocalDateTime updateTime;
}
