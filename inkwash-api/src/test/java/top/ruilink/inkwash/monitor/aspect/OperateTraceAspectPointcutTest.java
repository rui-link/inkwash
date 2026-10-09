package top.ruilink.inkwash.monitor.aspect;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;

import top.ruilink.inkwash.base.annotation.OperateTrace;

/**
 * Operation trace aspect pointcut expression matching unit tests.
 *
 * @author Dyllon
 * @since 0.5.1
 */
class OperateTraceAspectPointcutTest {

	@Test
	void aspect_isAnnotatedWithAspect() {
		assertTrue(OperateTraceAspect.class.isAnnotationPresent(Aspect.class));
	}

	@Test
	void aroundPointcutExpression_isWellFormed() throws Exception {
		Method around = OperateTraceAspect.class.getMethod("around", ProceedingJoinPoint.class);
		Around annotation = around.getAnnotation(Around.class);
		assertNotNull(annotation);
		String expression = annotation.value();

		assertDoesNotThrow(() -> {
			AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
			pointcut.setExpression(expression);
			pointcut.getPointcutExpression();
		}, "pointcut expression should be parseable: " + expression);
	}

	@Test
	void aroundPointcut_matchesClassLevelAnnotation() throws Exception {
		Method around = OperateTraceAspect.class.getMethod("around", ProceedingJoinPoint.class);
		String expression = around.getAnnotation(Around.class).value();

		AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
		pointcut.setExpression(expression);

		Method mutationMethod = SampleController.class.getMethod("mutationMethod");
		assertTrue(pointcut.matches(mutationMethod, SampleController.class),
				"pointcut should match a method in a class annotated @OperateTrace");
	}

	@OperateTrace(module = "测试")
	static class SampleController {
		public void mutationMethod() {
		}
	}
}
