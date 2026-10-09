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
package top.ruilink.inkwash.system.domain;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;
import top.ruilink.inkwash.base.domain.BaseEntity;
import top.ruilink.inkwash.base.enums.BaseStatus;

/**
 * Role entity.
 * 
 * @author Dyllon
 * @date 2025-09-21
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Getter
@Setter
public class SysRole extends BaseEntity {
	private static final long serialVersionUID = 1353596573717264573L;
	/**
	 * Role ID.
	 */
	private Integer id;
	/**
	 * Role name.
	 */
	private String name;
	/**
	 * Role code.
	 */
	private String code;
	/**
	 * Role remark.
	 */
	private String remark;
	/**
	 * Role status.
	 */
	private BaseStatus status;
	/**
	 * Associated user group ID, populated only when querying by user group.
	 */
	private Integer groupId;
	/**
	 * Role permissions.
	 */
	private Set<SysPermission> permissions = new HashSet<>();

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		SysRole sysRole = (SysRole) o;
		return Objects.equals(id, sysRole.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}
