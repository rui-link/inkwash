package top.ruilink.inkwash.cmd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import picocli.CommandLine;

/**
 * Licence keygen, generate and validate subcommand exit code unit tests.
 *
 * @author Dyllon
 * @since 0.5.1
 */
class LicenseCommandTest {

	private int execute(String... args) {
		return new CommandLine(new LicenseCommand()).execute(args);
	}

	@Test
	void rootWithNoArgsPrintsUsageAndExitsNonZero() {
		assertNotEquals(0, execute());
	}

	@Test
	void keygenRefusesToOverwriteExistingKeyWithoutForce(@TempDir Path dir) {
		Path priv = dir.resolve("private.pem");
		Path pub = dir.resolve("public.pem");
		assertEquals(0, execute("keygen", "--private-key-file", priv.toString(), "--public-key-file", pub.toString()));
		assertNotEquals(0,
				execute("keygen", "--private-key-file", priv.toString(), "--public-key-file", pub.toString()));
	}

	@Test
	void keygenOverwritesWhenForceIsPassed(@TempDir Path dir) {
		Path priv = dir.resolve("private.pem");
		Path pub = dir.resolve("public.pem");
		assertEquals(0, execute("keygen", "--private-key-file", priv.toString(), "--public-key-file", pub.toString()));
		assertEquals(0, execute("keygen", "--private-key-file", priv.toString(), "--public-key-file", pub.toString(),
				"--force"));
	}

	@Test
	void validateOnMissingFileExitsNonZero(@TempDir Path dir) {
		Path missingLicense = dir.resolve("missing.dat");
		Path publicKey = dir.resolve("public.pem");
		assertNotEquals(0,
				execute("validate", "--license", missingLicense.toString(), "--public-key-file", publicKey.toString()));
	}

	@Test
	void generateRejectsInvalidTypeWithNonZeroExit() {
		assertNotEquals(0, execute("generate", "-o", "X", "-e", "x@example.com", "--type", "BOGUS"));
	}

	@Test
	void generateRejectsPermanentLicenseWithoutMachineCode(@TempDir Path dir) {
		Path priv = dir.resolve("private.pem");
		Path pub = dir.resolve("public.pem");
		assertEquals(0, execute("keygen", "--private-key-file", priv.toString(), "--public-key-file", pub.toString()));
		assertNotEquals(0,
				execute("generate", "-o", "Acme", "-e", "a@example.com", "--private-key-file", priv.toString(),
						"--public-key-file", pub.toString(), "--output", dir.resolve("out.dat").toString()),
				"a PERMANENT license cannot be generated without a machine code");
	}

	@Test
	void generateAllowsTrialLicenseWithoutMachineCode(@TempDir Path dir) {
		Path priv = dir.resolve("private.pem");
		Path pub = dir.resolve("public.pem");
		Path lic = dir.resolve("trial.dat");
		assertEquals(0, execute("keygen", "--private-key-file", priv.toString(), "--public-key-file", pub.toString()));
		assertEquals(0,
				execute("generate", "-o", "Acme", "-e", "a@example.com", "--type", "TRIAL", "--private-key-file",
						priv.toString(), "--public-key-file", pub.toString(), "--output", lic.toString()));
		assertEquals(0, execute("validate", "--license", lic.toString(), "--public-key-file", pub.toString()));
	}

	@Test
	void unknownSubcommandExitsNonZero() {
		assertNotEquals(0, execute("does-not-exist"));
	}

	@Test
	void completeRoundTripKeygenGenerateValidate(@TempDir Path dir) {
		Path priv = dir.resolve("private.pem");
		Path pub = dir.resolve("public.pem");
		Path lic = dir.resolve("license.dat");
		assertEquals(0, execute("keygen", "--private-key-file", priv.toString(), "--public-key-file", pub.toString()));
		// enterprise edition has no user cap (unlimited) - exercises the null maxUsers
		// path
		assertEquals(0,
				execute("generate", "-o", "Acme", "-e", "a@example.com", "--edition", "enterprise", "--machine",
						"AA:BB:CC:DD:EE:FF", "--private-key-file", priv.toString(), "--public-key-file", pub.toString(),
						"--output", lic.toString()));
		assertEquals(0, execute("validate", "--license", lic.toString(), "--public-key-file", pub.toString(),
				"--machine", "AA:BB:CC:DD:EE:FF"));
	}
}