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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
import top.ruilink.inkwash.base.domain.PageResult;
import top.ruilink.inkwash.base.exception.BusinessException;
import top.ruilink.inkwash.base.util.PageUtil;
import top.ruilink.inkwash.cms.api.param.CommentParam;
import top.ruilink.inkwash.cms.api.query.CommentQuery;
import top.ruilink.inkwash.cms.api.view.CommentView;
import top.ruilink.inkwash.cms.domain.Article;
import top.ruilink.inkwash.cms.domain.Comment;
import top.ruilink.inkwash.cms.domain.CommentInteraction;
import top.ruilink.inkwash.cms.mapper.ArticleMapper;
import top.ruilink.inkwash.cms.mapper.CommentInteractionMapper;
import top.ruilink.inkwash.cms.mapper.CommentMapper;
import top.ruilink.inkwash.cms.service.CommentService;
import top.ruilink.inkwash.cms.service.converter.ArticleConverter;
import top.ruilink.inkwash.security.util.SecurityUtil;
import top.ruilink.inkwash.system.domain.SysUser;
import top.ruilink.inkwash.system.mapper.UserMapper;

/**
 * Comment management service implementation.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Slf4j
@Service
public class CommentServiceImpl implements CommentService {

	private final ArticleMapper articleMapper;
	private final CommentMapper commentMapper;
	private final CommentInteractionMapper commentInteractionMapper;
	private final UserMapper userMapper;

	public CommentServiceImpl(ArticleMapper articleMapper, CommentMapper commentMapper,
			CommentInteractionMapper commentInteractionMapper, UserMapper userMapper) {
		this.articleMapper = articleMapper;
		this.commentMapper = commentMapper;
		this.commentInteractionMapper = commentInteractionMapper;
		this.userMapper = userMapper;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public CommentView addComment(Long articleId, CommentParam param) {

		Article article = articleMapper.selectById(articleId);
		if (article == null) {
			throw new BusinessException("error.article.not_found");
		}
		if (!article.canReply()) {
			throw new BusinessException("error.article.comment_not_allowed");
		}

		Long currentUserId = SecurityUtil.getCurrentUserId();

		Comment comment = ArticleConverter.toCommentEntity(param, articleId, currentUserId);
		commentMapper.insert(comment);

		articleMapper.incrementCommentCount(articleId);

		log.info("添加评论, articleId={}, commentId={}, commenterId={}", articleId, comment.getId(), currentUserId);

		CommentView view = ArticleConverter.toCommentView(comment);
		fillCommenter(view);
		return view;
	}

	@Override
	@Transactional(readOnly = true)
	public List<CommentView> getComments(Long articleId) {
		Article article = articleMapper.selectById(articleId);
		if (article == null) {
			throw new BusinessException("error.article.not_found");
		}
		// 未发布文章的评论不得公开读取，否则可按 id 枚举出草稿 / 待审核 / 已撤回文章的评论
		if (!article.canView()) {
			throw new BusinessException(HttpStatus.FORBIDDEN, "error.comment.not_viewable");
		}
		List<Comment> comments = commentMapper.selectByArticleId(articleId);
		List<CommentView> views = buildCommentTree(comments);
		fillCommenters(views);
		return views;
	}

	@Override
	@Transactional(readOnly = true)
	public PageResult<CommentView> listComments(CommentQuery query) {
		int page = query.getPage() == null ? 1 : query.getPage();
		int size = query.getSize() == null ? 10 : query.getSize();
		int offset = (page - 1) * size;
		long total = commentMapper.count(query);
		List<Comment> comments = commentMapper.selectPage(query, offset, size);
		List<CommentView> views = comments.stream().map(ArticleConverter::toCommentView).toList();
		fillCommenters(views);
		fillArticleTitles(views);
		var pageable = PageUtil.of(page, size);
		var springPage = new PageImpl<>(views, pageable, total);
		return PageResult.of(springPage);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteComment(Long commentId) {
		Comment comment = commentMapper.selectById(commentId);
		if (comment == null) {
			throw new BusinessException("error.comment.not_found");
		}
		// 只有评论作者本人或管理员可以删除，避免普通用户删除他人评论
		Long currentUserId = SecurityUtil.getCurrentUserId();
		boolean isAuthor = Objects.equals(comment.getCommenterId(), currentUserId);
		if (!isAuthor && !SecurityUtil.isAdmin()) {
			throw new BusinessException(HttpStatus.FORBIDDEN, "error.comment.not_deletable");
		}
		commentInteractionMapper.deleteByComment(commentId);
		commentMapper.deleteById(commentId);
		Article article = articleMapper.selectById(comment.getArticleId());
		if (article != null) {
			articleMapper.decrementCommentCount(comment.getArticleId());
		}
		log.info("删除评论, commentId={}, operatorId={}", commentId, currentUserId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void agreeComment(Long commentId) {
		requireComment(commentId);
		Long actorId = SecurityUtil.getCurrentUserId();
		CommentInteraction interaction = commentInteractionMapper.selectByCommentAndUser(commentId, actorId);

		if (interaction == null) {
			interaction = new CommentInteraction();
			interaction.setCommentId(commentId);
			interaction.setActorId(actorId);
			interaction.setAgree(true);
			commentInteractionMapper.insert(interaction);
			commentMapper.incrementAgreeCount(commentId);
		} else if (!Boolean.TRUE.equals(interaction.getAgree())) {
			interaction.setAgree(true);
			commentInteractionMapper.update(interaction);
			commentMapper.incrementAgreeCount(commentId);
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void unagreeComment(Long commentId) {
		requireComment(commentId);
		Long actorId = SecurityUtil.getCurrentUserId();
		CommentInteraction interaction = commentInteractionMapper.selectByCommentAndUser(commentId, actorId);

		if (interaction != null && Boolean.TRUE.equals(interaction.getAgree())) {
			interaction.setAgree(false);
			commentInteractionMapper.update(interaction);
			commentMapper.decrementAgreeCount(commentId);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public boolean isCommentAgreed(Long commentId) {
		if (!SecurityUtil.isAuthenticated()) {
			return false;
		}
		CommentInteraction interaction = commentInteractionMapper.selectByCommentAndUser(commentId,
				SecurityUtil.getCurrentUserId());
		return interaction != null && Boolean.TRUE.equals(interaction.getAgree());
	}

	private Comment requireComment(Long commentId) {
		Comment comment = commentMapper.selectById(commentId);
		if (comment == null) {
			throw new BusinessException("error.comment.not_found");
		}
		return comment;
	}

	private void fillCommenter(CommentView view) {
		if (view == null || view.getCommenterId() == null) {
			return;
		}
		SysUser user = userMapper.selectById(view.getCommenterId());
		if (user != null) {
			view.setNickname(user.getNickname() != null && !user.getNickname().isBlank() ? user.getNickname()
					: user.getRealname());
		}
	}

	/**
	 * Resolves every distinct commenter in one query.
	 *
	 * <p>
	 * Previously looped {@code userMapper.selectById} per commenter, so the public
	 * comment endpoint issued one query per comment and nested replies multiplied
	 * it (ISS-025). {@code selectByIds} collapses that into a single statement; the
	 * distinct-id set already guards against repeating the same commenter many
	 * times.
	 */
	private void fillCommenters(List<CommentView> views) {
		if (views == null) {
			return;
		}
		Set<Long> commenterIds = new HashSet<>();
		collectCommenterIds(views, commenterIds);
		if (commenterIds.isEmpty()) {
			return;
		}
		Map<Long, SysUser> users = userMapper.selectByIds(new ArrayList<>(commenterIds)).stream()
				.collect(Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));
		applyCommenters(views, users);
	}

	private void collectCommenterIds(List<CommentView> views, Set<Long> commenterIds) {
		for (CommentView view : views) {
			if (view.getCommenterId() != null) {
				commenterIds.add(view.getCommenterId());
			}
			if (view.getChildren() != null) {
				collectCommenterIds(view.getChildren(), commenterIds);
			}
		}
	}

	private void applyCommenters(List<CommentView> views, Map<Long, SysUser> users) {
		for (CommentView view : views) {
			if (view.getCommenterId() != null) {
				SysUser user = users.get(view.getCommenterId());
				if (user != null) {
					view.setNickname(user.getNickname() != null && !user.getNickname().isBlank() ? user.getNickname()
							: user.getRealname());
				}
			}
			if (view.getChildren() != null) {
				applyCommenters(view.getChildren(), users);
			}
		}
	}

	private void fillArticleTitles(List<CommentView> views) {
		List<Long> articleIds = views.stream().map(CommentView::getArticleId).filter(Objects::nonNull).distinct()
				.toList();
		if (articleIds.isEmpty()) {
			return;
		}
		Map<Long, String> titles = articleMapper.selectByIds(articleIds).stream()
				.collect(Collectors.toMap(Article::getId, Article::getTitle, (a, b) -> a));
		for (CommentView view : views) {
			view.setArticleTitle(titles.get(view.getArticleId()));
		}
	}

	private List<CommentView> buildCommentTree(List<Comment> comments) {
		Map<Long, List<Comment>> byParentId = comments.stream()
				.collect(Collectors.groupingBy(c -> c.getParentId() != null ? c.getParentId() : 0L));
		return buildChildren(0L, byParentId);
	}

	private List<CommentView> buildChildren(Long parentId, Map<Long, List<Comment>> byParentId) {
		return byParentId.getOrDefault(parentId, List.of()).stream()
				.map(c -> ArticleConverter.toCommentView(c, buildChildren(c.getId(), byParentId)))
				.collect(Collectors.toList());
	}
}
