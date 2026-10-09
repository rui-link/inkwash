package top.ruilink.inkwash.cmd.license;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import top.ruilink.inkwash.cmd.license.HardwareFingerprinter.IfEntry;

/**
 * Machine code, fingerprint and MAC address selection unit tests.
 *
 * @author Dyllon
 * @since 0.5.1
 */
class HardwareFingerprinterTest {

	@Test
	void machineCodeIs32LowercaseHex() {
		String code = HardwareFingerprinter.machineCode();
		assertNotNull(code);
		assertEquals(32, code.length());
		assertTrue(code.matches("[0-9a-f]{32}"), "machine code must be continuous 32-hex, got: " + code);
	}

	@Test
	void machineCodeIsDeterministicWithinTheSameJvm() {
		String a = HardwareFingerprinter.machineCode();
		String b = HardwareFingerprinter.machineCode();
		assertEquals(a, b, "machine code must be stable across calls in the same JVM");
	}

	@Test
	void fingerprintIsNonEmptyAndStable() {
		var a = HardwareFingerprinter.fingerprint();
		var b = HardwareFingerprinter.fingerprint();
		assertNotNull(a.fingerprint());
		assertFalse(a.fingerprint().isEmpty(), "fingerprint must never be empty");
		assertEquals(a, b, "fingerprint must be stable across calls in the same JVM");
		assertNotNull(a.source(), "diagnostic source description must be present");
	}

	@Test
	void selectMacAddressesSkipsVirtualLoopbackAndSortsByName() {
		List<IfEntry> stub = List.of(
				new IfEntry("wlan0", "WLAN", false, new byte[] { (byte) 0xaa, 0x11, 0x22, 0x33, 0x44, 0x55 }),
				new IfEntry("vEthernet (Switch)", "Hyper-V Virtual Switch", false,
						new byte[] { 0x00, 0x11, 0x22, 0x33, 0x44, 0x66 }),
				new IfEntry("docker0", "Docker", false, new byte[] { 0x00, 0x11, 0x22, 0x33, 0x44, 0x77 }),
				new IfEntry("lo", "Loopback", true, new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 }),
				new IfEntry("en0", "Ethernet", false, new byte[] { 0x00, 0x11, 0x22, 0x33, 0x44, 0x55 }), new IfEntry(
						"tailscale0", "Tailscale", false, new byte[] { (byte) 0x88, 0x11, 0x22, 0x33, 0x44, 0x55 }));

		List<String> result = HardwareFingerprinter.selectMacAddresses(stub);

		// Only the two physical adapters survive; output is sorted by MAC string.
		assertEquals(List.of("00:11:22:33:44:55", "aa:11:22:33:44:55"), result);
	}

	@Test
	void emptyInterfaceListYieldsEmptyMacCollection() {
		List<String> result = HardwareFingerprinter.selectMacAddresses(List.of());
		assertTrue(result.isEmpty());
	}
}