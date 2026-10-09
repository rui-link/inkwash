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
package top.ruilink.inkwash.system.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import top.ruilink.inkwash.base.domain.PageResult;
import top.ruilink.inkwash.system.api.param.NoticeParam;
import top.ruilink.inkwash.system.api.param.NoticeReadParam;
import top.ruilink.inkwash.system.api.query.NoticeQuery;
import top.ruilink.inkwash.system.api.view.NoticeView;
import top.ruilink.inkwash.base.annotation.OperateTrace;
import top.ruilink.inkwash.security.util.SecurityUtil;
import top.ruilink.inkwash.system.domain.SysNotice;
import top.ruilink.inkwash.system.service.NoticeService;

/**
 * System notice administration and personal read-state REST endpoints.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Slf4j
@OperateTrace(module = "系统管理")
@RestController
@RequestMapping("/api/system/notices")
@Validated
public class NoticeController {

	private final NoticeService noticeService;

	public NoticeController(NoticeService noticeService) {
		this.noticeService = noticeService;
	}

	@GetMapping
	@PreAuthorize("hasAuthority('system:notice:query')")
	public ResponseEntity<PageResult<NoticeView>> listNotices(NoticeQuery query) {
		var result = noticeService.pageSearch(query);
		return ResponseEntity.ok(result);
	}

	@GetMapping("/active")
	@PreAuthorize("permitAll")
	public List<NoticeView> getActiveNotices() {
		return noticeService.getActiveNotices();
	}

	@GetMapping("/unread-count")
	public ResponseEntity<Map<String, Long>> unreadCount() {
		Map<String, Long> body = new HashMap<>();
		body.put("count", noticeService.unreadCount(SecurityUtil.getCurrentUserId()));
		return ResponseEntity.ok(body);
	}

	@PostMapping("/read")
	public ResponseEntity<Void> markRead(@RequestBody NoticeReadParam param) {
		Long userId = SecurityUtil.getCurrentUserId();
		if (!Boolean.TRUE.equals(param.getAll()) && (param.getIds() == null || param.getIds().isEmpty())) {
			return ResponseEntity.badRequest().build();
		}
		if (Boolean.TRUE.equals(param.getAll())) {
			noticeService.markAllRead(userId);
		} else {
			noticeService.markRead(param.getIds(), userId);
		}
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/delete")
	public ResponseEntity<Void> deleteOwn(@RequestBody List<Long> ids) {
		noticeService.deleteOwn(ids, SecurityUtil.getCurrentUserId());
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/clear")
	public ResponseEntity<Void> clearOwn() {
		noticeService.clearOwn(SecurityUtil.getCurrentUserId());
		return ResponseEntity.ok().build();
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAnyAuthority('system:notice:detail')")
	public ResponseEntity<?> getById(@PathVariable Long id) {
		SysNotice notice = noticeService.getById(id);
		if (notice == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(notice);
	}

	@PostMapping
	@PreAuthorize("hasAnyAuthority('system:notice:create')")
	public ResponseEntity<Long> create(@Valid @RequestBody NoticeParam param) {
		log.info("创建公告, title={}", param.getTitle());
		Long id = noticeService.create(param);
		return ResponseEntity.ok(id);
	}

	@PutMapping
	@PreAuthorize("hasAnyAuthority('system:notice:update')")
	public ResponseEntity<Void> update(@Valid @RequestBody NoticeParam param) {
		log.info("更新公告, id={}", param.getId());
		noticeService.update(param);
		return ResponseEntity.ok().build();
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasAnyAuthority('system:notice:delete')")
	public ResponseEntity<Void> deleteById(@PathVariable Long id) {
		noticeService.deleteById(id);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/batch")
	@PreAuthorize("hasAnyAuthority('system:notice:delete')")
	public ResponseEntity<Void> deleteByIds(@RequestBody List<Long> ids) {
		noticeService.deleteByIds(ids);
		return ResponseEntity.noContent().build();
	}
}
