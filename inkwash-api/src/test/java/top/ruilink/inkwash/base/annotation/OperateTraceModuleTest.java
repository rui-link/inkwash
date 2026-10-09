package top.ruilink.inkwash.base.annotation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * OperateTrace annotation module attribute unit tests.
 *
 * @author Dyllon
 * @since 0.5.1
 */
class OperateTraceModuleTest {

	@OperateTrace(module = "测试模块")
	static class SampleController {
	}

	@OperateTrace
	static class NoModuleController {
	}

	@Test
	void moduleAttribute_isReadable() {
		OperateTrace note = SampleController.class.getAnnotation(OperateTrace.class);
		assertEquals("测试模块", note.module());
	}

	@Test
	void defaultModule_isEmpty() {
		OperateTrace note = NoModuleController.class.getAnnotation(OperateTrace.class);
		assertEquals("", note.module());
	}
}
