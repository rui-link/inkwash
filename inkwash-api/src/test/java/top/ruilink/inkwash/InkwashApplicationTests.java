package top.ruilink.inkwash;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Disabled Spring Boot application context load smoke test.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@SpringBootTest
@Disabled("Requires test database connection")
class InkwashApplicationTests {

	@Test
	void contextLoads() {
	}
}
