package top.ruilink.inkwash.cmd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

import top.ruilink.inkwash.cmd.license.LicenseData;
import top.ruilink.inkwash.cmd.license.LicenseGenerator;
import top.ruilink.inkwash.cmd.license.LicenseKeyManager;
import top.ruilink.inkwash.cmd.license.LicenseVerifier;

/**
 * Licence payload signing, deterministic serialization and envelope structure
 * unit tests.
 *
 * @author Dyllon
 * @since 0.5.1
 */
class LicenseGeneratorTest {

	private static final LocalDateTime FIXED_DATE = LocalDateTime.of(2026, 1, 15, 10, 30, 0);

	private static final String MACHINE_CODE = "AA:BB:CC:DD:EE:FF";

	private LicenseData sampleData() {
		return LicenseData.builder().subject("Inkwash").holder("Test Holder").email("test@example.com")
				.issuedDate(FIXED_DATE).type(LicenseData.LicenseType.PERMANENT).maxUsers(30).edition("personal")
				.modules(List.of("system", "cms", "monitor")).machineCode(MACHINE_CODE).version(1).build();
	}

	@Test
	void signThenVerifyRoundTrip() {
		LicenseKeyManager keys = new LicenseKeyManager(null, null);
		LicenseGenerator generator = new LicenseGenerator(keys);
		String license = generator.generateLicense(sampleData());

		LicenseVerifier.Result result = new LicenseVerifier(keys.getPublicKey()).verify(license, MACHINE_CODE);
		assertTrue(result.valid(), result.message());
		assertEquals(sampleData().holder(), result.data().holder());
		assertEquals(sampleData().email(), result.data().email());
		assertEquals(FIXED_DATE, result.data().issuedDate());
		assertEquals("personal", result.data().edition());
		assertEquals(MACHINE_CODE, result.data().machineCode());
		assertEquals(LicenseData.LicenseType.PERMANENT, result.data().type());
	}

	@Test
	void machineBoundLicenseRejectsMissingMachineCode() {
		LicenseKeyManager keys = new LicenseKeyManager(null, null);
		String license = new LicenseGenerator(keys).generateLicense(sampleData());

		LicenseVerifier.Result result = new LicenseVerifier(keys.getPublicKey()).verify(license, null);
		assertFalse(result.valid(), "a machine-bound license must be rejected without a machine code");
	}

	@Test
	void machineBoundLicenseRejectsMismatchedMachineCode() {
		LicenseKeyManager keys = new LicenseKeyManager(null, null);
		String license = new LicenseGenerator(keys).generateLicense(sampleData());

		LicenseVerifier.Result result = new LicenseVerifier(keys.getPublicKey()).verify(license, "11:22:33:44:55:66");
		assertFalse(result.valid(), "a machine-bound license must reject a different machine code");
	}

	@Test
	void permanentLicenseWithoutMachineCodeIsRejected() {
		LicenseKeyManager keys = new LicenseKeyManager(null, null);
		LicenseData unbound = LicenseData.builder().subject("Inkwash").holder("Test Holder").email("test@example.com")
				.issuedDate(FIXED_DATE).type(LicenseData.LicenseType.PERMANENT).maxUsers(30).edition("personal")
				.modules(List.of("system")).version(1).build();
		String license = new LicenseGenerator(keys).generateLicense(unbound);

		LicenseVerifier.Result result = new LicenseVerifier(keys.getPublicKey()).verify(license, MACHINE_CODE);
		assertFalse(result.valid(), "a PERMANENT license must carry a machine code");
	}

	@Test
	void trialLicenseWithoutMachineCodeIsAccepted() {
		LicenseKeyManager keys = new LicenseKeyManager(null, null);
		LicenseData trial = LicenseData.builder().subject("Inkwash").holder("Test Holder").email("test@example.com")
				.issuedDate(FIXED_DATE).type(LicenseData.LicenseType.TRIAL).maxUsers(10).edition("trial")
				.modules(List.of("system")).version(1).build();
		String license = new LicenseGenerator(keys).generateLicense(trial);

		LicenseVerifier.Result result = new LicenseVerifier(keys.getPublicKey()).verify(license, null);
		assertTrue(result.valid(), result.message());
		assertEquals(LicenseData.LicenseType.TRIAL, result.data().type());
	}

	@Test
	void serializedLicenseIsDeterministicForSameInput() {
		LicenseKeyManager keys = new LicenseKeyManager(null, null);
		LicenseGenerator generator = new LicenseGenerator(keys);
		String first = generator.generateLicense(sampleData());
		String second = generator.generateLicense(sampleData());
		assertEquals(first, second);
	}

	@Test
	void envelopeHasContentAndSignatureStructure() throws Exception {
		LicenseKeyManager keys = new LicenseKeyManager(null, null);
		LicenseGenerator generator = new LicenseGenerator(keys);
		String license = generator.generateLicense(sampleData());

		JsonMapper mapper = JsonMapper.builder().build();
		String envelopeJson = new String(Base64.getDecoder().decode(license), StandardCharsets.UTF_8);
		var envelope = mapper.readTree(envelopeJson);
		assertNotNull(envelope.get("content"), "envelope must have a content field");
		assertNotNull(envelope.get("signature"), "envelope must have a signature field");

		String contentJson = new String(Base64.getDecoder().decode(envelope.get("content").asString()),
				StandardCharsets.UTF_8);
		var content = mapper.readTree(contentJson);
		assertEquals("Test Holder", content.get("holder").asString());
		assertEquals("personal", content.get("edition").asString());
		assertEquals("Inkwash", content.get("subject").asString());
	}
}