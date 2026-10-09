package top.ruilink.inkwash.base.support.file.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import top.ruilink.inkwash.base.exception.BusinessException;

/**
 * Upload declared MIME type versus extension validation unit tests.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@DisplayName("MimeTypeValidator declared MIME type unit tests")
class MimeTypeValidatorTest {

	private static void assertValid(String ext, String mimeType) {
		assertDoesNotThrow(() -> MimeTypeValidator.validate(ext, mimeType));
	}

	private static void assertMismatch(String ext, String mimeType) {
		assertThrows(BusinessException.class, () -> MimeTypeValidator.validate(ext, mimeType));
	}

	@Test
	@DisplayName("jpg/jpeg accept image/jpeg regardless of case or leading dot")
	void jpegFamily() {
		assertValid("jpg", "image/jpeg");
		assertValid("jpeg", "image/jpeg");
		assertValid(".JPG", "image/jpeg");
		assertValid("jpg", "IMAGE/JPEG");
	}

	@Test
	@DisplayName("raster image extensions accept their declared types")
	void rasterImages() {
		assertValid("png", "image/png");
		assertValid("gif", "image/gif");
		assertValid("webp", "image/webp");
		assertValid("bmp", "image/bmp");
		assertValid("bmp", "image/x-ms-bmp");
	}

	@Test
	@DisplayName("pdf accepts application/pdf")
	void pdf() {
		assertValid("pdf", "application/pdf");
	}

	@Test
	@DisplayName("audio extensions accept their declared types")
	void audio() {
		assertValid("mp3", "audio/mpeg");
		assertValid("mp3", "audio/mp3");
		assertValid("wav", "audio/wav");
		assertValid("wav", "audio/x-wav");
		assertValid("aac", "audio/aac");
		assertValid("ogg", "audio/ogg");
		assertValid("flac", "audio/flac");
	}

	@Test
	@DisplayName("office extensions accept MS and OpenXML declared types")
	void office() {
		assertValid("doc", "application/msword");
		assertValid("xls", "application/vnd.ms-excel");
		assertValid("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
		assertValid("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
	}

	@Test
	@DisplayName("archive and text extensions accept their declared types")
	void archivesAndText() {
		assertValid("zip", "application/zip");
		assertValid("rar", "application/vnd.rar");
		assertValid("rar", "application/x-rar-compressed");
		assertValid("txt", "text/plain");
	}

	@Test
	@DisplayName("video extensions accept their declared types")
	void video() {
		assertValid("mp4", "video/mp4");
		assertValid("mov", "video/quicktime");
		assertValid("mkv", "video/x-matroska");
		assertValid("webm", "video/webm");
		assertValid("avi", "video/x-msvideo");
	}

	@Test
	@DisplayName("MIME parameters such as charset are ignored")
	void mimeParametersAreStripped() {
		assertValid("txt", "text/plain; charset=UTF-8");
		assertValid("txt", "text/plain;charset=utf-8");
	}

	@Test
	@DisplayName("a declared type contradicting the extension is rejected")
	void mismatchRejected() {
		assertMismatch("jpg", "image/png");
		assertMismatch("png", "image/jpeg");
		assertMismatch("pdf", "application/zip");
		assertMismatch("mp4", "video/quicktime");
		assertMismatch("docx", "application/msword");
	}

	@Test
	@DisplayName("unknown extensions are accepted without inspection")
	void unknownExtensionPasses() {
		assertValid("bin", "application/octet-stream");
		assertValid("svg", "image/svg+xml");
		assertValid("exe", "application/x-msdownload");
	}

	@Test
	@DisplayName("blank or absent declared MIME types are accepted")
	void blankOrNullMimePasses() {
		assertValid("png", null);
		assertValid("png", "");
		assertValid("png", "   ");
	}

	@Test
	@DisplayName("absent extensions are accepted without inspection")
	void nullOrEmptyExtensionPasses() {
		assertValid(null, "image/jpeg");
		assertValid("", "image/jpeg");
	}
}
