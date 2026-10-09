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
package top.ruilink.inkwash.security.license;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * License configuration properties bound to the license section of
 * application.yaml.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Data
@Component
@ConfigurationProperties(prefix = "license")
public class LicenseConfig {
	/**
	 * Signature cache time in minutes.
	 */
	private Long cacheTtlMinutes = 720L;
	/**
	 * Paths excluded from license validation.
	 */
	private List<String> excludedPaths = new ArrayList<>(
			List.of("/api/license/", "/api/auth/", "/api/cms/articles", "/api/system/meta", "/health", "/error"));
}
