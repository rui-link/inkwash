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
package top.ruilink.inkwash.cms.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import lombok.Getter;
import lombok.Setter;
import top.ruilink.inkwash.base.domain.BaseEntity;
import org.springframework.http.HttpStatus;

import top.ruilink.inkwash.base.exception.BusinessException;
import top.ruilink.inkwash.cms.enums.ArticleStatus;

/**
 * Article entity whose status machine is DRAFT(1), PENDING(2), APPROVED(3),
 * REJECTED(4), PUBLISHED(5) and RETRACTED(6).
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Getter
@Setter
public class Article extends BaseEntity {
	private static final long serialVersionUID = 1L;

	/**
	 * Article ID.
	 */
	private Long id;

	/**
	 * Article title.
	 */
	private String title;

	/**
	 * URL-friendly article slug.
	 */
	private String slug;

	/**
	 * Short summary of the article content.
	 */
	private String summary;

	/**
	 * Cover image URL.
	 */
	private String coverUrl;
	/**
	 * Article content.
	 */
	private String content;

	/**
	 * Content type: markdown, html or plain.
	 */
	private String contentType;

	/**
	 * Author ID.
	 */
	private Long authorId;

	/**
	 * Category ID.
	 */
	private Integer categoryId;

	/**
	 * List of term IDs.
	 */
	private List<Integer> termIds;

	/**
	 * Article status.
	 */
	private ArticleStatus status;

	/**
	 * Reviewer ID.
	 */
	private Long reviewerId;

	/**
	 * Review comment.
	 */
	private String opinion;

	/**
	 * Article access statistics, a value object mapped from flat columns.
	 */
	private ArticleTally tally;

	// ========== Flat column mappings MyBatis fills in to initialise tally
	// ==========
	private transient Long viewCount = 0L;
	private transient Long commentCount = 0L;
	private transient Long agreeCount = 0L;
	private transient Long favoriteCount = 0L;
	private transient Long shareCount = 0L;
	private transient Long averseCount = 0L;

	/**
	 * Publication time.
	 */
	private LocalDateTime publishTime;

	/**
	 * Initialises tally from the flat columns, called after a MyBatis query.
	 */
	public void initTally() {
		if (this.tally == null) {
			this.tally = new ArticleTally();
		}
		this.tally.setViewCount(this.viewCount);
		this.tally.setCommentCount(this.commentCount);
		this.tally.setAgreeCount(this.agreeCount);
		this.tally.setFavoriteCount(this.favoriteCount);
		this.tally.setShareCount(this.shareCount);
		this.tally.setAverseCount(this.averseCount);
	}

	/**
	 * Whether the given user may modify this article.
	 */
	public boolean canEdit(Long userId) {
		if (status == ArticleStatus.PUBLISHED) {
			return false;
		}
		return Objects.equals(authorId, userId);
	}

	/**
	 * Whether the given user may delete this article.
	 */
	public boolean canDelete(Long userId, boolean isAdmin) {
		if (Objects.equals(authorId, userId) && status == ArticleStatus.DRAFT) {
			return true;
		}
		return isAdmin;
	}

	/**
	 * Whether an editor may approve this article.
	 */
	public boolean canReview() {
		return status == ArticleStatus.PENDING;
	}

	/**
	 * Whether the article may be published.
	 */
	public boolean canPublish() {
		return status == ArticleStatus.APPROVED;
	}

	/**
	 * Whether the article may be retracted.
	 */
	public boolean canRetract(Long userId, boolean isAdmin) {
		if (status != ArticleStatus.PUBLISHED) {
			return false;
		}
		// The author or an administrator may retract
		return Objects.equals(authorId, userId) || isAdmin;
	}

	public void submit() {
		if (status != ArticleStatus.DRAFT) {
			throw new BusinessException("error.article.submit_requires_draft");
		}
		this.status = ArticleStatus.PENDING;
		this.setUpdateTime(LocalDateTime.now());
	}

	public void approve(Long reviewerId) {
		if (status != ArticleStatus.PENDING) {
			throw new BusinessException("error.article.approve_requires_pending");
		}
		this.status = ArticleStatus.APPROVED;
		this.reviewerId = reviewerId;
		this.setUpdateTime(LocalDateTime.now());
	}

	public void reject(Long reviewerId, String opinion) {
		if (status != ArticleStatus.PENDING) {
			throw new BusinessException("error.article.reject_requires_pending");
		}
		this.status = ArticleStatus.REJECTED;
		this.reviewerId = reviewerId;
		this.opinion = opinion;
		this.setUpdateTime(LocalDateTime.now());
	}

	public void publish() {
		if (status != ArticleStatus.APPROVED) {
			throw new BusinessException("error.article.publish_requires_approved");
		}
		this.status = ArticleStatus.PUBLISHED;
		this.publishTime = LocalDateTime.now();
		this.setUpdateTime(LocalDateTime.now());
	}

	public void retract(Long userId, boolean isAdmin) {
		if (!canRetract(userId, isAdmin)) {
			throw new BusinessException(HttpStatus.FORBIDDEN, "error.article.not_retractable");
		}
		this.status = ArticleStatus.RETRACTED;
		this.setUpdateTime(LocalDateTime.now());
	}

	public void resubmit() {
		if (this.status != ArticleStatus.REJECTED) {
			throw new BusinessException("error.article.resubmit_requires_rejected");
		}
		this.status = ArticleStatus.PENDING;
		this.opinion = null;
		this.setUpdateTime(LocalDateTime.now());
	}

	/**
	 * Whether the article is visible, that is published.
	 */
	public boolean canView() {
		return status == ArticleStatus.PUBLISHED;
	}

	/**
	 * Whether the article accepts comments and likes, that is published.
	 */
	public boolean canReply() {
		return status == ArticleStatus.PUBLISHED;
	}
}
