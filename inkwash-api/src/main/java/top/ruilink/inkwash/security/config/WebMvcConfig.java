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
package top.ruilink.inkwash.security.config;

import java.util.Locale;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import top.ruilink.inkwash.security.license.LicenseInterceptor;

/**
 * Web MVC Configuration Registers interceptors for request processing
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

	private final LicenseInterceptor licenseInterceptor;

	public WebMvcConfig(LicenseInterceptor licenseInterceptor) {
		this.licenseInterceptor = licenseInterceptor;
	}

	@Bean
	public LocaleResolver localeResolver() {
		AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
		resolver.setDefaultLocale(Locale.CHINA);
		return resolver;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		// Register license interceptor for admin API endpoints (system + cms admin)
		registry.addInterceptor(licenseInterceptor)
				// Check system, cms, and monitor modules
				.addPathPatterns("/api/system/**").addPathPatterns("/api/cms/**").addPathPatterns("/api/monitor/**")
				// Exclude license management endpoints themselves
				.excludePathPatterns("/api/license/**");
	}
}
