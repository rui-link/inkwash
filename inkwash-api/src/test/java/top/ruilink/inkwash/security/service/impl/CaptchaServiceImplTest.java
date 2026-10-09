package top.ruilink.inkwash.security.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import top.ruilink.inkwash.security.api.view.CaptchaView;
import top.ruilink.inkwash.security.config.CredentialConfig;
import top.ruilink.inkwash.security.config.QrLoginConfig;
import top.ruilink.inkwash.security.service.VerifyCodeStore;

/**
 * QR login code generation, SSE token verification, and URL origin rules unit
 * tests.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@ExtendWith(MockitoExtension.class)
class CaptchaServiceImplTest {

	private static final String WEB_URL = "http://localhost:3001/qr-login";

	private CaptchaServiceImpl service;

	@BeforeEach
	void setUp() {
		CredentialConfig credentialConfig = new CredentialConfig();
		QrLoginConfig qrLoginConfig = new QrLoginConfig();
		qrLoginConfig.setWebUrl(WEB_URL);
		qrLoginConfig.setAllowedOrigins("http://192.168.1.10:3001");
		service = new CaptchaServiceImpl(credentialConfig, qrLoginConfig, new VerifyCodeStore(),
				new QrTicketService()) {
			@Override
			SseEmitter createQrEmitter(long timeoutMillis) {
				return new RecordingSseEmitter(timeoutMillis);
			}
		};
	}

	private String buildQrLoginUrl(String client, String origin, String qrCodeId) throws Exception {
		java.lang.reflect.Method m = CaptchaServiceImpl.class.getDeclaredMethod("buildQrLoginUrl", String.class,
				String.class, String.class);
		m.setAccessible(true);
		return (String) m.invoke(service, client, origin, qrCodeId);
	}

	private String decodeQrContent(CaptchaView view) throws Exception {
		String base64 = view.getCaptchaImage().substring("data:image/png;base64,".length());
		BufferedImage image = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(base64)));
		BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
		MultiFormatReader reader = new MultiFormatReader();
		reader.setHints(Map.of(DecodeHintType.TRY_HARDER, Boolean.TRUE, DecodeHintType.PURE_BARCODE, Boolean.TRUE));
		Result result = reader.decode(bitmap);
		return result.getText();
	}

	@Test
	@DisplayName("generateCaptcha 返回的视图不包含验证码值")
	void generateCaptcha_DoesNotLeakAnswer() {
		CaptchaView view = service.generateCaptcha();

		assertNotNull(view.getCaptchaId());
		assertNotNull(view.getCaptchaImage());

		JsonNode tree = new JsonMapper().valueToTree(view);
		List<String> fields = new ArrayList<>(tree.propertyNames());
		assertEquals(2, fields.size());
		assertTrue(fields.contains("captchaId"));
		assertTrue(fields.contains("captchaImage"));
		assertFalse(fields.contains("captchaKey"));
		assertFalse(fields.contains("captchaValue"));
		assertFalse(fields.contains("answer"));
	}

	@Test
	@DisplayName("web client encodes web confirm url with qrCodeId")
	void qrContent_WebClient() throws Exception {
		CaptchaView view = service.generateQrCode("web", null);
		String expected = WEB_URL + "?qrCodeId=" + view.getCaptchaId();
		assertEquals(expected, decodeQrContent(view));
	}

	@Test
	@DisplayName("admin client encodes web confirm url with qrCodeId")
	void qrContent_AdminClient() throws Exception {
		CaptchaView view = service.generateQrCode("admin", null);
		String expected = WEB_URL + "?qrCodeId=" + view.getCaptchaId();
		assertEquals(expected, decodeQrContent(view));
	}

	@Test
	@DisplayName("null client defaults to web url")
	void qrContent_NullClient_DefaultsToWeb() throws Exception {
		CaptchaView view = service.generateQrCode(null, null);
		assertTrue(decodeQrContent(view).startsWith(WEB_URL));
	}

	@Test
	@DisplayName("unknown client defaults to web url")
	void qrContent_UnknownClient_DefaultsToWeb() throws Exception {
		CaptchaView view = service.generateQrCode("desktop", null);
		assertTrue(decodeQrContent(view).startsWith(WEB_URL));
	}

	@Test
	@DisplayName("custom origin overrides default for web client when in allowlist")
	void qrContent_CustomOrigin_Web() throws Exception {
		CaptchaView view = service.generateQrCode("web", "http://192.168.1.10:3001/");
		String expected = "http://192.168.1.10:3001/qr-login?qrCodeId=" + view.getCaptchaId();
		assertEquals(expected, decodeQrContent(view));
	}

	@Test
	@DisplayName("admin client ignores origin and uses web url")
	void qrContent_CustomOrigin_Admin() throws Exception {
		CaptchaView view = service.generateQrCode("admin", "https://inkwash.example.com");
		String expected = WEB_URL + "?qrCodeId=" + view.getCaptchaId();
		assertEquals(expected, decodeQrContent(view));
	}

	@Test
	@DisplayName("non-http origin falls back to configured url")
	void qrContent_InvalidOrigin_FallsBackToDefault() throws Exception {
		CaptchaView view = service.generateQrCode("web", "ftp://evil.com");
		assertTrue(decodeQrContent(view).startsWith(WEB_URL));
	}

	@Test
	@DisplayName("qrCodeId is a uuid")
	void qrCodeId_IsUuid() {
		CaptchaView view = service.generateQrCode("web", null);
		UUID.fromString(view.getCaptchaId());
	}

	@Test
	@DisplayName("generateQrCode returns a distinct sseToken that is not embedded in the qr content")
	void generateQrCode_ReturnsSseToken_NotInQrContent() throws Exception {
		CaptchaView view = service.generateQrCode("web", null);

		assertNotNull(view.getSseToken());
		assertFalse(view.getSseToken().isEmpty());
		assertNotEquals(view.getCaptchaId(), view.getSseToken());
		assertFalse(decodeQrContent(view).contains(view.getSseToken()));
	}

	@Test
	@DisplayName("sseToken is verified only against the bound qr entry")
	void checkSseToken_MatchesBoundToken() {
		CaptchaView view = service.generateQrCode("web", null);

		assertTrue(service.checkSseToken(view.getCaptchaId(), view.getSseToken()));
		assertFalse(service.checkSseToken(view.getCaptchaId(), "wrong-token"));
		assertFalse(service.checkSseToken(view.getCaptchaId(), null));
		assertFalse(service.checkSseToken("no-such-id", view.getSseToken()));
	}

	@Test
	@DisplayName("qrPayload confirmed carries status and ticket")
	void qrPayload_Confirmed_CarriesStatusAndTicket() {
		Map<String, Object> payload = service.qrPayload("CONFIRMED", "ticket-abc");

		assertEquals("CONFIRMED", payload.get("status"));
		assertEquals("ticket-abc", payload.get("ticket"));
		assertNull(payload.get("token"));
	}

	@Test
	@DisplayName("qrPayload expired carries status only")
	void qrPayload_Expired_CarriesStatusOnly() {
		Map<String, Object> payload = service.qrPayload("EXPIRED", null);

		assertEquals("EXPIRED", payload.get("status"));
		assertNull(payload.get("ticket"));
	}

	@Test
	@DisplayName("publish without subscriber is a no-op")
	void publish_WithoutSubscriber_NoOp() {
		String id = service.generateQrCode("web", null).getCaptchaId();
		service.setQrToken(id, "access:refresh");

		assertDoesNotThrow(() -> service.publishQrConfirmed(id));
	}

	@Test
	@DisplayName("subscribe for a valid qr keeps the emitter open")
	void subscribe_ValidQr_KeepsEmitterOpen() {
		CaptchaView view = service.generateQrCode("web", null);

		RecordingSseEmitter emitter = (RecordingSseEmitter) service.subscribeQrCode(view.getCaptchaId(),
				view.getSseToken());

		assertTrue(emitter.sent.isEmpty());
		assertFalse(emitter.completed);
	}

	@Test
	@DisplayName("subscribe for an already confirmed qr sends CONFIRMED and completes")
	void subscribe_AlreadyConfirmed_SendsEventAndCompletes() {
		CaptchaView view = service.generateQrCode("web", null);
		service.setQrToken(view.getCaptchaId(), "access:refresh");

		RecordingSseEmitter emitter = (RecordingSseEmitter) service.subscribeQrCode(view.getCaptchaId(),
				view.getSseToken());

		assertTrue(emitter.completed);
		assertTrue(emitter.sent.stream().anyMatch(o -> o.toString().contains("CONFIRMED")));
		assertTrue(emitter.sent.stream().anyMatch(o -> o.toString().contains("ticket")));
		assertTrue(emitter.sent.stream().noneMatch(o -> o.toString().contains("access:refresh")),
				"CONFIRMED event must never carry the credential");
	}

	@Test
	@DisplayName("subscribe for an unknown qr sends EXPIRED and completes")
	void subscribe_UnknownQr_SendsExpiredAndCompletes() {
		RecordingSseEmitter emitter = (RecordingSseEmitter) service.subscribeQrCode("no-such-id", "sse-token");

		assertTrue(emitter.completed);
		assertTrue(emitter.sent.stream().anyMatch(o -> o.toString().contains("EXPIRED")));
	}

	@Test
	@DisplayName("subscribe with a wrong sseToken never exposes a token")
	void subscribe_WrongSseToken_DoesNotExposeToken() {
		CaptchaView view = service.generateQrCode("web", null);
		service.setQrToken(view.getCaptchaId(), "access:refresh");

		RecordingSseEmitter emitter = (RecordingSseEmitter) service.subscribeQrCode(view.getCaptchaId(), "wrong-token");

		assertTrue(emitter.completed);
		assertTrue(emitter.sent.isEmpty());
	}

	@Test
	@DisplayName("subscribe without an sseToken never exposes a token")
	void subscribe_MissingSseToken_DoesNotExposeToken() {
		CaptchaView view = service.generateQrCode("web", null);
		service.setQrToken(view.getCaptchaId(), "access:refresh");

		RecordingSseEmitter emitter = (RecordingSseEmitter) service.subscribeQrCode(view.getCaptchaId(), null);

		assertTrue(emitter.completed);
		assertTrue(emitter.sent.isEmpty());
	}

	@Test
	@DisplayName("publish pushes CONFIRMED and completes a pending subscription")
	void publish_CompletesPendingSubscription() {
		CaptchaView view = service.generateQrCode("web", null);
		String id = view.getCaptchaId();
		RecordingSseEmitter emitter = (RecordingSseEmitter) service.subscribeQrCode(id, view.getSseToken());

		service.setQrToken(id, "access:refresh");
		service.publishQrConfirmed(id);

		assertTrue(emitter.completed);
		assertTrue(emitter.sent.stream().anyMatch(o -> o.toString().contains("CONFIRMED")));
		assertTrue(emitter.sent.stream().anyMatch(o -> o.toString().contains("ticket")));
		assertTrue(emitter.sent.stream().noneMatch(o -> o.toString().contains("access:refresh")),
				"CONFIRMED event must never carry the credential");
	}

	@Test
	@DisplayName("re-subscribe completes the previous emitter for the same id")
	void resubscribe_CompletesPreviousEmitter() {
		CaptchaView view = service.generateQrCode("web", null);
		String id = view.getCaptchaId();
		RecordingSseEmitter first = (RecordingSseEmitter) service.subscribeQrCode(id, view.getSseToken());

		service.subscribeQrCode(id, view.getSseToken());

		assertTrue(first.completed);
	}

	@Test
	@DisplayName("admin 客户端固定使用配置 webUrl，忽略上报来源")
	void adminClient_usesConfiguredWebUrl() throws Exception {
		assertEquals("http://localhost:3001/qr-login?qrCodeId=abc",
				buildQrLoginUrl("admin", "https://evil.example.com", "abc"));
	}

	@Test
	@DisplayName("web 客户端上报白名单来源时使用上报来源")
	void webClientAllowedOrigin_usesReportedOrigin() throws Exception {
		assertEquals("http://192.168.1.10:3001/qr-login?qrCodeId=abc",
				buildQrLoginUrl("web", "http://192.168.1.10:3001/", "abc"));
	}

	@Test
	@DisplayName("web 客户端非白名单来源回退到配置 webUrl")
	void webClientDisallowedOrigin_fallsBack() throws Exception {
		assertEquals("http://localhost:3001/qr-login?qrCodeId=abc",
				buildQrLoginUrl("web", "https://evil.example.com", "abc"));
	}

	@Test
	@DisplayName("web 客户端空来源回退到配置 webUrl")
	void webClientNullOrigin_fallsBack() throws Exception {
		assertEquals("http://localhost:3001/qr-login?qrCodeId=abc", buildQrLoginUrl("web", null, "abc"));
	}

	private static final class RecordingSseEmitter extends SseEmitter {

		final List<Object> sent = new ArrayList<>();
		boolean completed;

		RecordingSseEmitter(long timeout) {
			super(timeout);
		}

		@Override
		public void send(SseEmitter.SseEventBuilder builder) {
			builder.build().forEach(d -> sent.add(d.getData()));
		}

		@Override
		public void complete() {
			completed = true;
		}
	}
}
