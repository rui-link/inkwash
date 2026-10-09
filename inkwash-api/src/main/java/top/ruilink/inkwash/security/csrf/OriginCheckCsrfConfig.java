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
package top.ruilink.inkwash.security.csrf;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.ruilink.inkwash.security.config.AuthCookieConfig;
import top.ruilink.inkwash.security.config.CorsConfig;

/**
 * H-FS-3: registers OriginCheckCsrfFilter as an injectable bean for
 * SecurityConfig constructor injection. A disabled FilterRegistrationBean stops
 * Spring Boot from also registering it as a global servlet filter, since the
 * security chain adds it with addFilterBefore.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Configuration
public class OriginCheckCsrfConfig {

	@Bean
	public OriginCheckCsrfFilter originCheckCsrfFilter(CorsConfig corsConfig, AuthCookieConfig cookieProperties) {
		return new OriginCheckCsrfFilter(corsConfig, cookieProperties);
	}

	@Bean
	public FilterRegistrationBean<OriginCheckCsrfFilter> originCheckCsrfFilterRegistration(
			OriginCheckCsrfFilter originCheckCsrfFilter) {
		FilterRegistrationBean<OriginCheckCsrfFilter> registration = new FilterRegistrationBean<>(
				originCheckCsrfFilter);
		registration.setEnabled(false);
		return registration;
	}
}
