package top.ruilink.inkwash.base.support.file.enums;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * FileFolderType extension whitelist unit tests.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@DisplayName("FileFolderType 上传类型白名单")
class FileFolderTypeTest {

	@Test
	@DisplayName("公开文件目录不允许上传 svg（存储型 XSS 防护）")
	void publicFolder_doesNotAllowSvg() {
		assertFalse(FileFolderType.PUBLIC.isExtensionAllowed("svg"), "PUBLIC 必须拒绝 svg，防止存储型 XSS");
		assertFalse(FileFolderType.PUBLIC.isExtensionAllowed("SVG"));
	}

	@Test
	@DisplayName("文章附件目录不允许上传 svg（存储型 XSS 防护）")
	void articleFolder_doesNotAllowSvg() {
		assertFalse(FileFolderType.ARTICLE.isExtensionAllowed("svg"), "ARTICLE 必须拒绝 svg，防止存储型 XSS");
	}

	@Test
	@DisplayName("常规图片类型仍被允许")
	void commonImageTypesStillAllowed() {
		assertTrue(FileFolderType.PUBLIC.isExtensionAllowed("png"));
		assertTrue(FileFolderType.PUBLIC.isExtensionAllowed("jpg"));
		assertTrue(FileFolderType.ARTICLE.isExtensionAllowed("webp"));
		assertTrue(FileFolderType.AVATAR.isExtensionAllowed("png"));
	}
}