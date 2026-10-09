package top.ruilink.inkwash.monitor.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import top.ruilink.inkwash.cms.api.DashboardController;
import top.ruilink.inkwash.security.config.SecurityConfig;

/**
 * Authorisation blind spot regression: easily abused read endpoints such as the
 * login log detail, audit and statistics panels must carry method level
 * permission annotations and must never rely on isAuthenticated() alone or be
 * left open.
 * 
 * @author Dyllon
 * @since 0.5.1
 */
@DisplayName("方法级权限注解回归")
class MethodSecurityPolicyTest {

	private static String authority(String expected) {
		return "hasAuthority('" + expected + "')";
	}

	private static void assertAnnotated(Class<?> clazz, String method, Class<?>... params) throws Exception {
		PreAuthorize ann = clazz.getMethod(method, params).getAnnotation(PreAuthorize.class);
		assertTrue(ann != null && !ann.value().isBlank(),
				clazz.getSimpleName() + "#" + method + " 必须声明非空 @PreAuthorize");
	}

	@Test
	@DisplayName("注册登录日志详情需要有 monitor:login:query 权限")
	void loginInfoDetail_requiresMonitorLoginQuery() throws Exception {
		PreAuthorize ann = LoginInfoController.class.getMethod("getById", Long.class).getAnnotation(PreAuthorize.class);
		assertEquals(authority("monitor:login:query"), ann.value());
	}

	@Test
	@DisplayName("审计看板需要有 monitor:system:query 权限")
	void auditDashboard_requiresMonitorSystemQuery() throws Exception {
		PreAuthorize ann = AuditController.class.getMethod("getAuditDashboard").getAnnotation(PreAuthorize.class);
		assertEquals(authority("monitor:system:query"), ann.value());
	}

	@Test
	@DisplayName("CMS 统计看板端点按 cms:dashboard:* 权限分级")
	void cmsDashboard_endpoints_requireDashboardPermissions() throws Exception {
		assertAnnotated(DashboardController.class, "getDashboard");
		assertAnnotated(DashboardController.class, "getArticleStats", String.class);
		assertAnnotated(DashboardController.class, "getSummary");
		assertAnnotated(DashboardController.class, "getMyArticleStats", String.class);
		assertAnnotated(DashboardController.class, "getUserStats", String.class);
		assertAnnotated(DashboardController.class, "getCategoryStats", String.class);

		// /dashboard and /my-article-stats stay on cms:article:query: ROLE_USER needs
		// it to manage its
		// own articles, and its dashboard only ever shows the caller's own data.
		assertEquals(authority("cms:article:query"),
				DashboardController.class.getMethod("getDashboard").getAnnotation(PreAuthorize.class).value());
		assertEquals(authority("cms:article:query"), DashboardController.class
				.getMethod("getMyArticleStats", String.class).getAnnotation(PreAuthorize.class).value());

		// Platform-wide counts need their own permissions. cms:article:query cannot
		// express this:
		// ROLE_USER holds it too, so it would hand every registered user the
		// whole-platform
		// statistics. PermissionSeedIT locks who actually gets each of these.
		assertEquals(authority("cms:dashboard:query"), DashboardController.class
				.getMethod("getArticleStats", String.class).getAnnotation(PreAuthorize.class).value());
		assertEquals(authority("cms:dashboard:query"),
				DashboardController.class.getMethod("getSummary").getAnnotation(PreAuthorize.class).value());
		assertEquals(authority("cms:dashboard:query"), DashboardController.class
				.getMethod("getCategoryStats", String.class).getAnnotation(PreAuthorize.class).value());
		assertEquals(authority("cms:dashboard:user-stats"), DashboardController.class
				.getMethod("getUserStats", String.class).getAnnotation(PreAuthorize.class).value());
	}

	@Test
	@DisplayName("方法级安全必须已启用")
	void methodSecurityEnabled() {
		assertTrue(SecurityConfig.class.isAnnotationPresent(EnableMethodSecurity.class),
				"SecurityConfig 必须启用 @EnableMethodSecurity 才能让 @PreAuthorize 生效");
	}
}