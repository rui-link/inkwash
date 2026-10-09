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

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.springframework.util.StringUtils;

import lombok.Getter;
import lombok.Setter;
import top.ruilink.inkwash.base.domain.BaseEntity;
import top.ruilink.inkwash.base.enums.BaseStatus;
import top.ruilink.inkwash.base.exception.BusinessException;

/**
 * User group entity.
 * 
 * @author Dyllon
 * @date 2025-10-01
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Getter
@Setter
public class SysGroup extends BaseEntity {
	private static final long serialVersionUID = 80989805485550416L;

	/**
	 * User group ID.
	 */
	private Integer id;

	/**
	 * User group name.
	 */
	private String name;

	/**
	 * Parent user group ID, where 0 means the root.
	 */
	private Integer parentId;
	/**
	 * User group level.
	 */
	private Integer level;
	/**
	 * Status.
	 */
	private BaseStatus status;
	/**
	 * Remark.
	 */
	private String remark;
	/**
	 * Roles held by the user group.
	 */
	private Set<SysRole> roles = new HashSet<>();

	/**
	 * Business behaviour: assign a role.
	 */
	public void assignRole(SysRole role) {
		if (role == null) {
			throw new BusinessException("error.role.id_required");
		}
		if (this.roles.contains(role)) {
			throw new BusinessException("error.group.role_already_bound");
		}
		this.roles.add(role);
		setUpdateTime(LocalDateTime.now());
	}

	/**
	 * Business behaviour: remove a role.
	 */
	public void removeRole(SysRole role) {
		if (!this.roles.contains(role)) {
			throw new BusinessException("error.group.role_not_bound");
		}
		this.roles.remove(role);
		setUpdateTime(LocalDateTime.now());
	}

	// Validation logic
	public void setName(String name) {
		if (!StringUtils.hasText(name)) {
			throw new BusinessException("error.group.name_required");
		}
		this.name = name;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		SysGroup sysGroup = (SysGroup) o;
		return Objects.equals(id, sysGroup.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}
