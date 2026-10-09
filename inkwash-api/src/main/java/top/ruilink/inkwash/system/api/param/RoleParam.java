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
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.Default;
import lombok.Data;
import top.ruilink.inkwash.base.enums.BaseStatus;

/**
 * Create role request payload.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Data
public class RoleParam {
	public interface Create extends Default {
	}

	public interface Update extends Default {
	}

	private Integer id;

	@NotBlank(message = "角色编码不能为空", groups = Create.class)
	@Pattern(regexp = "^ROLE_\\w+$", message = "角色编码必须以ROLE_开头")
	private String code;

	@NotBlank(message = "角色名不能为空", groups = Create.class)
	@Size(min = 2, max = 50, message = "角色名称长度必须在2-50之间")
	private String name;

	private BaseStatus status;

	private String remark;
}
