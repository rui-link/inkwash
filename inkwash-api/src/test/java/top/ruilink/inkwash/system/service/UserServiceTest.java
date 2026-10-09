package top.ruilink.inkwash.system.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import top.ruilink.inkwash.base.enums.AuthType;
import top.ruilink.inkwash.base.enums.BaseStatus;
import top.ruilink.inkwash.base.enums.Gender;
import top.ruilink.inkwash.base.enums.IdentityType;
import top.ruilink.inkwash.base.enums.IdentityVerifier;
import top.ruilink.inkwash.base.exception.BusinessException;
import top.ruilink.inkwash.base.util.SortValidator;
import top.ruilink.inkwash.security.service.EmailService;
import top.ruilink.inkwash.security.util.CryptoUtil;
import top.ruilink.inkwash.system.api.param.UserParam;
import top.ruilink.inkwash.system.api.query.UserQuery;
import top.ruilink.inkwash.system.api.view.GroupView;
import top.ruilink.inkwash.system.api.view.ResetPasswordView;
import top.ruilink.inkwash.system.api.view.UserView;
import top.ruilink.inkwash.system.domain.SysAccount;
import top.ruilink.inkwash.system.domain.SysGroup;
import top.ruilink.inkwash.system.domain.SysIdentity;
import top.ruilink.inkwash.system.domain.SysUser;
import top.ruilink.inkwash.system.enums.UserStatus;
import top.ruilink.inkwash.system.mapper.AccountMapper;
import top.ruilink.inkwash.system.mapper.GroupMapper;
import top.ruilink.inkwash.system.mapper.UserMapper;
import top.ruilink.inkwash.system.service.impl.UserServiceImpl;

/**
 * UserService unit test.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceTest {

	@Mock
	private UserMapper userMapper;

	@Mock
	private AccountMapper accountMapper;

	@Mock
	private IdentityService identityService;

	@Mock
	private SortValidator sortValidator;

	@Mock
	private GroupMapper groupMapper;

	@Mock
	private EmailService emailService;

	@InjectMocks
	private UserServiceImpl userService;

	private SysUser user1;
	private SysUser user2;
	private SysUser user3;

	@BeforeEach
	void setUp() {
		try {
			var field = CryptoUtil.class.getDeclaredField("passwordEncoder");
			field.setAccessible(true);
			field.set(null, new BCryptPasswordEncoder(12));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		reset(userMapper, accountMapper, identityService, groupMapper);

		// Create test user 1
		user1 = new SysUser();
		user1.setId(1L);
		user1.setNickname("alice");
		user1.setRealname("Alice Wang");
		user1.setGender(Gender.FEMALE);
		user1.setPhone("13800138001");
		user1.setEmail("alice@example.com");
		user1.setAvatar("https://example.com/alice.jpg");
		user1.setStatus(UserStatus.ENABLE);
		user1.setCreateTime(LocalDateTime.now().minusDays(30));
		user1.setUpdateTime(LocalDateTime.now());

		// Create test user 2
		user2 = new SysUser();
		user2.setId(2L);
		user2.setNickname("bob");
		user2.setRealname("Bob Li");
		user2.setGender(Gender.MALE);
		user2.setPhone("13800138002");
		user2.setEmail("bob@example.com");
		user2.setStatus(UserStatus.ENABLE);
		user2.setCreateTime(LocalDateTime.now().minusDays(20));
		user2.setUpdateTime(LocalDateTime.now());

		// Create test user 3 in the disabled state
		user3 = new SysUser();
		user3.setId(3L);
		user3.setNickname("charlie");
		user3.setRealname("Charlie Chen");
		user3.setGender(Gender.MALE);
		user3.setStatus(UserStatus.DISABLE);
		user3.setCreateTime(LocalDateTime.now().minusDays(10));
		user3.setUpdateTime(LocalDateTime.now());

		// createUser and updateUser obtain the current user through SecurityUtil, so a
		// security context must be injected
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("1", null));
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Nested
	@DisplayName("按身份查询用户测试")
	class FindByIdentityAndTypeTests {

		@Test
		@DisplayName("根据用户名和密码类型查询用户成功")
		void findByIdentityAndType_Password_Success() {
			// Given
			when(userMapper.selectByIdentityAndType("alice", AuthType.PASSWORD)).thenReturn(user1);

			// When
			SysUser result = userService.findByIdentityAndType("alice", AuthType.PASSWORD);

			// Then
			assertNotNull(result);
			assertEquals("alice", result.getNickname());
			verify(userMapper).selectByIdentityAndType("alice", AuthType.PASSWORD);
		}

		@Test
		@DisplayName("根据手机号和SMS类型查询用户成功")
		void findByIdentityAndType_SMS_Success() {
			// Given
			when(userMapper.selectByIdentityAndType("13800138001", AuthType.SMS_CODE)).thenReturn(user1);

			// When
			SysUser result = userService.findByIdentityAndType("13800138001", AuthType.SMS_CODE);

			// Then
			assertNotNull(result);
			assertEquals("13800138001", result.getPhone());
		}

		@Test
		@DisplayName("根据不存在的身份查询返回null")
		void findByIdentityAndType_NotFound() {
			// Given
			when(userMapper.selectByIdentityAndType("nonexistent", AuthType.PASSWORD)).thenReturn(null);

			// When
			SysUser result = userService.findByIdentityAndType("nonexistent", AuthType.PASSWORD);

			// Then
			assertNull(result);
		}
	}

	@Nested
	@DisplayName("按ID查询用户测试")
	class GetByIdTests {

		@Test
		@DisplayName("根据ID查询用户成功")
		void getById_Success() {
			// Given
			when(userMapper.selectById(1L)).thenReturn(user1);

			// When
			SysUser result = userService.getById(1L);

			// Then
			assertNotNull(result);
			assertEquals("alice", result.getNickname());
			verify(userMapper).selectById(1L);
		}

		@Test
		@DisplayName("根据不存在的ID查询返回null")
		void getById_NotFound() {
			// Given
			when(userMapper.selectById(999L)).thenReturn(null);

			// When
			SysUser result = userService.getById(999L);

			// Then
			assertNull(result);
		}
	}

	@Nested
	@DisplayName("按手机号/邮箱查询用户测试")
	class GetByContactTests {

		@Test
		@DisplayName("按手机号查询用户成功（走claim）")
		void getByPhone_Success() {
			// Given
			SysIdentity claim = new SysIdentity();
			claim.setUserId(1L);
			when(identityService.findByTypeValue(IdentityType.PHONE, "13800138001")).thenReturn(claim);
			when(userMapper.selectById(1L)).thenReturn(user1);

			// When
			SysUser result = userService.getByPhone("13800138001");

			// Then
			assertNotNull(result);
			assertEquals(1L, result.getId());
			verify(identityService).findByTypeValue(IdentityType.PHONE, "13800138001");
			verify(userMapper).selectById(1L);
		}

		@Test
		@DisplayName("按手机号查询无claim返回null")
		void getByPhone_NoClaim_ReturnsNull() {
			// Given
			when(identityService.findByTypeValue(IdentityType.PHONE, "13800999000")).thenReturn(null);

			// When
			SysUser result = userService.getByPhone("13800999000");

			// Then
			assertNull(result);
			verify(userMapper, never()).selectById(anyLong());
		}

		@Test
		@DisplayName("按邮箱查询用户成功（走claim）")
		void getByEmail_Success() {
			// Given
			SysIdentity claim = new SysIdentity();
			claim.setUserId(1L);
			when(identityService.findByTypeValue(IdentityType.EMAIL, "alice@example.com")).thenReturn(claim);
			when(userMapper.selectById(1L)).thenReturn(user1);

			// When
			SysUser result = userService.getByEmail("alice@example.com");

			// Then
			assertNotNull(result);
			assertEquals("alice", result.getNickname());
			verify(identityService).findByTypeValue(IdentityType.EMAIL, "alice@example.com");
		}

		@Test
		@DisplayName("空手机号/邮箱返回null")
		void getByContact_Blank_ReturnsNull() {
			assertNull(userService.getByPhone(null));
			assertNull(userService.getByPhone(""));
			assertNull(userService.getByEmail(null));
			assertNull(userService.getByEmail(""));
		}
	}

	@Nested
	@DisplayName("分页查询用户测试")
	class ListUsersTests {

		@Test
		@DisplayName("分页查询用户成功 - 第一页")
		void pageSearch_FirstPage_Success() {
			// Given
			List<SysUser> users = Arrays.asList(user1, user2, user3);
			var query = new UserQuery();
			query.setPage(1);
			query.setSize(10);
			when(sortValidator.resolve(null, null)).thenReturn(null);
			when(userMapper.countUser(query)).thenReturn(3L);
			when(userMapper.selectUserList(query, 0L, 10, null)).thenReturn(users);

			// When
			var result = userService.pageSearch(query);

			// Then
			assertNotNull(result);
			assertEquals(3, result.getList().size());
			assertEquals(1, result.getPageNum());
			assertEquals(10, result.getPageSize());
			assertEquals(3, result.getTotal());
		}

		@Test
		@DisplayName("分页查询用户 - 第二页")
		void pageSearch_SecondPage() {
			// Given
			List<SysUser> users = Arrays.asList(user1, user2, user3);
			var query = new UserQuery();
			query.setPage(2);
			query.setSize(10);
			when(sortValidator.resolve(null, null)).thenReturn(null);
			when(userMapper.countUser(query)).thenReturn(15L);
			when(userMapper.selectUserList(query, 10L, 10, null)).thenReturn(users);

			// When
			var result = userService.pageSearch(query);

			// Then
			assertNotNull(result);
			assertEquals(2, result.getPageNum());
			assertEquals(3, result.getList().size());
		}

		@Test
		@DisplayName("分页查询用户 - 空列表")
		void pageSearch_EmptyList() {
			// Given
			var query = new UserQuery();
			query.setPage(1);
			query.setSize(10);
			when(sortValidator.resolve(null, null)).thenReturn(null);
			when(userMapper.countUser(query)).thenReturn(0L);
			when(userMapper.selectUserList(query, 0L, 10, null)).thenReturn(List.of());

			// When
			var result = userService.pageSearch(query);

			// Then
			assertNotNull(result);
			assertEquals(0, result.getList().size());
			assertEquals(0, result.getTotal());
		}

		@Test
		@DisplayName("分页查询用户 - 超出范围的页码")
		void pageSearch_PageOutOfRange() {
			// Given
			List<SysUser> users = Arrays.asList(user1, user2);
			var query = new UserQuery();
			query.setPage(100);
			query.setSize(10);
			when(sortValidator.resolve(null, null)).thenReturn(null);
			when(userMapper.countUser(query)).thenReturn(2L);
			when(userMapper.selectUserList(query, 990L, 10, null)).thenReturn(users);

			// When
			var result = userService.pageSearch(query);

			// Then
			assertNotNull(result);
			assertEquals(100, result.getPageNum());
			assertNotNull(result.getTotal());
		}
	}

	@Nested
	@DisplayName("获取用户详情测试")
	class GetUserDetailTests {

		@Test
		@DisplayName("获取用户详情成功")
		void getUserDetail_Success() {
			// Given
			when(userMapper.selectById(1L)).thenReturn(user1);
			when(groupMapper.selectByUserId(1L)).thenReturn(List.of());
			when(accountMapper.selectByUserId(1L)).thenReturn(List.of());

			// When
			UserView result = userService.getUserDetail(1L);

			// Then
			assertNotNull(result);
			assertEquals(1L, result.getId());
			assertEquals("alice", result.getNickname());
			assertEquals("Alice Wang", result.getRealname());
			assertEquals(Gender.FEMALE, result.getGender());
			assertEquals("13800138001", result.getPhone());
			assertEquals("alice@example.com", result.getEmail());
		}

		@Test
		@DisplayName("获取用户详情返回所属用户组")
		void getUserDetail_ReturnsGroups() {
			// Given
			when(userMapper.selectById(1L)).thenReturn(user1);
			when(accountMapper.selectByUserId(1L)).thenReturn(List.of());
			SysGroup g1 = new SysGroup();
			g1.setId(10);
			g1.setName("编辑部");
			when(groupMapper.selectByUserId(1L)).thenReturn(List.of(g1));

			// When
			UserView result = userService.getUserDetail(1L);

			// Then
			assertNotNull(result);
			assertNotNull(result.getGroups());
			assertEquals(1, result.getGroups().size());
			GroupView gv = result.getGroups().iterator().next();
			assertEquals(10, gv.getId());
			assertEquals("编辑部", gv.getName());
			verify(groupMapper).selectByUserId(1L);
		}

		@Test
		@DisplayName("获取不存在的用户详情抛出异常")
		void getUserDetail_NotFound() {
			// Given
			when(userMapper.selectById(999L)).thenReturn(null);

			// When & Then
			assertThrows(BusinessException.class, () -> userService.getUserDetail(999L));
		}

		@Test
		@DisplayName("查询已删除用户详情抛出用户不存在")
		void getUserDetail_Deleted_Throws() {
			SysUser deleted = new SysUser();
			deleted.setId(9L);
			deleted.setNickname("ghost");
			deleted.setStatus(UserStatus.DELETED);
			when(userMapper.selectById(9L)).thenReturn(deleted);

			assertThrows(BusinessException.class, () -> userService.getUserDetail(9L));
		}
	}

	@Nested
	@DisplayName("统计用户总数测试")
	class CountTests {

		@Test
		@DisplayName("返回用户总数")
		void count_Success() {
			// Given
			when(userMapper.count()).thenReturn(42L);

			// When
			long result = userService.count();

			// Then
			assertEquals(42L, result);
			verify(userMapper).count();
		}
	}

	@Nested
	@DisplayName("创建用户测试")
	class CreateUserTests {

		@Test
		@DisplayName("创建用户成功")
		void createUser_Success() {
			// Given
			UserParam param = new UserParam();
			param.setNickname("david");
			param.setUsername("david");
			param.setPassword("password123");
			param.setRealname("David Zhang");
			param.setGender(Gender.MALE);
			param.setPhone("13800138999");
			param.setEmail("david@example.com");
			param.setAvatar("https://example.com/david.jpg");
			param.setBiography("测试用户");

			when(accountMapper.selectByIdentityAndType("david", AuthType.PASSWORD)).thenReturn(null);
			when(identityService.findByTypeValue(any(IdentityType.class), anyString())).thenReturn(null);

			doAnswer(invocation -> {
				SysUser user = invocation.getArgument(0);
				user.setId(100L);
				return 1L;
			}).when(userMapper).create(any(SysUser.class));

			doAnswer(invocation -> {
				SysAccount acct = invocation.getArgument(0);
				acct.setId(200L);
				return 200L;
			}).when(accountMapper).create(any(SysAccount.class));

			// When
			UserView result = userService.createUser(param);

			// Then
			assertNotNull(result);
			assertEquals(100L, result.getId());
			assertEquals("david", result.getNickname());
			assertEquals("David Zhang", result.getRealname());
			assertEquals(Gender.MALE, result.getGender());
			verify(userMapper).create(any(SysUser.class));
			verify(accountMapper).create(any(SysAccount.class));
		}

		@Test
		@DisplayName("创建用户时用户名重复应抛出异常")
		void createUser_DuplicateUsername_ThrowsException() {
			// Given
			UserParam param = new UserParam();
			param.setUsername("alice");
			param.setNickname("david");
			param.setPassword("password123");

			when(accountMapper.selectByIdentityAndType("alice", AuthType.PASSWORD)).thenReturn(new SysAccount());

			// When & Then
			assertThrows(BusinessException.class, () -> userService.createUser(param));
			verify(userMapper, never()).create(any());
		}

		@Test
		@DisplayName("创建用户设置默认启用状态")
		void createUser_DefaultStatus() {
			// Given
			UserParam param = new UserParam();
			param.setNickname("emma");
			param.setUsername("emma");
			param.setPassword("password123");
			param.setRealname("Emma Liu");

			when(accountMapper.selectByIdentityAndType("emma", AuthType.PASSWORD)).thenReturn(null);
			when(userMapper.create(any(SysUser.class))).thenReturn(101L);
			when(accountMapper.create(any(SysAccount.class))).thenReturn(200L);

			// When
			UserView result = userService.createUser(param);

			// Then
			assertNotNull(result);
			assertEquals(UserStatus.ENABLE, result.getStatus());
		}

		@Test
		@DisplayName("创建用户成功后同步phone/email身份声明")
		void createUser_SyncsClaims() {
			// Given
			UserParam param = new UserParam();
			param.setNickname("david");
			param.setUsername("david");
			param.setPassword("password123");
			param.setPhone("13800138999");
			param.setEmail("david@example.com");

			when(accountMapper.selectByIdentityAndType("david", AuthType.PASSWORD)).thenReturn(null);
			when(identityService.findByTypeValue(any(IdentityType.class), anyString())).thenReturn(null);
			doAnswer(invocation -> {
				SysUser user = invocation.getArgument(0);
				user.setId(100L);
				return 1L;
			}).when(userMapper).create(any(SysUser.class));

			// When
			userService.createUser(param);

			// Then
			verify(identityService).findOrCreateVerified(IdentityType.PHONE, "13800138999", null, 100L,
					IdentityVerifier.ADMIN);
			verify(identityService).findOrCreateVerified(IdentityType.EMAIL, "david@example.com", null, 100L,
					IdentityVerifier.ADMIN);
		}

		@Test
		@DisplayName("创建用户时手机号被他人占用抛出异常")
		void createUser_PhoneOccupied_ThrowsException() {
			// Given
			UserParam param = new UserParam();
			param.setNickname("david");
			param.setUsername("david");
			param.setPassword("password123");
			param.setPhone("13800138001");

			when(accountMapper.selectByIdentityAndType("david", AuthType.PASSWORD)).thenReturn(null);
			SysIdentity existing = new SysIdentity();
			existing.setUserId(2L);
			when(identityService.findByTypeValue(IdentityType.PHONE, "13800138001")).thenReturn(existing);

			// When & Then
			assertThrows(BusinessException.class, () -> userService.createUser(param));
			verify(userMapper, never()).create(any());
		}
	}

	@Nested
	@DisplayName("更新用户测试")
	class UpdateUserTests {

		@Test
		@DisplayName("更新用户成功 - 全量更新")
		void updateUser_FullUpdate_Success() {
			// Given
			when(userMapper.selectById(1L)).thenReturn(user1);
			when(identityService.findByTypeValue(any(IdentityType.class), anyString())).thenReturn(null);

			UserParam param = new UserParam();
			param.setNickname("alice_updated");
			param.setRealname("Alice Wang Updated");
			param.setGender(Gender.FEMALE);
			param.setPhone("13900139001");
			param.setEmail("alice.new@example.com");
			param.setAvatar("https://example.com/alice_new.jpg");
			param.setBiography("更新备注");

			// When
			UserView result = userService.updateUser(1L, param);

			// Then
			assertNotNull(result);
			assertEquals("alice_updated", result.getNickname());
			assertEquals("Alice Wang Updated", result.getRealname());
			verify(userMapper).update(any(SysUser.class));
		}

		@Test
		@DisplayName("更新用户成功 - 部分更新")
		void updateUser_PartialUpdate_Success() {
			// Given
			when(userMapper.selectById(1L)).thenReturn(user1);
			when(identityService.findByTypeValue(any(IdentityType.class), anyString())).thenReturn(null);

			UserParam param = new UserParam();
			param.setPhone("13900139001"); // 只更新手机号
			param.setBiography("仅更新备注"); // 只更新备注

			// When
			UserView result = userService.updateUser(1L, param);

			// Then
			assertNotNull(result);
			assertEquals("13900139001", result.getPhone());
			assertEquals("仅更新备注", result.getBiography());
			// Fields that were not updated stay unchanged
			assertEquals("alice", result.getNickname());
			assertEquals("Alice Wang", result.getRealname());
		}

		@Test
		@DisplayName("更新不存在的用户抛出异常")
		void updateUser_NotFound() {
			// Given
			when(userMapper.selectById(999L)).thenReturn(null);

			UserParam param = new UserParam();
			param.setNickname("newname");

			// When & Then
			assertThrows(BusinessException.class, () -> userService.updateUser(999L, param));
		}

		@Test
		@DisplayName("更新手机号时新增新claim并删除旧claim")
		void updateUser_ChangePhone_ReplacesClaim() {
			// Given
			when(userMapper.selectById(1L)).thenReturn(user1);
			SysIdentity oldClaim = new SysIdentity();
			oldClaim.setId(10L);
			oldClaim.setUserId(1L);
			when(identityService.findByTypeValue(IdentityType.PHONE, "13800138001")).thenReturn(oldClaim);
			when(identityService.findByTypeValue(IdentityType.PHONE, "13900139001")).thenReturn(null);

			UserParam param = new UserParam();
			param.setPhone("13900139001");

			// When
			UserView result = userService.updateUser(1L, param);

			// Then
			assertEquals("13900139001", result.getPhone());
			verify(identityService).findOrCreateVerified(IdentityType.PHONE, "13900139001", null, 1L,
					IdentityVerifier.ADMIN);
			verify(identityService).deleteById(10L);
		}

		@Test
		@DisplayName("清空手机号时删除原claim并清空字段")
		void updateUser_ClearPhone_DeletesOldClaim() {
			// Given
			when(userMapper.selectById(1L)).thenReturn(user1);
			SysIdentity oldClaim = new SysIdentity();
			oldClaim.setId(10L);
			oldClaim.setUserId(1L);
			when(identityService.findByTypeValue(IdentityType.PHONE, "13800138001")).thenReturn(oldClaim);

			UserParam param = new UserParam();
			param.setPhone("");

			// When
			UserView result = userService.updateUser(1L, param);

			// Then
			assertNull(result.getPhone());
			verify(identityService).deleteById(10L);
		}

		@Test
		@DisplayName("更新手机号为他人占用时抛出异常")
		void updateUser_PhoneOccupiedByOther_ThrowsException() {
			// Given
			when(userMapper.selectById(1L)).thenReturn(user1);
			SysIdentity otherClaim = new SysIdentity();
			otherClaim.setUserId(2L);
			when(identityService.findByTypeValue(IdentityType.PHONE, "13900139001")).thenReturn(otherClaim);

			UserParam param = new UserParam();
			param.setPhone("13900139001");

			// When & Then
			assertThrows(BusinessException.class, () -> userService.updateUser(1L, param));
			verify(userMapper, never()).update(any());
		}
	}

	@Nested
	@DisplayName("删除用户测试")
	class DeleteUserTests {

		@Test
		@DisplayName("删除用户成功")
		void deleteUser_Success() {
			// Given
			when(userMapper.selectById(1L)).thenReturn(user1);

			// When
			userService.deleteUser(1L);

			// Then
			verify(userMapper, never()).remove(1L);
			verify(userMapper).updateStatus(eq(1L), eq(UserStatus.DELETED.getCode()), any(LocalDateTime.class));
		}

		@Test
		@DisplayName("软删除：设置 DELETED 状态而非物理删除")
		void deleteUser_SoftDelete_SetsDeletedStatus() {
			when(userMapper.selectById(1L)).thenReturn(user1);

			userService.deleteUser(1L);

			verify(userMapper, never()).remove(1L);
			verify(userMapper).updateStatus(eq(1L), eq(UserStatus.DELETED.getCode()), any(LocalDateTime.class));
		}

		@Test
		@DisplayName("重复删除已删除用户抛出用户不存在")
		void deleteUser_AlreadyDeleted_Throws() {
			SysUser deleted = new SysUser();
			deleted.setId(1L);
			deleted.setNickname("alice");
			deleted.setStatus(UserStatus.DELETED);
			when(userMapper.selectById(1L)).thenReturn(deleted);

			assertThrows(BusinessException.class, () -> userService.deleteUser(1L));
		}

		@Test
		@DisplayName("删除不存在的用户抛出异常")
		void deleteUser_NotFound() {
			// Given
			when(userMapper.selectById(999L)).thenReturn(null);

			// When & Then
			assertThrows(BusinessException.class, () -> userService.deleteUser(999L));
		}
	}

	@Nested
	@DisplayName("分配用户组测试")
	class AssignGroupsTests {

		@Test
		@DisplayName("分配用户组成功")
		void assignGroups_Success() {
			// Given
			when(userMapper.selectById(1L)).thenReturn(user1);
			List<Long> groupIds = Arrays.asList(10L, 20L, 30L);

			// When
			userService.assignGroups(1L, groupIds);

			// Then
			verify(userMapper).removeGroups(1L);
			verify(userMapper).assignGroupsBatch(1L, groupIds);
		}

		@Test
		@DisplayName("分配空用户组成功")
		void assignGroups_EmptyList_Success() {
			// Given
			when(userMapper.selectById(1L)).thenReturn(user1);

			// When
			userService.assignGroups(1L, List.of());

			// Then
			verify(userMapper).removeGroups(1L);
			// An empty list must not call the batch method
			verify(userMapper, never()).assignGroupsBatch(anyLong(), any());
		}

		@Test
		@DisplayName("为不存在的用户分配用户组抛出异常")
		void assignGroups_UserNotFound() {
			// Given
			when(userMapper.selectById(999L)).thenReturn(null);

			// When & Then
			assertThrows(BusinessException.class, () -> userService.assignGroups(999L, Arrays.asList(1L, 2L)));
		}
	}

	@Nested
	@DisplayName("加入默认用户组测试")
	class JoinDefaultGroupTests {

		@BeforeEach
		void setDefaultGroupName() {
			ReflectionTestUtils.setField(userService, "defaultGroupName", "Users");
		}

		@Test
		@DisplayName("用户未在默认组时加入默认用户组")
		void join_NotMember_Assigns() {
			when(userMapper.selectById(1L)).thenReturn(user1);
			SysGroup group = new SysGroup();
			group.setId(4);
			group.setName("Users");
			when(groupMapper.selectByName("Users")).thenReturn(group);
			when(userMapper.selectGroupsByUserId(1L)).thenReturn(List.of());

			userService.joinDefaultGroup(1L);

			verify(userMapper).assignGroup(1L, 4);
		}

		@Test
		@DisplayName("已是默认组成员时跳过，避免重复分配")
		void join_AlreadyMember_Skips() {
			when(userMapper.selectById(1L)).thenReturn(user1);
			SysGroup group = new SysGroup();
			group.setId(4);
			group.setName("Users");
			when(groupMapper.selectByName("Users")).thenReturn(group);
			SysGroup member = new SysGroup();
			member.setId(4);
			when(userMapper.selectGroupsByUserId(1L)).thenReturn(List.of(member));

			userService.joinDefaultGroup(1L);

			verify(userMapper, never()).assignGroup(anyLong(), any());
		}

		@Test
		@DisplayName("默认用户组不存在时跳过自动分组")
		void join_GroupMissing_Skips() {
			when(userMapper.selectById(1L)).thenReturn(user1);
			when(groupMapper.selectByName("Users")).thenReturn(null);

			userService.joinDefaultGroup(1L);

			verify(userMapper, never()).assignGroup(anyLong(), any());
		}
	}

	@Nested
	@DisplayName("重置密码测试")
	class ResetPasswordTests {

		@Test
		@DisplayName("重置密码成功：返回一次性密码视图并通过邮件发送新密码")
		void resetPassword_Success() {
			// Given
			when(userMapper.selectById(1L)).thenReturn(user1);
			when(accountMapper.selectByUserId(1L)).thenReturn(List.of());
			when(identityService.findByTypeValue(IdentityType.PHONE, "13800138001")).thenReturn(null);
			when(emailService.sendMail(eq("alice@example.com"), anyString(), anyString())).thenReturn(true);

			// When
			ResetPasswordView view = userService.resetPassword(1L);

			// Then
			verify(userMapper).selectById(1L);
			verify(accountMapper).create(any(SysAccount.class));
			assertNotNull(view);
			assertEquals(1L, view.getUserId());
			assertNotNull(view.getPassword(), "重置密码视图必须返回一次性密码");
			assertTrue(view.getPassword().length() >= 12);
			assertTrue(view.isEmailSent(), "用户存在邮箱且发送成功时 emailSent 应为 true");
			verify(emailService).sendMail(eq("alice@example.com"), anyString(), contains(view.getPassword()));
		}

		@Test
		@DisplayName("重置密码时存在密码账号则更新而非创建，并发送邮件")
		void resetPassword_UpdatesExistingPasswordAccount() {
			// Given
			when(userMapper.selectById(1L)).thenReturn(user1);
			SysAccount passwordAccount = new SysAccount();
			passwordAccount.setId(10L);
			passwordAccount.setUserId(1L);
			passwordAccount.setAuthType(AuthType.PASSWORD);
			when(accountMapper.selectByUserId(1L)).thenReturn(List.of(passwordAccount));
			when(emailService.sendMail(eq("alice@example.com"), anyString(), anyString())).thenReturn(true);

			// When
			ResetPasswordView view = userService.resetPassword(1L);

			// Then
			verify(accountMapper).update(passwordAccount);
			verify(accountMapper, never()).create(any(SysAccount.class));
			assertNotNull(view.getPassword());
			assertTrue(view.isEmailSent());
		}

		@Test
		@DisplayName("重置密码成功但邮件未配置/发送失败：emailSent 为 false，密码仍返回")
		void resetPassword_EmailNotSent_ReturnsPassword() {
			// Given
			when(userMapper.selectById(1L)).thenReturn(user1);
			when(accountMapper.selectByUserId(1L)).thenReturn(List.of());
			when(identityService.findByTypeValue(IdentityType.PHONE, "13800138001")).thenReturn(null);
			when(emailService.sendMail(eq("alice@example.com"), anyString(), anyString())).thenReturn(false);

			// When
			ResetPasswordView view = userService.resetPassword(1L);

			// Then
			assertNotNull(view.getPassword(), "邮件发送失败也必须在响应中返回一次性密码");
			assertFalse(view.isEmailSent());
		}

		@Test
		@DisplayName("重置没有邮箱的用户：不调用邮件发送，emailSent 为 false")
		void resetPassword_NoEmail_SkipsSend() {
			// Given
			when(userMapper.selectById(3L)).thenReturn(user3);
			when(accountMapper.selectByUserId(3L)).thenReturn(List.of());

			// When
			ResetPasswordView view = userService.resetPassword(3L);

			// Then
			assertNotNull(view.getPassword());
			assertFalse(view.isEmailSent());
			verify(emailService, never()).sendMail(anyString(), anyString(), anyString());
		}

		@Test
		@DisplayName("重置不存在用户的密码抛出异常")
		void resetPassword_UserNotFound() {
			// Given
			when(userMapper.selectById(999L)).thenReturn(null);

			// When & Then
			assertThrows(BusinessException.class, () -> userService.resetPassword(999L));
		}
	}

	@Nested
	@DisplayName("创建简单用户测试")
	class CreateSimpleUserTests {

		@Test
		@DisplayName("创建简单用户成功后同步email声明")
		void createSimpleUser_SyncsEmailClaim() {
			// Given
			doAnswer(invocation -> {
				SysUser user = invocation.getArgument(0);
				user.setId(100L);
				return 1L;
			}).when(userMapper).create(any(SysUser.class));

			// When
			SysUser result = userService.createSimpleUser("GitHub User", "github@example.com");

			// Then
			assertNotNull(result);
			assertEquals(100L, result.getId());
			assertEquals("github@example.com", result.getEmail());
			verify(identityService).findOrCreateVerified(IdentityType.EMAIL, "github@example.com", null, 100L,
					IdentityVerifier.OAUTH2);
		}

		@Test
		@DisplayName("无邮箱时创建简单用户不写claim")
		void createSimpleUser_NoEmail_SkipsClaim() {
			// Given
			doAnswer(invocation -> {
				SysUser user = invocation.getArgument(0);
				user.setId(100L);
				return 1L;
			}).when(userMapper).create(any(SysUser.class));

			// When
			userService.createSimpleUser("GitHub User", null);

			// Then
			verify(identityService, never()).findOrCreateVerified(any(), any(), any(), anyLong(), any());
		}
	}

	@Nested
	@DisplayName("更新用户状态测试")
	class UpdateStatusTests {

		@Test
		@DisplayName("启用用户成功")
		void updateStatus_Enable_Success() {
			// Given
			when(userMapper.selectById(1L)).thenReturn(user1);

			// When
			userService.updateStatus(1L, BaseStatus.ENABLE);

			// Then
			verify(userMapper).updateStatus(eq(1L), eq(UserStatus.ENABLE.getCode()), any(LocalDateTime.class));
		}

		@Test
		@DisplayName("禁用用户成功")
		void updateStatus_Disable_Success() {
			// Given
			when(userMapper.selectById(1L)).thenReturn(user1);

			// When
			userService.updateStatus(1L, BaseStatus.DISABLE);

			// Then
			verify(userMapper).updateStatus(eq(1L), eq(UserStatus.DISABLE.getCode()), any(LocalDateTime.class));
		}

		@Test
		@DisplayName("更新不存在用户的状态抛出异常")
		void updateStatus_UserNotFound() {
			// Given
			when(userMapper.selectById(999L)).thenReturn(null);

			// When & Then
			assertThrows(BusinessException.class, () -> userService.updateStatus(999L, BaseStatus.ENABLE));
		}

		@Test
		@DisplayName("对已删除用户启用/禁用抛出用户不存在")
		void updateStatus_Deleted_Throws() {
			SysUser deleted = new SysUser();
			deleted.setId(9L);
			deleted.setNickname("ghost");
			deleted.setStatus(UserStatus.DELETED);
			when(userMapper.selectById(9L)).thenReturn(deleted);

			assertThrows(BusinessException.class, () -> userService.updateStatus(9L, BaseStatus.ENABLE));
		}
	}

	@Nested
	@DisplayName("已删除用户写路径拒绝测试")
	class DeletedUserWritePathTests {

		private SysUser deletedUser;

		@BeforeEach
		void setUp() {
			deletedUser = new SysUser();
			deletedUser.setId(9L);
			deletedUser.setNickname("ghost");
			deletedUser.setStatus(UserStatus.DELETED);
			when(userMapper.selectById(9L)).thenReturn(deletedUser);
		}

		@Test
		@DisplayName("updateUser 拒绝已删除用户")
		void updateUser_Deleted_Throws() {
			UserParam param = new UserParam();
			param.setNickname("newname");

			assertThrows(BusinessException.class, () -> userService.updateUser(9L, param));
			verify(userMapper, never()).update(any(SysUser.class));
		}

		@Test
		@DisplayName("assignGroups 拒绝已删除用户")
		void assignGroups_Deleted_Throws() {
			assertThrows(BusinessException.class, () -> userService.assignGroups(9L, Arrays.asList(1L, 2L)));
			verify(userMapper, never()).removeGroups(anyLong());
			verify(userMapper, never()).assignGroupsBatch(anyLong(), any());
		}

		@Test
		@DisplayName("resetPassword 拒绝已删除用户")
		void resetPassword_Deleted_Throws() {
			assertThrows(BusinessException.class, () -> userService.resetPassword(9L));
			verify(accountMapper, never()).create(any(SysAccount.class));
			verify(accountMapper, never()).update(any(SysAccount.class));
		}

		@Test
		@DisplayName("joinDefaultGroup 拒绝已删除用户")
		void joinDefaultGroup_Deleted_Throws() {
			assertThrows(BusinessException.class, () -> userService.joinDefaultGroup(9L));
			verify(userMapper, never()).assignGroup(anyLong(), any());
		}

		@Test
		@DisplayName("updateRawUser 拒绝已删除用户")
		void updateRawUser_Deleted_Throws() {
			SysUser raw = new SysUser();
			raw.setId(9L);

			assertThrows(BusinessException.class, () -> userService.updateRawUser(raw));
			verify(userMapper, never()).update(any(SysUser.class));
		}

		@Test
		@DisplayName("updateRawUser 拒绝不存在的用户")
		void updateRawUser_NotFound_Throws() {
			SysUser raw = new SysUser();
			raw.setId(999L);
			when(userMapper.selectById(999L)).thenReturn(null);

			assertThrows(BusinessException.class, () -> userService.updateRawUser(raw));
			verify(userMapper, never()).update(any(SysUser.class));
		}
	}

	@Nested
	@DisplayName("SysUser实体方法测试")
	class SysUserEntityTests {

		@Test
		@DisplayName("用户状态设置正确")
		void status_SetCorrectly() {
			assertEquals(UserStatus.ENABLE, user1.getStatus()); // ENABLE状态
			assertEquals(UserStatus.DISABLE, user3.getStatus()); // DISABLE状态
		}

		@Test
		@DisplayName("DELETED 状态码为 5")
		void status_DELETED_CodeIs5() {
			assertEquals(5, UserStatus.DELETED.getCode());
			assertEquals("已删除", UserStatus.DELETED.getName());
		}

		@Test
		@DisplayName("用户信息设置正确")
		void userInfo_SetCorrectly() {
			assertEquals("alice", user1.getNickname());
			assertEquals("Alice Wang", user1.getRealname());
			assertEquals(Gender.FEMALE, user1.getGender());
			assertEquals("13800138001", user1.getPhone());
			assertEquals("alice@example.com", user1.getEmail());
		}
	}
}
