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
package top.ruilink.inkwash.security.jwt;

import java.io.OutputStream;

import io.jsonwebtoken.io.AbstractSerializer;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * JJWT JSON serializer built on Jackson 3 (tools.jackson), replacing the
 * removed jjwt-jackson.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public class Jackson3Serializer<T> extends AbstractSerializer<T> {

	private final ObjectMapper objectMapper;

	public Jackson3Serializer() {
		this(JsonMapper.builder().build());
	}

	public Jackson3Serializer(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	protected void doSerialize(T value, OutputStream out) throws Exception {
		objectMapper.writeValue(out, value);
	}
}
