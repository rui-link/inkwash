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
package top.ruilink.inkwash.cms.service.converter;

import java.time.LocalDateTime;
import java.util.List;

import top.ruilink.inkwash.base.util.SlugUtil;
import top.ruilink.inkwash.cms.api.param.ArticleParam;
import top.ruilink.inkwash.cms.api.param.CommentParam;
import top.ruilink.inkwash.cms.api.view.ArticleView;
import top.ruilink.inkwash.cms.api.view.CommentView;
import top.ruilink.inkwash.cms.domain.Article;
import top.ruilink.inkwash.cms.domain.ArticleTally;
import top.ruilink.inkwash.cms.domain.Comment;
import top.ruilink.inkwash.cms.domain.CommentTally;
import top.ruilink.inkwash.cms.enums.ArticleStatus;

/**
 * CMS entity converter centralising the Article to View and Param mappings.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public final class ArticleConverter {

	private ArticleConverter() {
	}

	// ==================== Article conversion ====================

	public static ArticleView toArticleView(Article article) {
		if (article == null)
			return null;
		// Ensure tally is initialized from flat columns
		article.initTally();

		ArticleView view = new ArticleView();
		view.setId(article.getId());
		view.setTitle(article.getTitle());
		view.setSlug(article.getSlug());
		view.setSummary(article.getSummary());
		view.setCoverUrl(article.getCoverUrl());
		view.setContent(article.getContent());
		view.setContentType(article.getContentType());
		view.setAuthorId(article.getAuthorId());
		view.setStatus(article.getStatus());
		view.setPublishTime(article.getPublishTime());
		view.setTally(article.getTally());
		view.setCreateTime(article.getCreateTime());
		view.setUpdateTime(article.getUpdateTime());
		view.setReviewerId(article.getReviewerId());
		view.setOpinion(article.getOpinion());
		return view;
	}

	public static Article toArticleEntity(ArticleParam param, Long authorId) {
		if (param == null)
			return null;
		Article article = new Article();
		article.setTitle(param.getTitle());
		article.setContent(param.getContent());
		article.setContentType(param.getContentType() != null ? param.getContentType() : "markdown");
		article.setSummary(param.getSummary());
		article.setCoverUrl(param.getCoverUrl());
		article.setSlug(SlugUtil.generate(param.getTitle()));
		article.setAuthorId(authorId);
		article.setCategoryId(param.getCategoryId());
		article.setTermIds(param.getTermIds());
		article.setStatus(ArticleStatus.DRAFT);
		article.setTally(new ArticleTally());
		article.setCreator(authorId);
		article.setCreateTime(LocalDateTime.now());
		article.setUpdateTime(LocalDateTime.now());
		return article;
	}

	public static void updateArticleEntity(Article article, ArticleParam param) {
		if (article == null || param == null)
			return;
		if (param.getTitle() != null) {
			article.setTitle(param.getTitle());
			article.setSlug(SlugUtil.generate(param.getTitle()));
		}
		if (param.getContent() != null)
			article.setContent(param.getContent());
		if (param.getContentType() != null)
			article.setContentType(param.getContentType());
		if (param.getSummary() != null)
			article.setSummary(param.getSummary());
		if (param.getCoverUrl() != null)
			article.setCoverUrl(param.getCoverUrl());
		if (param.getCategoryId() != null)
			article.setCategoryId(param.getCategoryId());
		if (param.getTermIds() != null)
			article.setTermIds(param.getTermIds());
		article.setUpdateTime(LocalDateTime.now());
	}

	// ==================== Comment conversion ====================

	public static CommentView toCommentView(Comment comment) {
		if (comment == null)
			return null;
		CommentView view = new CommentView();
		view.setId(comment.getId());
		view.setArticleId(comment.getArticleId());
		view.setCommenterId(comment.getCommenterId());
		view.setParentId(comment.getParentId());
		view.setContent(comment.getContent());
		view.setTally(comment.getTally());
		view.setCreateTime(comment.getCreateTime());
		return view;
	}

	public static CommentView toCommentView(Comment comment, List<CommentView> children) {
		CommentView view = toCommentView(comment);
		if (view != null) {
			view.setChildren(children);
		}
		return view;
	}

	public static Comment toCommentEntity(CommentParam param, Long articleId, Long commenterId) {
		if (param == null)
			return null;
		Comment comment = new Comment();
		comment.setArticleId(articleId);
		comment.setCommenterId(commenterId);
		comment.setParentId(param.getParentId());
		comment.setContent(param.getContent());
		comment.setTally(new CommentTally());
		comment.setCreateTime(LocalDateTime.now());
		return comment;
	}
}
