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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package top.ruilink.inkwash.cms.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import top.ruilink.inkwash.cms.domain.Comment;
import top.ruilink.inkwash.cms.mapper.CommentInteractionMapper;
import top.ruilink.inkwash.cms.mapper.CommentMapper;
import top.ruilink.inkwash.cms.service.impl.CommentServiceImpl;
import top.ruilink.inkwash.system.domain.SysUser;
import top.ruilink.inkwash.system.mapper.UserMapper;

/**
 * Locks the single-query commenter resolution in {@code CommentServiceImpl}
 * (ISS-025).
 *
 * <p>
 * {@code fillCommenters} used to call {@code userMapper.selectById} once per
 * distinct commenter, so the public comment endpoint's query count grew with
 * the number of comments — and {@code fillCommenters} recurses into replies,
 * multiplying it further. It now issues one {@code selectByIds}.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("评论者批量解析（ISS-025）")
class CommentServiceNPlusOneTest {

	@Mock
	private CommentMapper commentMapper;
	@Mock
	private CommentInteractionMapper commentInteractionMapper;
	@Mock
	private UserMapper userMapper;
	@Mock
	private top.ruilink.inkwash.cms.mapper.ArticleMapper articleMapper;

	@InjectMocks
	private CommentServiceImpl commentService;

	private static Comment comment(long id, long articleId, Long commenterId, Long parentId) {
		Comment c = new Comment();
		c.setId(id);
		c.setArticleId(articleId);
		c.setCommenterId(commenterId);
		c.setParentId(parentId);
		c.setContent("c" + id);
		return c;
	}

	private static SysUser user(long id, String nickname) {
		SysUser u = new SysUser();
		u.setId(id);
		u.setNickname(nickname);
		return u;
	}

	/**
	 * {@code getComments} refuses to read comments of anything but a published
	 * article (enumeration guard), so every test needs a published article in
	 * place.
	 */
	private void givenPublishedArticle(long articleId) {
		top.ruilink.inkwash.cms.domain.Article article = new top.ruilink.inkwash.cms.domain.Article();
		article.setId(articleId);
		article.setStatus(top.ruilink.inkwash.cms.enums.ArticleStatus.PUBLISHED);
		article.setTitle("已发布文章");
		when(articleMapper.selectById(articleId)).thenReturn(article);
	}

	@Test
	@DisplayName("多位不同评论者只触发一次批量查询")
	void distinctCommentersUseOneBatchQuery() {
		List<Comment> comments = List.of(comment(1L, 10L, 100L, null), comment(2L, 10L, 200L, null),
				comment(3L, 10L, 300L, null), comment(4L, 10L, 400L, null));
		givenPublishedArticle(10L);
		when(commentMapper.selectByArticleId(10L)).thenReturn(new ArrayList<>(comments));
		when(userMapper.selectByIds(any()))
				.thenReturn(List.of(user(100L, "甲"), user(200L, "乙"), user(300L, "丙"), user(400L, "丁")));

		var views = commentService.getComments(10L);

		verify(userMapper, times(1)).selectByIds(any());
		verify(userMapper, never()).selectById(any());
		assertEquals(4, views.size());
	}

	@Test
	@DisplayName("重复评论者会被去重，批量查询只带一个 id")
	void duplicateCommentersAreDeduplicated() {
		List<Comment> comments = List.of(comment(1L, 10L, 100L, null), comment(2L, 10L, 100L, null),
				comment(3L, 10L, 100L, null));
		givenPublishedArticle(10L);
		when(commentMapper.selectByArticleId(10L)).thenReturn(new ArrayList<>(comments));
		when(userMapper.selectByIds(any())).thenReturn(List.of(user(100L, "甲")));

		commentService.getComments(10L);

		org.mockito.ArgumentCaptor<List<Long>> captor = org.mockito.ArgumentCaptor.forClass(List.class);
		verify(userMapper, times(1)).selectByIds(captor.capture());
		assertEquals(List.of(100L), captor.getValue(), "同一位评论者只应出现在批量查询中一次");
	}

	@Test
	@DisplayName("嵌套回复不产生额外查询")
	void nestedRepliesDoNotAddQueries() {
		// flat storage: parent_id links replies, so one batch covers the whole thread
		List<Comment> comments = List.of(comment(1L, 10L, 100L, null), comment(2L, 10L, 200L, 1L),
				comment(3L, 10L, 300L, 2L), comment(4L, 10L, 400L, 3L));
		givenPublishedArticle(10L);
		when(commentMapper.selectByArticleId(10L)).thenReturn(new ArrayList<>(comments));
		when(userMapper.selectByIds(any()))
				.thenReturn(List.of(user(100L, "甲"), user(200L, "乙"), user(300L, "丙"), user(400L, "丁")));

		var views = commentService.getComments(10L);

		verify(userMapper, atMostOnce()).selectByIds(any());
		verify(userMapper, never()).selectById(any());
		// tree is still assembled: one root with a nested chain
		assertEquals(1, views.size());
	}

	@Test
	@DisplayName("没有评论时完全不查用户表")
	void noCommentsMeansNoUserQuery() {
		givenPublishedArticle(10L);
		when(commentMapper.selectByArticleId(10L)).thenReturn(new ArrayList<>());

		var views = commentService.getComments(10L);

		assertTrue(views.isEmpty());
		verify(userMapper, never()).selectByIds(any());
		verify(userMapper, never()).selectById(any());
	}

	@Test
	@DisplayName("批量查询返回缺失用户时不抛异常")
	void missingUsersAreTolerated() {
		List<Comment> comments = List.of(comment(1L, 10L, 100L, null), comment(2L, 10L, 999L, null));
		givenPublishedArticle(10L);
		when(commentMapper.selectByArticleId(10L)).thenReturn(new ArrayList<>(comments));
		// user 999 was deleted between the comment write and this read
		when(userMapper.selectByIds(any())).thenReturn(List.of(user(100L, "甲")));

		var views = commentService.getComments(10L);

		assertEquals(2, views.size());
	}
}