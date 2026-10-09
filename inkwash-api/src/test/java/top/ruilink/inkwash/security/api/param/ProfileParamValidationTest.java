package top.ruilink.inkwash.security.api.param;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * ProfileParam bean-validation tests.
 * <p>
 * Profile updates are partial by design: clients send only the fields they
 * edited (id/username/password are never part of a profile update). These tests
 * pin the contract so a partial payload passes validation while still rejecting
 * malformed values.
 *
 * @author Dyllon
 * @since 0.5.1
 */
class ProfileParamValidationTest {

	private final Validator validator;

	ProfileParamValidationTest() {
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		validator = factory.getValidator();
	}

	@Test
	@DisplayName("partial payload without id/username/password is valid")
	void partialPayloadWithoutIdUsernamePasswordIsValid() {
		ProfileParam param = new ProfileParam();
		param.setNickname("张三");
		param.setAvatar("/uploads/avatars/1.png");

		Set<ConstraintViolation<ProfileParam>> violations = validator.validate(param);

		assertTrue(violations.isEmpty(), () -> "expected no violations but got: " + violations);
	}

	@Test
	@DisplayName("blank nickname is rejected")
	void blankNicknameIsRejected() {
		ProfileParam param = new ProfileParam();
		param.setNickname("  ");

		Set<ConstraintViolation<ProfileParam>> violations = validator.validate(param);

		assertEquals(1, violations.size());
	}
}
