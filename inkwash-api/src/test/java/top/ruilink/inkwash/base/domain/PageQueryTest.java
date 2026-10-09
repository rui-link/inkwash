package top.ruilink.inkwash.base.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * PageQuery pagination parameter validation unit tests.
 *
 * @author Dyllon
 * @since 0.5.1
 */
class PageQueryTest {

	private final Validator validator;

	PageQueryTest() {
		try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
			this.validator = factory.getValidator();
		}
	}

	@Test
	@DisplayName("默认值：page=1, size=10")
	void defaultValues() {
		PageQuery query = new PageQuery();
		assertEquals(1, query.getPage());
		assertEquals(10, query.getSize());
	}

	@Test
	@DisplayName("page=1 合法，无违规")
	void page_One_NoViolation() {
		PageQuery query = new PageQuery();
		query.setPage(1);
		Set<ConstraintViolation<PageQuery>> violations = validator.validate(query);
		assertEquals(0, violations.size());
	}

	@Test
	@DisplayName("page=0 违反 @Min(1)")
	void page_Zero_Violation() {
		PageQuery query = new PageQuery();
		query.setPage(0);
		Set<ConstraintViolation<PageQuery>> violations = validator.validate(query);
		boolean hasPageViolation = violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("page"));
		assertEquals(true, hasPageViolation);
	}

	@Test
	@DisplayName("size=100 合法，无违规")
	void size_100_NoViolation() {
		PageQuery query = new PageQuery();
		query.setSize(100);
		Set<ConstraintViolation<PageQuery>> violations = validator.validate(query);
		assertEquals(0, violations.size());
	}

	@Test
	@DisplayName("size=101 违反 @Max(100)")
	void size_101_Violation() {
		PageQuery query = new PageQuery();
		query.setSize(101);
		Set<ConstraintViolation<PageQuery>> violations = validator.validate(query);
		boolean hasSizeViolation = violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("size"));
		assertEquals(true, hasSizeViolation);
	}
}
