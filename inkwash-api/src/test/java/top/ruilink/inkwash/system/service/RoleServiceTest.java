package top.ruilink.inkwash.system.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import top.ruilink.inkwash.base.enums.BaseStatus;
import top.ruilink.inkwash.base.exception.BusinessException;
import top.ruilink.inkwash.base.util.SortValidator;
import top.ruilink.inkwash.system.api.param.RoleParam;
import top.ruilink.inkwash.system.api.query.RoleQuery;
import top.ruilink.inkwash.system.api.view.PermissionView;
import top.ruilink.inkwash.system.api.view.RoleView;
import top.ruilink.inkwash.system.domain.SysPermission;
import top.ruilink.inkwash.system.domain.SysRole;
import top.ruilink.inkwash.system.mapper.PermissionMapper;
import top.ruilink.inkwash.system.mapper.RoleMapper;
import top.ruilink.inkwash.system.service.impl.RoleServiceImpl;

/**
 * RoleService unit test.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RoleServiceTest {

	@Mock
	private RoleMapper roleMapper;

	@Mock
	private PermissionMapper permissionMapper;

	@Mock
	private SortValidator sortValidator;

	@InjectMocks
	private RoleServiceImpl roleService;

	private SysRole adminRole;
	private SysRole editorRole;
	private SysRole viewerRole;
	private SysPermission permission1;
	private SysPermission permission2;

	@BeforeEach
	void setUp() {
		// Reset every mock so tests stay isolated
		reset(roleMapper, permissionMapper);

		// Create the administrator role
		adminRole = new SysRole();
		adminRole.setId(1);
		adminRole.setName("系统管理员");
		adminRole.setCode("ROLE_ADMIN");
		adminRole.setStatus(BaseStatus.ENABLE);
		adminRole.setRemark("拥有系统全部权限");
		adminRole.setCreateTime(LocalDateTime.now().minusDays(60));
		adminRole.setUpdateTime(LocalDateTime.now());

		// Create the editor role
		editorRole = new SysRole();
		editorRole.setId(2);
		editorRole.setName("内容编辑");
		editorRole.setCode("ROLE_EDITOR");
		editorRole.setStatus(BaseStatus.ENABLE);
		editorRole.setRemark("负责内容编辑");
		editorRole.setCreateTime(LocalDateTime.now().minusDays(30));
		editorRole.setUpdateTime(LocalDateTime.now());

		// Create the viewer role
		viewerRole = new SysRole();
		viewerRole.setId(3);
		viewerRole.setName("普通用户");
		viewerRole.setCode("ROLE_VIEWER");
		viewerRole.setStatus(BaseStatus.ENABLE);
		viewerRole.setCreateTime(LocalDateTime.now().minusDays(10));
		viewerRole.setUpdateTime(LocalDateTime.now());

		// Create permission 1
		permission1 = new SysPermission();
		permission1.setId(1);
		permission1.setName("创建文章");
		permission1.setAuthority("cms:article:create");
		permission1.setModule("cms");
		permission1.setResource("article");
		permission1.setAction("create");
		permission1.setCreateTime(LocalDateTime.now());

		// Create permission 2
		permission2 = new SysPermission();
		permission2.setId(2);
		permission2.setName("查看文章");
		permission2.setAuthority("cms:article:query");
		permission2.setModule("cms");
		permission2.setResource("article");
		permission2.setAction("view");
		permission2.setCreateTime(LocalDateTime.now());
	}

	@Nested
	@DisplayName("按ID查询角色测试")
	class GetByIdTests {

		@Test
		@DisplayName("根据ID查询角色成功")
		void getById_Success() {
			// Given
			when(roleMapper.selectById(1)).thenReturn(adminRole);

			// When
			SysRole result = roleService.getById(1);

			// Then
			assertNotNull(result);
			assertEquals("ROLE_ADMIN", result.getCode());
			assertEquals("系统管理员", result.getName());
			verify(roleMapper).selectById(1);
		}

		@Test
		@DisplayName("根据不存在的ID查询返回null")
		void getById_NotFound() {
			// Given
			when(roleMapper.selectById(999)).thenReturn(null);

			// When
			SysRole result = roleService.getById(999);

			// Then
			assertNull(result);
		}
	}

	@Nested
	@DisplayName("分页查询角色测试")
	class ListRolesTests {

		@Test
		@DisplayName("分页查询角色成功 - 第一页")
		void listRoles_FirstPage_Success() {
			List<SysRole> roles = Arrays.asList(adminRole, editorRole, viewerRole);
			when(sortValidator.resolve(any(), any())).thenReturn("id ASC");
			when(roleMapper.countRoles(any(RoleQuery.class))).thenReturn(3L);
			when(roleMapper.selectRoleList(any(RoleQuery.class), anyLong(), anyInt(), any())).thenReturn(roles);

			RoleQuery query = new RoleQuery();
			query.setPage(1);
			query.setSize(10);

			var result = roleService.listRoles(query);

			assertNotNull(result);
			assertEquals(3, result.getList().size());
			assertEquals(1, result.getPageNum());
			assertEquals(10, result.getPageSize());
			assertEquals(3, result.getTotal());
		}

		@Test
		@DisplayName("分页查询角色 - 第二页")
		void listRoles_SecondPage() {
			List<SysRole> roles = Arrays.asList(adminRole, editorRole, viewerRole);
			when(sortValidator.resolve(any(), any())).thenReturn("id ASC");
			when(roleMapper.countRoles(any(RoleQuery.class))).thenReturn(15L);
			when(roleMapper.selectRoleList(any(RoleQuery.class), anyLong(), anyInt(), any())).thenReturn(roles);

			RoleQuery query = new RoleQuery();
			query.setPage(2);
			query.setSize(10);

			var result = roleService.listRoles(query);

			assertNotNull(result);
			assertEquals(3, result.getList().size());
			assertEquals(2, result.getPageNum());
			assertEquals(13, result.getTotal());
		}

		@Test
		@DisplayName("分页查询角色 - 空列表")
		void listRoles_EmptyList() {
			when(sortValidator.resolve(any(), any())).thenReturn("id ASC");
			when(roleMapper.countRoles(any(RoleQuery.class))).thenReturn(0L);
			when(roleMapper.selectRoleList(any(RoleQuery.class), anyLong(), anyInt(), any())).thenReturn(List.of());

			RoleQuery query = new RoleQuery();

			var result = roleService.listRoles(query);

			assertNotNull(result);
			assertEquals(0, result.getList().size());
			assertEquals(0, result.getTotal());
		}

		@Test
		@DisplayName("分页查询角色 - 超出范围的页码")
		void listRoles_PageOutOfRange() {
			List<SysRole> roles = Arrays.asList(adminRole, editorRole);
			when(sortValidator.resolve(any(), any())).thenReturn("id ASC");
			when(roleMapper.countRoles(any(RoleQuery.class))).thenReturn(2L);
			when(roleMapper.selectRoleList(any(RoleQuery.class), anyLong(), anyInt(), any())).thenReturn(roles);

			RoleQuery query = new RoleQuery();
			query.setPage(100);
			query.setSize(10);

			var result = roleService.listRoles(query);

			assertNotNull(result);
			assertEquals(2, result.getList().size());
			assertEquals(100, result.getPageNum());
		}
	}

	@Nested
	@DisplayName("获取角色详情测试")
	class GetRoleDetailTests {

		@Test
		@DisplayName("获取角色详情成功 - 带权限列表")
		void getRoleDetail_WithPermissions_Success() {
			// Given
			Set<SysPermission> permissions = new HashSet<>(Arrays.asList(permission1, permission2));
			when(roleMapper.selectById(1)).thenReturn(adminRole);
			when(permissionMapper.selectByRoleId(1)).thenReturn(permissions);

			// When
			RoleView result = roleService.getRoleDetail(1);

			// Then
			assertNotNull(result);
			assertEquals(1, result.getId());
			assertEquals("系统管理员", result.getName());
			assertEquals("ROLE_ADMIN", result.getCode());
			assertNotNull(result.getPermissions());
			assertEquals(2, result.getPermissions().size());
		}

		@Test
		@DisplayName("获取角色详情成功 - 无权限")
		void getRoleDetail_NoPermissions_Success() {
			// Given
			when(roleMapper.selectById(3)).thenReturn(viewerRole);
			when(permissionMapper.selectByRoleId(3)).thenReturn(new HashSet<>());

			// When
			RoleView result = roleService.getRoleDetail(3);

			// Then
			assertNotNull(result);
			assertNotNull(result.getPermissions());
			assertTrue(result.getPermissions().isEmpty());
		}

		@Test
		@DisplayName("获取不存在的角色详情抛出异常")
		void getRoleDetail_NotFound() {
			// Given
			when(roleMapper.selectById(999)).thenReturn(null);

			// When & Then
			assertThrows(BusinessException.class, () -> roleService.getRoleDetail(999));
		}
	}

	@Nested
	@DisplayName("创建角色测试")
	class CreateRoleTests {

		@Test
		@DisplayName("创建角色成功")
		void createRole_Success() {
			// Given
			RoleParam param = new RoleParam();
			param.setName("新角色");
			param.setCode("ROLE_NEW_ROLE");
			param.setRemark("新建角色");

			doAnswer(invocation -> {
				SysRole role = invocation.getArgument(0);
				role.setId(100);
				return 1;
			}).when(roleMapper).create(any(SysRole.class));

			// When
			RoleView result = roleService.createRole(param);

			// Then
			assertNotNull(result);
			assertEquals(100, result.getId());
			assertEquals("新角色", result.getName());
			assertEquals("ROLE_NEW_ROLE", result.getCode());
			verify(roleMapper).create(any(SysRole.class));
		}

		@Test
		@DisplayName("创建角色设置默认排序值")
		void createRole_DefaultSort() {
			// Given
			RoleParam param = new RoleParam();
			param.setName("测试角色");
			param.setCode("ROLE_TEST");
			// sort is null

			doAnswer(invocation -> {
				SysRole role = invocation.getArgument(0);
				role.setId(101);
				return 1;
			}).when(roleMapper).create(any(SysRole.class));

			// When
			RoleView result = roleService.createRole(param);

			// Then
			assertNotNull(result);
			assertEquals(101, result.getId());
		}
	}

	@Nested
	@DisplayName("更新角色测试")
	class UpdateRoleTests {

		@Test
		@DisplayName("更新角色成功 - 全量更新")
		void updateRole_FullUpdate_Success() {
			// Given
			when(roleMapper.selectById(2)).thenReturn(editorRole);

			RoleParam param = new RoleParam();
			param.setName("高级编辑");
			param.setCode("ROLE_SENIOR_EDITOR");
			param.setStatus(BaseStatus.ENABLE);
			param.setRemark("更新后的角色");

			// When
			RoleView result = roleService.updateRole(2, param);

			// Then
			assertNotNull(result);
			assertEquals("高级编辑", result.getName());
			assertEquals("ROLE_SENIOR_EDITOR", result.getCode());
			verify(roleMapper).update(any(SysRole.class));
		}

		@Test
		@DisplayName("更新角色成功 - 部分更新")
		void updateRole_PartialUpdate_Success() {
			// Given
			when(roleMapper.selectById(2)).thenReturn(editorRole);

			RoleParam param = new RoleParam();
			param.setName("内容编辑");
			param.setCode("ROLE_EDITOR");
			param.setRemark("仅更新备注"); // 只更新备注

			// When
			RoleView result = roleService.updateRole(2, param);

			// Then
			assertNotNull(result);
			assertEquals("仅更新备注", result.getRemark());
			// Fields that were not updated stay unchanged
			assertEquals("内容编辑", result.getName());
			assertEquals("ROLE_EDITOR", result.getCode());
		}

		@Test
		@DisplayName("更新不存在的角色抛出异常")
		void updateRole_NotFound() {
			// Given
			when(roleMapper.selectById(999)).thenReturn(null);

			RoleParam param = new RoleParam();
			param.setName("New Name");

			// When & Then
			assertThrows(BusinessException.class, () -> roleService.updateRole(999, param));
		}
	}

	@Nested
	@DisplayName("删除角色测试")
	class DeleteRoleTests {

		@Test
		@DisplayName("删除角色成功")
		void deleteRole_Success() {
			// Given
			when(roleMapper.selectById(3)).thenReturn(viewerRole);

			// When
			roleService.deleteRole(3);

			// Then
			verify(roleMapper).delete(3);
		}

		@Test
		@DisplayName("删除不存在的角色抛出异常")
		void deleteRole_NotFound() {
			// Given
			when(roleMapper.selectById(999)).thenReturn(null);

			// When & Then
			assertThrows(BusinessException.class, () -> roleService.deleteRole(999));
		}
	}

	@Nested
	@DisplayName("分配角色权限测试")
	class AssignPermissionsTests {

		@Test
		@DisplayName("分配权限成功")
		void assignPermissions_Success() {
			// Given
			when(roleMapper.selectById(2)).thenReturn(editorRole);
			List<Long> permissionIds = Arrays.asList(1L, 2L, 3L);

			// When
			roleService.assignPermissions(2, permissionIds);

			// Then
			verify(roleMapper).removePermissions(2);
			verify(roleMapper).assignPermissions(2, permissionIds);
		}

		@Test
		@DisplayName("分配空权限列表成功")
		void assignPermissions_EmptyList_Success() {
			// Given
			when(roleMapper.selectById(2)).thenReturn(editorRole);

			// When
			roleService.assignPermissions(2, List.of());

			// Then
			verify(roleMapper).removePermissions(2);
			// assignPermission must not be called
			verify(roleMapper, never()).assignPermission(anyInt(), anyInt());
		}

		@Test
		@DisplayName("为不存在的角色分配权限抛出异常")
		void assignPermissions_RoleNotFound() {
			// Given
			when(roleMapper.selectById(999)).thenReturn(null);

			// When & Then
			assertThrows(BusinessException.class, () -> roleService.assignPermissions(999, Arrays.asList(1L, 2L)));
		}
	}

	@Nested
	@DisplayName("SysRole实体方法测试")
	class SysRoleEntityTests {

		@Test
		@DisplayName("角色启用状态判断正确")
		void isEnabled_ReturnsCorrectStatus() {
			assertTrue(adminRole.getStatus().equals(BaseStatus.ENABLE)); // ENABLE状态
			assertTrue(editorRole.getStatus().equals(BaseStatus.ENABLE)); // ENABLE状态
			assertFalse(viewerRole.getStatus().equals(BaseStatus.DISABLE)); // DISABLE状态
		}

		@Test
		@DisplayName("角色信息设置正确")
		void roleInfo_SetCorrectly() {
			assertEquals("系统管理员", adminRole.getName());
			assertEquals("ROLE_ADMIN", adminRole.getCode());
			assertEquals("拥有系统全部权限", adminRole.getRemark());
		}
	}

	@Nested
	@DisplayName("视图转换测试")
	class ViewConversionTests {

		@Test
		@DisplayName("RoleView转换正确")
		void roleView_ConversionCorrect() {
			// Given
			when(roleMapper.selectById(1)).thenReturn(adminRole);

			// When
			RoleView result = roleService.getRoleDetail(1);

			// Then
			assertNotNull(result);
			assertEquals(1, result.getId());
			assertEquals("系统管理员", result.getName());
			assertEquals("ROLE_ADMIN", result.getCode());
		}

		@Test
		@DisplayName("权限视图转换正确")
		void permissionView_ConversionCorrect() {
			// Given
			Set<SysPermission> permissions = new HashSet<>(List.of(permission1));
			when(roleMapper.selectById(1)).thenReturn(adminRole);
			when(permissionMapper.selectByRoleId(1)).thenReturn(permissions);

			// When
			RoleView result = roleService.getRoleDetail(1);

			// Then
			assertNotNull(result.getPermissions());
			assertEquals(1, result.getPermissions().size());

			PermissionView permView = result.getPermissions().iterator().next();
			assertEquals(1, permView.getId());
			assertEquals("创建文章", permView.getName());
			assertEquals("cms:article:create", permView.getAuthority());
			assertEquals("cms", permView.getModule());
			assertEquals("article", permView.getResource());
			assertEquals("create", permView.getAction());
		}
	}
}
