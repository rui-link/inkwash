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
package top.ruilink.inkwash.cms.service;

import top.ruilink.inkwash.base.domain.PageResult;
import top.ruilink.inkwash.cms.api.view.ArticleView;

/**
 * Article interaction service interface.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public interface InteractionService {

	void agreeArticle(Long articleId);

	void unagreeArticle(Long articleId);

	void favoriteArticle(Long articleId);

	void unfavoriteArticle(Long articleId);

	boolean isAgree(Long articleId);

	boolean isFavorited(Long articleId);

	boolean isShared(Long articleId);

	void averseArticle(Long articleId);

	void unaverseArticle(Long articleId);

	boolean isAversed(Long articleId);

	void shareArticle(Long articleId);

	void unshareArticle(Long articleId);

	PageResult<ArticleView> getFavorites(int page, int size);

	PageResult<ArticleView> getAgrees(int page, int size);

	PageResult<ArticleView> getAverses(int page, int size);

	PageResult<ArticleView> getCommentedArticles(int page, int size);
}
