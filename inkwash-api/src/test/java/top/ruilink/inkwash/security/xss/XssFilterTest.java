package top.ruilink.inkwash.security.xss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

/**
 * XSS filter markdown and rich-text sanitizing with credential pass-through
 * unit tests.
 *
 * @author Dyllon
 * @since 0.5.1
 */
class XssFilterTest {

	private static final String JSON_CONTENT_TYPE = "application/json";

	private XssFilter filter;

	private static final class ProbeChain implements FilterChain {
		byte[] body;
		ServletRequest wrappedRequest;

		@Override
		public void doFilter(ServletRequest request, ServletResponse response) {
			this.wrappedRequest = request;
			try {
				this.body = request.getInputStream().readAllBytes();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	@BeforeEach
	void setUp() {
		filter = new XssFilter();
	}

	private ProbeChain postJson(String uri, String json) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
		request.setContentType(JSON_CONTENT_TYPE);
		request.setContent(json.getBytes(StandardCharsets.UTF_8));
		ProbeChain chain = new ProbeChain();
		filter.doFilter(request, new MockHttpServletResponse(), chain);
		return chain;
	}

	private String bodyOf(ProbeChain chain) {
		return new String(chain.body, StandardCharsets.UTF_8);
	}

	@Test
	@DisplayName("JSON 凭证字段 password 原样透传，不做 HTML 转义")
	void passwordJson_passesThroughRaw() throws Exception {
		ProbeChain chain = postJson("/api/auth/profile/password", "{\"password\":\"aa&bb\"}");

		assertTrue(bodyOf(chain).contains("\"aa&bb\""), "password must survive unescaped");
		assertFalse(bodyOf(chain).contains("&amp;"), "password must not be entity-escaped");
	}

	@Test
	@DisplayName("JSON 凭证字段 token/accessToken 原样透传")
	void tokenJson_passesThroughRaw() throws Exception {
		ProbeChain chain = postJson("/api/auth/token/refresh", "{\"refreshToken\":\"a&b=c+d\"}");

		assertTrue(bodyOf(chain).contains("\"a&b=c+d\""), "refreshToken must survive unescaped");
		assertFalse(bodyOf(chain).contains("&amp;"));
	}

	@Test
	@DisplayName("JSON 任意普通字段不再被统一实体转义")
	void genericFieldJson_notEscaped() throws Exception {
		ProbeChain chain = postJson("/api/cms/articles", "{\"biography\":\"a&b <em>x</em>\"}");

		assertTrue(bodyOf(chain).contains("\"a&b <em>x</em>\""), "arbitrary field must pass through unmodified");
		assertFalse(bodyOf(chain).contains("&amp;"), "arbitrary field must not be entity-escaped");
	}

	@Test
	@DisplayName("JSON content 字段走 markdown 清洗，脚本删除且 & 保留")
	void contentJson_markdownSanitized() throws Exception {
		ProbeChain chain = postJson("/api/cms/articles", "{\"content\":\"line & <script>x</script> next\"}");
		String body = bodyOf(chain);

		assertFalse(body.contains("<script"), "script tag must be stripped from markdown content");
		assertFalse(body.contains("&amp;"), "bare & must not be entity-escaped in markdown content");
		assertTrue(body.contains("&"), "& must be preserved for the markdown renderer");
	}

	@Test
	@DisplayName("JSON title 字段走富文本清洗，脚本删除")
	void titleJson_richTextSanitized() throws Exception {
		ProbeChain chain = postJson("/api/cms/articles", "{\"title\":\"<script>alert(1)</script>hello\"}");
		String body = bodyOf(chain);

		assertFalse(body.contains("<script"), "script tag must be stripped from rich-text title");
		assertTrue(body.contains("hello"), "normal text must survive rich-text sanitization");
	}

	@Test
	@DisplayName("查询参数 password 原样透传")
	void passwordParam_passesThroughRaw() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/system/users");
		request.addParameter("password", "aa&bb");
		ProbeChain chain = new ProbeChain();
		filter.doFilter(request, new MockHttpServletResponse(), chain);

		assertEquals("aa&bb", chain.wrappedRequest.getParameter("password"));
	}

	@Test
	@DisplayName("查询参数 content 走 markdown 清洗")
	void contentParam_markdownSanitized() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/cms/articles");
		request.addParameter("content", "line & <script>x</script> next");
		ProbeChain chain = new ProbeChain();
		filter.doFilter(request, new MockHttpServletResponse(), chain);

		String value = chain.wrappedRequest.getParameter("content");
		assertFalse(value.contains("<script"), "script tag must be stripped from markdown param");
		assertFalse(value.contains("&amp;"), "bare & must not be entity-escaped in markdown param");
		assertTrue(value.contains("&"));
	}

	@Test
	@DisplayName("JSON summary 字段含 ASCII 逗号应原样保留")
	void summaryJson_commaNotEscaped() throws Exception {
		ProbeChain chain = postJson("/api/cms/articles", "{\"summary\":\"a, b, c\"}");
		String body = bodyOf(chain);

		assertTrue(body.contains("\"a, b, c\""), "summary commas must survive unescaped, got: " + body);
	}

	@Test
	@DisplayName("JSON summary 字段含全角逗号应原样保留，不被实体转义")
	void summaryJson_fullWidthCommaNotEscaped() throws Exception {
		ProbeChain chain = postJson("/api/cms/articles", "{\"summary\":\"a，b\"}");
		String body = bodyOf(chain);

		assertTrue(body.contains("\"a，b\""), "full-width comma must survive unescaped, got: " + body);
		assertFalse(body.contains("&#xff0c;"), "full-width comma must not become an HTML entity, got: " + body);
	}

	@Test
	@DisplayName("JSON summary 字段的 HTML 标记应作为字面文本保留，不被重新解析")
	void summaryJson_markupNotReparsed() throws Exception {
		ProbeChain chain = postJson("/api/cms/articles", "{\"summary\":\"a<b>, c\"}");
		String body = bodyOf(chain);

		assertTrue(body.contains("\"a<b>, c\""), "markup must survive as literal text, got: " + body);
	}

	@Test
	@DisplayName("放行路径 /api/auth/login 不进行 JSON 清洗")
	void excludedLoginPath_notFiltered() throws Exception {
		ProbeChain chain = postJson("/api/auth/login", "{\"password\":\"aa&bb\"}");

		assertTrue(bodyOf(chain).contains("\"aa&bb\""));
		assertFalse(bodyOf(chain).contains("&amp;"));
	}
}