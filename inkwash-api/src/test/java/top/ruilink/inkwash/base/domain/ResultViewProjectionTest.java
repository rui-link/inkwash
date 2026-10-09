/*
 * This file is part of Inkwash.
 * Copyright (C) 2026 ruilink team.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package top.ruilink.inkwash.base.domain;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonView;

import tools.jackson.databind.json.JsonMapper;

import top.ruilink.inkwash.base.enums.AuthType;
import top.ruilink.inkwash.cms.api.ArticleController;
import top.ruilink.inkwash.cms.api.view.ArticleView;
import top.ruilink.inkwash.cms.api.view.CategoryView;
import top.ruilink.inkwash.cms.api.view.TermView;
import top.ruilink.inkwash.cms.domain.ArticleTally;
import top.ruilink.inkwash.security.api.view.AccountView;
import top.ruilink.inkwash.system.api.GroupController;
import top.ruilink.inkwash.system.api.MenuController;
import top.ruilink.inkwash.system.api.PermissionController;
import top.ruilink.inkwash.system.api.RoleController;
import top.ruilink.inkwash.system.api.UserController;
import top.ruilink.inkwash.system.api.view.UserView;

/**
 * Locks the {@code @JsonView} projection contract required by design doc S2.4 /
 * constraint D8.
 *
 * <p>
 * ISS-017 existed because {@code ArticleView} declared 19 field-level
 * {@code @JsonView} annotations while {@code ArticleController} activated no
 * view at all. Jackson leaves {@code DEFAULT_VIEW_INCLUSION} at {@code true},
 * so a handler without an activated view serialises <em>every</em> field — the
 * Basic/Detail/Full split was pure decoration, and the unauthenticated
 * {@code GET /api/cms/articles} returned {@code opinion} and {@code reviewerId}
 * for every published article.
 *
 * <p>
 * The activation mechanism is a {@code @JsonView} annotation <em>on the
 * controller method</em>, read by Spring MVC in
 * {@code AbstractMessageConverterMethodProcessor}. There is no
 * {@code activate()} call anywhere — grepping for one is what made this
 * mechanism look dead when it was merely applied inconsistently.
 *
 * <p>
 * Assertions are on the annotations themselves rather than on serialised
 * output, so the contract is checked without depending on Jackson's
 * view-filtering internals.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@DisplayName("@JsonView 投影契约（D8 / ISS-017）")
class ResultViewProjectionTest {

	private static final Pattern FIELD_VIEW = Pattern.compile("@JsonView\\(ResultView\\.(\\w+)\\.class\\)");

	private record Expected(Class<?> controller, String method, Class<?> view, Class<?>[] params) {
	}

	private static final List<Expected> EXPECTED = List.of(
			new Expected(ArticleController.class, "listArticles", ResultView.Detail.class,
					new Class<?>[] { int.class, int.class }),
			new Expected(ArticleController.class, "getArticle", ResultView.Full.class, new Class<?>[] { Long.class }),
			new Expected(GroupController.class, "getGroup", ResultView.Detail.class, new Class<?>[] { Integer.class }),
			new Expected(RoleController.class, "getRole", ResultView.Detail.class, new Class<?>[] { Integer.class }),
			new Expected(UserController.class, "listUsers", ResultView.Detail.class,
					new Class<?>[] { top.ruilink.inkwash.system.api.query.UserQuery.class }),
			new Expected(MenuController.class, "listMenus", ResultView.Detail.class,
					new Class<?>[] { top.ruilink.inkwash.system.api.query.MenuQuery.class }),
			new Expected(PermissionController.class, "listPermissions", ResultView.Detail.class,
					new Class<?>[] { top.ruilink.inkwash.system.api.query.PermissionQuery.class }));

	@Test
	@DisplayName("关键端点必须激活 @JsonView，否则分级形同虚设")
	void keyEndpointsActivateAView() throws NoSuchMethodException {
		for (Expected e : EXPECTED) {
			Method m = e.controller().getDeclaredMethod(e.method(), e.params());
			JsonView view = m.getAnnotation(JsonView.class);

			assertNotNull(view, e.controller().getSimpleName() + "#" + e.method()
					+ " 未标注 @JsonView：返回分级视图时必须显式激活，否则 Jackson 会输出全部字段");
			assertArrayEquals(new Class<?>[] { e.view() }, view.value(),
					e.controller().getSimpleName() + "#" + e.method() + " 激活的视图级别与约定不符");
		}
	}

	@Test
	@DisplayName("公开列表激活 Detail：恰好剥掉 opinion / reviewerId，保留 ArticleCard 所需字段")
	void publicListLevelStripsExactlyTheFullFields() {
		List<String> visibleAtDetail = fieldsVisibleAt(ArticleView.class, ResultView.Detail.class);
		List<String> visibleAtFull = fieldsVisibleAt(ArticleView.class, ResultView.Full.class);

		assertFalse(visibleAtDetail.contains("opinion"), "未鉴权的 GET /api/cms/articles 不得返回审核意见: " + visibleAtDetail);
		assertFalse(visibleAtDetail.contains("reviewerId"), "未鉴权的 GET /api/cms/articles 不得返回审核人: " + visibleAtDetail);
		assertTrue(visibleAtDetail.containsAll(List.of("summary", "coverUrl", "tally")),
				"ArticleCard 依赖 summary / coverUrl / tally，Detail 级必须包含: " + visibleAtDetail);

		assertTrue(visibleAtFull.contains("opinion") && visibleAtFull.contains("reviewerId"),
				"管理端编辑器依赖 opinion / reviewerId，Full 级必须包含: " + visibleAtFull);
		assertTrue(visibleAtFull.containsAll(visibleAtDetail), "Full ⊃ Detail，Full 可见字段必须是 Detail 的超集");
	}

	@Test
	@DisplayName("管理端依赖 opinion，故共享详情端点必须保持 Full")
	void sharedDetailEndpointStaysFull() throws NoSuchMethodException {
		// ArticleEditor.vue / review.vue / publish.vue 都通过 GET /api/cms/articles/{id}
		// 取文章并渲染 opinion。该端点被收窄为 Detail 会让编辑器静默失去驳回原因。
		Method m = ArticleController.class.getDeclaredMethod("getArticle", Long.class);
		assertArrayEquals(new Class<?>[] { ResultView.Full.class }, m.getAnnotation(JsonView.class).value(),
				"公开门户与管理端编辑器共用 getArticle，收窄会破坏编辑器");
	}

	@Test
	@DisplayName("分组视图的字段必须全部带 @JsonView 标注")
	void levelledViewsAnnotateEveryField() {
		for (Class<?> view : List.of(ArticleView.class, UserView.class)) {
			List<String> unannotated = new ArrayList<>();
			for (Field f : view.getDeclaredFields()) {
				if (f.isSynthetic() || Modifier.isStatic(f.getModifiers())) {
					continue;
				}
				if (!f.isAnnotationPresent(JsonView.class)) {
					unannotated.add(f.getName());
				}
			}
			assertTrue(unannotated.isEmpty(), view.getSimpleName() + " 以下字段未标注 @JsonView，分级不完整: " + unannotated);
		}
	}

	@Test
	@DisplayName("不得对未分级视图激活视图，否则响应被过滤为空对象")
	void noControllerActivatesAnUnlevelledView() throws IOException {
		Pattern annotated = Pattern
				.compile("@JsonView\\(ResultView\\.\\w+\\.class\\)\\s*\\r?\\n\\s*public\\s+ResponseEntity<([^\\n]*)>");
		List<String> offenders = new ArrayList<>();

		try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
			for (Path p : files.filter(f -> f.toString().endsWith("Controller.java")).toList()) {
				Matcher m = annotated.matcher(Files.readString(p, StandardCharsets.UTF_8));
				while (m.find()) {
					Matcher v = Pattern.compile("(\\w+View)\\b").matcher(m.group(1));
					if (!v.find()) {
						continue;
					}
					String viewName = v.group(1);
					if (fieldViewCount(viewName) == 0) {
						offenders.add(p.getFileName() + " -> " + viewName);
					}
				}
			}
		}

		assertTrue(offenders.isEmpty(), () -> "以下端点激活了视图，但其 View 未声明任何字段分级，响应会被过滤成 {}: " + offenders);
	}

	@Test
	@DisplayName("ResultView 层级本身保持继承关系（Detail ⊃ Basic，Full ⊃ Detail）")
	void viewHierarchyIsInherited() {
		assertTrue(ResultView.Basic.class.isAssignableFrom(ResultView.Detail.class),
				"Detail 必须继承 Basic，否则激活 Detail 时会丢掉 Basic 字段");
		assertTrue(ResultView.Detail.class.isAssignableFrom(ResultView.Full.class),
				"Full 必须继承 Detail，否则激活 Full 时会丢掉 Detail 字段");
	}

	@Test
	@DisplayName("任何返回分级视图的控制器都必须至少激活一个视图")
	void everyControllerReturningALevelledViewActivatesOne() throws IOException {
		// The failure mode this guards: a controller returns a levelled view but never
		// activates a view, so DEFAULT_VIEW_INCLUSION=true dumps every field.
		// ProfileController
		// hit exactly this (getProfile -> UserView, getPermissions -> PermissionView).
		List<String> levelledNames = new ArrayList<>();
		try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
			for (Path p : files.filter(f -> f.toString().endsWith("View.java")).toList()) {
				String name = p.getFileName().toString().replace(".java", "");
				if (fieldViewCount(name) > 0) {
					levelledNames.add(name);
				}
			}
		}
		assertFalse(levelledNames.isEmpty(), "应至少找到一个声明了字段分级的 View");

		List<String> offenders = new ArrayList<>();
		// AuthController is the deliberate exception. It returns AccountView — now a
		// levelled
		// view — WITHOUT activating a view, because its `credential` field carries the
		// raw
		// access:refresh token pair that non-browser clients read from the login and
		// refresh
		// responses. Activating any view there would filter that field out and break
		// token
		// transport, so the guard must not fire for it.
		List<String> exempt = List.of("AuthController.java");
		try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
			for (Path p : files.filter(f -> f.toString().endsWith("Controller.java")).toList()) {
				String fileName = p.getFileName().toString();
				if (exempt.contains(fileName)) {
					continue;
				}
				String src = Files.readString(p, StandardCharsets.UTF_8);
				for (String view : levelledNames) {
					if (src.matches("(?s).*public ResponseEntity<[^>]*\\b" + view + "\\b.*")
							&& !src.contains("@JsonView(ResultView.")) {
						offenders.add(fileName + " 返回 " + view);
					}
				}
			}
		}

		assertTrue(offenders.isEmpty(), () -> "以下控制器返回分级视图却未激活任何视图，全部字段都会被输出: " + offenders
				+ "。AuthController 为刻意豁免：其 credential 承载令牌对，不能激活视图。");
	}

	@Test
	@DisplayName("分页包装器必须标注视图层级，否则激活视图时整个响应被过滤成 {}")
	void pagedResponseSurvivesAnActiveView() throws IOException {
		// PageResult wraps the levelled view, and Jackson applies view filtering to the
		// ROOT
		// object first: every property of PageResult that carries no @JsonView is
		// dropped, so
		// `list` disappears before the wrapped UserView is ever reached. The endpoint
		// then
		// answers 200 with a literal `{}` — no exception, no warning, just no data.
		PageResult<UserView> page = new PageResult<>();
		UserView user = new UserView();
		user.setId(7L);
		user.setNickname("admin");
		page.setList(List.of(user));
		page.setTotal(1);
		page.setPageNum(1);
		page.setPageSize(10);
		page.setTotalPages(1);

		String json = JsonMapper.builder().build().writerWithView(ResultView.Detail.class).writeValueAsString(page);

		assertTrue(json.contains("\"list\""), "Detail 视图下分页响应必须带 list，实际: " + json);
		assertTrue(json.contains("\"total\""), "Detail 视图下分页响应必须带 total，实际: " + json);
		assertTrue(json.contains("admin"), "Detail 视图下分页元素必须真的序列化出来，实际: " + json);
	}

	@Test
	@DisplayName("分页包装器的每个字段都必须标注视图层级")
	void pageResultAnnotatesEveryField() {
		List<String> unannotated = new ArrayList<>();
		for (Field f : PageResult.class.getDeclaredFields()) {
			if (f.isSynthetic() || Modifier.isStatic(f.getModifiers())) {
				continue;
			}
			if (!f.isAnnotationPresent(JsonView.class)) {
				unannotated.add(f.getName());
			}
		}
		assertTrue(unannotated.isEmpty(), () -> "PageResult 以下字段未标注 @JsonView：任何激活视图的分页端点都会返回 {} —— " + unannotated);
	}

	@Test
	@DisplayName("Full 视图同样必须保留分页包装器")
	void pagedResponseSurvivesTheFullView() throws IOException {
		PageResult<UserView> page = new PageResult<>();
		page.setList(List.of());
		page.setTotal(0);

		String json = JsonMapper.builder().build().writerWithView(ResultView.Full.class).writeValueAsString(page);

		assertTrue(json.contains("\"total\""), "Full 视图下分页响应必须带 total，实际: " + json);
	}

	@Test
	@DisplayName("文章视图的嵌套类型必须在激活视图下真的序列化出来")
	void nestedArticleTypesSurviveAnActiveView() throws IOException {
		// The portal card renders category / terms / tally, and the public list
		// endpoint
		// activates Detail. Those three nested types carried no @JsonView at all, so
		// Jackson
		// erased them — the card silently lost them while the endpoint still answered
		// 200.
		ArticleView article = new ArticleView();
		article.setId(1L);
		article.setTitle("水墨");

		CategoryView category = new CategoryView();
		category.setId(3);
		category.setName("技术");
		article.setCategory(category);

		TermView term = new TermView();
		term.setId(5);
		term.setName("Spring");
		article.setTerms(List.of(term));

		ArticleTally tally = new ArticleTally();
		tally.setViewCount(42L);
		article.setTally(tally);

		String json = JsonMapper.builder().build().writerWithView(ResultView.Detail.class).writeValueAsString(article);

		assertTrue(json.contains("技术"), "Detail 视图下 article.category 必须序列化出来，实际: " + json);
		assertTrue(json.contains("Spring"), "Detail 视图下 article.terms 必须序列化出来，实际: " + json);
		assertTrue(json.contains("42"), "Detail 视图下 article.tally 必须序列化出来，实际: " + json);
	}

	@Test
	@DisplayName("被分级视图嵌套引用的类型必须自己声明字段分级")
	void nestedTypesOfLevelledViewsAnnotateEveryField() {
		for (Class<?> nested : List.of(CategoryView.class, TermView.class, ArticleTally.class)) {
			List<String> unannotated = new ArrayList<>();
			for (Field f : nested.getDeclaredFields()) {
				if (f.isSynthetic() || Modifier.isStatic(f.getModifiers())) {
					continue;
				}
				if (!f.isAnnotationPresent(JsonView.class)) {
					unannotated.add(f.getName());
				}
			}
			assertTrue(unannotated.isEmpty(), () -> nested.getSimpleName()
					+ " 被 ArticleView 嵌套引用，但以下字段未标注 @JsonView，激活视图时会被整体擦除: " + unannotated);
		}
	}

	@Test
	@DisplayName("用户详情下的登录方式必须真的序列化出来")
	void nestedAccountsSurviveAnActiveView() throws IOException {
		// UserView.accounts is visible at Detail, so AccountView must declare its own
		// levels —
		// otherwise it collapses to [{}] and an administrator cannot see which login
		// methods a
		// user has. This is what made UserView.accounts unusable before it was
		// annotated.
		UserView user = new UserView();
		user.setId(2L);
		user.setNickname("admin");

		AccountView account = new AccountView();
		account.setId(11L);
		account.setUserId(2L);
		account.setIdentity("admin");
		account.setAuthType(AuthType.PASSWORD);
		account.setStatus("ENABLE");
		account.setLoginTime(LocalDateTime.of(2026, 10, 6, 22, 0));
		user.setAccounts(List.of(account));

		String json = JsonMapper.builder().build().writerWithView(ResultView.Detail.class).writeValueAsString(user);

		assertTrue(json.contains("\"accounts\":[{"), "Detail 视图下 accounts 必须是一个数组元素，实际: " + json);
		assertTrue(json.contains("admin"), "Detail 视图下 account.identity 必须序列化出来，实际: " + json);
		// AuthType serialises as its numeric code, so assert the key rather than the
		// enum name.
		assertTrue(json.contains("\"authType\":"), "Detail 视图下 account.authType 必须序列化出来，实际: " + json);
		assertTrue(json.contains("\"loginTime\":"), "Detail 视图下 account.loginTime 必须序列化出来，实际: " + json);
	}

	@Test
	@DisplayName("任何激活的视图都不得输出 account.credential")
	void accountCredentialIsNeverProjected() throws IOException {
		AccountView account = new AccountView();
		account.setId(11L);
		account.setIdentity("admin");
		account.setCredential("$2a$10$SOMEBCRYPTHASH");

		for (Class<?> active : List.of(ResultView.Basic.class, ResultView.Detail.class, ResultView.Full.class)) {
			String json = JsonMapper.builder().build().writerWithView(active).writeValueAsString(account);
			assertFalse(json.contains("credential"),
					() -> active.getSimpleName() + " 视图下不得输出 account.credential，实际: " + json);
		}
	}

	@Test
	@DisplayName("回归守卫：无激活视图时 credential 必须仍在（登录/刷新依赖它传令牌）")
	void credentialSurvivesWhenNoViewIsActivated() throws IOException {
		// /api/auth/login and /api/auth/token/refresh return AccountView WITHOUT
		// activating a
		// view, and read credential to hand the token pair to non-browser clients.
		// Jackson only
		// filters un-annotated properties when a view IS active, so leaving credential
		// un-annotated keeps that contract. Annotating it "just for consistency" would
		// silently strip the token from those responses.
		AccountView account = new AccountView();
		account.setUserId(2L);
		account.setIdentity("admin");
		account.setCredential("access.jwt.value:refresh.jwt.value");

		String json = JsonMapper.builder().build().writeValueAsString(account);

		assertTrue(json.contains("access.jwt.value:refresh.jwt.value"),
				"未激活视图时 credential 必须照常输出，否则登录与刷新接口不再返回令牌，实际: " + json);
	}

	@Test
	@DisplayName("AccountView 只允许 credential 一个字段不带 @JsonView")
	void accountViewLeavesExactlyOneFieldUnlevelled() {
		List<String> unannotated = new ArrayList<>();
		for (Field f : AccountView.class.getDeclaredFields()) {
			if (f.isSynthetic() || Modifier.isStatic(f.getModifiers())) {
				continue;
			}
			if (!f.isAnnotationPresent(JsonView.class)) {
				unannotated.add(f.getName());
			}
		}
		// credential stays un-annotated on purpose: it is a transport for the token
		// pair on the
		// login / refresh path, and must never survive an activated view.
		assertEquals(List.of("credential"), unannotated,
				"AccountView 应仅 credential 一个字段未标注 @JsonView（刻意如此），实际: " + unannotated);
	}

	/**
	 * Fields Jackson emits when {@code active} is the activated view.
	 *
	 * <p>
	 * Jackson keeps a property annotated {@code @JsonView(X)} when the active view
	 * is {@code X} <em>or a subtype of X</em> — hence
	 * {@code X.isAssignableFrom(active)}. With the hierarchy
	 * {@code Basic <- Detail <- Full}, activating {@code Detail} therefore keeps
	 * {@code Basic} and {@code Detail} fields and drops {@code Full} ones.
	 */
	private static List<String> fieldsVisibleAt(Class<?> view, Class<?> active) {
		List<String> names = new ArrayList<>();
		for (Field f : view.getDeclaredFields()) {
			if (f.isSynthetic() || Modifier.isStatic(f.getModifiers())) {
				continue;
			}
			JsonView a = f.getAnnotation(JsonView.class);
			if (a != null && Arrays.stream(a.value()).anyMatch(declared -> declared.isAssignableFrom(active))) {
				names.add(f.getName());
			}
		}
		return names;
	}

	private static int fieldViewCount(String simpleName) throws IOException {
		try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
			for (Path p : files.filter(f -> f.getFileName().toString().equals(simpleName + ".java")).toList()) {
				return (int) FIELD_VIEW.matcher(Files.readString(p, StandardCharsets.UTF_8)).results().count();
			}
		}
		return 0;
	}
}