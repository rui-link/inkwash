package top.ruilink.inkwash.security.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;

import jakarta.mail.Message;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeUtility;
import top.ruilink.inkwash.security.config.CredentialConfig;
import top.ruilink.inkwash.security.service.VerifyCodeStore;

/**
 * Mail service test covering both the real SMTP path through the embedded
 * GreenMail server and the development fallback where an unconfigured SMTP
 * server only logs.
 * 
 * @author Dyllon
 * @since 0.5.1
 */
class EmailServiceImplTest {

	private GreenMail greenMail;
	private EmailServiceImpl emailService;

	@AfterEach
	void tearDown() {
		if (greenMail != null) {
			greenMail.stop();
			greenMail = null;
		}
	}

	private EmailServiceImpl buildEmailService(JavaMailSender sender, boolean enabled, String from) {
		return new EmailServiceImpl(realCodeStore(), credentialConfig(), providerOf(sender), enabled, from);
	}

	private VerifyCodeStore realCodeStore() {
		return new VerifyCodeStore();
	}

	private CredentialConfig credentialConfig() {
		return new CredentialConfig();
	}

	private ObjectProvider<JavaMailSender> providerOf(JavaMailSender sender) {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		if (sender != null) {
			beanFactory.registerSingleton("mailSender", sender);
		}
		return beanFactory.getBeanProvider(JavaMailSender.class);
	}

	private JavaMailSenderImpl greenMailSender() {
		ServerSetup setup = new ServerSetup(0, "127.0.0.1", ServerSetup.PROTOCOL_SMTP);
		greenMail = new GreenMail(setup);
		greenMail.start();

		JavaMailSenderImpl sender = new JavaMailSenderImpl();
		sender.setHost("127.0.0.1");
		sender.setPort(greenMail.getSmtp().getPort());
		sender.setDefaultEncoding("UTF-8");
		Properties props = new Properties();
		props.setProperty("mail.smtp.connectiontimeout", "2000");
		props.setProperty("mail.smtp.timeout", "2000");
		sender.setJavaMailProperties(props);
		return sender;
	}

	private String decodeSubject(MimeMessage message) throws Exception {
		return MimeUtility.decodeText(message.getSubject());
	}

	@Test
	@DisplayName("sendCode - 邮件启用时真实发送验证码邮件，收件人/主题/正文正确")
	void sendCode_enabled_sendsRealMail() throws Exception {
		emailService = buildEmailService(greenMailSender(), true, "no-reply@example.com");

		String code = emailService.sendCode("zhang@example.com");

		assertNotNull(code);
		assertEquals(6, code.length());
		assertTrue(greenMail.waitForIncomingEmail(5000, 1), "SMTP 应收到一封邮件");

		MimeMessage received = greenMail.getReceivedMessages()[0];
		assertEquals("zhang@example.com",
				((InternetAddress) received.getRecipients(Message.RecipientType.TO)[0]).getAddress());
		assertTrue(decodeSubject(received).contains("验证码"));
		assertTrue(received.getContent().toString().contains(code), "正文应包含验证码");
		// The code is also written to local storage so it can be validated
		assertTrue(emailService.verifyCode("zhang@example.com", code));
	}

	@Test
	@DisplayName("sendMail - 邮件启用时真实发送，收件人/主题/正文正确且返回 true")
	void sendMail_enabled_sendsRealMail() throws Exception {
		emailService = buildEmailService(greenMailSender(), true, "no-reply@example.com");

		boolean sent = emailService.sendMail("li@example.com", "密码重置通知", "新密码为：Abcd1234!");

		assertTrue(sent);
		assertTrue(greenMail.waitForIncomingEmail(5000, 1), "SMTP 应收到一封邮件");

		MimeMessage received = greenMail.getReceivedMessages()[0];
		assertEquals("li@example.com",
				((InternetAddress) received.getRecipients(Message.RecipientType.TO)[0]).getAddress());
		assertEquals("密码重置通知", decodeSubject(received));
		assertEquals("新密码为：Abcd1234!", received.getContent().toString());
	}

	@Test
	@DisplayName("sendCode - 未启用邮件时回退本地存储并打印日志（不抛异常）")
	void sendCode_disabled_fallsBackToLocalStore() {
		emailService = buildEmailService(null, false, "");

		String code = emailService.sendCode("dev@example.com");

		assertNotNull(code);
		assertEquals(6, code.length());
		assertTrue(emailService.verifyCode("dev@example.com", code), "未启用邮件时验证码仍应存在本地存储");
	}

	@Test
	@DisplayName("sendMail - 未启用邮件时返回 false（spring.mail.host 未配置或 mail.enabled=false）")
	void sendMail_disabled_returnsFalse() {
		emailService = buildEmailService(null, false, "");

		assertFalse(emailService.sendMail("a@example.com", "subject", "body"));
	}

	@Test
	@DisplayName("sendMail - mail.enabled=true 但无 JavaMailSender 视为未启用，返回 false")
	void sendMail_enabledButNoSender_returnsFalse() {
		emailService = buildEmailService(null, true, "");

		assertFalse(emailService.sendMail("a@example.com", "subject", "body"));
	}

	@Test
	@DisplayName("sendMail - SMTP 发送失败时记录告警并返回 false，不向调用方抛异常")
	void sendMail_smtpFailure_returnsFalse() {
		JavaMailSenderImpl deadSender = new JavaMailSenderImpl();
		deadSender.setHost("127.0.0.1");
		deadSender.setPort(1); // 无监听端口，连接将被拒绝
		deadSender.setDefaultEncoding("UTF-8");
		Properties props = new Properties();
		props.setProperty("mail.smtp.connectiontimeout", "2000");
		props.setProperty("mail.smtp.timeout", "2000");
		deadSender.setJavaMailProperties(props);
		emailService = buildEmailService(deadSender, true, "no-reply@example.com");

		boolean sent = emailService.sendMail("a@example.com", "subject", "body");

		assertFalse(sent, "SMTP 连接失败应返回 false（成功路径返回 true）");
	}
}