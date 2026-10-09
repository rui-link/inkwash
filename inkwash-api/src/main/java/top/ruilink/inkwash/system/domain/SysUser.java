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

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;
import top.ruilink.inkwash.base.annotation.DataMask;
import top.ruilink.inkwash.base.annotation.DataMask.MaskType;
import top.ruilink.inkwash.base.domain.BaseEntity;
import top.ruilink.inkwash.base.enums.Gender;
import top.ruilink.inkwash.system.enums.IdType;
import top.ruilink.inkwash.system.enums.UserEducation;
import top.ruilink.inkwash.system.enums.UserStatus;

/**
 * User domain object.
 * 
 * @author Dyllon
 * @date 2025-09-21
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Getter
@Setter
public class SysUser extends BaseEntity {
	private static final long serialVersionUID = 2003697041757590403L;
	/**
	 * User ID.
	 *
	 * <p>
	 * Not annotated with {@code @DataMask}: the annotation drives a {@code String}
	 * serializer and would corrupt a numeric id. Ownership opt-in lives on the
	 * masked fields instead (see {@code @DataMask(exemptForSelf = true)}). —
	 * ISS-049 / D-13
	 */
	private Long id;
	/**
	 * Nickname.
	 */
	private String nickname;
	/**
	 * Real name.
	 */
	@DataMask(value = MaskType.NAME, exemptForSelf = true)
	private String realname;
	/**
	 * Gender.
	 */
	private Gender gender;
	/**
	 * User avatar.
	 */
	private String avatar;
	/**
	 * User phone number.
	 */
	@DataMask(value = MaskType.PHONE, exemptForSelf = true)
	private String phone;
	/**
	 * User email address.
	 */
	@DataMask(value = MaskType.EMAIL, exemptForSelf = true)
	private String email;
	/**
	 * User status.
	 */
	private UserStatus status;

	/**
	 * Identity document type.
	 */
	private IdType idtype;
	/**
	 * Identity document number.
	 */
	@DataMask(value = MaskType.ID_CARD, exemptForSelf = true)
	private String idcode;
	/**
	 * Personal motto.
	 */
	private String motto;
	/**
	 * Date of birth.
	 */
	private LocalDate birthDate;
	/**
	 * Education level.
	 */
	private UserEducation education;
	/**
	 * Region.
	 */
	private String location;
	/**
	 * Biography.
	 */
	private String biography;
	/**
	 * Groups the user belongs to.
	 */
	private Set<SysGroup> groups = new HashSet<>();

	/**
	 * Enables the account.
	 */
	public void enable() {
		this.status = UserStatus.ENABLE;
	}

	/**
	 * Disables the account.
	 */
	public void disable() {
		this.status = UserStatus.DISABLE;
	}

	/**
	 * Whether the account is locked.
	 */
	public boolean isDisabled() {
		return UserStatus.DISABLE.equals(this.status);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		SysUser sysUser = (SysUser) o;
		return Objects.equals(id, sysUser.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}
