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

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import top.ruilink.inkwash.cms.api.view.DashboardView;
import top.ruilink.inkwash.cms.enums.ArticleStatus;
import top.ruilink.inkwash.cms.mapper.ArticleMapper;
import top.ruilink.inkwash.cms.mapper.CommentMapper;
import top.ruilink.inkwash.cms.mapper.InteractionMapper;
import top.ruilink.inkwash.cms.service.DashboardService;

/**
 * Role-based dashboard data service implementation.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Service
public class DashboardServiceImpl implements DashboardService {

	private final ArticleMapper articleMapper;
	private final CommentMapper commentMapper;
	private final InteractionMapper interactionMapper;

	public DashboardServiceImpl(ArticleMapper articleMapper, CommentMapper commentMapper,
			InteractionMapper interactionMapper) {
		this.articleMapper = articleMapper;
		this.commentMapper = commentMapper;
		this.interactionMapper = interactionMapper;
	}

	@Override
	@Transactional(readOnly = true)
	public DashboardView.UserDashboard getUserDashboard(Long userId) {
		DashboardView.UserDashboard dashboard = new DashboardView.UserDashboard();
		dashboard.setMyArticles(articleMapper.countByAuthorId(userId));
		dashboard.setMyComments(commentMapper.countByCommenter(userId));
		dashboard.setMyFavorites(interactionMapper.countFavoritesByUser(userId));
		dashboard.setMyAgrees(interactionMapper.countAgreesByUser(userId));
		dashboard.setAgreesReceived(interactionMapper.countAgreesReceived(userId));
		dashboard.setFavoritesReceived(interactionMapper.countFavoritesReceived(userId));
		dashboard.setCommentsReceived(commentMapper.countDistinctArticlesByCommenter(userId));
		return dashboard;
	}

	@Override
	@Transactional(readOnly = true)
	public DashboardView.EditorDashboard getEditorDashboard() {
		DashboardView.EditorDashboard dashboard = new DashboardView.EditorDashboard();
		dashboard.setPendingReview(articleMapper.countByStatus(ArticleStatus.PENDING.getCode()));
		dashboard.setApproved(articleMapper.countByStatus(ArticleStatus.APPROVED.getCode()));
		dashboard.setRejected(articleMapper.countByStatus(ArticleStatus.REJECTED.getCode()));
		dashboard.setRecentArticles(
				articleMapper.selectByStatusLimit(ArticleStatus.PENDING.getCode(), 5).stream().map(a -> {
					var s = new DashboardView.ArticleSummary();
					s.setId(a.getId());
					s.setTitle(a.getTitle());
					s.setStatus(a.getStatus());
					s.setCreateTime(a.getCreateTime());
					return s;
				}).toList());
		return dashboard;
	}

	@Override
	@Transactional(readOnly = true)
	public DashboardView.AdminDashboard getAdminDashboard() {
		DashboardView.AdminDashboard dashboard = new DashboardView.AdminDashboard();
		dashboard.setPublished(articleMapper.countByStatus(ArticleStatus.PUBLISHED.getCode()));
		dashboard.setRetracted(articleMapper.countByStatus(ArticleStatus.RETRACTED.getCode()));
		dashboard.setPendingPublish(articleMapper.countByStatus(ArticleStatus.APPROVED.getCode()));
		dashboard.setRecentArticles(
				articleMapper.selectByStatusLimit(ArticleStatus.PUBLISHED.getCode(), 5).stream().map(a -> {
					var s = new DashboardView.ArticleSummary();
					s.setId(a.getId());
					s.setTitle(a.getTitle());
					s.setStatus(a.getStatus());
					s.setCreateTime(a.getCreateTime());
					return s;
				}).toList());
		return dashboard;
	}
}
