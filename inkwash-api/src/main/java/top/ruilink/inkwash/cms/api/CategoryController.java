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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package top.ruilink.inkwash.cms.api;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import com.fasterxml.jackson.annotation.JsonView;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import top.ruilink.inkwash.base.annotation.OperateTrace;
import top.ruilink.inkwash.base.domain.PageResult;
import top.ruilink.inkwash.base.domain.ResultView;
import top.ruilink.inkwash.cms.api.param.CategoryParam;
import top.ruilink.inkwash.cms.api.query.CategoryQuery;
import top.ruilink.inkwash.cms.api.view.CategoryView;
import top.ruilink.inkwash.cms.service.CategoryService;

/**
 * Category REST endpoints.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@OperateTrace(module = "内容管理")
@RestController
@RequestMapping("/api/cms/categories")
@Validated
public class CategoryController {

	private final CategoryService categoryService;

	public CategoryController(CategoryService categoryService) {
		this.categoryService = categoryService;
	}

	@GetMapping
	@JsonView(ResultView.Basic.class)
	public ResponseEntity<PageResult<CategoryView>> list(@Valid CategoryQuery query) {
		PageResult<CategoryView> result = categoryService.listCategories(query);
		return ResponseEntity.ok(result);
	}

	@GetMapping("/tree")
	@JsonView(ResultView.Basic.class)
	public ResponseEntity<List<CategoryView>> tree() {
		List<CategoryView> result = categoryService.listCategoryTree();
		return ResponseEntity.ok(result);
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('cms:category:detail')")
	@JsonView(ResultView.Basic.class)
	public ResponseEntity<CategoryView> detail(@PathVariable Long id) {
		CategoryView result = categoryService.getCategory(id);
		return ResponseEntity.ok(result);
	}

	@PostMapping
	@PreAuthorize("hasAuthority('cms:category:create')")
	@JsonView(ResultView.Basic.class)
	public ResponseEntity<CategoryView> create(@Valid @RequestBody CategoryParam param) {
		CategoryView result = categoryService.createCategory(param);
		return ResponseEntity.ok(result);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('cms:category:update')")
	@JsonView(ResultView.Basic.class)
	public ResponseEntity<CategoryView> update(@PathVariable Long id, @Valid @RequestBody CategoryParam param) {
		CategoryView result = categoryService.updateCategory(id, param);
		return ResponseEntity.ok(result);
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasAuthority('cms:category:delete')")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		categoryService.deleteCategory(id);
		return ResponseEntity.noContent().build();
	}
}
