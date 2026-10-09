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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import lombok.extern.slf4j.Slf4j;
import top.ruilink.inkwash.security.config.CredentialConfig;
import top.ruilink.inkwash.security.config.QrLoginConfig;
import top.ruilink.inkwash.security.service.CaptchaService;
import top.ruilink.inkwash.security.service.VerifyCodeStore;
import top.ruilink.inkwash.security.api.view.CaptchaView;

/**
 * Captcha service implementation.
 *
 * <p>
 * Note: both the image captcha and QR code caches are JVM local
 * ConcurrentHashMaps, so in a multi node deployment a code only validates on
 * the node that issued it. A distributed deployment could replace this with a
 * Redis backed VerifyCodeStore.
 * </p>
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Slf4j
@Service
public class CaptchaServiceImpl implements CaptchaService {

	private final CredentialConfig credentialConfig;
	private final QrLoginConfig qrLoginConfig;
	private final VerifyCodeStore verifyCodeStore;
	private final QrTicketService qrTicketService;

	public CaptchaServiceImpl(CredentialConfig credentialConfig, QrLoginConfig qrLoginConfig,
			VerifyCodeStore verifyCodeStore, QrTicketService qrTicketService) {
		this.credentialConfig = credentialConfig;
		this.qrLoginConfig = qrLoginConfig;
		this.verifyCodeStore = verifyCodeStore;
		this.qrTicketService = qrTicketService;
	}

	// The expiry time comes from configuration
	private int getCaptchaExpireMinutes() {
		return credentialConfig.getPassword().getCaptchaExpireMinutes();
	}

	private int getSmsExpireMinutes() {
		return credentialConfig.getSmsExpireMinutes();
	}

	private int getQrExpireMinutes() {
		return credentialConfig.getQrExpireMinutes();
	}

	/** Image captcha cache mapping captchaId to CaptchaEntry */
	private final Map<String, CaptchaEntry> captchaCache = new ConcurrentHashMap<>();

	/** QR code cache mapping qrCodeId to QrCodeEntry */
	private final Map<String, QrCodeEntry> qrCodeCache = new ConcurrentHashMap<>();

	/** QR code login SSE subscriptions mapping qrCodeId to SseEmitter */
	private final Map<String, SseEmitter> qrEmitters = new ConcurrentHashMap<>();

	private static final String QR_EVENT_NAME = "qr";

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	@PostConstruct
	public void init() {
		startCleanupTask();
	}

	@Override
	public CaptchaView generateCaptcha() {
		LineCaptcha captcha = CaptchaUtil.createLineCaptcha(120, 40, 4, 30);
		String captchaValue = captcha.getCode();
		String captchaId = UUID.randomUUID().toString();
		String captchaImage = "data:image/png;base64," + captcha.getImageBase64();

		captchaCache.put(captchaId, new CaptchaEntry(captchaValue, getCaptchaExpireMinutes()));

		CaptchaView captchaView = new CaptchaView();
		captchaView.setCaptchaId(captchaId);
		captchaView.setCaptchaImage(captchaImage);

		log.debug("生成图形验证码, captchaId={}", captchaId);
		return captchaView;
	}

	@Override
	public boolean verifyCaptcha(String captchaId, String inputCode) {
		if (!StringUtils.hasText(captchaId) || !StringUtils.hasText(inputCode)) {
			return false;
		}
		CaptchaEntry entry = captchaCache.get(captchaId);
		if (entry == null || entry.isExpired()) {
			log.warn("图形验证码不存在或已过期, captchaId={}", captchaId);
			return false;
		}
		boolean verified = entry.code.equalsIgnoreCase(inputCode);
		if (verified) {
			captchaCache.remove(captchaId);
			log.debug("图形验证码验证成功, captchaId={}", captchaId);
		} else {
			log.warn("图形验证码错误, captchaId={}, input={}", captchaId, inputCode);
		}
		return verified;
	}

	@Override
	public String generateSmsCode(String phone) {
		String smsCode = generateRandomCode(6);
		verifyCodeStore.save(phone, smsCode, getSmsExpireMinutes());
		log.info("生成短信验证码, phone={}", phone);
		return smsCode;
	}

	@Override
	public boolean verifySmsCode(String phone, String inputCode) {
		if (!StringUtils.hasText(phone) || !StringUtils.hasText(inputCode)) {
			return false;
		}
		boolean verified = verifyCodeStore.verify(phone, inputCode);
		if (verified) {
			log.debug("短信验证码验证成功, phone={}", phone);
		} else {
			log.warn("短信验证码错误或已失效, phone={}", phone);
		}
		return verified;
	}

	@Override
	public void clearSmsCode(String phone) {
		verifyCodeStore.clear(phone);
	}

	@Override
	public CaptchaView generateQrCode(String client, String origin) {
		String qrCodeId = UUID.randomUUID().toString();
		String sseToken = generateSseToken();
		String qrCodeContent = buildQrLoginUrl(client, origin, qrCodeId);

		try {
			QRCodeWriter qrCodeWriter = new QRCodeWriter();
			BitMatrix bitMatrix = qrCodeWriter.encode(qrCodeContent, BarcodeFormat.QR_CODE, 300, 300);

			try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
				MatrixToImageWriter.writeToStream(bitMatrix, "png", outputStream);
				String qrCodeImage = "data:image/png;base64,"
						+ Base64.getEncoder().encodeToString(outputStream.toByteArray());

				qrCodeCache.put(qrCodeId, new QrCodeEntry(getQrExpireMinutes(), sseToken));

				CaptchaView captchaView = new CaptchaView();
				captchaView.setCaptchaId(qrCodeId);
				captchaView.setCaptchaImage(qrCodeImage);
				captchaView.setSseToken(sseToken);

				log.debug("生成二维码登录凭证, qrCodeId={}, client={}", qrCodeId, client);
				return captchaView;
			}
		} catch (Exception e) {
			log.error("生成二维码图片失败", e);
			throw new RuntimeException("生成二维码失败", e);
		}
	}

	@Override
	public boolean checkSseToken(String qrCodeId, String sseToken) {
		QrCodeEntry entry = qrCodeCache.get(qrCodeId);
		if (entry == null || entry.isExpired() || !StringUtils.hasText(sseToken)) {
			return false;
		}
		return MessageDigest.isEqual(entry.getSseToken().getBytes(StandardCharsets.UTF_8),
				sseToken.getBytes(StandardCharsets.UTF_8));
	}

	private String generateSseToken() {
		byte[] bytes = new byte[16];
		SECURE_RANDOM.nextBytes(bytes);
		return HexFormat.of().formatHex(bytes);
	}

	/**
	 * Builds the confirmation page address carried in the QR code: the admin client
	 * always uses the configured webUrl, ignoring the reported origin, while the
	 * web client uses a reported origin only when allowlisted so a real device can
	 * reach it, otherwise falling back to the configured webUrl. allowlisted so a
	 * real device can reach it, otherwise falling back to the configured webUrl.
	 */
	private String buildQrLoginUrl(String client, String origin, String qrCodeId) {
		boolean admin = "admin".equalsIgnoreCase(client);
		if (admin) {
			return qrLoginConfig.getWebUrl() + "?qrCodeId=" + qrCodeId;
		}
		if (StringUtils.hasText(origin) && (origin.startsWith("http://") || origin.startsWith("https://"))
				&& qrLoginConfig.isOriginAllowed(origin)) {
			String base = origin.replaceAll("/+$", "");
			return base + "/qr-login?qrCodeId=" + qrCodeId;
		}
		log.warn("二维码展示来源不在白名单，回退到默认地址, origin={}", origin);
		return qrLoginConfig.getWebUrl() + "?qrCodeId=" + qrCodeId;
	}

	@Override
	public boolean checkQrCodeStatus(String qrCodeId) {
		QrCodeEntry entry = qrCodeCache.get(qrCodeId);
		if (entry == null || entry.isExpired()) {
			return false;
		}
		return confirmedUsers.containsKey(qrCodeId);
	}

	@Override
	public boolean isQrCodeValid(String qrCodeId) {
		QrCodeEntry entry = qrCodeCache.get(qrCodeId);
		return entry != null && !entry.isExpired();
	}

	/**
	 * Cache of the user who confirmed the QR code login, mapping qrCodeId to userId
	 */
	private final Map<String, Long> confirmedUsers = new ConcurrentHashMap<>();

	/** Cache of the user who scanned the QR code, mapping qrCodeId to userId */
	private final Map<String, Long> scannedUsers = new ConcurrentHashMap<>();

	/** QR code login token cache mapping qrCodeId to token */
	private final Map<String, String> qrTokens = new ConcurrentHashMap<>();

	@Override
	public void scanQrCode(String qrCodeId, Long userId) {
		QrCodeEntry entry = qrCodeCache.get(qrCodeId);
		if (entry == null || entry.isExpired()) {
			return;
		}
		if (confirmedUsers.containsKey(qrCodeId) || qrTokens.containsKey(qrCodeId)) {
			return;
		}
		scannedUsers.put(qrCodeId, userId);
		log.info("二维码已扫描, qrCodeId={}, userId={}", qrCodeId, userId);
	}

	@Override
	public Long getScannedUser(String qrCodeId) {
		QrCodeEntry entry = qrCodeCache.get(qrCodeId);
		if (entry == null || entry.isExpired()) {
			return null;
		}
		return scannedUsers.get(qrCodeId);
	}

	@Override
	public void confirmQrCode(String qrCodeId, Long userId) {
		QrCodeEntry entry = qrCodeCache.get(qrCodeId);
		if (entry != null && !entry.isExpired()) {
			confirmedUsers.put(qrCodeId, userId);
			log.info("二维码登录确认, qrCodeId={}, userId={}", qrCodeId, userId);
		}
	}

	@Override
	public String getQrToken(String qrCodeId) {
		QrCodeEntry entry = qrCodeCache.get(qrCodeId);
		if (entry == null || entry.isExpired()) {
			return null;
		}
		return qrTokens.get(qrCodeId);
	}

	@Override
	public void setQrToken(String qrCodeId, String token) {
		qrTokens.put(qrCodeId, token);
	}

	@Override
	public Long getQrConfirmedUser(String qrCodeId) {
		QrCodeEntry entry = qrCodeCache.get(qrCodeId);
		if (entry == null || entry.isExpired()) {
			return null;
		}
		return confirmedUsers.get(qrCodeId);
	}

	@Override
	public SseEmitter subscribeQrCode(String qrCodeId, String sseToken) {
		long timeoutMillis = getQrExpireMinutes() * 60_000L + 30_000L;
		SseEmitter emitter = createQrEmitter(timeoutMillis);
		if (!checkSseToken(qrCodeId, sseToken)) {
			if (!isQrCodeValid(qrCodeId)) {
				sendAndComplete(emitter, qrEvent("EXPIRED", null));
			} else {
				emitter.complete();
			}
			return emitter;
		}

		emitter.onCompletion(() -> qrEmitters.remove(qrCodeId, emitter));
		emitter.onError(_ -> qrEmitters.remove(qrCodeId, emitter));
		emitter.onTimeout(() -> {
			qrEmitters.remove(qrCodeId, emitter);
			sendAndComplete(emitter, qrEvent("EXPIRED", null));
		});

		SseEmitter previous = qrEmitters.put(qrCodeId, emitter);
		if (previous != null) {
			previous.complete();
		}

		String credential = getQrToken(qrCodeId);
		if (credential != null) {
			sendAndComplete(emitter, qrEvent("CONFIRMED", qrTicketService.issue(qrCodeId, credential)));
		} else if (!isQrCodeValid(qrCodeId)) {
			sendAndComplete(emitter, qrEvent("EXPIRED", null));
		}
		return emitter;
	}

	@Override
	public void publishQrConfirmed(String qrCodeId) {
		String credential = getQrToken(qrCodeId);
		if (credential == null) {
			return;
		}
		SseEmitter emitter = qrEmitters.get(qrCodeId);
		if (emitter != null && qrEmitters.remove(qrCodeId, emitter)) {
			sendAndComplete(emitter, qrEvent("CONFIRMED", qrTicketService.issue(qrCodeId, credential)));
		}
	}

	/** SSE event payload, carrying only the ticket and never the token (H-FS-3) */
	Map<String, Object> qrPayload(String status, String ticket) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("status", status);
		if (ticket != null) {
			payload.put("ticket", ticket);
		}
		return payload;
	}

	/** Creates the SSE emitter, which tests can replace through a subclass */
	SseEmitter createQrEmitter(long timeoutMillis) {
		return new SseEmitter(timeoutMillis);
	}

	private SseEmitter.SseEventBuilder qrEvent(String status, String token) {
		return SseEmitter.event().name(QR_EVENT_NAME).data(qrPayload(status, token));
	}

	private void sendAndComplete(SseEmitter emitter, SseEmitter.SseEventBuilder event) {
		try {
			emitter.send(event);
		} catch (IOException | IllegalStateException e) {
			log.debug("二维码SSE推送失败, 连接可能已断开, {}", e.getMessage());
		} finally {
			emitter.complete();
		}
	}

	private String generateRandomCode(int length) {
		return ThreadLocalRandom.current().ints(length, 0, 10).mapToObj(String::valueOf).collect(Collectors.joining());
	}

	private void startCleanupTask() {
		ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
			Thread t = new Thread(r, "captcha-cleanup");
			t.setDaemon(true);
			return t;
		});

		scheduler.scheduleAtFixedRate(() -> {
			try {
				captchaCache.entrySet().removeIf(e -> e.getValue().isExpired());
				qrCodeCache.entrySet().removeIf(e -> e.getValue().isExpired());
				confirmedUsers.keySet().removeIf(qrId -> !qrCodeCache.containsKey(qrId));
				scannedUsers.keySet().removeIf(qrId -> !qrCodeCache.containsKey(qrId));
				qrTokens.keySet().removeIf(qrId -> !qrCodeCache.containsKey(qrId));
				qrEmitters.keySet().removeIf(qrId -> !qrCodeCache.containsKey(qrId));
				log.debug("验证码缓存清理完成, captchaSize={}, qrSize={}", captchaCache.size(), qrCodeCache.size());
			} catch (Exception e) {
				log.error("验证码缓存清理任务异常", e);
			}
		}, 1, 1, TimeUnit.MINUTES);
	}

	private static class CaptchaEntry {
		private final String code;
		private final LocalDateTime createTime = LocalDateTime.now();
		private final int expireMinutes;

		CaptchaEntry(String code, int expireMinutes) {
			this.code = code;
			this.expireMinutes = expireMinutes;
		}

		boolean isExpired() {
			return createTime.plusMinutes(expireMinutes).isBefore(LocalDateTime.now());
		}
	}

	private static class QrCodeEntry {
		private LocalDateTime createTime = LocalDateTime.now();
		private final int expireMinutes;
		private final String sseToken;

		QrCodeEntry(int expireMinutes, String sseToken) {
			this.expireMinutes = expireMinutes;
			this.sseToken = sseToken;
		}

		String getSseToken() {
			return sseToken;
		}

		boolean isExpired() {
			return createTime.plusMinutes(expireMinutes).isBefore(LocalDateTime.now());
		}
	}
}
