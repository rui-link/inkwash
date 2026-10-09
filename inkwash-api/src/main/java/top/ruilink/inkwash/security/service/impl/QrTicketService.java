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
package top.ruilink.inkwash.security.service.impl;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Service;

/**
 * Single-use QR code login ticket, following the H-FS-3 authorization code
 * pattern.
 *
 * <p>
 * The ticket lets the QR desktop client redeem across pages: it is bound to the
 * credential of the qrCodeId when issued, lives for 30 seconds and is redeemed
 * atomically exactly once.
 * </p>
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Service
public class QrTicketService {

	public static final long DEFAULT_TTL_SECONDS = 30;

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private final long ttlSeconds;
	private final ConcurrentMap<String, Ticket> tickets = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, String> issued = new ConcurrentHashMap<>();

	public QrTicketService() {
		this(DEFAULT_TTL_SECONDS);
	}

	public QrTicketService(long ttlSeconds) {
		this.ttlSeconds = ttlSeconds;
	}

	/**
	 * Issues a single-use ticket for a QR code. The same qrCodeId with an unrotated
	 * credential returns the ticket already issued, while a rotated credential
	 * issues a new one.
	 */
	public String issue(String qrCodeId, String credential) {
		if (qrCodeId == null || credential == null) {
			throw new IllegalArgumentException("qrCodeId and credential are required");
		}
		String existing = issued.get(qrCodeId);
		if (existing != null) {
			Ticket current = tickets.get(existing);
			if (current != null && !current.isExpired() && credential.equals(current.credential())) {
				return existing;
			}
			issued.remove(qrCodeId, existing);
			tickets.remove(existing);
		}
		String ticket = randomTicket();
		tickets.put(ticket, new Ticket(qrCodeId, credential, System.currentTimeMillis() + ttlSeconds * 1000));
		issued.put(qrCodeId, ticket);
		return ticket;
	}

	/**
	 * Atomic single redemption. The ticket information carrying the credential is
	 * returned and then deleted immediately; an invalid, expired or already reused
	 * ticket returns null.
	 */
	public Ticket exchange(String ticket) {
		if (ticket == null) {
			return null;
		}
		Ticket consumed = tickets.remove(ticket);
		if (consumed == null) {
			return null;
		}
		issued.remove(consumed.qrCodeId(), ticket);
		return consumed.isExpired() ? null : consumed;
	}

	private static String randomTicket() {
		byte[] bytes = new byte[24];
		SECURE_RANDOM.nextBytes(bytes);
		return HexFormat.of().formatHex(bytes);
	}

	public record Ticket(String qrCodeId, String credential, long expiredAt) {
		public boolean isExpired() {
			return System.currentTimeMillis() >= expiredAt;
		}
	}
}
