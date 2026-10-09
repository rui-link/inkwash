/*
 * This file is part of Inkwash.
 * Copyright (C) 2026 ruilink team.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package top.ruilink.inkwash.cmd;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import top.ruilink.inkwash.cmd.license.LicenseConstants;
import top.ruilink.inkwash.cmd.license.LicenseData;
import top.ruilink.inkwash.cmd.license.LicenseGenerator;
import top.ruilink.inkwash.cmd.license.LicenseKeyManager;
import top.ruilink.inkwash.cmd.license.LicenseTypeConverter;
import top.ruilink.inkwash.cmd.license.HardwareFingerprinter;

/**
 * Builds a signed license envelope {content, signature} via
 * {@link LicenseGenerator}. The private key is never accepted as a raw CLI
 * argument value: use --private-key-file <path> or the INKWASH_PRIVATE_KEY
 * environment variable.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Command(name = "generate", mixinStandardHelpOptions = true, description = "Build a signed license envelope {content, signature}")
public class GenerateCommand implements Callable<Integer> {

	@Option(names = { "-o", "--holder" }, paramLabel = "HOLDER", required = true, description = "License holder name")
	private String holder;

	@Option(names = { "-e", "--email" }, paramLabel = "EMAIL", required = true, description = "License holder email")
	private String email;

	@Option(names = { "-s",
			"--subject" }, paramLabel = "SUBJECT", description = "License subject (default: ${DEFAULT-VALUE})")
	private String subject = "Inkwash";

	@Option(names = { "-t",
			"--type" }, paramLabel = "TYPE", converter = LicenseTypeConverter.class, description = "License type: PERMANENT, TRIAL (default: ${DEFAULT-VALUE})")
	private LicenseData.LicenseType type = LicenseData.LicenseType.PERMANENT;

	@Option(names = { "-d",
			"--days" }, paramLabel = "DAYS", description = "License validity in days (0 = permanent; TRIAL defaults to "
					+ LicenseConstants.DEFAULT_TRIAL_DAYS + ")")
	private int days = 0;

	@Option(names = { "-u", "--users" }, paramLabel = "USERS", description = "Maximum users (0 = use edition default)")
	private int maxUsers = 0;

	@Option(names = { "-m", "--machine" }, paramLabel = "CODE", description = "Machine code to bind the license to")
	private String machineCode;

	@Option(names = "--machine-auto", description = "Auto-detect the current machine code and bind the license to it")
	private boolean machineAuto;

	@Option(names = "--features", paramLabel = "FEATURES", description = "Enabled features (comma-separated)")
	private String features;

	@Option(names = "--edition", paramLabel = "EDITION", description = "License edition: personal (30 users), professional (100 users), "
			+ "enterprise (unlimited), trial (" + LicenseConstants.TRIAL_MAX_USERS
			+ " users) (default: ${DEFAULT-VALUE})")
	private String edition = "professional";

	@Option(names = "--private-key-file", paramLabel = "PATH", description = "Path to the RSA private key PEM file (or set INKWASH_PRIVATE_KEY)")
	private String privateKeyFile;

	@Option(names = "--public-key-file", paramLabel = "PATH", description = "Path to the RSA public key PEM file (or set INKWASH_PUBLIC_KEY)")
	private String publicKeyFile;

	@Option(names = "--output", paramLabel = "FILE", description = "Write the license envelope to FILE instead of stdout")
	private String outputFile;

	@Override
	public Integer call() {
		try {
			String privateKeyMaterial = resolvePrivateKey();
			String publicKeyMaterial = resolvePublicKey();
			if (privateKeyMaterial == null) {
				System.err.println("Warning: no private key specified (--private-key-file or INKWASH_PRIVATE_KEY)");
				System.err.println(
						"Generated an ephemeral development key pair - the license cannot be validated later.");
			}
			LicenseKeyManager keyManager = new LicenseKeyManager(privateKeyMaterial, publicKeyMaterial);

			// Handle --machine-auto: auto-detect machine code
			String effectiveMachineCode = machineCode;
			if (machineAuto) {
				if (effectiveMachineCode != null && !effectiveMachineCode.isEmpty()) {
					System.err.println("Warning: both --machine and --machine-auto specified. Using --machine value.");
				} else {
					effectiveMachineCode = HardwareFingerprinter.machineCode();
					System.out.println("Auto-detected machine code: " + effectiveMachineCode);
				}
			}

			// Every non-TRIAL license must carry a machine code, otherwise the
			// verifiers reject it. Fail here rather than mint an unusable license.
			if (type != LicenseData.LicenseType.TRIAL
					&& (effectiveMachineCode == null || effectiveMachineCode.isEmpty())) {
				System.err.println(
						"Error: " + type + " licenses require a machine code. Pass --machine CODE or --machine-auto.");
				return 1;
			}

			String editionLower = edition.trim().toLowerCase();
			Integer editionMaxUsers = LicenseConstants.maxUsersForEdition(editionLower);
			Integer effectiveMaxUsers = (maxUsers == 0) ? editionMaxUsers : Integer.valueOf(maxUsers);

			List<String> modules = List.of("system", "cms", "monitor");

			int effectiveDays = days;
			if (effectiveDays <= 0 && type == LicenseData.LicenseType.TRIAL) {
				effectiveDays = LicenseConstants.DEFAULT_TRIAL_DAYS;
			}

			// Build extra map (features only; modules live as a top-level field)
			var extra = new HashMap<String, Serializable>();
			if (features != null && !features.isEmpty()) {
				extra.put("features", new ArrayList<>(List.of(features.split(","))));
			}

			var builder = LicenseData.builder().subject(subject).holder(holder).email(email)
					.issuedDate(LocalDateTime.now()).type(type).maxUsers(effectiveMaxUsers).edition(editionLower)
					.modules(modules).machineCode(effectiveMachineCode).version(1);

			if (effectiveDays > 0) {
				builder.expirationDate(LocalDateTime.now().plusDays(effectiveDays));
			}
			if (!extra.isEmpty()) {
				builder.extra(extra);
			}

			LicenseData data = builder.build();

			LicenseGenerator generator = new LicenseGenerator(keyManager);
			String license = generator.generateLicense(data);

			System.out.println();
			System.out.println("======================================");
			System.out.println("  License Generated Successfully!");
			System.out.println("======================================");
			System.out.println();
			System.out.println("Subject: " + subject);
			System.out.println("Holder: " + holder);
			System.out.println("Email: " + email);
			System.out.println("Type: " + type);
			System.out.println("Edition: " + editionLower);
			System.out.println("Max Users: " + (effectiveMaxUsers == null ? "Unlimited" : effectiveMaxUsers));
			System.out.println("Modules: " + String.join(", ", modules));
			if (effectiveMachineCode != null) {
				System.out.println("Machine Code: " + effectiveMachineCode);
			}
			System.out.println();

			if (outputFile != null && !outputFile.isEmpty()) {
				Files.writeString(Path.of(outputFile), license + "\n", StandardCharsets.UTF_8);
				System.out.println("License written to: " + Path.of(outputFile).toAbsolutePath());
			} else {
				System.out.println("--- LICENSE STRING ---");
				System.out.println(license);
				System.out.println("--- END LICENSE ---");
			}
			System.out.println();

			if (publicKeyMaterial == null) {
				System.out.println("--- PUBLIC KEY (embed in inkwash-api) ---");
				System.out.println(keyManager.getPublicKeyBase64());
				System.out.println("--- END PUBLIC KEY ---");
			}

			return 0;
		} catch (IllegalArgumentException e) {
			System.err.println("Error generating license: " + e.getMessage());
			return 1;
		} catch (Exception e) {
			System.err.println("Error generating license: " + e.getMessage());
			return 1;
		}
	}

	private String resolvePrivateKey() {
		if (privateKeyFile != null && !privateKeyFile.isBlank()) {
			return readFile(privateKeyFile, "private key");
		}
		String env = System.getenv("INKWASH_PRIVATE_KEY");
		if (env != null && !env.isBlank()) {
			return env;
		}
		return null;
	}

	private String resolvePublicKey() {
		if (publicKeyFile != null && !publicKeyFile.isBlank()) {
			return readFile(publicKeyFile, "public key");
		}
		String env = System.getenv("INKWASH_PUBLIC_KEY");
		if (env != null && !env.isBlank()) {
			return env;
		}
		return null;
	}

	private static String readFile(String path, String what) {
		try {
			return Files.readString(Path.of(path)).trim();
		} catch (IOException e) {
			throw new IllegalStateException("Failed to read " + what + " file: " + path, e);
		}
	}
}
