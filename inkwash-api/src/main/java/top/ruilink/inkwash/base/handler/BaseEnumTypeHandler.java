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
package top.ruilink.inkwash.base.handler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import top.ruilink.inkwash.base.enums.BaseEnum;

/**
 * Generic enum type handler supporting every enum that implements BaseEnum.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@MappedTypes(BaseEnum.class)
public class BaseEnumTypeHandler<E extends Enum<E> & BaseEnum> extends BaseTypeHandler<E> {

	private Class<E> enumClass;

	public BaseEnumTypeHandler() {
	}

	public BaseEnumTypeHandler(Class<E> enumClass) {
		this.enumClass = enumClass;
	}

	public void setEnumClass(Class<E> enumClass) {
		this.enumClass = enumClass;
	}

	/**
	 * Writing to the database: enum to code.
	 */
	@Override
	public void setNonNullParameter(PreparedStatement ps, int i, E parameter, JdbcType jdbcType) throws SQLException {
		ps.setInt(i, parameter.getCode());
	}

	/**
	 * Reading from the database: code to enum.
	 */
	@Override
	public E getNullableResult(ResultSet rs, String columnName) throws SQLException {
		int code = rs.getInt(columnName);
		return rs.wasNull() ? null : BaseEnum.fromCode(enumClass, code);
	}

	@Override
	public E getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
		int code = rs.getInt(columnIndex);
		return rs.wasNull() ? null : BaseEnum.fromCode(enumClass, code);
	}

	@Override
	public E getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
		int code = cs.getInt(columnIndex);
		return cs.wasNull() ? null : BaseEnum.fromCode(enumClass, code);
	}

	@Override
	public void setParameter(PreparedStatement ps, int i, E parameter, JdbcType jdbcType) throws SQLException {
		if (parameter == null) {
			ps.setNull(i, jdbcType.TYPE_CODE);
		} else {
			ps.setInt(i, parameter.getCode());
		}
	}
}
