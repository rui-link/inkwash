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
package top.ruilink.inkwash.security.api.view;

/**
 * QR code login status response view.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public record QrCodeStatusView(String status, String ticket, String nickname, String phone) {
	public static QrCodeStatusView confirmed(String ticket) {
		return new QrCodeStatusView("CONFIRMED", ticket, null, null);
	}

	public static QrCodeStatusView pending() {
		return new QrCodeStatusView("PENDING", null, null, null);
	}

	public static QrCodeStatusView scanned(String nickname, String phone) {
		return new QrCodeStatusView("SCANNED", null, nickname, phone);
	}

	public static QrCodeStatusView expired() {
		return new QrCodeStatusView("EXPIRED", null, null, null);
	}
}
