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
import top.ruilink.inkwash.base.util.SortValidator;
import top.ruilink.inkwash.cms.api.param.CategoryParam;
import top.ruilink.inkwash.cms.domain.Category;
import top.ruilink.inkwash.cms.mapper.ArticleMapper;
import top.ruilink.inkwash.cms.mapper.CategoryMapper;

/**
 * Category service unit tests for name-derived slugs and reference-guarded
 * deletion.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

	@Mock
	private CategoryMapper categoryMapper;

	@Mock
	private ArticleMapper articleMapper;

	@Mock
	private SortValidator sortValidator;

	@InjectMocks
	private CategoryServiceImpl categoryService;

	private CategoryParam param;

	@BeforeEach
	void setUp() {
		param = new CategoryParam();
		param.setName("技术文章 Tech");
	}

	@Test
	@DisplayName("创建分类时 slug 为空会自动生成")
	void createCategory_autoGeneratesSlugWhenBlank() {
		param.setSlug("");

		categoryService.createCategory(param);

		ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
		verify(categoryMapper).insert(captor.capture());
		assertEquals("技术文章-tech", captor.getValue().getSlug());
	}

	@Test
	@DisplayName("创建分类时保留用户填写的 slug")
	void createCategory_keepsProvidedSlug() {
		param.setSlug("tech-articles");

		categoryService.createCategory(param);

		ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
		verify(categoryMapper).insert(captor.capture());
		assertEquals("tech-articles", captor.getValue().getSlug());
	}

	@Test
	@DisplayName("更新分类时 slug 为空会根据新名称重新生成")
	void updateCategory_autoGeneratesSlugWhenBlank() {
		Category existing = new Category();
		existing.setId(1);
		existing.setName("旧名字");
		existing.setSlug("old-name");
		when(categoryMapper.selectById(1L)).thenReturn(existing);

		param.setSlug("");

		categoryService.updateCategory(1L, param);

		verify(categoryMapper).update(existing);
		assertEquals("技术文章-tech", existing.getSlug());
	}

	@Test
	@DisplayName("删除有子分类的分类被拒绝")
	void deleteCategory_hasChildren_Rejected() {
		when(categoryMapper.countByParentId(1L)).thenReturn(2L);

		assertThrows(BusinessException.class, () -> categoryService.deleteCategory(1L));

		verify(categoryMapper, never()).deleteById(1L);
	}

	@Test
	@DisplayName("删除有文章引用的分类被拒绝")
	void deleteCategory_hasArticles_Rejected() {
		when(categoryMapper.countByParentId(1L)).thenReturn(0L);
		when(articleMapper.countByCategoryId(1)).thenReturn(3L);

		assertThrows(BusinessException.class, () -> categoryService.deleteCategory(1L));

		verify(categoryMapper, never()).deleteById(1L);
	}

	@Test
	@DisplayName("删除无引用分类成功")
	void deleteCategory_noReferences_Succeeds() {
		when(categoryMapper.countByParentId(1L)).thenReturn(0L);
		when(articleMapper.countByCategoryId(1)).thenReturn(0L);

		categoryService.deleteCategory(1L);

		verify(categoryMapper).deleteById(1L);
	}
}
