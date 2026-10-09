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
package top.ruilink.inkwash.base;

import javax.sql.DataSource;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Database initializer that applies the H2 schema in the dev environment only.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Slf4j
@Component
@Profile("dev")
public class DatabaseInitializer implements CommandLineRunner {

	private final DataSource dataSource;

	public DatabaseInitializer(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	@Override
	public void run(String... args) throws Exception {
		log.info("Initializing database schema for dev...");

		ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
		populator.addScript(new FileSystemResource("database/h2-schema.sql"));
		populator.addScript(new FileSystemResource("database/init-data.sql"));
		populator.setContinueOnError(true);

		populator.execute(dataSource);

		log.info("Database schema and initial data loaded successfully");
	}
}
