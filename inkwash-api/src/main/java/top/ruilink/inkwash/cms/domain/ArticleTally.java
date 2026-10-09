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

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import top.ruilink.inkwash.base.domain.ResultView;

/**
 * Snapshot of an article's counters, used to populate the field of a response.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Getter
@Setter
public class ArticleTally {
	@JsonView(ResultView.Basic.class)
	private Long viewCount = 0L;

	@JsonView(ResultView.Basic.class)
	private Long commentCount = 0L;

	@JsonView(ResultView.Basic.class)
	private Long agreeCount = 0L;

	@JsonView(ResultView.Basic.class)
	private Long favoriteCount = 0L;

	@JsonView(ResultView.Basic.class)
	private Long shareCount = 0L;

	@JsonView(ResultView.Basic.class)
	private Long averseCount = 0L;

	public synchronized void incrementView() {
		this.viewCount++;
	}

	public synchronized void incrementComment() {
		this.commentCount++;
	}

	public synchronized void decrementComment() {
		this.commentCount = Math.max(0, this.commentCount - 1);
	}

	public synchronized void incrementAgree() {
		this.agreeCount++;
	}

	public synchronized void decrementAgree() {
		this.agreeCount = Math.max(0, this.agreeCount - 1);
	}

	public synchronized void incrementFavorite() {
		this.favoriteCount++;
	}

	public synchronized void decrementFavorite() {
		this.favoriteCount = Math.max(0, this.favoriteCount - 1);
	}

	public synchronized void incrementShare() {
		this.shareCount++;
	}
}
