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
package top.ruilink.inkwash.cms.api.param;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.Default;
import lombok.Data;

/**
 * Article request payload, a single VO whose validation groups separate create
 * from update.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Data
public class ArticleParam {
	/**
	 * Validation group for creation
	 */
	public interface Create extends Default {
	}

	/**
	 * Validation group for update
	 */
	public interface Update extends Default {
	}

	// No `id` field (ISS-030): the update endpoint reads the article id from the
	// path and
	// `ArticleConverter.updateArticleEntity` copies field by field, so a body id
	// could never
	// take effect. Declaring `@NotNull(groups = Update.class)` on it made every
	// request from
	// the admin editor fail with 400, because its payload contains no id.

	@NotBlank(message = "文章标题不能为空", groups = Create.class)
	@Size(min = 1, max = 240, message = "标题长度必须在1-240之间")
	@Pattern(regexp = "^(?!.*(<script|javascript:|on\\w+=)).*$", message = "标题包含危险内容", flags = Pattern.Flag.CASE_INSENSITIVE)
	private String title;

	@Size(max = 500, message = "摘要长度不能超过500")
	private String summary;

	@Size(max = 240, message = "封面图片URL过长")
	private String coverUrl;

	@NotBlank(message = "文章内容不能为空")
	@Size(max = 50000, message = "内容长度不能超过50000")
	private String content;

	private String contentType;

	@NotNull(message = "分类ID不能为空")
	private Integer categoryId;

	private List<Integer> termIds;

	/**
	 * Editorial comment addressed to the author.
	 *
	 * <p>
	 * Bounded to match the {@code cms_article.opinion VARCHAR(240)} column. Without
	 * this the value could exceed the column and surface as a database truncation
	 * error instead of a 400 validation failure (ISS-028).
	 */
	@Size(max = 240, message = "审核意见长度不能超过240")
	private String opinion;
}
