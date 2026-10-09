package top.ruilink.inkwash.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;

/**
 * Permission seed data initialization integration tests granting comment rights
 * to registered users.
 *
 * <p>
 * Runs the real {@code database/init-data.sql} against H2 rather than asserting
 * against a fixture, so a grant written in the seed file and a grant the
 * application relies on cannot drift apart.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@MybatisTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:inkwash_seed_it;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL;NON_KEYWORDS=DAY",
		"spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
		"spring.datasource.password=" })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("dev")
class PermissionSeedIT {

	@Autowired
	private DataSource dataSource;

	private void loadSeed() {
		ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
		populator.addScript(new FileSystemResource("database/h2-schema.sql"));
		populator.addScript(new FileSystemResource("database/init-data.sql"));
		populator.setContinueOnError(true);
		populator.execute(dataSource);
	}

	@Test
	void roleUserIsGrantedCmsArticleComment() {
		loadSeed();

		JdbcTemplate jdbc = new JdbcTemplate(dataSource);
		Long count = jdbc.queryForObject("SELECT COUNT(*) FROM sys_role_permission rp "
				+ "JOIN sys_permission p ON p.id = rp.permission_id "
				+ "WHERE rp.role_id = 4 AND p.module = 'cms' AND p.resource = 'article' AND p.action = 'comment'",
				Long.class);

		assertEquals(1L, count, "ROLE_USER 应被授予 cms:article:comment 权限，否则登录用户在 web 端评论会返回 403");
	}

	@Test
	@DisplayName("ROLE_EDITOR 持有 cms:dashboard:query，editor 仪表盘才能加载文章/分类统计")
	void roleEditorIsGrantedDashboardQuery() {
		loadSeed();

		assertEquals(1L, dashboardGrant(3L, "query"), "editor 视图渲染了 category 统计图，缺少该权限会固定返回 403");
	}

	@Test
	@DisplayName("cms:dashboard:user-stats 只授予 ROLE_SYSTEM / ROLE_ADMIN，与 SecurityUtil.isAdmin 同集")
	void dashboardUserStatsStaysAdminOnly() {
		loadSeed();

		assertEquals(1L, dashboardGrant(1L, "user-stats"), "ROLE_SYSTEM 权限最高，必须能看用户统计");
		assertEquals(1L, dashboardGrant(2L, "user-stats"), "ROLE_ADMIN 需要该权限才能看到用户统计");
		assertEquals(0L, dashboardGrant(3L, "user-stats"), "user-stats 统计全平台用户数，不能给 editor");
		assertEquals(0L, dashboardGrant(4L, "user-stats"), "ROLE_USER 不得持有任何 dashboard 权限");
		assertEquals(0L, dashboardGrant(4L, "query"), "ROLE_USER 仪表盘只看自己的文章，不需要全平台统计");
	}

	private long dashboardGrant(long roleId, String action) {
		JdbcTemplate jdbc = new JdbcTemplate(dataSource);
		return jdbc.queryForObject(
				"SELECT COUNT(*) FROM sys_role_permission rp " + "JOIN sys_permission p ON p.id = rp.permission_id "
						+ "WHERE rp.role_id = ? AND p.module = 'cms' AND p.resource = 'dashboard' AND p.action = ?",
				Long.class, roleId, action);
	}
}
