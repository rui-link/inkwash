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

import org.springframework.data.domain.PageImpl;
import top.ruilink.inkwash.base.util.PageUtil;
import top.ruilink.inkwash.base.util.SortValidator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import top.ruilink.inkwash.base.domain.PageResult;
import top.ruilink.inkwash.base.exception.BusinessException;
import top.ruilink.inkwash.base.util.SlugUtil;
import top.ruilink.inkwash.base.exception.NotFoundException;
import top.ruilink.inkwash.cms.api.param.TermParam;
import top.ruilink.inkwash.cms.api.query.TermQuery;
import top.ruilink.inkwash.cms.api.view.TermView;
import top.ruilink.inkwash.cms.domain.Term;
import top.ruilink.inkwash.cms.service.converter.TermConverter;
import top.ruilink.inkwash.cms.mapper.ArticleMapper;
import top.ruilink.inkwash.cms.mapper.TermMapper;
import top.ruilink.inkwash.cms.service.TermService;

/**
 * Article tag management service implementation.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Service
public class TermServiceImpl implements TermService {

	private final TermMapper termMapper;
	private final ArticleMapper articleMapper;
	private final SortValidator sortValidator;

	public TermServiceImpl(TermMapper termMapper, ArticleMapper articleMapper, SortValidator sortValidator) {
		this.termMapper = termMapper;
		this.articleMapper = articleMapper;
		this.sortValidator = sortValidator;
	}

	@Override
	@Transactional(readOnly = true)
	public PageResult<TermView> listTerms(TermQuery query) {
		var sortSql = sortValidator.resolve(query.getSortField(), query.getSortOrder());
		var offset = (long) (query.getPage() - 1) * query.getSize();
		var total = termMapper.countTerms(query);
		var list = termMapper.selectTermList(query, offset, query.getSize(), sortSql);
		var views = list.stream().map(TermConverter::toView).toList();
		var pageable = PageUtil.of(query.getPage(), query.getSize());
		var page = new PageImpl<>(views, pageable, total);
		return PageResult.of(page);
	}

	@Override
	@Transactional(readOnly = true)
	public TermView getTerm(Long id) {
		Term term = termMapper.selectById(id);
		if (term == null)
			throw new NotFoundException("标签不存在");
		return TermConverter.toView(term);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public TermView createTerm(TermParam param) {
		applySlug(param);
		Term entity = TermConverter.toEntity(param);
		termMapper.insert(entity);
		return TermConverter.toView(entity);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public TermView updateTerm(Long id, TermParam param) {
		Term entity = termMapper.selectById(id);
		if (entity == null)
			throw new NotFoundException("标签不存在");
		applySlug(param);
		TermConverter.updateEntity(entity, param);
		termMapper.update(entity);
		return TermConverter.toView(entity);
	}

	private void applySlug(TermParam param) {
		if (StringUtils.hasText(param.getSlug())) {
			param.setSlug(param.getSlug().trim());
		} else {
			String slug = SlugUtil.generate(param.getName());
			param.setSlug(slug.length() > 60 ? slug.substring(0, 60) : slug);
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteTerm(Long id) {
		if (articleMapper.countByTermId(id.intValue()) > 0) {
			throw new BusinessException("error.term.in_use");
		}
		termMapper.deleteById(id);
	}
}
