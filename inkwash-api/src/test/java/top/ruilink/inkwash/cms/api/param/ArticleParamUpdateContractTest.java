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
package top.ruilink.inkwash.cms.api.param;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * Locks the {@code ArticleParam} contract for the update endpoint (ISS-030).
 *
 * <p>
 * The update endpoint takes the article id from the <em>path</em> and never
 * reads it from the body — {@code ArticleConverter.updateArticleEntity} copies
 * field by field and does not touch the identifier. The payload nevertheless
 * declared {@code @NotNull(groups = Update.class) private Long id;}, which made
 * the request impossible to satisfy correctly:
 *
 * <ul>
 * <li>{@code admin/src/components/ArticleEditor.vue} sends
 * {@code {...form, status}} where {@code form} declares no {@code id}, so every
 * update returned 400;</li>
 * <li>a client that did send {@code id} had it silently discarded.</li>
 * </ul>
 *
 * <p>
 * {@code ArticleControllerTest} calls the controller method directly, which
 * bypasses {@code @Validated} entirely — that is why this survived. These
 * assertions therefore go through a real {@link Validator} instead.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@DisplayName("ArticleParam 更新契约（ISS-030）")
class ArticleParamUpdateContractTest {

	private static Set<ConstraintViolation<ArticleParam>> validateUpdate(ArticleParam param) {
		try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
			Validator validator = factory.getValidator();
			return validator.validate(param, ArticleParam.Update.class);
		}
	}

	private static Set<ConstraintViolation<ArticleParam>> validateCreate(ArticleParam param) {
		try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
			Validator validator = factory.getValidator();
			return validator.validate(param, ArticleParam.Create.class);
		}
	}

	/** Mirrors the object {@code ArticleEditor.vue} puts on the wire. */
	private static ArticleParam frontendUpdatePayload() {
		ArticleParam param = new ArticleParam();
		param.setTitle("文章标题");
		param.setContent("正文");
		param.setContentType("markdown");
		param.setSummary("摘要");
		param.setCoverUrl("");
		param.setCategoryId(1);
		param.setTermIds(java.util.List.of(2, 3));
		return param;
	}

	@Test
	@DisplayName("前端实际发送的 payload（不含 id）应通过 Update 分组校验")
	void frontendShapedPayloadValidatesOnUpdate() {
		Set<ConstraintViolation<ArticleParam>> violations = validateUpdate(frontendUpdatePayload());

		assertTrue(violations.isEmpty(), () -> "更新请求被校验拒绝: "
				+ violations.stream().map(v -> v.getPropertyPath() + " -> " + v.getMessage()).toList());
	}

	@Test
	@DisplayName("ArticleParam 不应再声明 id 字段")
	void idFieldIsRemoved() {
		Field id = null;
		try {
			id = ArticleParam.class.getDeclaredField("id");
		} catch (NoSuchFieldException expected) {
			// the point of the test
		}

		assertNull(id, "ArticleParam.id 应移除：更新端点的 id 取自 path，保留它只会诱导调用方填写并被忽略");
		assertTrue(Arrays.stream(ArticleParam.class.getMethods()).noneMatch(m -> m.getName().equals("setId")),
				"ArticleParam 不应再暴露 setId");
	}

	@Test
	@DisplayName("updateArticle 的 id 参数标注 @PathVariable")
	void updateArticleIdComesFromThePath() throws NoSuchMethodException {
		Method m = top.ruilink.inkwash.cms.api.ArticleController.class.getMethod("updateArticle", Long.class,
				ArticleParam.class);

		assertTrue(
				Arrays.stream(m.getParameterAnnotations()[0]).anyMatch(
						a -> a.annotationType() == org.springframework.web.bind.annotation.PathVariable.class),
				"updateArticle 的第一个参数必须是 @PathVariable，id 不来自 body");
		assertEquals(Long.class, m.getParameterTypes()[0]);
	}

	@Test
	@DisplayName("Update 分组不要求 title —— 更新是部分更新（转换器逐字段判空）")
	void updateIsPartialSoTitleIsOptional() {
		// Update requires content + categoryId (@NotNull sits in Default, which Update
		// extends)
		// but NOT title (@NotBlank is scoped to Create), matching the field-by-field
		// copy in
		// ArticleConverter.updateArticleEntity.
		ArticleParam param = new ArticleParam();
		param.setContent("只改正文");
		param.setCategoryId(7);

		Set<ConstraintViolation<ArticleParam>> violations = validateUpdate(param);

		assertTrue(violations.isEmpty(), () -> "更新只提交部分字段时应通过校验: "
				+ violations.stream().map(v -> v.getPropertyPath() + " -> " + v.getMessage()).toList());
	}

	@Test
	@DisplayName("Update 分组仍要求 content 与 categoryId")
	void updateStillRequiresContentAndCategory() {
		Set<ConstraintViolation<ArticleParam>> violations = validateUpdate(new ArticleParam());

		assertTrue(violations.stream().anyMatch(v -> "content".contentEquals(String.valueOf(v.getPropertyPath()))),
				"content 在 Update 下仍必填");
		assertTrue(violations.stream().anyMatch(v -> "categoryId".contentEquals(String.valueOf(v.getPropertyPath()))),
				"categoryId 在 Update 下仍必填");
		assertTrue(violations.stream().noneMatch(v -> "id".contentEquals(String.valueOf(v.getPropertyPath()))),
				"id 不应再出现在任何校验违规中");
	}

	@Test
	@DisplayName("Create 分组仍要求 title")
	void createStillRequiresTitle() {
		ArticleParam param = frontendUpdatePayload();
		param.setTitle(null);

		Set<ConstraintViolation<ArticleParam>> violations = validateCreate(param);

		assertTrue(violations.stream().anyMatch(v -> "title".contentEquals(String.valueOf(v.getPropertyPath()))),
				() -> "创建时 title 必须必填，实际违规: "
						+ violations.stream().map(v -> String.valueOf(v.getPropertyPath())).toList());
	}

	@Test
	@DisplayName("空白 title 在 Create 分组下被 @NotBlank 拦截")
	void createRejectsBlankTitle() {
		ArticleParam param = frontendUpdatePayload();
		param.setTitle("   ");

		Set<ConstraintViolation<ArticleParam>> violations = validateCreate(param);

		assertTrue(violations.stream().anyMatch(v -> "title".contentEquals(String.valueOf(v.getPropertyPath()))),
				"@NotBlank 应拒绝纯空白标题");
	}

	@Test
	@DisplayName("Create 分组同样不要求 id")
	void createGroupDoesNotRequireId() {
		Set<ConstraintViolation<ArticleParam>> violations = validateCreate(frontendUpdatePayload());

		assertTrue(violations.stream().noneMatch(v -> "id".contentEquals(String.valueOf(v.getPropertyPath()))),
				"创建时 id 由数据库生成，Create 分组不应要求它");
	}
}