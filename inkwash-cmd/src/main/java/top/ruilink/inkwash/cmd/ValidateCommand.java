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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import top.ruilink.inkwash.cmd.license.LicenseData;
import top.ruilink.inkwash.cmd.license.LicenseKeyManager;
import top.ruilink.inkwash.cmd.license.LicenseVerifier;

/**
 * Verifies a license file exactly as the inkwash-api backend does: Base64
 * envelope {content, signature}, SHA256withRSA signature over the base64
 * content, then content parsing and optional machine-code binding. Exits
 * non-zero on any failure.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Command(name = "validate", mixinStandardHelpOptions = true, description = "Verify a license file (as the inkwash-api backend does)")
public class ValidateCommand implements Callable<Integer> {

	@Option(names = "--license", paramLabel = "FILE", required = true, description = "Path to the license file")
	private String licenseFile;

	@Option(names = "--public-key-file", paramLabel = "PATH", description = "Path to the RSA public key PEM file (or set INKWASH_PUBLIC_KEY)")
	private String publicKeyFile;

	@Option(names = { "-m",
			"--machine" }, paramLabel = "CODE", description = "Expected machine code for machine-bound licenses")
	private String machineCode;

	@Override
	public Integer call() {
		try {
			String publicKeyMaterial = resolvePublicKey();
			if (publicKeyMaterial == null) {
				System.err.println(
						"Error: no public key provided. Use --public-key-file <path> or set INKWASH_PUBLIC_KEY.");
				return 1;
			}

			String license = Files.readString(Path.of(licenseFile)).trim();
			if (license.isEmpty()) {
				System.err.println("Error: license file is empty: " + licenseFile);
				return 1;
			}

			LicenseKeyManager keyManager = new LicenseKeyManager(null, publicKeyMaterial);
			LicenseVerifier.Result result = new LicenseVerifier(keyManager.getPublicKey()).verify(license, machineCode);

			if (!result.valid()) {
				System.err.println("License validation failed: " + result.message());
				return 1;
			}

			LicenseData data = result.data();
			System.out.println("License is valid: " + result.message());
			System.out.println("Subject: " + data.subject());
			System.out.println("Holder: " + data.holder());
			System.out.println("Email: " + data.email());
			System.out.println("Type: " + data.type());
			System.out.println("Edition: " + data.edition());
			System.out.println("Max Users: " + (data.maxUsers() == null ? "Unlimited" : data.maxUsers()));
			System.out.println(
					"Modules: " + String.join(", ", data.modules() == null ? List.<String>of() : data.modules()));
			System.out.println("Machine Bound: " + (data.machineCode() == null ? "No" : data.machineCode()));
			System.out.println("Issued At: " + data.issuedDate());
			System.out.println("Expires At: "
					+ (data.expirationDate() == null ? "Never (not enforced by backend)" : data.expirationDate()));
			return 0;
		} catch (IOException e) {
			System.err.println("Error reading license file: " + licenseFile + " (" + e.getMessage() + ")");
			return 1;
		} catch (Exception e) {
			System.err.println("License validation failed: " + e.getMessage());
			return 1;
		}
	}

	private String resolvePublicKey() {
		if (publicKeyFile != null && !publicKeyFile.isBlank()) {
			try {
				return Files.readString(Path.of(publicKeyFile)).trim();
			} catch (IOException e) {
				throw new IllegalStateException("Failed to read public key file: " + publicKeyFile, e);
			}
		}
		String env = System.getenv("INKWASH_PUBLIC_KEY");
		if (env != null && !env.isBlank()) {
			return env;
		}
		return null;
	}
}
