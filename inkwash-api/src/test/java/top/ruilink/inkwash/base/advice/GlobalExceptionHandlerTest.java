package top.ruilink.inkwash.base.advice;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Global exception handler unit tests for missing resources.
 *
 * @author Dyllon
 * @since 0.5.1
 */
class GlobalExceptionHandlerTest {

	private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

	@Test
	void unknownStaticResourceReturns404InsteadOf500() {
		ResponseEntity<ProblemDetail> response = handler.handleNoResourceFound(
				new NoResourceFoundException(HttpMethod.GET, "No static resource login", "/login"));

		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		assertEquals("资源不存在: /login", response.getBody().getDetail());
	}
}