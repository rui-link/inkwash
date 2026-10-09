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

import java.time.Instant;

import lombok.Data;

/**
 * Application build metadata response view, served by
 * {@code GET /api/system/meta} and {@code GET /api/system/about}.
 *
 * <p>
 * Before ISS-047 both endpoints returned the {@code system.domain.SysMeta}
 * entity directly, which meant any field added to that domain object would
 * silently become part of the public response contract. The field names below
 * are deliberately identical to {@code SysMeta}'s so the wire format is
 * unchanged — the frontend ({@code useSiteStore}, {@code AboutDialog}) keeps
 * reading {@code shortName}, {@code projectName}, {@code formalName},
 * {@code description} and {@code version}.
 *
 * <p>
 * {@code spring.jackson.default-property-inclusion: non-null} drops the fields
 * an endpoint did not populate, which is why {@code /meta} omits
 * {@code springBoot} and {@code license} while {@code /about} includes them.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Data
public class SysMetaView {

	private String groupId;
	private String artifactId;
	private String projectName;
	private String version;
	private Instant buildTime;

	// Custom fields, coming from BuildProperties additionalProperties
	private String formalName;
	private String shortName;
	private String copyright;
	private String author;
	private String javaVersion;

	// About page only
	private String springBoot;
	private String description;
	private String license;
}