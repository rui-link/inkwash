package top.ruilink.inkwash.system.service.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import top.ruilink.inkwash.base.enums.BaseStatus;
import top.ruilink.inkwash.system.api.param.GroupParam;
import top.ruilink.inkwash.system.domain.SysGroup;

/**
 * Group converter unit tests for parameter to entity status defaulting and
 * updates.
 *
 * @author Dyllon
 * @since 0.5.1
 */
class GroupConverterTest {

	@Test
	void toGroupEntity_defaultsStatusToEnable() {
		GroupParam param = new GroupParam();
		param.setName("天文馆");

		SysGroup group = GroupConverter.toEntity(param);

		assertEquals(BaseStatus.ENABLE, group.getStatus(), "未传状态时应默认启用，以免新建分组在树列表中被过滤");
	}

	@Test
	void toGroupEntity_keepsProvidedStatus() {
		GroupParam param = new GroupParam();
		param.setName("天文馆");
		param.setStatus(BaseStatus.DISABLE);

		SysGroup group = GroupConverter.toEntity(param);

		assertEquals(BaseStatus.DISABLE, group.getStatus());
	}

	@Test
	void updateGroupEntity_updatesStatusWhenProvided() {
		SysGroup group = new SysGroup();
		group.setName("天文馆");
		group.setStatus(BaseStatus.ENABLE);

		GroupParam param = new GroupParam();
		param.setName("天文馆");
		param.setStatus(BaseStatus.DISABLE);

		GroupConverter.updateEntity(group, param);

		assertEquals(BaseStatus.DISABLE, group.getStatus());
	}
}