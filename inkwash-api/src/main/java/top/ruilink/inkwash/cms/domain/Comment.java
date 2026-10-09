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

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/**
 * Comment entity.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Getter
@Setter
public class Comment implements Serializable {
	private static final long serialVersionUID = 1L;

	/** Comment ID */
	private Long id;

	/** Article ID */
	private Long articleId;

	/** Comment author ID */
	private Long commenterId;

	/** Parent comment ID used for replies */
	private Long parentId;

	/** Comment body */
	private String content;

	/** Comment access statistics */
	private CommentTally tally;

	/** Creation time */
	private LocalDateTime createTime;
}
