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
package top.ruilink.inkwash.cms.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
import top.ruilink.inkwash.base.domain.PageResult;
import top.ruilink.inkwash.base.exception.BusinessException;
import top.ruilink.inkwash.base.util.PageUtil;
import top.ruilink.inkwash.cms.api.view.ArticleView;
import top.ruilink.inkwash.cms.domain.Article;
import top.ruilink.inkwash.cms.domain.Interaction;
import top.ruilink.inkwash.cms.mapper.ArticleMapper;
import top.ruilink.inkwash.cms.mapper.CommentMapper;
import top.ruilink.inkwash.cms.mapper.InteractionMapper;
import top.ruilink.inkwash.cms.service.ArticleViewResolver;
import top.ruilink.inkwash.cms.service.InteractionService;
import top.ruilink.inkwash.security.util.SecurityUtil;

/**
 * Article interaction service implementation.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Slf4j
@Service
public class InteractionServiceImpl implements InteractionService {

	private final ArticleMapper articleMapper;
	private final InteractionMapper interactionMapper;
	private final CommentMapper commentMapper;
	private final ArticleViewResolver articleViewResolver;

	public InteractionServiceImpl(ArticleMapper articleMapper, InteractionMapper interactionMapper,
			CommentMapper commentMapper, ArticleViewResolver articleViewResolver) {
		this.articleMapper = articleMapper;
		this.interactionMapper = interactionMapper;
		this.commentMapper = commentMapper;
		this.articleViewResolver = articleViewResolver;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void agreeArticle(Long articleId) {
		Article article = requireArticle(articleId);
		if (!article.canReply()) {
			throw new BusinessException("error.article.like_not_allowed");
		}
		toggleInteraction(article, Interaction::getAgree, Interaction::setAgree, articleMapper::incrementAgreeCount,
				articleMapper::decrementAgreeCount);
		log.info("点赞文章, articleId={}, userId={}", articleId, SecurityUtil.getCurrentUserId());
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void unagreeArticle(Long articleId) {
		Article article = requireArticle(articleId);
		toggleInteractionOff(article, Interaction::getAgree, Interaction::setAgree, articleMapper::decrementAgreeCount);
		log.info("取消点赞, articleId={}, userId={}", articleId, SecurityUtil.getCurrentUserId());
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void favoriteArticle(Long articleId) {
		Article article = requireArticle(articleId);
		if (!article.canReply()) {
			throw new BusinessException("error.article.favorite_not_allowed");
		}
		toggleInteraction(article, Interaction::getFavorite, Interaction::setFavorite,
				articleMapper::incrementFavoriteCount, articleMapper::decrementFavoriteCount);
		log.info("收藏文章, articleId={}, userId={}", articleId, SecurityUtil.getCurrentUserId());
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void unfavoriteArticle(Long articleId) {
		Article article = requireArticle(articleId);
		toggleInteractionOff(article, Interaction::getFavorite, Interaction::setFavorite,
				articleMapper::decrementFavoriteCount);
		log.info("取消收藏, articleId={}, userId={}", articleId, SecurityUtil.getCurrentUserId());
	}

	@Override
	@Transactional(readOnly = true)
	public boolean isAgree(Long articleId) {
		return isInteractionEnabled(articleId, Interaction::getAgree);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean isFavorited(Long articleId) {
		return isInteractionEnabled(articleId, Interaction::getFavorite);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void shareArticle(Long articleId) {
		Article article = requireArticle(articleId);
		toggleInteraction(article, Interaction::getShare, Interaction::setShare, articleMapper::incrementShareCount,
				articleMapper::decrementShareCount);
		log.info("分享文章, articleId={}, userId={}", articleId, SecurityUtil.getCurrentUserId());
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void unshareArticle(Long articleId) {
		Article article = requireArticle(articleId);
		toggleInteractionOff(article, Interaction::getShare, Interaction::setShare, articleMapper::decrementShareCount);
		log.info("取消分享, articleId={}, userId={}", articleId, SecurityUtil.getCurrentUserId());
	}

	@Override
	@Transactional(readOnly = true)
	public boolean isShared(Long articleId) {
		return isInteractionEnabled(articleId, Interaction::getShare);
	}

	@Override
	@Transactional(readOnly = true)
	public PageResult<ArticleView> getFavorites(int page, int size) {
		Long userId = SecurityUtil.getCurrentUserId();
		return pageFromIds((limit, offset) -> interactionMapper.selectFavoriteArticleIdsPaged(userId, limit, offset),
				() -> interactionMapper.countFavorites(userId), page, size);
	}

	@Override
	@Transactional(readOnly = true)
	public PageResult<ArticleView> getAgrees(int page, int size) {
		Long userId = SecurityUtil.getCurrentUserId();
		return pageFromIds((limit, offset) -> interactionMapper.selectAgreeArticleIdsPaged(userId, limit, offset),
				() -> interactionMapper.countAgrees(userId), page, size);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void averseArticle(Long articleId) {
		Article article = requireArticle(articleId);
		toggleInteraction(article, Interaction::getAverse, Interaction::setAverse, articleMapper::incrementAverseCount,
				articleMapper::decrementAverseCount);
		log.info("点踩文章, articleId={}, userId={}", articleId, SecurityUtil.getCurrentUserId());
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void unaverseArticle(Long articleId) {
		Article article = requireArticle(articleId);
		toggleInteractionOff(article, Interaction::getAverse, Interaction::setAverse,
				articleMapper::decrementAverseCount);
		log.info("取消点踩, articleId={}, userId={}", articleId, SecurityUtil.getCurrentUserId());
	}

	@Override
	@Transactional(readOnly = true)
	public boolean isAversed(Long articleId) {
		return isInteractionEnabled(articleId, Interaction::getAverse);
	}

	@Override
	@Transactional(readOnly = true)
	public PageResult<ArticleView> getAverses(int page, int size) {
		Long userId = SecurityUtil.getCurrentUserId();
		return pageFromIds((limit, offset) -> interactionMapper.selectAverseArticleIdsPaged(userId, limit, offset),
				() -> interactionMapper.countAverses(userId), page, size);
	}

	@Override
	@Transactional(readOnly = true)
	public PageResult<ArticleView> getCommentedArticles(int page, int size) {
		Long userId = SecurityUtil.getCurrentUserId();
		return pageFromIds(
				(limit, offset) -> commentMapper.selectDistinctArticleIdsByCommenterPaged(userId, limit, offset),
				() -> commentMapper.countDistinctArticlesByCommenter(userId), page, size);
	}

	// ========== Internal helpers ==========

	private Article requireArticle(Long articleId) {
		Article article = articleMapper.selectById(articleId);
		if (article == null) {
			throw new BusinessException("error.article.not_found");
		}
		article.initTally();
		return article;
	}

	/**
	 * Generic toggle: turn an interaction field ON for the current user. Creates a
	 * new interaction if none exists, or updates the existing one.
	 */
	private void toggleInteraction(Article article, Function<Interaction, Boolean> getter,
			BiConsumer<Interaction, Boolean> setter, Consumer<Long> onIncrement, Consumer<Long> onDecrement) {
		Long userId = SecurityUtil.getCurrentUserId();
		Interaction interaction = interactionMapper.selectByArticleAndUser(article.getId(), userId);

		if (interaction == null) {
			interaction = new Interaction();
			interaction.setArticleId(article.getId());
			interaction.setActorId(userId);
			interaction.setAgree(false);
			interaction.setFavorite(false);
			interaction.setShare(false);
			interaction.setAverse(false);
			// Stamped here, on the insert branch only. The update branch below must leave
			// createTime untouched: it records the first interaction between the pair, and
			// the "my favourites/likes" lists order by it (D-16).
			interaction.setCreateTime(LocalDateTime.now());
			setter.accept(interaction, true);
			interactionMapper.insert(interaction);
			onIncrement.accept(article.getId());
		} else if (!Boolean.TRUE.equals(getter.apply(interaction))) {
			setter.accept(interaction, true);
			interactionMapper.update(interaction);
			onIncrement.accept(article.getId());
		}
	}

	/**
	 * Generic toggle: turn an interaction field OFF for the current user.
	 */
	private void toggleInteractionOff(Article article, Function<Interaction, Boolean> getter,
			BiConsumer<Interaction, Boolean> setter, Consumer<Long> onDecrement) {
		Long userId = SecurityUtil.getCurrentUserId();
		Interaction interaction = interactionMapper.selectByArticleAndUser(article.getId(), userId);

		if (interaction != null && Boolean.TRUE.equals(getter.apply(interaction))) {
			setter.accept(interaction, false);
			interactionMapper.update(interaction);
			onDecrement.accept(article.getId());
		}
	}

	/**
	 * Check if an interaction field is enabled for the current user.
	 */
	private boolean isInteractionEnabled(Long articleId, Function<Interaction, Boolean> getter) {
		if (!SecurityUtil.isAuthenticated()) {
			return false;
		}
		Long userId = SecurityUtil.getCurrentUserId();
		Interaction interaction = interactionMapper.selectByArticleAndUser(articleId, userId);
		return interaction != null && Boolean.TRUE.equals(getter.apply(interaction));
	}

	private PageResult<ArticleView> pageFromIds(BiFunction<Integer, Integer, List<Long>> idQuery,
			Supplier<Long> countQuery, int page, int size) {
		int offset = (page - 1) * size;
		List<Long> pageIds = idQuery.apply(size, offset);

		if (pageIds.isEmpty()) {
			return PageResult.of(Page.empty());
		}

		long total = countQuery.get();
		var articleMap = articleMapper.selectByIds(pageIds).stream().collect(Collectors.toMap(Article::getId, a -> a));
		var articleList = pageIds.stream().filter(articleMap::containsKey).map(articleMap::get).toList();
		List<ArticleView> articles = articleViewResolver.toViews(articleList);
		var pageable = PageUtil.of(page, size);
		var springPage = new PageImpl<>(articles, pageable, total);
		return PageResult.of(springPage);
	}
}
