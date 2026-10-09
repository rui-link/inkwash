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

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Preference save payload.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Data
public class PreferenceParam {
	/**
	 * Sole validation group: this Param has no create/update differences (D-20).
	 *
	 * <p>
	 * Declared explicitly so that "no differences" becomes a stated fact rather
	 * than an ambiguous absence of grouping. Constraints on this class carry no
	 * {@code groups} attribute and therefore belong to
	 * {@code jakarta.validation.groups.Default}, of which this interface is a
	 * subtype, so both {@code @Valid} and
	 * {@code @Validated(PreferenceParam.Default.class)} validate every constraint
	 * declared here.
	 *
	 * <p>
	 * The supertype is fully qualified because the simple name {@code Default}
	 * would shadow the {@code jakarta.validation.groups.Default} import inside this
	 * class body.
	 */
	public interface Default extends jakarta.validation.groups.Default {
	}

	@Size(max = 20, message = "主题ID长度不能超过20")
	private String theme;
	@Size(max = 10, message = "语言代码长度不能超过10")
	private String language;
	@Size(max = 20, message = "菜单样式长度不能超过20")
	private String menuStyle;
	private Boolean showTabs;
	private Boolean showLogo;
	private Boolean showFooter;
	private Boolean fixHeader;
}
