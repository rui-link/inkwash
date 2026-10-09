package top.ruilink.inkwash.base.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import top.ruilink.inkwash.base.exception.BusinessException;

/**
 * SortValidator field whitelist and injection rejection unit tests.
 *
 * @author Dyllon
 * @since 0.5.1
 */
class SortValidatorTest {

	private final SortValidator validator = new SortValidator();

	@Test
	void nullFieldReturnsDefault() {
		assertEquals("id ASC", validator.resolve(null, "asc"));
	}

	@Test
	void blankFieldReturnsDefault() {
		assertEquals("id ASC", validator.resolve("  ", "desc"));
	}

	@Test
	void unknownFieldIsRejected() {
		assertThrows(BusinessException.class, () -> validator.resolve("nonExistent", "asc"));
	}

	@Test
	void validFieldAsc() {
		assertEquals("create_time ASC", validator.resolve("createTime", "asc"));
	}

	@Test
	void validFieldDesc() {
		assertEquals("create_time DESC", validator.resolve("createTime", "desc"));
	}

	@Test
	void caseInsensitiveOrder() {
		assertEquals("nickname DESC", validator.resolve("nickname", "DESC"));
		assertEquals("nickname DESC", validator.resolve("nickname", "Desc"));
	}

	@Test
	void nullOrderDefaultsToAsc() {
		assertEquals("status ASC", validator.resolve("status", null));
	}

	@Test
	void allWhitelistedFieldsProduceSafeSql() {
		var safePattern = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]* (ASC|DESC)$");

		String[] fields = { "createTime", "updateTime", "nickname", "id", "status", "name", "module", "resource",
				"action", "code", "parentId", "level", "type", "sort", "title", "slug", "word", "categoryId",
				"authorId", "publishTime", "userId", "identity", "loginType", "address", "location", "device",
				"browser", "ostype", "loginTime" };

		for (String field : fields) {
			String result = validator.resolve(field, "desc");
			assertTrue(safePattern.matcher(result).matches(),
					"Unsafe SQL generated for field '" + field + "': " + result);
		}
	}

	@Test
	void sqlInjectionInFieldIsRejected() {
		assertThrows(BusinessException.class, () -> validator.resolve("'; DROP TABLE users; --", "asc"));
	}

	@Test
	void unionInjectionInFieldIsRejected() {
		assertThrows(BusinessException.class, () -> validator.resolve("id UNION SELECT * FROM sys_account", "asc"));
	}

	@Test
	void invalidOrderIsRejected() {
		assertThrows(BusinessException.class, () -> validator.resolve("createTime", "up"));
	}

	@Test
	void orderInjectionIsRejected() {
		assertThrows(BusinessException.class, () -> validator.resolve("createTime", "asc; DROP TABLE users; --"));
	}

	@Test
	void blankFieldUsesProvidedDefault() {
		assertEquals("nickname ASC", validator.resolve(null, null, "nickname ASC"));
	}

	@Test
	void unsafeDefaultFallsBackToIdAsc() {
		assertEquals("id ASC", validator.resolve(null, null, "1; DROP TABLE users; --"));
	}
}
