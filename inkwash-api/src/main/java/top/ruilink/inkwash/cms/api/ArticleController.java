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
package top.ruilink.inkwash.cms.api;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import top.ruilink.inkwash.base.annotation.OperateTrace;
import top.ruilink.inkwash.base.domain.PageResult;
import top.ruilink.inkwash.cms.api.param.ArticleParam;
import top.ruilink.inkwash.cms.api.param.CommentParam;
import top.ruilink.inkwash.cms.api.query.ArticleQuery;
import com.fasterxml.jackson.annotation.JsonView;
import top.ruilink.inkwash.base.domain.ResultView;
import top.ruilink.inkwash.cms.api.query.CommentQuery;
import top.ruilink.inkwash.cms.api.view.ArticleView;
import top.ruilink.inkwash.cms.api.view.CommentView;
import top.ruilink.inkwash.cms.api.view.TallyView;
import top.ruilink.inkwash.cms.service.ArticleService;
import top.ruilink.inkwash.cms.service.CommentService;
import top.ruilink.inkwash.cms.service.InteractionService;

/**
 * Article, comment, and reader interaction REST endpoints.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Slf4j
@OperateTrace(module = "内容管理")
@Validated
@RestController
@RequestMapping("/api/cms")
public class ArticleController {

	private final ArticleService articleService;
	private final CommentService commentService;
	private final InteractionService interactionService;

	public ArticleController(ArticleService articleService, CommentService commentService,
			InteractionService interactionService) {
		this.articleService = articleService;
		this.commentService = commentService;
		this.interactionService = interactionService;
	}

	// ========== Public endpoints ==========
	@GetMapping("/articles")
	@JsonView(ResultView.Detail.class)
	public ResponseEntity<PageResult<ArticleView>> listArticles(@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "20") int size) {
		PageResult<ArticleView> result = articleService.listPublish(page, size);
		return ResponseEntity.ok(result);
	}

	@GetMapping("/articles/{id}")
	@JsonView(ResultView.Full.class)
	public ResponseEntity<ArticleView> getArticle(@PathVariable Long id) {
		ArticleView article = articleService.getArticle(id);
		return ResponseEntity.ok(article);
	}

	@GetMapping("/articles/{id}/comments")
	public ResponseEntity<List<CommentView>> getComments(@PathVariable Long id) {
		List<CommentView> comments = commentService.getComments(id);
		return ResponseEntity.ok(comments);
	}

	// ========== Authenticated endpoints ==========
	@PostMapping("/articles")
	@PreAuthorize("hasAuthority('cms:article:create')")
	@JsonView(ResultView.Full.class)
	public ResponseEntity<ArticleView> createArticle(
			@Validated(ArticleParam.Create.class) @RequestBody ArticleParam param) {
		ArticleView result = articleService.createArticle(param);
		return ResponseEntity.ok(result);
	}

	@PutMapping("/articles/{id}")
	@PreAuthorize("hasAuthority('cms:article:update')")
	@JsonView(ResultView.Full.class)
	public ResponseEntity<ArticleView> updateArticle(@PathVariable Long id,
			@Validated(ArticleParam.Update.class) @RequestBody ArticleParam param) {
		ArticleView result = articleService.updateArticle(id, param);
		return ResponseEntity.ok(result);
	}

	@PostMapping("/articles/{id}/comments")
	@PreAuthorize("hasAuthority('cms:article:comment')")
	public ResponseEntity<CommentView> addComment(@PathVariable Long id, @Valid @RequestBody CommentParam param) {
		CommentView result = commentService.addComment(id, param);
		return ResponseEntity.ok(result);
	}

	@PostMapping("/articles/{id}/agree")
	@PreAuthorize("hasAuthority('cms:article:agree')")
	public ResponseEntity<Void> agreeArticle(@PathVariable Long id) {
		interactionService.agreeArticle(id);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/articles/{id}/unagree")
	@PreAuthorize("hasAuthority('cms:article:unagree')")
	public ResponseEntity<Void> unagreeArticle(@PathVariable Long id) {
		interactionService.unagreeArticle(id);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/articles/{id}/favorite")
	@PreAuthorize("hasAuthority('cms:article:favorite')")
	public ResponseEntity<Void> favoriteArticle(@PathVariable Long id) {
		interactionService.favoriteArticle(id);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/articles/{id}/unfavorite")
	@PreAuthorize("hasAuthority('cms:article:unfavorite')")
	public ResponseEntity<Void> unfavoriteArticle(@PathVariable Long id) {
		interactionService.unfavoriteArticle(id);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/articles/{id}/share")
	@PreAuthorize("hasAuthority('cms:article:share')")
	public ResponseEntity<Void> shareArticle(@PathVariable Long id) {
		interactionService.shareArticle(id);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/articles/{id}/unshare")
	@PreAuthorize("hasAuthority('cms:article:unshare')")
	public ResponseEntity<Void> unshareArticle(@PathVariable Long id) {
		interactionService.unshareArticle(id);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/articles/{id}/averse")
	@PreAuthorize("hasAuthority('cms:article:averse')")
	public ResponseEntity<Void> averseArticle(@PathVariable Long id) {
		interactionService.averseArticle(id);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/articles/{id}/unaverse")
	@PreAuthorize("hasAuthority('cms:article:unaverse')")
	public ResponseEntity<Void> unaverseArticle(@PathVariable Long id) {
		interactionService.unaverseArticle(id);
		return ResponseEntity.ok().build();
	}

	@GetMapping("/articles/{id}/tally")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<TallyView> getTally(@PathVariable Long id) {
		TallyView view = new TallyView();
		view.setAgreed(interactionService.isAgree(id));
		view.setFavorited(interactionService.isFavorited(id));
		view.setShared(interactionService.isShared(id));
		view.setAversed(interactionService.isAversed(id));
		return ResponseEntity.ok(view);
	}

	@GetMapping("/articles/user/owner")
	@PreAuthorize("hasAuthority('cms:article:query')")
	@JsonView(ResultView.Full.class)
	public ResponseEntity<PageResult<ArticleView>> listMyArticles(@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "20") int size) {
		PageResult<ArticleView> result = articleService.listUserArticles(page, size);
		return ResponseEntity.ok(result);
	}

	@GetMapping("/articles/user/favorites")
	@PreAuthorize("hasAuthority('cms:article:query')")
	@JsonView(ResultView.Full.class)
	public ResponseEntity<PageResult<ArticleView>> getMyFavorites(@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "20") int size) {
		PageResult<ArticleView> result = interactionService.getFavorites(page, size);
		return ResponseEntity.ok(result);
	}

	@GetMapping("/articles/user/agrees")
	@PreAuthorize("hasAuthority('cms:article:query')")
	@JsonView(ResultView.Full.class)
	public ResponseEntity<PageResult<ArticleView>> getMyAgrees(@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "20") int size) {
		PageResult<ArticleView> result = interactionService.getAgrees(page, size);
		return ResponseEntity.ok(result);
	}

	@GetMapping("/articles/user/averses")
	@PreAuthorize("hasAuthority('cms:article:query')")
	@JsonView(ResultView.Full.class)
	public ResponseEntity<PageResult<ArticleView>> getMyAverses(@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "20") int size) {
		PageResult<ArticleView> result = interactionService.getAverses(page, size);
		return ResponseEntity.ok(result);
	}

	@GetMapping("/articles/user/comments")
	@PreAuthorize("hasAuthority('cms:article:query')")
	@JsonView(ResultView.Full.class)
	public ResponseEntity<PageResult<ArticleView>> getMyComments(@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "20") int size) {
		PageResult<ArticleView> result = interactionService.getCommentedArticles(page, size);
		return ResponseEntity.ok(result);
	}

	// ========== Management endpoints ==========
	@DeleteMapping("/articles/{id}")
	@PreAuthorize("hasAuthority('cms:article:delete')")
	public ResponseEntity<Void> deleteArticle(@PathVariable Long id) {
		articleService.deleteArticle(id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/articles/{id}/commit")
	@PreAuthorize("hasAuthority('cms:article:commit')")
	@JsonView(ResultView.Full.class)
	public ResponseEntity<ArticleView> commitArticle(@PathVariable Long id) {
		ArticleView result = articleService.commitArticle(id);
		return ResponseEntity.ok(result);
	}

	@PostMapping("/articles/{id}/resubmit")
	@PreAuthorize("hasAuthority('cms:article:resubmit')")
	@JsonView(ResultView.Full.class)
	public ResponseEntity<ArticleView> resubmitArticle(@PathVariable Long id) {
		ArticleView result = articleService.resubmitArticle(id);
		return ResponseEntity.ok(result);
	}

	@PostMapping("/articles/{id}/review")
	@PreAuthorize("hasAuthority('cms:article:review')")
	@JsonView(ResultView.Full.class)
	public ResponseEntity<ArticleView> reviewArticle(@PathVariable Long id, @RequestParam boolean approved,
			@RequestParam(required = false) String opinion) {
		ArticleView result = articleService.reviewArticle(id, approved, opinion != null ? opinion : "");
		return ResponseEntity.ok(result);
	}

	@PostMapping("/articles/{id}/publish")
	@PreAuthorize("hasAuthority('cms:article:publish')")
	@JsonView(ResultView.Full.class)
	public ResponseEntity<ArticleView> publishArticle(@PathVariable Long id) {
		ArticleView result = articleService.publishArticle(id);
		return ResponseEntity.ok(result);
	}

	@PostMapping("/articles/{id}/retract")
	@PreAuthorize("hasAuthority('cms:article:retract')")
	@JsonView(ResultView.Full.class)
	public ResponseEntity<ArticleView> retractArticle(@PathVariable Long id) {
		ArticleView result = articleService.retractArticle(id);
		return ResponseEntity.ok(result);
	}

	@GetMapping("/articles/listAll")
	@PreAuthorize("hasAuthority('cms:article:query')")
	@JsonView(ResultView.Full.class)
	public ResponseEntity<PageResult<ArticleView>> listAll(@Valid ArticleQuery query) {
		PageResult<ArticleView> result = articleService.listAll(query);
		return ResponseEntity.ok(result);
	}

	@GetMapping("/comments")
	@PreAuthorize("hasAuthority('cms:comment:query')")
	public ResponseEntity<PageResult<CommentView>> listComments(@Valid CommentQuery query) {
		PageResult<CommentView> result = commentService.listComments(query);
		return ResponseEntity.ok(result);
	}

	@DeleteMapping("/comments/{id}")
	@PreAuthorize("hasAuthority('cms:comment:delete')")
	public ResponseEntity<Void> deleteComment(@PathVariable Long id) {
		commentService.deleteComment(id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/comments/{id}/agree")
	@PreAuthorize("hasAuthority('cms:comment:agree')")
	public ResponseEntity<Void> agreeComment(@PathVariable Long id) {
		commentService.agreeComment(id);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/comments/{id}/unagree")
	@PreAuthorize("hasAuthority('cms:comment:unagree')")
	public ResponseEntity<Void> unagreeComment(@PathVariable Long id) {
		commentService.unagreeComment(id);
		return ResponseEntity.ok().build();
	}
}
