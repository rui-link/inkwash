package top.ruilink.inkwash.base.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import top.ruilink.inkwash.system.domain.credential.OAuth2Status;
import top.ruilink.inkwash.system.domain.credential.PasswordStatus;
import top.ruilink.inkwash.system.domain.credential.QRCodeStatus;
import top.ruilink.inkwash.system.domain.credential.SmsCodeStatus;
import top.ruilink.inkwash.system.enums.NoticeType;

/**
 * BaseEnum and StringCodeEnum code lookup unit tests.
 *
 * @author Dyllon
 * @since 0.5.1
 */
class BaseEnumTest {

	@Test
	@DisplayName("BaseEnum.code() 默认委托 getCode()")
	void baseEnumCode_DelegatesToGetCode() {
		BaseEnum e = BaseStatus.ENABLE;
		assertEquals(e.getCode(), e.code());
	}

	@Test
	@DisplayName("BaseEnum.fromCode 按 int 编码查找枚举")
	void baseEnumFromCode_IntCode() {
		assertEquals(Gender.MALE, BaseEnum.fromCode(Gender.class, 1));
		assertEquals(Gender.UNKNOWN, BaseEnum.fromCode(Gender.class, 0));
	}

	@Test
	@DisplayName("BaseEnum.fromCode 编码不存在抛异常")
	void baseEnumFromCode_InvalidCode_Throws() {
		assertThrows(IllegalArgumentException.class, () -> BaseEnum.fromCode(Gender.class, 999));
	}

	@Test
	@DisplayName("BaseStatus 枚举序列化保持 int 编码（@JsonValue）")
	void baseStatus_SerializesAsInt() {
		assertEquals(0, BaseStatus.DISABLE.getCode());
		assertEquals(1, BaseStatus.ENABLE.getCode());
	}

	@Test
	@DisplayName("Gender 枚举序列化保持 int 编码")
	void gender_SerializesAsInt() {
		assertEquals(0, Gender.UNKNOWN.getCode());
		assertEquals(1, Gender.MALE.getCode());
		assertEquals(2, Gender.FEMALE.getCode());
	}

	@Test
	@DisplayName("OAuth2Provider.getCode 返回 int 编码")
	void oauth2Provider_Code() {
		assertEquals(1, OAuth2Provider.ALIPAY.getCode());
		assertEquals(5, OAuth2Provider.APPLE.getCode());
	}

	@Test
	@DisplayName("StringCodeEnum.fromCode 按 String 编码查找枚举")
	void stringCodeEnumFromCode_ValidCode() {
		assertEquals(OAuth2Status.VALID, StringCodeEnum.fromCode(OAuth2Status.class, "VALID"));
		assertEquals(PasswordStatus.ENABLE, StringCodeEnum.fromCode(PasswordStatus.class, "ENABLE"));
		assertEquals(QRCodeStatus.INIT, StringCodeEnum.fromCode(QRCodeStatus.class, "INIT"));
		assertEquals(SmsCodeStatus.USED, StringCodeEnum.fromCode(SmsCodeStatus.class, "USED"));
	}

	@Test
	@DisplayName("StringCodeEnum.fromCode null/blank 返回 null")
	void stringCodeEnumFromCode_NullBlank_ReturnsNull() {
		assertEquals(null, StringCodeEnum.fromCode(OAuth2Status.class, null));
		assertEquals(null, StringCodeEnum.fromCode(OAuth2Status.class, "  "));
	}

	@Test
	@DisplayName("StringCodeEnum.fromCode 无效编码抛异常")
	void stringCodeEnumFromCode_InvalidCode_Throws() {
		assertThrows(IllegalArgumentException.class, () -> StringCodeEnum.fromCode(OAuth2Status.class, "INVALID"));
	}

	@Test
	@DisplayName("StringCodeEnum 实现类 getCode 返回正确值")
	void stringCodeEnumImplementations_Code() {
		assertEquals("VALID", OAuth2Status.VALID.getCode());
		assertEquals("LOCKED", PasswordStatus.LOCKED.getCode());
		assertEquals("PENDING", QRCodeStatus.PENDING.getCode());
		assertEquals("INIT", SmsCodeStatus.INIT.getCode());
	}

	@Test
	@DisplayName("NoticeType 枚举 @JsonValue/@JsonCreator round-trip")
	void noticeType_RoundTrip() {
		assertEquals(1, NoticeType.INFORM.getCode());
		assertEquals(2, NoticeType.NOTICE.getCode());
		assertEquals(3, NoticeType.WARNING.getCode());
		NoticeType roundTripped = NoticeType.fromCode(2);
		assertEquals(NoticeType.NOTICE, roundTripped);
	}
}
