package top.ruilink.inkwash.cms.api.view;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tally view unit tests for agreed and favorited defaults with accessor
 * round-tripping.
 *
 * @author Dyllon
 * @since 0.5.1
 */
class TallyViewTest {

	@Test
	@DisplayName("TallyView 默认值为 false")
	void tallyView_DefaultsAreFalse() {
		TallyView view = new TallyView();
		assertFalse(view.isAgreed());
		assertFalse(view.isFavorited());
	}

	@Test
	@DisplayName("TallyView getter/setter 正常工作")
	void tallyView_GetterSetter() {
		TallyView view = new TallyView();
		view.setAgreed(true);
		view.setFavorited(true);
		assertTrue(view.isAgreed());
		assertTrue(view.isFavorited());
	}
}
