package top.ruilink.inkwash.cms.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import top.ruilink.inkwash.base.enums.StatisticRange;
import top.ruilink.inkwash.base.enums.StatisticUnit;
import top.ruilink.inkwash.base.exception.BusinessException;

/**
 * Statistic range unit tests for value parsing, case insensitivity, and unit
 * granularity mapping.
 *
 * @author Dyllon
 * @since 0.5.1
 */
class StatisticRangeTest {

	@Test
	@DisplayName("fromValue 解析合法值")
	void fromValue_parsesValidValues() {
		assertEquals(StatisticRange.LAST_7D, StatisticRange.fromValue("last7d"));
		assertEquals(StatisticRange.LAST_30D, StatisticRange.fromValue("last30d"));
		assertEquals(StatisticRange.LAST_12M, StatisticRange.fromValue("last12m"));
		assertEquals(StatisticRange.THIS_MONTH, StatisticRange.fromValue("thisMonth"));
		assertEquals(StatisticRange.LAST_MONTH, StatisticRange.fromValue("lastMonth"));
		assertEquals(StatisticRange.THIS_YEAR, StatisticRange.fromValue("thisYear"));
		assertEquals(StatisticRange.LAST_YEAR, StatisticRange.fromValue("lastYear"));
		assertEquals(StatisticRange.LAST_7D, StatisticRange.fromValue("LAST7D"));
	}

	@Test
	@DisplayName("fromValue 非法值抛业务异常")
	void fromValue_rejectsInvalidValue() {
		assertThrows(BusinessException.class, () -> StatisticRange.fromValue("decade"));
	}

	@Test
	@DisplayName("各 range 基础粒度映射")
	void granularityMapping() {
		assertEquals(StatisticUnit.DAY, StatisticRange.LAST_7D.getUnit());
		assertEquals(StatisticUnit.DAY, StatisticRange.LAST_30D.getUnit());
		assertEquals(StatisticUnit.MONTH, StatisticRange.LAST_12M.getUnit());
		assertEquals(StatisticUnit.WEEK, StatisticRange.THIS_MONTH.getUnit());
		assertEquals(StatisticUnit.WEEK, StatisticRange.LAST_MONTH.getUnit());
		assertEquals(StatisticUnit.MONTH, StatisticRange.THIS_YEAR.getUnit());
		assertEquals(StatisticUnit.MONTH, StatisticRange.LAST_YEAR.getUnit());
	}
}