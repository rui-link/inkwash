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
package top.ruilink.inkwash.system.handler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import top.ruilink.inkwash.base.enums.AuthType;
import top.ruilink.inkwash.base.enums.OAuth2Provider;
import top.ruilink.inkwash.system.domain.credential.Credential;
import top.ruilink.inkwash.system.domain.credential.OAuth2Credential;
import top.ruilink.inkwash.system.domain.credential.PasswordCredential;
import top.ruilink.inkwash.system.domain.credential.QRCredential;

/**
 * Credential JSON column type handler.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@MappedTypes(Credential.class)
public class CredentialTypeHandler extends BaseTypeHandler<Credential> {

	private static final JsonMapper objectMapper;

	static {
		objectMapper = JsonMapper.builder().build();
	}

	@Override
	public void setNonNullParameter(PreparedStatement ps, int i, Credential parameter, JdbcType jdbcType)
			throws SQLException {
		try {
			String json = objectMapper.writeValueAsString(parameter);
			ps.setString(i, json);
		} catch (Exception e) {
			throw new RuntimeException("Failed to serialize Credential", e);
		}
	}

	@Override
	public Credential getNullableResult(ResultSet rs, String columnName) throws SQLException {
		String credentialJson = rs.getString(columnName);
		if (credentialJson == null || credentialJson.isEmpty()) {
			return null;
		}
		try {
			int authTypeCode = rs.getInt("auth_type");
			AuthType authType = AuthType.fromCode(authTypeCode);
			JsonNode jsonNode = objectMapper.readTree(credentialJson);
			return mapToCredential(jsonNode, authType);
		} catch (Exception e) {
			throw new RuntimeException("Failed to deserialize Credential from column " + columnName, e);
		}
	}

	@Override
	public Credential getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
		String credentialJson = rs.getString(columnIndex);
		if (credentialJson == null || credentialJson.isEmpty()) {
			return null;
		}
		try {
			int authTypeCode;
			try {
				authTypeCode = rs.getInt("auth_type");
			} catch (SQLException e) {
				throw new SQLException(
						"Unable to read auth_type column for credential deserialization at column index " + columnIndex,
						e);
			}
			AuthType authType = AuthType.fromCode(authTypeCode);
			JsonNode jsonNode = objectMapper.readTree(credentialJson);
			return mapToCredential(jsonNode, authType);
		} catch (SQLException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("Failed to deserialize Credential from column index " + columnIndex, e);
		}
	}

	@Override
	public Credential getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
		String credentialJson = cs.getString(columnIndex);
		if (credentialJson == null || credentialJson.isEmpty()) {
			return null;
		}
		try {
			int authTypeCode = cs.getInt("auth_type");
			AuthType authType = AuthType.fromCode(authTypeCode);
			JsonNode jsonNode = objectMapper.readTree(credentialJson);
			return mapToCredential(jsonNode, authType);
		} catch (Exception e) {
			throw new RuntimeException("Failed to deserialize Credential from callable statement column " + columnIndex,
					e);
		}
	}

	private Credential mapToCredential(JsonNode jsonNode, AuthType authType) {
		return switch (authType) {
		case PASSWORD ->
			new PasswordCredential(jsonNode.has("passwordHash") ? jsonNode.get("passwordHash").asString() : null);
		// Legacy SMS_CODE rows have no sys_account credential, since SMS login moved to
		// the PHONE claim, so they deserialise to null
		case SMS_CODE -> null;
		case OAUTH2 -> {
			OAuth2Provider provider = jsonNode.has("provider")
					? OAuth2Provider.valueOf(jsonNode.get("provider").asString())
					: null;
			String openId = jsonNode.has("openId") ? jsonNode.get("openId").asString() : null;
			String accessToken = jsonNode.has("accessToken") ? jsonNode.get("accessToken").asString() : null;
			String refreshToken = jsonNode.has("refreshToken") ? jsonNode.get("refreshToken").asString() : null;
			LocalDateTime tokenExpiresAt = jsonNode.has("expireTime")
					? objectMapper.convertValue(jsonNode.get("expireTime"), LocalDateTime.class)
					: null;
			yield new OAuth2Credential(provider, openId, accessToken, refreshToken, tokenExpiresAt);
		}
		case QR_CODE -> new QRCredential(jsonNode.has("qrCodeId") ? jsonNode.get("qrCodeId").asString() : null,
				jsonNode.has("qrStatus") ? jsonNode.get("qrStatus").asString() : null);
		};
	}
}
