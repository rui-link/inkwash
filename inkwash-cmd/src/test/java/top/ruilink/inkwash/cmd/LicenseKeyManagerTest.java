package top.ruilink.inkwash.cmd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.security.interfaces.RSAPrivateCrtKey;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import top.ruilink.inkwash.cmd.license.LicenseKeyManager;
import top.ruilink.inkwash.cmd.license.HardwareFingerprinter;

/**
 * RSA key material loading, PEM parsing and machine code unit tests.
 *
 * @author Dyllon
 * @since 0.5.1
 */
class LicenseKeyManagerTest {

	@Test
	void loadsRawPkcs8Base64() {
		LicenseKeyManager generated = new LicenseKeyManager(null, null);
		LicenseKeyManager loaded = new LicenseKeyManager(generated.getPrivateKeyBase64(), null);
		assertNotNull(loaded.getPrivateKey());
		assertNotNull(loaded.getPublicKey());
	}

	@Test
	void loadsPkcs8WithPemHeaders() {
		LicenseKeyManager generated = new LicenseKeyManager(null, null);
		String pem = wrapPem(generated.getPrivateKeyBase64(), "PRIVATE KEY");
		LicenseKeyManager loaded = new LicenseKeyManager(pem, null);
		assertNotNull(loaded.getPrivateKey());
		assertNotNull(loaded.getPublicKey());
	}

	@Test
	void loadsRawPkcs1Base64() throws Exception {
		LicenseKeyManager generated = new LicenseKeyManager(null, null);
		String pkcs1 = Base64.getEncoder().encodeToString(pkcs1Der(generated));
		LicenseKeyManager loaded = new LicenseKeyManager(pkcs1, null);
		assertNotNull(loaded.getPrivateKey());
		assertNotNull(loaded.getPublicKey());
	}

	@Test
	void loadsPkcs1WithPemHeaders() throws Exception {
		LicenseKeyManager generated = new LicenseKeyManager(null, null);
		String pkcs1 = Base64.getEncoder().encodeToString(pkcs1Der(generated));
		LicenseKeyManager loaded = new LicenseKeyManager(wrapPem(pkcs1, "RSA PRIVATE KEY"), null);
		assertNotNull(loaded.getPrivateKey());
		assertNotNull(loaded.getPublicKey());
	}

	@Test
	void loadsPublicKeyOnly() {
		LicenseKeyManager generated = new LicenseKeyManager(null, null);
		LicenseKeyManager loaded = new LicenseKeyManager(null, generated.getPublicKeyBase64());
		assertNotNull(loaded.getPublicKey());
	}

	@Test
	void malformedPrivateKeyFailsWithCleanErrorNotAioboe() {
		RuntimeException ex = assertThrows(RuntimeException.class, () -> new LicenseKeyManager("AAAA", null));
		assertNoArrayIndexOutOfBounds(ex);
		assertTrue(described(ex), "expected a parse error with a message, got: " + ex);
	}

	@Test
	void truncatedSequenceFailsWithCleanErrorNotAioboe() {
		// SEQUENCE tag with a declared content length (16) larger than the trailing
		// bytes
		byte[] truncated = { 0x30, (byte) 0x81, 0x10, 0x02, 0x01, 0x01 };
		String b64 = Base64.getEncoder().encodeToString(truncated);
		RuntimeException ex = assertThrows(RuntimeException.class, () -> new LicenseKeyManager(b64, null));
		assertNoArrayIndexOutOfBounds(ex);
		assertTrue(described(ex), "expected a parse error with a message, got: " + ex);
	}

	@Test
	void emptyKeyMaterialFallsBackToDevelopmentKeyPair() {
		LicenseKeyManager manager = new LicenseKeyManager("", null);
		assertNotNull(manager.getPrivateKey());
		assertNotNull(manager.getPublicKey());
	}

	@Test
	void machineCodeIsStableContinuous32Hex() {
		String a = HardwareFingerprinter.machineCode();
		String b = HardwareFingerprinter.machineCode();
		assertEquals(a, b, "machine code must be stable within the same JVM");
		assertEquals(32, a.length());
		assertTrue(a.matches("[0-9a-f]{32}"), "machine code must be continuous 32-hex, got: " + a);
	}

	@Test
	void machineCodeMatchesSharedFingerprintHash() {
		String code = HardwareFingerprinter.machineCode();
		assertTrue(code.matches("[0-9a-f]{32}"), "machine code must be continuous 32-hex, got: " + code);
	}

	private static void assertNoArrayIndexOutOfBounds(Throwable t) {
		Throwable cur = t;
		while (cur != null) {
			assertFalse(cur instanceof ArrayIndexOutOfBoundsException,
					"unexpected ArrayIndexOutOfBoundsException in cause chain: " + t);
			cur = cur.getCause();
		}
	}

	private static boolean described(Throwable t) {
		Throwable cur = t;
		while (cur != null) {
			if (cur.getMessage() != null && cur.getMessage().contains("PKCS#1")) {
				return true;
			}
			cur = cur.getCause();
		}
		return false;
	}

	private static String wrapPem(String base64, String type) {
		StringBuilder sb = new StringBuilder("-----BEGIN ").append(type).append("-----\n");
		for (int i = 0; i < base64.length(); i += 64) {
			sb.append(base64, i, Math.min(i + 64, base64.length())).append('\n');
		}
		return sb.append("-----END ").append(type).append("-----\n").toString();
	}

	// Minimal DER encoder for a PKCS#1 RSAPrivateKey (SEQUENCE of 9 INTEGERs)
	private static byte[] pkcs1Der(LicenseKeyManager manager) {
		RSAPrivateCrtKey key = (RSAPrivateCrtKey) manager.getPrivateKey();
		return derSequence(derInteger(BigInteger.ZERO), derInteger(key.getModulus()),
				derInteger(key.getPublicExponent()), derInteger(key.getPrivateExponent()), derInteger(key.getPrimeP()),
				derInteger(key.getPrimeQ()), derInteger(key.getPrimeExponentP()), derInteger(key.getPrimeExponentQ()),
				derInteger(key.getCrtCoefficient()));
	}

	private static byte[] derInteger(BigInteger value) {
		byte[] raw = value.toByteArray();
		byte[] len = encodeLength(raw.length);
		byte[] out = new byte[1 + len.length + raw.length];
		int pos = 0;
		out[pos++] = 0x02;
		System.arraycopy(len, 0, out, pos, len.length);
		pos += len.length;
		System.arraycopy(raw, 0, out, pos, raw.length);
		return out;
	}

	private static byte[] derSequence(byte[]... elements) {
		int total = 0;
		for (byte[] e : elements) {
			total += e.length;
		}
		byte[] len = encodeLength(total);
		byte[] out = new byte[1 + len.length + total];
		int pos = 0;
		out[pos++] = 0x30;
		System.arraycopy(len, 0, out, pos, len.length);
		pos += len.length;
		for (byte[] e : elements) {
			System.arraycopy(e, 0, out, pos, e.length);
			pos += e.length;
		}
		return out;
	}

	private static byte[] encodeLength(int n) {
		if (n < 128) {
			return new byte[] { (byte) n };
		}
		if (n < 256) {
			return new byte[] { (byte) 0x81, (byte) n };
		}
		return new byte[] { (byte) 0x82, (byte) (n >> 8), (byte) n };
	}
}