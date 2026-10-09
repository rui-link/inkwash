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

import lombok.Getter;
import lombok.Setter;

/**
 * Interaction entity.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Getter
@Setter
public class Interaction {
	/** Interaction ID */
	private Long id;

	/** Article ID */
	private Long articleId;

	/** Interactor ID */
	private Long actorId;

	/** Approval count */
	private Boolean agree;

	/** Bookmark count */
	private Boolean favorite;

	/** Share count */
	private Boolean share;

	private Boolean averse;

	/**
	 * Time of the first interaction between this user and this article.
	 *
	 * <p>
	 * Added by D-16 so the favourite/like/dislike lists can be ordered by
	 * interaction time instead of by surrogate key.
	 */
	private LocalDateTime createTime;
}
