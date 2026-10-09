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

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.Default;
import lombok.Data;
import top.ruilink.inkwash.base.enums.BaseStatus;

/**
 * User group request payload, a unified VO with validation groups.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Data
public class GroupParam {

	public interface Create extends Default {
	}

	public interface Update extends Default {
	}

	@NotNull(message = "用户组ID不能为空", groups = Update.class)
	private Integer id;

	@NotBlank(message = "用户组名称不能为空", groups = Create.class)
	@Size(min = 3, max = 50, message = "用户组名称长度必须在3-50之间")
	private String name;

	private Integer parentId;

	private BaseStatus status;

	private String remark;
}
