package top.ruilink.inkwash.monitor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import top.ruilink.inkwash.base.enums.AuthType;
import top.ruilink.inkwash.monitor.domain.LoginContext;
import top.ruilink.inkwash.monitor.domain.LoginEvent;
import top.ruilink.inkwash.monitor.domain.LogoutEvent;
import top.ruilink.inkwash.monitor.enums.LoginStatus;

/**
 * Login event listener unit tests covering success and failure persistence with
 * parsed client details.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("登录日志监听器单元测试")
class LoginEventListenerTest {

	@Mock
	private LoginInfoService loginInfoService;

	@InjectMocks
	private LoginEventListener listener;

	private static final String UA_CHROME_WINDOWS = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
			+ "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";

	@Nested
	@DisplayName("登录事件落库")
	class RecordTests {

		@Test
		@DisplayName("成功登录记录IP、浏览器、操作系统与登录设备")
		void successEventRecordsClientInfo() {
			LoginEvent event = new LoginEvent("principal",
					new LoginContext(1L, "admin", AuthType.PASSWORD, "203.0.113.5", UA_CHROME_WINDOWS), true, null);

			listener.handleLoginEvent(event);

			verify(loginInfoService).recordLoginInfo(argThat(info -> {
				assertEquals(1L, info.getUserId());
				assertEquals("admin", info.getIdentity());
				assertEquals(AuthType.PASSWORD, info.getLoginType());
				assertEquals(LoginStatus.SUCCESS, info.getStatus());
				assertEquals("203.0.113.5", info.getAddress());
				assertEquals("Chrome", info.getBrowser());
				assertEquals("Windows 10", info.getOstype());
				assertEquals("电脑端", info.getDevice());
				return true;
			}));
		}

		@Test
		@DisplayName("失败登录记录IP与浏览器信息")
		void failedEventRecordsClientInfo() {
			LoginEvent event = new LoginEvent(null,
					new LoginContext(null, "admin", AuthType.PASSWORD, "127.0.0.1", UA_CHROME_WINDOWS), false, "密码错误");

			listener.handleLoginEvent(event);

			verify(loginInfoService).recordLoginInfo(argThat(info -> {
				assertEquals(LoginStatus.FAILED, info.getStatus());
				assertEquals("密码错误", info.getMessage());
				assertEquals("127.0.0.1", info.getAddress());
				assertEquals("Chrome", info.getBrowser());
				return true;
			}));
		}

		@Test
		@DisplayName("无User-Agent时客户端字段为空字符串")
		void blankUserAgentLeavesClientFieldsEmpty() {
			LoginEvent event = new LoginEvent(null, new LoginContext(1L, "admin", AuthType.PASSWORD, null, null), true,
					null);

			listener.handleLoginEvent(event);

			verify(loginInfoService).recordLoginInfo(argThat(info -> {
				assertEquals("", info.getBrowser());
				assertEquals("", info.getOstype());
				assertEquals("", info.getDevice());
				return true;
			}));
		}
	}

	@Nested
	@DisplayName("登出事件 —— ISS-043 / D-04")
	class LogoutTests {

		@Test
		@DisplayName("应关闭该用户最近一条未登出的会话")
		void closesLatestOpenSession() {
			listener.handleLogoutEvent(new LogoutEvent(7L));

			verify(loginInfoService).recordLogout(eq(7L));
		}

		@Test
		@DisplayName("匿名登出不应抛错，也不应误关他人会话")
		void anonymousLogoutIsIgnored() {
			listener.handleLogoutEvent(new LogoutEvent(null));

			verify(loginInfoService).recordLogout(null);
		}

		@Test
		@DisplayName("监听器异常不得向上传播")
		void swallowsServiceFailure() {
			org.mockito.Mockito.doThrow(new IllegalStateException("db down")).when(loginInfoService)
					.recordLogout(any());

			listener.handleLogoutEvent(new LogoutEvent(7L));

			verify(loginInfoService).recordLogout(7L);
		}

		@Test
		@DisplayName("登出不得写入 mon_journal（两张表职责分工）")
		void doesNotWriteJournal() {
			listener.handleLogoutEvent(new LogoutEvent(7L));

			verify(loginInfoService, never()).recordLoginInfo(org.mockito.ArgumentMatchers.any());
		}
	}
}