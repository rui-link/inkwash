package top.ruilink.inkwash.security.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * CORS allowed origin parsing and normalization unit tests.
 *
 * @author Dyllon
 * @since 0.5.1
 */
class CorsConfigTest {

	@Test
	@DisplayName("parseOrigins trims and drops empty entries")
	void parseOrigins_TrimsAndDropsEmpty() {
		List<String> origins = CorsConfig.parseOrigins(" http://localhost:9089 , http://localhost:3000, ");
		assertEquals(List.of("http://localhost:9089", "http://localhost:3000"), origins);
	}

	@Test
	@DisplayName("parseOrigins strips trailing slashes")
	void parseOrigins_stripsTrailingSlash() {
		List<String> origins = CorsConfig.parseOrigins("http://localhost:9089/");
		assertEquals(List.of("http://localhost:9089"), origins);
	}

	@Test
	@DisplayName("getAllowedOrigins reflects configured value")
	void getAllowedOrigins() {
		CorsConfig config = new CorsConfig();
		ReflectionTestUtils.setField(config, "allowedOrigin", "http://a.example.com,http://b.example.com");
		List<String> origins = config.getAllowedOrigins();
		assertTrue(origins.contains("http://a.example.com"));
		assertTrue(origins.contains("http://b.example.com"));
	}
}