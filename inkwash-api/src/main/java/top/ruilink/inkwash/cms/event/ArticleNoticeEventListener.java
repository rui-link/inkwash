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
package top.ruilink.inkwash.cms.event;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import top.ruilink.inkwash.cms.enums.ArticleStatus;
import top.ruilink.inkwash.system.domain.SysNotice;
import top.ruilink.inkwash.system.enums.NoticeType;
import top.ruilink.inkwash.system.mapper.NoticeMapper;
import top.ruilink.inkwash.system.mapper.UserMapper;

/**
 * Article status notice event listener.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Slf4j
@Component
public class ArticleNoticeEventListener {

	private final NoticeMapper noticeMapper;
	private final UserMapper userMapper;

	public ArticleNoticeEventListener(NoticeMapper noticeMapper, UserMapper userMapper) {
		this.noticeMapper = noticeMapper;
		this.userMapper = userMapper;
	}

	/**
	 * Fans out workflow notifications for an article status transition.
	 *
	 * <p>
	 * Notification failure must not roll back the article transition that already
	 * committed, so the listener deliberately never rethrows. That makes logging
	 * the <em>only</em> signal that a notification was lost, and the previous
	 * {@code log.warn("...: {}", e.getMessage())} was too weak:
	 * {@code getMessage()} on a {@code BusinessException} is the resolved i18n
	 * text, which loses the exception type and stack trace entirely (ISS-039). An
	 * unknown status code, for example, arrived here as a bare
	 * "无效的枚举编码：99，类型：ArticleStatus" with nothing tying it to the article or the
	 * failing path.
	 *
	 * <p>
	 * The message now carries the article id and status code, and the throwable is
	 * passed through so the stack trace reaches the log.
	 */
	@EventListener
	public void handle(ArticleNoticeEvent event) {
		try {
			ArticleStatus status = ArticleStatus.fromCode(event.statusCode());
			switch (status) {
			case PENDING -> notifyEditors(event);
			case APPROVED -> sendTo(event.authorId(), "文章《" + event.articleTitle() + "》已通过",
					"您的文章《" + event.articleTitle() + "》已通过审核，可以发布。");
			case REJECTED -> sendTo(event.authorId(), "文章《" + event.articleTitle() + "》被驳回",
					"您的文章《" + event.articleTitle() + "》被驳回，意见：" + (event.opinion() == null ? "" : event.opinion()));
			case PUBLISHED -> sendTo(event.authorId(), "文章《" + event.articleTitle() + "》已发布",
					"您的文章《" + event.articleTitle() + "》已成功发布。");
			default -> log.debug("状态 {} 无对应通知动作, articleId={}", status, event.articleId());
			}
		} catch (Exception e) {
			log.warn("记录文章通知失败, articleId={}, statusCode={}", event.articleId(), event.statusCode(), e);
		}
	}

	private void notifyEditors(ArticleNoticeEvent event) {
		List<Long> editorIds = userMapper.selectUserIdsByRoleCode("ROLE_EDITOR");
		if (editorIds == null || editorIds.isEmpty()) {
			return;
		}
		String title = "文章《" + event.articleTitle() + "》待审核";
		String content = "作者 " + (event.operatorName() == null ? "" : event.operatorName()) + " 提交了文章《"
				+ event.articleTitle() + "》，请审核。";
		LocalDateTime now = LocalDateTime.now();

		List<SysNotice> notices = new ArrayList<>(editorIds.size());
		for (Long editorId : editorIds) {
			if (editorId == null || Objects.equals(editorId, event.operatorId())) {
				continue;
			}
			notices.add(buildNotice(editorId, title, content, now));
		}
		noticeMapper.createBatchIfAny(notices);
	}

	private void sendTo(Long recipientId, String title, String content) {
		if (recipientId == null) {
			return;
		}
		noticeMapper.create(buildNotice(recipientId, title, content, LocalDateTime.now()));
	}

	private SysNotice buildNotice(Long recipientId, String title, String content, LocalDateTime now) {
		SysNotice notice = new SysNotice();
		notice.setTitle(title);
		notice.setType(NoticeType.INFORM);
		notice.setContent(content);
		notice.setStatus("0");
		notice.setRecipientId(recipientId);
		notice.setCreateTime(now);
		notice.setUpdateTime(now);
		return notice;
	}
}
