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
package top.ruilink.inkwash.system.domain.credential;

import top.ruilink.inkwash.base.enums.AuthType;

/**
 * QR code login credential record.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public record QRCredential(String qrCodeId, String qrStatus) implements Credential {
	@Override
	public AuthType getType() {
		return AuthType.QR_CODE;
	}

	/**
	 * Validates the QR code identifier.
	 */
	public boolean verify(String inputQrCodeId) {
		return qrCodeId != null && qrCodeId.equals(inputQrCodeId);
	}

	/**
	 * Returns a new QrCredential instance with the updated status.
	 */
	public QRCredential withStatus(String newStatus) {
		return new QRCredential(qrCodeId, newStatus);
	}
}
