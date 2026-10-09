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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Component;

import top.ruilink.inkwash.cms.api.view.ArticleView;
import top.ruilink.inkwash.cms.api.view.CategoryView;
import top.ruilink.inkwash.cms.api.view.TermView;
import top.ruilink.inkwash.cms.domain.Article;
import top.ruilink.inkwash.cms.api.view.ArticleTermView;
import top.ruilink.inkwash.cms.domain.Category;
import top.ruilink.inkwash.cms.domain.Term;
import top.ruilink.inkwash.cms.mapper.ArticleMapper;
import top.ruilink.inkwash.cms.mapper.CategoryMapper;
import top.ruilink.inkwash.cms.mapper.TermMapper;
import top.ruilink.inkwash.cms.service.converter.ArticleConverter;
import top.ruilink.inkwash.cms.service.converter.CategoryConverter;
import top.ruilink.inkwash.cms.service.converter.TermConverter;
import top.ruilink.inkwash.system.domain.SysUser;
import top.ruilink.inkwash.system.mapper.UserMapper;

/**
 * Resolves related author, category and term data for article views in bulk to
 * avoid N+1 queries.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Component
public class ArticleViewResolver {

	private final ArticleMapper articleMapper;
	private final CategoryMapper categoryMapper;
	private final TermMapper termMapper;
	private final UserMapper userMapper;

	public ArticleViewResolver(ArticleMapper articleMapper, CategoryMapper categoryMapper, TermMapper termMapper,
			UserMapper userMapper) {
		this.articleMapper = articleMapper;
		this.categoryMapper = categoryMapper;
		this.termMapper = termMapper;
		this.userMapper = userMapper;
	}

	public ArticleView toView(Article article) {
		if (article == null) {
			return null;
		}
		return toViews(List.of(article)).get(0);
	}

	public List<ArticleView> toViews(List<Article> articles) {
		if (articles == null || articles.isEmpty()) {
			return List.of();
		}
		List<Long> articleIds = articles.stream().map(Article::getId).filter(Objects::nonNull).distinct().toList();
		Map<Long, SysUser> authors = resolveAuthors(articles);
		Map<Integer, CategoryView> categories = resolveCategories(articles);
		Map<Long, List<TermView>> termsByArticle = resolveTerms(articleIds);
		return articles.stream().map(article -> {
			ArticleView view = ArticleConverter.toArticleView(article);
			if (view == null) {
				return null;
			}
			if (article.getAuthorId() != null) {
				SysUser author = authors.get(article.getAuthorId());
				if (author != null) {
					view.setAuthor(
							author.getNickname() != null && !author.getNickname().isBlank() ? author.getNickname()
									: author.getRealname());
				}
			}
			if (article.getCategoryId() != null) {
				view.setCategory(categories.get(article.getCategoryId()));
			}
			view.setTerms(termsByArticle.getOrDefault(article.getId(), List.of()));
			return view;
		}).toList();
	}

	private Map<Long, SysUser> resolveAuthors(List<Article> articles) {
		List<Long> authorIds = articles.stream().map(Article::getAuthorId).filter(Objects::nonNull).distinct().toList();
		if (authorIds.isEmpty()) {
			return Map.of();
		}
		List<SysUser> users = userMapper.selectByIds(authorIds);
		if (users == null) {
			return Map.of();
		}
		Map<Long, SysUser> result = new HashMap<>();
		for (SysUser author : users) {
			if (author != null && author.getId() != null) {
				result.put(author.getId(), author);
			}
		}
		return result;
	}

	private Map<Integer, CategoryView> resolveCategories(List<Article> articles) {
		List<Integer> categoryIds = articles.stream().map(Article::getCategoryId).filter(Objects::nonNull).distinct()
				.toList();
		if (categoryIds.isEmpty()) {
			return Map.of();
		}
		List<Category> categories = categoryMapper.selectByIds(categoryIds.stream().map(Integer::longValue).toList());
		Map<Integer, CategoryView> result = new HashMap<>();
		for (Category category : categories) {
			CategoryView view = CategoryConverter.toView(category);
			if (view != null && category.getId() != null) {
				result.put(category.getId(), view);
			}
		}
		return result;
	}

	private Map<Long, List<TermView>> resolveTerms(List<Long> articleIds) {
		if (articleIds.isEmpty()) {
			return Map.of();
		}
		List<ArticleTermView> rows = articleMapper.selectArticleTermsByArticleIds(articleIds);
		if (rows == null || rows.isEmpty()) {
			return Map.of();
		}
		List<Integer> termIds = rows.stream().map(ArticleTermView::getTermId).filter(Objects::nonNull).distinct()
				.toList();
		Map<Integer, TermView> termViews = new HashMap<>();
		if (!termIds.isEmpty()) {
			for (Term term : termMapper.selectByIds(termIds)) {
				TermView view = TermConverter.toView(term);
				if (view != null && term.getId() != null) {
					termViews.put(term.getId(), view);
				}
			}
		}
		Map<Long, List<TermView>> result = new HashMap<>();
		for (ArticleTermView row : rows) {
			if (row.getArticleId() == null || row.getTermId() == null) {
				continue;
			}
			result.computeIfAbsent(row.getArticleId(), k -> new ArrayList<>()).add(termViews.get(row.getTermId()));
		}
		result.replaceAll((k, v) -> v.stream().filter(Objects::nonNull).toList());
		return result;
	}
}
