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
import top.ruilink.inkwash.system.enums.MenuType;

/**
 * Permission request payload, a unified VO with validation groups.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Data
public class PermissionParam {

	public interface Create extends Default {
	}

	public interface Update extends Default {
	}

	@NotNull(message = "权限ID不能为空", groups = Update.class)
	private Integer id;

	private MenuType type;

	@NotBlank(message = "权限名称不能为空", groups = Create.class)
	@Size(min = 2, max = 60, message = "权限名称长度必须在2-60之间")
	private String name;

	private String module;

	private String resource;

	private String action;

	private BaseStatus status;

	private String remark;
}
