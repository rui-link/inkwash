package top.ruilink.inkwash.security.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Single-use QR login ticket issue and exchange unit tests.
 *
 * @author Dyllon
 * @since 0.5.1
 */
class QrTicketServiceTest {

	@Test
	@DisplayName("issue creates a ticket exchangeable exactly once")
	void issue_ExchangeOnce() {
		QrTicketService service = new QrTicketService();
		String ticket = service.issue("qr-1", "access:refresh");

		assertNotNull(ticket);
		QrTicketService.Ticket first = service.exchange(ticket);
		assertNotNull(first);
		assertEquals("qr-1", first.qrCodeId());
		assertEquals("access:refresh", first.credential());

		assertNull(service.exchange(ticket), "ticket must be single-use");
	}

	@Test
	@DisplayName("issue is idempotent for the same qrCodeId with the same credential")
	void issue_Idempotent() {
		QrTicketService service = new QrTicketService();
		String first = service.issue("qr-1", "access:refresh");
		String second = service.issue("qr-1", "access:refresh");
		assertEquals(first, second);
	}

	@Test
	@DisplayName("rotated credential issues a fresh ticket for the same qrCodeId")
	void issue_RotatedCredential() {
		QrTicketService service = new QrTicketService();
		String first = service.issue("qr-1", "access:refresh");
		String second = service.issue("qr-1", "access2:refresh2");
		assertNotEquals(first, second);
	}

	@Test
	@DisplayName("expired ticket cannot be exchanged")
	void exchange_ExpiredTicket() {
		QrTicketService service = new QrTicketService(-1);
		String ticket = service.issue("qr-1", "access:refresh");
		assertNull(service.exchange(ticket));
	}

	@Test
	@DisplayName("unknown ticket cannot be exchanged")
	void exchange_UnknownTicket() {
		QrTicketService service = new QrTicketService();
		assertNull(service.exchange("no-such-ticket"));
	}

	@Test
	@DisplayName("exchanging a valid ticket also invalidates the issue mapping")
	void exchangeClearsIssuedMapping() {
		QrTicketService service = new QrTicketService();
		String ticket = service.issue("qr-1", "access:refresh");
		assertNotNull(service.exchange(ticket));
		// after single use, a second issue attempt must mint a new ticket, not the dead
		// one
		String fresh = service.issue("qr-1", "access:refresh");
		assertNotEquals(ticket, fresh);
	}
}