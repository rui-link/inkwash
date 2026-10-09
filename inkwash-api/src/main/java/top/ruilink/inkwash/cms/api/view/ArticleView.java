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
package top.ruilink.inkwash.cms.api.view;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.Data;
import top.ruilink.inkwash.base.domain.ResultView;
import top.ruilink.inkwash.cms.domain.ArticleTally;
import top.ruilink.inkwash.cms.enums.ArticleStatus;

/**
 * Article response view split across three {@code ResultView} exposure levels:
 * Basic carries the list fields, Detail adds content and taxonomy, and Full
 * adds review metadata. The levels are cumulative, since Detail extends Basic
 * and Full extends Detail.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Data
public class ArticleView {
	@JsonView(ResultView.Basic.class)
	private Long id;

	@JsonView(ResultView.Basic.class)
	private String title;

	@JsonView(ResultView.Basic.class)
	private Long authorId;

	@JsonView(ResultView.Basic.class)
	private String author;

	@JsonView(ResultView.Basic.class)
	private ArticleStatus status;

	@JsonView(ResultView.Detail.class)
	private CategoryView category;

	@JsonView(ResultView.Detail.class)
	private List<TermView> terms;

	@JsonView(ResultView.Basic.class)
	private LocalDateTime publishTime;

	@JsonView(ResultView.Basic.class)
	private LocalDateTime createTime;

	// ========== Detail view fields ==========
	@JsonView(ResultView.Detail.class)
	private String slug;

	@JsonView(ResultView.Detail.class)
	private ArticleTally tally;

	@JsonView(ResultView.Detail.class)
	private LocalDateTime updateTime;

	@JsonView(ResultView.Detail.class)
	private String summary;

	@JsonView(ResultView.Detail.class)
	private String coverUrl;

	@JsonView(ResultView.Detail.class)
	private String content;

	@JsonView(ResultView.Basic.class)
	private String contentType;

	@JsonView(ResultView.Full.class)
	private Long reviewerId;

	@JsonView(ResultView.Full.class)
	private String opinion;
}
