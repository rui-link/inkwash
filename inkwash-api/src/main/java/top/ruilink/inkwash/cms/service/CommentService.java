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

import java.util.List;

import top.ruilink.inkwash.base.domain.PageResult;
import top.ruilink.inkwash.cms.api.param.CommentParam;
import top.ruilink.inkwash.cms.api.query.CommentQuery;
import top.ruilink.inkwash.cms.api.view.CommentView;

/**
 * Comment management service interface.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public interface CommentService {

	CommentView addComment(Long articleId, CommentParam param);

	List<CommentView> getComments(Long articleId);

	PageResult<CommentView> listComments(CommentQuery query);

	void deleteComment(Long commentId);

	/**
	 * Toggles the current user's like on a comment.
	 *
	 * <p>
	 * Backed by {@code cms_comment_interaction}, a table mirroring
	 * {@code cms_interaction} but keyed by comment. Repeated calls are idempotent:
	 * a user holds at most one like per comment.
	 */
	void agreeComment(Long commentId);

	/** Removes the current user's like on a comment. */
	void unagreeComment(Long commentId);

	/** @return whether the current user has liked the comment */
	boolean isCommentAgreed(Long commentId);
}
