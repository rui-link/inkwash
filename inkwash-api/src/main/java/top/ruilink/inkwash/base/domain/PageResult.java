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
package top.ruilink.inkwash.base.domain;

import java.util.List;
import org.springframework.data.domain.Page;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;

/**
 * Paged query result wrapper.
 *
 * <p>
 * Every field carries {@link JsonView} at {@link ResultView.Basic} because
 * Jackson applies view filtering to the root object first: under an active
 * view, any property without a matching annotation is dropped — so an
 * un-annotated {@code list} would erase the page before the wrapped view was
 * reached, and the endpoint would answer {@code 200} with a literal {@code {}}.
 * Basic is the supertype of Detail and Full, so the envelope survives at every
 * level while the wrapped element still projects per its own annotations.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Getter
@Setter
public class PageResult<T> {

	@JsonView(ResultView.Basic.class)
	private List<T> list;

	@JsonView(ResultView.Basic.class)
	private long total;

	@JsonView(ResultView.Basic.class)
	private int pageNum;

	@JsonView(ResultView.Basic.class)
	private int pageSize;

	@JsonView(ResultView.Basic.class)
	private int totalPages;

	@JsonView(ResultView.Basic.class)
	private boolean hasNext;

	@JsonView(ResultView.Basic.class)
	private boolean hasPrevious;

	public static <T> PageResult<T> of(Page<T> page) {
		var r = new PageResult<T>();
		r.setList(page.getContent());
		r.setTotal(page.getTotalElements());
		r.setPageNum(page.getNumber() + 1);
		r.setPageSize(page.getSize());
		r.setTotalPages(page.getTotalPages());
		r.setHasNext(page.hasNext());
		r.setHasPrevious(page.hasPrevious());
		return r;
	}
}
