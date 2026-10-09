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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import top.ruilink.inkwash.base.support.file.service.FileUploadService;
import top.ruilink.inkwash.base.util.SortValidator;
import top.ruilink.inkwash.cms.domain.Article;
import top.ruilink.inkwash.cms.domain.ArticleTally;
import top.ruilink.inkwash.cms.enums.ArticleStatus;
import top.ruilink.inkwash.cms.mapper.ArticleMapper;
import top.ruilink.inkwash.cms.mapper.CategoryMapper;
import top.ruilink.inkwash.cms.mapper.CommentInteractionMapper;
import top.ruilink.inkwash.cms.mapper.CommentMapper;
import top.ruilink.inkwash.cms.mapper.InteractionMapper;
import top.ruilink.inkwash.cms.mapper.TermMapper;
import top.ruilink.inkwash.cms.service.impl.ArticleServiceImpl;
import top.ruilink.inkwash.security.adapter.SysUserDetails;
import top.ruilink.inkwash.system.domain.SysUser;

/**
 * Guards the cover-file deletion ordering introduced by ISS-050.
 *
 * <p>
 * The bug being pinned down: the cover was unlinked from storage
 * <em>before</em> the database rows were removed inside the same transaction. A
 * rollback therefore left a live article whose {@code cover_url} pointed at a
 * file that no longer existed. The fix defers the unlink to
 * {@code afterCommit}.
 */
class ArticleCoverDeletionTest {

	private static final String COVER_URL = "2026/10/06/cover.png";

	private FileUploadService fileUploadService;
	private ArticleMapper articleMapper;
	private ArticleServiceImpl articleService;

	@BeforeEach
	void setUp() {
		fileUploadService = mock(FileUploadService.class);
		articleMapper = mock(ArticleMapper.class);

		articleService = new ArticleServiceImpl(articleMapper, mock(SensitiveService.class), mock(CategoryMapper.class),
				mock(TermMapper.class), mock(SortValidator.class), fileUploadService,
				mock(ApplicationEventPublisher.class), mock(ArticleViewResolver.class), mock(CommentMapper.class),
				mock(CommentInteractionMapper.class), mock(InteractionMapper.class));

		SecurityContext context = mock(SecurityContext.class);
		Authentication authentication = mock(Authentication.class);
		SysUserDetails principal = new SysUserDetails(mockUser(), null,
				List.of(new SimpleGrantedAuthority("cms:article:create")));
		when(context.getAuthentication()).thenReturn(authentication);
		when(authentication.getPrincipal()).thenReturn(principal);
		SecurityContextHolder.setContext(context);
	}

	private static SysUser mockUser() {
		SysUser user = new SysUser();
		user.setId(1L);
		return user;
	}

	private static Article draftWithCover(String coverUrl) {
		Article article = new Article();
		article.setId(10L);
		article.setAuthorId(1L);
		article.setStatus(ArticleStatus.DRAFT);
		article.setTally(new ArticleTally());
		article.setCoverUrl(coverUrl);
		return article;
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	/**
	 * Drives the {@code afterCommit} callbacks that a real transaction would fire.
	 */
	private static void commit() {
		TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
		TransactionSynchronizationManager.clearSynchronization();
	}

	@Test
	@DisplayName("事务提交前不删除封面文件")
	void coverIsNotDeletedBeforeCommit() {
		when(articleMapper.selectById(10L)).thenReturn(draftWithCover(COVER_URL));
		TransactionSynchronizationManager.initSynchronization();

		articleService.deleteArticle(10L);

		verify(fileUploadService, never()).deleteByKey(any());
	}

	@Test
	@DisplayName("事务提交后才删除封面文件")
	void coverIsDeletedAfterCommit() {
		when(articleMapper.selectById(10L)).thenReturn(draftWithCover(COVER_URL));
		TransactionSynchronizationManager.initSynchronization();

		articleService.deleteArticle(10L);
		commit();

		verify(fileUploadService, times(1)).deleteByKey(COVER_URL);
	}

	@Test
	@DisplayName("事务回滚时封面文件保持不变，文章行不会指向已删文件")
	void rollbackLeavesCoverIntact() {
		when(articleMapper.selectById(10L)).thenReturn(draftWithCover(COVER_URL));
		TransactionSynchronizationManager.initSynchronization();

		articleService.deleteArticle(10L);
		// simulate a rollback: afterCommit callbacks never run
		TransactionSynchronizationManager.clearSynchronization();

		verify(fileUploadService, never()).deleteByKey(any());
	}

	@Test
	@DisplayName("无事务上下文时立即删除，不静默跳过")
	void withoutTransaction_deletesImmediately() {
		when(articleMapper.selectById(10L)).thenReturn(draftWithCover(COVER_URL));

		articleService.deleteArticle(10L);

		verify(fileUploadService, times(1)).deleteByKey(COVER_URL);
	}

	@Test
	@DisplayName("封面删除失败不影响文章删除结果")
	void coverFailure_doesNotBreakArticleDeletion() {
		when(articleMapper.selectById(10L)).thenReturn(draftWithCover(COVER_URL));
		doThrow(new IllegalStateException("object storage unreachable")).when(fileUploadService).deleteByKey(COVER_URL);
		TransactionSynchronizationManager.initSynchronization();

		articleService.deleteArticle(10L);
		assertDoesNotThrow(() -> commit());

		verify(articleMapper, times(1)).deleteById(10L);
	}

	@Test
	@DisplayName("封面为空时不做任何文件操作")
	void nullCover_isSkipped() {
		when(articleMapper.selectById(10L)).thenReturn(draftWithCover(null));
		TransactionSynchronizationManager.initSynchronization();

		articleService.deleteArticle(10L);
		commit();

		verify(fileUploadService, never()).deleteByKey(any());
	}
}