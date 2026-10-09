package top.ruilink.inkwash.cms.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import top.ruilink.inkwash.base.enums.StatisticRange;
import top.ruilink.inkwash.base.exception.BusinessException;
import top.ruilink.inkwash.cms.api.view.DashboardView;
import top.ruilink.inkwash.cms.api.view.StatisticView;
import top.ruilink.inkwash.cms.service.DashboardStatsService;

/**
 * Dashboard endpoint unit tests for range parsing, delegation, and the
 * authenticated caller.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

	@Mock
	private DashboardStatsService dashboardStatsService;

	private DashboardController controller;

	@BeforeEach
	void setUp() {
		controller = new DashboardController(null, dashboardStatsService);
	}

	private StatisticView.ArticleStatItem articleItem() {
		StatisticView.ArticleStatItem item = new StatisticView.ArticleStatItem();
		item.setTimeAxis("2026-09-05");
		item.setCreated(1);
		return item;
	}

	private StatisticView.UserStatItem userItem() {
		StatisticView.UserStatItem item = new StatisticView.UserStatItem();
		item.setTimeAxis("2026-09-05");
		item.setNewUsers(1);
		return item;
	}

	private StatisticView.CategoryStatItem categoryItem() {
		StatisticView.CategoryStatItem item = new StatisticView.CategoryStatItem();
		item.setTimeAxis("2026-09-05");
		return item;
	}

	private void authenticateAs(Long userId) {
		SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken(String.valueOf(userId), null));
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("GET /article-stats 透传 range 并返回列表")
	void articleStats_delegates() {
		authenticateAs(42L);
		when(dashboardStatsService.getArticleStats(StatisticRange.LAST_7D, 42L)).thenReturn(List.of(articleItem()));

		ResponseEntity<List<StatisticView.ArticleStatItem>> response = controller.getArticleStats("last7d");

		assertEquals(200, response.getStatusCode().value());
		assertEquals(1, response.getBody().size());
		verify(dashboardStatsService).getArticleStats(StatisticRange.LAST_7D, 42L);
	}

	@Test
	@DisplayName("GET /article-stats 非法 range 抛业务异常")
	void articleStats_invalidRange() {
		authenticateAs(1L);
		assertThrows(BusinessException.class, () -> controller.getArticleStats("decade"));
	}

	@Test
	@DisplayName("GET /user-stats 委托服务")
	void userStats_delegates() {
		authenticateAs(42L);
		when(dashboardStatsService.getUserStats(StatisticRange.LAST_30D, 42L)).thenReturn(List.of(userItem()));

		ResponseEntity<List<StatisticView.UserStatItem>> response = controller.getUserStats("last30d");

		assertEquals(200, response.getStatusCode().value());
		assertEquals(1, response.getBody().size());
		verify(dashboardStatsService).getUserStats(StatisticRange.LAST_30D, 42L);
	}

	@Test
	@DisplayName("GET /category-stats 委托服务")
	void categoryStats_delegates() {
		authenticateAs(7L);
		when(dashboardStatsService.getCategoryStats(StatisticRange.LAST_12M, 7L)).thenReturn(List.of(categoryItem()));

		ResponseEntity<List<StatisticView.CategoryStatItem>> response = controller.getCategoryStats("last12m");

		assertEquals(200, response.getStatusCode().value());
		assertEquals(1, response.getBody().size());
		verify(dashboardStatsService).getCategoryStats(StatisticRange.LAST_12M, 7L);
	}

	@Test
	@DisplayName("GET /summary 委托服务")
	void summary_delegates() {
		authenticateAs(42L);
		DashboardView.DashboardSummary summary = new DashboardView.DashboardSummary();
		when(dashboardStatsService.getSummary(42L)).thenReturn(summary);

		ResponseEntity<DashboardView.DashboardSummary> response = controller.getSummary();

		assertEquals(200, response.getStatusCode().value());
		verify(dashboardStatsService).getSummary(42L);
	}

	@Test
	@DisplayName("GET /my-article-stats 委托服务")
	void myArticleStats_delegates() {
		authenticateAs(42L);
		StatisticView.MyArticleStats stats = new StatisticView.MyArticleStats();
		stats.setItems(List.of(articleItem()));
		stats.setPrevCreated(7L);
		when(dashboardStatsService.getMyArticleStats(StatisticRange.THIS_MONTH, 42L)).thenReturn(stats);

		ResponseEntity<StatisticView.MyArticleStats> response = controller.getMyArticleStats("thisMonth");

		assertEquals(200, response.getStatusCode().value());
		assertEquals(1, response.getBody().getItems().size());
		assertEquals(7L, response.getBody().getPrevCreated());
		verify(dashboardStatsService).getMyArticleStats(StatisticRange.THIS_MONTH, 42L);
	}

	@Test
	@DisplayName("GET /my-article-stats 非法 range 抛业务异常")
	void myArticleStats_invalidRange() {
		authenticateAs(1L);
		assertThrows(BusinessException.class, () -> controller.getMyArticleStats("decade"));
	}
}
