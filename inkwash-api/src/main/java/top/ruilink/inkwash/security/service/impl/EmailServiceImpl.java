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

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import top.ruilink.inkwash.security.config.CredentialConfig;
import top.ruilink.inkwash.security.service.EmailService;
import top.ruilink.inkwash.security.service.VerifyCodeStore;

/**
 * Email verification code and general purpose mail service implementation.
 *
 * <p>
 * The mail channel is governed by {@code mail.enabled}, false by default,
 * together with the {@link JavaMailSender} auto-configured by Spring Boot,
 * which is only registered when {@code spring.mail.host} is set. While
 * disabled, mail is only logged and never really delivered, which keeps
 * development and test environments from connecting to an external SMTP server.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

	private final VerifyCodeStore store;
	private final CredentialConfig credentialConfig;
	private final ObjectProvider<JavaMailSender> mailSenderProvider;
	private final boolean enabled;
	private final String from;

	public EmailServiceImpl(VerifyCodeStore store, CredentialConfig credentialConfig,
			ObjectProvider<JavaMailSender> mailSenderProvider, @Value("${mail.enabled:false}") boolean enabled,
			@Value("${mail.from:}") String from) {
		this.store = store;
		this.credentialConfig = credentialConfig;
		this.mailSenderProvider = mailSenderProvider;
		this.enabled = enabled;
		this.from = from;
	}

	@Override
	public String sendCode(String email) {
		String code = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
		store.save(email, code, credentialConfig.getEmailExpireMinutes());
		JavaMailSender sender = providedSender();
		if (sender == null) {
			// TODO: an email provider or local integration dependency, so codes are only
			// stored locally and logged
			log.info("邮箱验证码(邮件未启用，仅本机可见), email={}, code={}", email, code);
		} else {
			String text = "您的验证码为：" + code + "，有效期" + credentialConfig.getEmailExpireMinutes() + "分钟，请勿向他人泄露。";
			if (sendInternal(sender, email, "邮箱验证码", text)) {
				log.info("邮箱验证码已发送, email={}", email);
			}
		}
		return code;
	}

	@Override
	public boolean sendMail(String to, String subject, String text) {
		JavaMailSender sender = providedSender();
		if (sender == null) {
			log.warn("邮件未启用(mail.enabled=false 或 spring.mail.host 未配置)，无法投递: to={}, subject={}", to, subject);
			return false;
		}
		return sendInternal(sender, to, subject, text);
	}

	@Override
	public boolean verifyCode(String email, String code) {
		return store.verify(email, code);
	}

	@Override
	public void clearCode(String email) {
		store.clear(email);
	}

	/**
	 * Condition for the mail channel being enabled: mail.enabled is true and a
	 * usable JavaMailSender exists, which requires spring.mail.host.
	 */
	private JavaMailSender providedSender() {
		if (!enabled) {
			return null;
		}
		return mailSenderProvider.getIfAvailable();
	}

	private boolean sendInternal(JavaMailSender sender, String to, String subject, String text) {
		try {
			MimeMessageHelper helper = new MimeMessageHelper(sender.createMimeMessage(), StandardCharsets.UTF_8.name());
			helper.setFrom(buildFrom(sender));
			helper.setTo(to);
			helper.setSubject(subject);
			helper.setText(text);
			sender.send(helper.getMimeMessage());
			return true;
		} catch (MessagingException | MailException e) {
			log.warn("邮件发送失败, to={}, subject={}, err={}", to, subject, e.getMessage());
			return false;
		}
	}

	private String buildFrom(JavaMailSender sender) {
		if (StringUtils.hasText(from)) {
			return from;
		}
		if (sender instanceof JavaMailSenderImpl impl && StringUtils.hasText(impl.getUsername())) {
			return impl.getUsername();
		}
		return "no-reply@localhost";
	}
}
