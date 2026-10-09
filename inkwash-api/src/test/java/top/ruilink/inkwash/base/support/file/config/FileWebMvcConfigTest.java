package top.ruilink.inkwash.base.support.file.config;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

import jakarta.servlet.ServletContext;

/**
 * Upload resource handler path traversal and whitelist unit tests.
 *
 * @author Dyllon
 * @since 0.5.1
 */
class FileWebMvcConfigTest {

	private static final class ExposedResourceHandlerRegistry extends ResourceHandlerRegistry {

		private ExposedResourceHandlerRegistry(ApplicationContext applicationContext, ServletContext servletContext) {
			super(applicationContext, servletContext);
		}

		@Override
		public AbstractHandlerMapping getHandlerMapping() {
			return super.getHandlerMapping();
		}
	}

	@TempDir
	Path uploadDir;

	private ResourceHttpRequestHandler uploadsHandler;

	@BeforeEach
	void setUp() {
		ApplicationContext applicationContext = mock(ApplicationContext.class);
		when(applicationContext.getResource(anyString()))
				.thenReturn(new FileSystemResource(uploadDir.toString() + "/"));
		ServletContext servletContext = mock(ServletContext.class);

		FileWebMvcConfig config = new FileWebMvcConfig(uploadDir.toString());
		ExposedResourceHandlerRegistry registry = new ExposedResourceHandlerRegistry(applicationContext,
				servletContext);
		config.addResourceHandlers(registry);

		SimpleUrlHandlerMapping mapping = (SimpleUrlHandlerMapping) registry.getHandlerMapping();
		mapping.setApplicationContext(applicationContext);
		uploadsHandler = (ResourceHttpRequestHandler) mapping.getHandlerMap().get("/uploads/**");
	}

	private MockHttpServletRequest request(String pathWithinHandler) {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/uploads/" + pathWithinHandler);
		request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, pathWithinHandler);
		return request;
	}

	private void assertBlocked(String pathWithinHandler) {
		assertThrows(NoResourceFoundException.class,
				() -> uploadsHandler.handleRequest(request(pathWithinHandler), new MockHttpServletResponse()),
				"expected request blocked: " + pathWithinHandler);
	}

	@Test
	void normalUuidImageIsServed() throws Exception {
		Path file = uploadDir.resolve("2026-09/uuid-1234-abcd.png");
		Files.createDirectories(file.getParent());
		byte[] content = { (byte) 0x89, 0x50, 0x4E, 0x47, 1, 2, 3, 4 };
		Files.write(file, content);

		MockHttpServletResponse response = new MockHttpServletResponse();
		uploadsHandler.handleRequest(request("2026-09/uuid-1234-abcd.png"), response);

		assertArrayEquals(content, response.getContentAsByteArray());
	}

	@Test
	void uppercaseExtensionIsServed() throws Exception {
		Path file = uploadDir.resolve("2026-09/uuid-5678.JPG");
		Files.createDirectories(file.getParent());
		Files.write(file, new byte[] { 1, 2, 3 });

		MockHttpServletResponse response = new MockHttpServletResponse();
		uploadsHandler.handleRequest(request("2026-09/uuid-5678.JPG"), response);

		assertArrayEquals(new byte[] { 1, 2, 3 }, response.getContentAsByteArray());
	}

	@Test
	void dotDotSegmentIsBlocked() {
		assertBlocked("2026-09/../secret.png");
	}

	@Test
	void trailingDotDotIsBlocked() {
		assertBlocked("2026-09/..");
	}

	@Test
	void directoryRequestIsBlocked() throws Exception {
		Files.createDirectories(uploadDir.resolve("2026-09"));

		assertBlocked("2026-09");
		assertBlocked("2026-09/");
	}

	@Test
	void nonWhitelistedExtensionIsBlocked() throws Exception {
		Path file = uploadDir.resolve("2026-09/uploaded-page.html");
		Files.createDirectories(file.getParent());
		Files.write(file, "<script>alert(1)</script>".getBytes(java.nio.charset.StandardCharsets.UTF_8));

		assertBlocked("2026-09/uploaded-page.html");
	}

	@Test
	void extensionlessAndHiddenFilesAreBlocked() throws Exception {
		Files.createDirectories(uploadDir.resolve("2026-09"));
		Files.write(uploadDir.resolve("2026-09/README"), new byte[] { 1 });
		Files.write(uploadDir.resolve("2026-09/.env"), new byte[] { 1 });

		assertBlocked("2026-09/README");
		assertBlocked("2026-09/.env");
	}

	@Test
	void driveLetterPathIsBlocked() {
		assertBlocked("C:/windows/system32/config/sam");
	}

	@Test
	void nullBytePathIsBlocked() {
		assertBlocked("2026-09/uuid\u0000.png");
	}
}