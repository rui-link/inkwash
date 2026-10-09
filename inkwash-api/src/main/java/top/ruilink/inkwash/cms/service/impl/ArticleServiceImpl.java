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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import lombok.extern.slf4j.Slf4j;
import top.ruilink.inkwash.base.domain.PageResult;
import top.ruilink.inkwash.base.exception.BusinessException;
import top.ruilink.inkwash.base.support.file.service.FileUploadService;
import top.ruilink.inkwash.base.util.PageUtil;
import top.ruilink.inkwash.base.util.SortValidator;
import top.ruilink.inkwash.cms.api.param.ArticleParam;
import top.ruilink.inkwash.cms.api.query.ArticleQuery;
import top.ruilink.inkwash.cms.api.view.ArticleView;
import top.ruilink.inkwash.cms.api.view.CategoryView;
import top.ruilink.inkwash.cms.api.view.TermView;
import top.ruilink.inkwash.cms.domain.Article;
import top.ruilink.inkwash.cms.domain.Category;
import top.ruilink.inkwash.cms.domain.Term;
import top.ruilink.inkwash.cms.enums.ArticleStatus;
import top.ruilink.inkwash.cms.event.ArticleNoticeEvent;
import top.ruilink.inkwash.cms.mapper.ArticleMapper;
import top.ruilink.inkwash.cms.mapper.CategoryMapper;
import top.ruilink.inkwash.cms.mapper.CommentInteractionMapper;
import top.ruilink.inkwash.cms.mapper.CommentMapper;
import top.ruilink.inkwash.cms.mapper.InteractionMapper;
import top.ruilink.inkwash.cms.mapper.TermMapper;
import top.ruilink.inkwash.cms.service.ArticleService;
import top.ruilink.inkwash.cms.service.ArticleViewResolver;
import top.ruilink.inkwash.cms.service.SensitiveService;
import top.ruilink.inkwash.cms.service.converter.ArticleConverter;
import top.ruilink.inkwash.cms.service.converter.CategoryConverter;
import top.ruilink.inkwash.cms.service.converter.TermConverter;
import top.ruilink.inkwash.base.util.SensitiveMatcher;
import top.ruilink.inkwash.security.util.SecurityUtil;
import top.ruilink.inkwash.security.xss.XssUtil;
import top.ruilink.inkwash.system.domain.SysUser;

/**
 * Article management service implementation.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Slf4j
@Service
public class ArticleServiceImpl implements ArticleService {

	private final ArticleMapper articleMapper;
	private final SensitiveService sensitiveService;
	private final CategoryMapper categoryMapper;
	private final TermMapper termMapper;
	private final SortValidator sortValidator;
	private final FileUploadService fileUploadService;
	private final ApplicationEventPublisher eventPublisher;
	private final ArticleViewResolver articleViewResolver;
	/** Injected so deleteArticle can cascade; see ISS-012. */
	private final CommentMapper commentMapper;
	private final CommentInteractionMapper commentInteractionMapper;
	private final InteractionMapper interactionMapper;

	public ArticleServiceImpl(ArticleMapper articleMapper, SensitiveService sensitiveService,
			CategoryMapper categoryMapper, TermMapper termMapper, SortValidator sortValidator,
			FileUploadService fileUploadService, ApplicationEventPublisher eventPublisher,
			ArticleViewResolver articleViewResolver, CommentMapper commentMapper,
			CommentInteractionMapper commentInteractionMapper, InteractionMapper interactionMapper) {
		this.articleMapper = articleMapper;
		this.sensitiveService = sensitiveService;
		this.categoryMapper = categoryMapper;
		this.termMapper = termMapper;
		this.sortValidator = sortValidator;
		this.fileUploadService = fileUploadService;
		this.eventPublisher = eventPublisher;
		this.articleViewResolver = articleViewResolver;
		this.commentMapper = commentMapper;
		this.commentInteractionMapper = commentInteractionMapper;
		this.interactionMapper = interactionMapper;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public ArticleView createArticle(ArticleParam param) {

		Long authorId = SecurityUtil.getCurrentUserId();

		param.setContent(XssUtil.sanitizeMarkdown(param.getContent()));
		if (param.getTitle() != null) {
			param.setTitle(XssUtil.sanitizeMarkdown(param.getTitle()));
		}

		Article article = ArticleConverter.toArticleEntity(param, authorId);

		articleMapper.insert(article);
		Long id = article.getId();
		syncTerms(article);

		log.info("创建文章, articleId={}, authorId={}", id, authorId);
		return toView(article);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public ArticleView updateArticle(Long articleId, ArticleParam param) {

		Article article = articleMapper.selectById(articleId);
		if (article == null) {
			throw new BusinessException("error.article.not_found");
		}

		Long currentUserId = SecurityUtil.getCurrentUserId();

		if (!article.canEdit(currentUserId)) {
			throw new BusinessException(HttpStatus.FORBIDDEN, "error.article.not_editable");
		}

		if (param.getContent() != null) {
			param.setContent(XssUtil.sanitizeMarkdown(param.getContent()));
		}
		if (param.getTitle() != null) {
			param.setTitle(XssUtil.sanitizeMarkdown(param.getTitle()));
		}

		ArticleConverter.updateArticleEntity(article, param);
		articleMapper.update(article);
		syncTerms(article);

		log.info("修改文章, articleId={}, authorId={}", articleId, currentUserId);
		return toView(article);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteArticle(Long articleId) {
		Article article = articleMapper.selectById(articleId);
		if (article == null) {
			throw new BusinessException("error.article.not_found");
		}

		Long currentUserId = SecurityUtil.getCurrentUserId();
		boolean isAdmin = SecurityUtil.isAdmin();

		if (!article.canDelete(currentUserId, isAdmin)) {
			throw new BusinessException(HttpStatus.FORBIDDEN, "error.article.not_deletable");
		}

		String coverUrl = article.getCoverUrl();

		// Order matters: comment-interaction rows reference comments, so they must go
		// first.
		// Replies are stored flat (parent_id), so one statement per table covers the
		// whole thread.
		// Without these, cms_comment / cms_interaction accumulate one orphan row per
		// (article, reader) pair and the "my favourites/likes" queries return deleted
		// articles (ISS-012).
		commentInteractionMapper.deleteByArticleId(articleId);
		commentMapper.deleteByArticleId(articleId);
		interactionMapper.deleteByArticleId(articleId);
		articleMapper.deleteById(articleId);
		articleMapper.deleteTermsByArticleId(articleId);
		deleteCoverAfterCommit(coverUrl, articleId);
		log.info("删除文章, articleId={}, operatorId={}, isAdmin={}", articleId, currentUserId, isAdmin);
	}

	/**
	 * Removes the cover file only once the surrounding transaction has committed
	 * (ISS-050).
	 *
	 * <p>
	 * Deleting the file inline, before the row deletions, made the two sides
	 * disagree in both directions: a later rollback left a live article row
	 * pointing at a file that no longer existed, and a failed file deletion left an
	 * orphan file behind after the row was gone. Registering an {@code afterCommit}
	 * callback removes the first failure mode entirely — a rollback simply never
	 * runs the callback, so the file stays with its article.
	 *
	 * <p>
	 * The second failure mode (a storage error <em>after</em> commit) cannot be
	 * undone by ordering, so it is logged at error level with both identifiers and
	 * left to a future compensation pass; see the residual note in
	 * {@code ISSUES.md}.
	 */
	private void deleteCoverAfterCommit(String coverUrl, Long articleId) {
		if (coverUrl == null || coverUrl.isEmpty()) {
			return;
		}
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			deleteCover(coverUrl, articleId);
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				deleteCover(coverUrl, articleId);
			}
		});
	}

	private void deleteCover(String coverUrl, Long articleId) {
		try {
			fileUploadService.deleteByKey(coverUrl);
		} catch (Exception e) {
			log.error("删除文章封面文件失败，文章行已删除但文件留存（需补偿任务清理）, articleId={}, coverUrl={}", articleId, coverUrl, e);
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public ArticleView commitArticle(Long articleId) {
		Article article = articleMapper.selectById(articleId);
		if (article == null) {
			throw new BusinessException("error.article.not_found");
		}

		Long currentUserId = SecurityUtil.getCurrentUserId();

		if (!article.getAuthorId().equals(currentUserId)) {
			throw new BusinessException(HttpStatus.FORBIDDEN, "error.article.not_owner");
		}

		// One read, not two: the matcher owns the word list it was built from, so
		// resolving
		// match indices through it can never mix snapshots (ISS-015). Fetching the list
		// separately would let `words.get(idx)` return a different word, or blow up,
		// when
		// the cache was evicted between the two calls.
		SensitiveMatcher matcher = sensitiveService.getMatcher();
		List<String> matched = new ArrayList<>();

		if (article.getTitle() != null) {
			List<Integer> titleMatches = matcher.findMatches(article.getTitle());
			for (int idx : titleMatches) {
				matched.add(matcher.wordAt(idx));
			}
		}
		if (article.getContent() != null) {
			List<Integer> contentMatches = matcher.findMatches(article.getContent());
			for (int idx : contentMatches) {
				String word = matcher.wordAt(idx);
				if (!matched.contains(word)) {
					matched.add(word);
				}
			}
		}
		if (!matched.isEmpty()) {
			throw new BusinessException("error.sensitive.content_blocked", String.join(", ", matched));
		}

		int expected = article.getStatus().getCode();
		article.submit();
		article.setUpdater(currentUserId);
		applyTransition(article, expected);
		eventPublisher.publishEvent(new ArticleNoticeEvent(article.getId(), article.getTitle(), article.getAuthorId(),
				ArticleStatus.PENDING.getCode(), currentUserId, currentNickname(), null));
		log.info("提交文章审核, articleId={}, authorId={}", articleId, currentUserId);
		return toView(article);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public ArticleView resubmitArticle(Long articleId) {
		Article article = articleMapper.selectById(articleId);
		if (article == null) {
			throw new BusinessException("error.article.not_found");
		}

		Long currentUserId = SecurityUtil.getCurrentUserId();
		if (!article.getAuthorId().equals(currentUserId)) {
			throw new BusinessException(HttpStatus.FORBIDDEN, "error.article.not_owner");
		}

		int expected = article.getStatus().getCode();
		article.resubmit();
		article.setUpdater(currentUserId);
		applyTransition(article, expected);
		log.info("驳回后重新送审, articleId={}, authorId={}", articleId, currentUserId);
		return toView(article);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public ArticleView reviewArticle(Long articleId, boolean approved, String proposal) {
		Article article = articleMapper.selectById(articleId);
		if (article == null) {
			throw new BusinessException("error.article.not_found");
		}

		if (!article.canReview()) {
			throw new BusinessException("error.article.review_requires_pending");
		}

		// 自我审核防护：作者不得审核自己提交的文章（职责分离）
		Long currentUserId = SecurityUtil.getCurrentUserId();
		if (Objects.equals(article.getAuthorId(), currentUserId)) {
			throw new BusinessException(HttpStatus.FORBIDDEN, "error.article.not_reviewable_self");
		}

		int expected = article.getStatus().getCode();
		if (approved) {
			article.approve(currentUserId);
		} else {
			article.reject(currentUserId, proposal);
		}

		applyTransition(article, expected);
		eventPublisher.publishEvent(new ArticleNoticeEvent(article.getId(), article.getTitle(), article.getAuthorId(),
				article.getStatus().getCode(), currentUserId, currentNickname(), proposal));

		log.info("审核文章, articleId={}, approved={}", articleId, approved);
		return toView(article);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public ArticleView publishArticle(Long articleId) {
		Article article = articleMapper.selectById(articleId);
		if (article == null) {
			throw new BusinessException("error.article.not_found");
		}

		if (!article.canPublish()) {
			throw new BusinessException("error.article.publish_requires_approved");
		}

		// 自我发布防护：作者不得发布自己提交的文章
		if (Objects.equals(article.getAuthorId(), SecurityUtil.getCurrentUserId())) {
			throw new BusinessException(HttpStatus.FORBIDDEN, "error.article.not_publishable_self");
		}

		int expected = article.getStatus().getCode();
		article.publish();
		applyTransition(article, expected);
		eventPublisher.publishEvent(new ArticleNoticeEvent(article.getId(), article.getTitle(), article.getAuthorId(),
				ArticleStatus.PUBLISHED.getCode(), null, null, null));

		log.info("发布文章, articleId={}", articleId);
		return toView(article);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public ArticleView retractArticle(Long articleId) {
		Article article = articleMapper.selectById(articleId);
		if (article == null) {
			throw new BusinessException("error.article.not_found");
		}

		Long currentUserId = SecurityUtil.getCurrentUserId();
		boolean isAdmin = SecurityUtil.isAdmin();

		int expected = article.getStatus().getCode();
		article.retract(currentUserId, isAdmin);
		applyTransition(article, expected);

		log.info("撤回文章, articleId={}, operatorId={}", articleId, currentUserId);
		return toView(article);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public ArticleView getArticle(Long articleId) {
		Article article = articleMapper.selectById(articleId);
		if (article == null) {
			throw new BusinessException("error.article.not_found");
		}
		if (article.canView()) {
			articleMapper.incrementViewCount(articleId);
			article.initTally();
			article.getTally().incrementView();
			return toView(article);
		}
		Long currentUserId;
		try {
			currentUserId = SecurityUtil.getCurrentUserId();
		} catch (BusinessException e) {
			throw new BusinessException(HttpStatus.FORBIDDEN, "error.article.not_viewable");
		}
		boolean isAdmin = SecurityUtil.isAdmin();
		if (article.getAuthorId().equals(currentUserId) || isAdmin || SecurityUtil.hasAuthority("cms:article:review")) {
			return toView(article);
		}
		throw new BusinessException(HttpStatus.FORBIDDEN, "error.article.not_viewable");
	}

	@Override
	@Transactional(readOnly = true)
	public PageResult<ArticleView> listPublish(int page, int size) {
		int offset = (page - 1) * size;
		List<Article> articles = articleMapper.selectPublished(offset, size);
		long total = articleMapper.countPublished();
		var views = articleViewResolver.toViews(articles);
		var pageable = PageUtil.of(page, size);
		var springPage = new PageImpl<>(views, pageable, total);
		return PageResult.of(springPage);
	}

	@Override
	@Transactional(readOnly = true)
	public PageResult<ArticleView> listAll(ArticleQuery query) {
		var sortSql = sortValidator.resolve(query.getSortField(), query.getSortOrder(), "create_time DESC");
		var offset = (long) (query.getPage() - 1) * query.getSize();
		var total = articleMapper.countArticles(query);
		var list = articleMapper.selectArticleList(query, offset, query.getSize(), sortSql);
		var views = articleViewResolver.toViews(list);
		var pageable = PageUtil.of(query.getPage(), query.getSize());
		var page = new PageImpl<>(views, pageable, total);
		return PageResult.of(page);
	}

	@Override
	@Transactional(readOnly = true)
	public PageResult<ArticleView> listUserArticles(int page, int size) {
		Long userId = SecurityUtil.getCurrentUserId();
		int offset = (page - 1) * size;
		List<Article> articles = articleMapper.selectByAuthorId(userId, offset, size);
		long total = articleMapper.countByAuthorId(userId);
		var views = articleViewResolver.toViews(articles);
		var pageable = PageUtil.of(page, size);
		var springPage = new PageImpl<>(views, pageable, total);
		return PageResult.of(springPage);
	}

	@Override
	@Transactional(readOnly = true)
	public List<CategoryView> listActiveCategories() {
		List<Category> categories = categoryMapper.selectAll();
		return categories.stream().map(CategoryConverter::toView).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<TermView> listActiveTerms() {
		List<Term> terms = termMapper.selectAll();
		return terms.stream().map(TermConverter::toView).toList();
	}

	private String currentNickname() {
		try {
			SysUser user = SecurityUtil.getCurrentUser();
			if (user.getNickname() != null && !user.getNickname().isBlank()) {
				return user.getNickname();
			}
			return SecurityUtil.getCurrentUsername();
		} catch (Exception e) {
			return null;
		}
	}

	private void syncTerms(Article article) {
		articleMapper.deleteTermsByArticleId(article.getId());
		if (article.getTermIds() != null && !article.getTermIds().isEmpty()) {
			articleMapper.insertArticleTerms(article.getId(), article.getTermIds());
		}
	}

	private ArticleView toView(Article article) {
		return articleViewResolver.toView(article);
	}

	/**
	 * 以「期望的旧状态」为条件提交状态迁移，实现乐观并发控制。
	 *
	 * <p>
	 * 迁移方法在内存中完成状态变更后调用本方法。若受影响行数为 0，说明 读取与写入之间已有其他迁移改变了状态，本次迁移必须放弃而非静默覆盖。
	 */
	private void applyTransition(Article article, int expectedStatus) {
		int rows = articleMapper.updateStatus(article, expectedStatus);
		if (rows == 0) {
			throw new BusinessException("error.article.status_conflict");
		}
	}
}