package top.ruilink.inkwash.cms.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import top.ruilink.inkwash.base.exception.BusinessException;
import top.ruilink.inkwash.base.exception.NotFoundException;
import top.ruilink.inkwash.base.util.SortValidator;
import top.ruilink.inkwash.cms.api.param.TermParam;
import top.ruilink.inkwash.cms.domain.Term;
import top.ruilink.inkwash.cms.mapper.ArticleMapper;
import top.ruilink.inkwash.cms.mapper.TermMapper;

/**
 * Term service unit tests for name-derived slugs, reference-guarded deletion,
 * and missing lookup.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@ExtendWith(MockitoExtension.class)
class TermServiceTest {

	@Mock
	private TermMapper termMapper;

	@Mock
	private ArticleMapper articleMapper;

	@Mock
	private SortValidator sortValidator;

	@InjectMocks
	private TermServiceImpl termService;

	private TermParam param;

	@BeforeEach
	void setUp() {
		param = new TermParam();
		param.setName("前端开发 Frontend");
	}

	@Test
	@DisplayName("创建标签时 slug 为空会自动生成")
	void createTerm_autoGeneratesSlugWhenBlank() {
		param.setSlug("");

		termService.createTerm(param);

		ArgumentCaptor<Term> captor = ArgumentCaptor.forClass(Term.class);
		verify(termMapper).insert(captor.capture());
		assertEquals("前端开发-frontend", captor.getValue().getSlug());
	}

	@Test
	@DisplayName("创建标签时保留用户填写的 slug")
	void createTerm_keepsProvidedSlug() {
		param.setSlug("frontend");

		termService.createTerm(param);

		ArgumentCaptor<Term> captor = ArgumentCaptor.forClass(Term.class);
		verify(termMapper).insert(captor.capture());
		assertEquals("frontend", captor.getValue().getSlug());
	}

	@Test
	@DisplayName("更新标签时 slug 为空会根据新名称重新生成")
	void updateTerm_autoGeneratesSlugWhenBlank() {
		Term existing = new Term();
		existing.setId(1);
		existing.setName("旧标签");
		existing.setSlug("old-tag");
		when(termMapper.selectById(1L)).thenReturn(existing);

		param.setSlug("");

		termService.updateTerm(1L, param);

		verify(termMapper).update(existing);
		assertEquals("前端开发-frontend", existing.getSlug());
	}

	@Test
	@DisplayName("删除已被文章使用的标签被拒绝")
	void deleteTerm_hasArticles_Rejected() {
		when(articleMapper.countByTermId(1)).thenReturn(2L);

		assertThrows(BusinessException.class, () -> termService.deleteTerm(1L));

		verify(termMapper, never()).deleteById(1L);
	}

	@Test
	@DisplayName("删除无引用的标签成功")
	void deleteTerm_noReferences_Succeeds() {
		when(articleMapper.countByTermId(1)).thenReturn(0L);

		termService.deleteTerm(1L);

		verify(termMapper).deleteById(1L);
	}

	@Test
	@DisplayName("查询不存在的标签抛出业务异常而非返回null")
	void getTerm_unknownId_ThrowsNotFound() {
		when(termMapper.selectById(99L)).thenReturn(null);

		NotFoundException ex = assertThrows(NotFoundException.class, () -> termService.getTerm(99L));

		assertEquals(404, ex.getHttpStatus().value());
		verify(termMapper).selectById(99L);
	}
}
