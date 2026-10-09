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
package top.ruilink.inkwash.base.config;

import java.util.Properties;

import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.mapping.VendorDatabaseIdProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes the active database as MyBatis' {@code _databaseId} so that a mapper
 * can keep one SQL statement while still branching on dialect.
 *
 * <p>
 * Statistics queries need this: MySQL spells a week-bucket offset as
 * {@code DATE_SUB(d, INTERVAL n DAY)} while H2 spells it
 * {@code DATEADD('DAY', -n, d)} and additionally inverts the sign of
 * {@code DATEDIFF}. Both must produce identical buckets, so the expressions are
 * selected per vendor rather than degraded to a lowest-common-denominator form.
 *
 * <p>
 * This class cannot be replaced by MyBatis-Spring-Boot-Starter configuration.
 * The starter's only auto-configurations are {@code MybatisAutoConfiguration}
 * and {@code MybatisLanguageDriverAutoConfiguration}, neither of which builds a
 * {@link DatabaseIdProvider}, and there is no
 * {@code mybatis.database-id-provider} property to point at one. Declaring the
 * bean is therefore the only way to get {@code _databaseId}.
 *
 * <p>
 * When no vendor matches, MyBatis leaves {@code _databaseId} {@code null} and
 * every {@code <when test="_databaseId == '...'">} falls through to
 * {@code <otherwise>}, which is deliberately the MySQL form to keep production
 * behaviour unchanged.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Configuration
public class MybatisConfig {

	@Bean
	public DatabaseIdProvider databaseIdProvider() {
		VendorDatabaseIdProvider provider = new VendorDatabaseIdProvider();
		Properties vendors = new Properties();
		vendors.setProperty("H2", "h2");
		vendors.setProperty("MySQL", "mysql");
		vendors.setProperty("MariaDB", "mysql");
		provider.setProperties(vendors);
		return provider;
	}
}
