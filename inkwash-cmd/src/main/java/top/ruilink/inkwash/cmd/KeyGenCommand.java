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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import top.ruilink.inkwash.cmd.license.LicenseKeyManager;

/**
 * Generates an RSA key pair and writes public/private PEM files. The private
 * key is never written to stdout; both keys go to files. Existing files are
 * never overwritten unless --force is passed.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Command(name = "keygen", mixinStandardHelpOptions = true, description = "Generate an RSA key pair and write PEM files")
public class KeyGenCommand implements Callable<Integer> {

	@Option(names = { "-k",
			"--private-key-file" }, paramLabel = "PATH", description = "Output path for the private key (default: ${DEFAULT-VALUE})")
	private String privateKeyFile = "inkwash-private-key.pem";

	@Option(names = {
			"--public-key-file" }, paramLabel = "PATH", description = "Output path for the public key (default: ${DEFAULT-VALUE})")
	private String publicKeyFile = "inkwash-public-key.pem";

	@Option(names = "--force", description = "Overwrite existing key files")
	private boolean force;

	@Override
	public Integer call() {
		try {
			Path priv = Path.of(privateKeyFile);
			Path pub = Path.of(publicKeyFile);

			if (!force) {
				if (Files.exists(priv)) {
					System.err.println("Error: private key file already exists: " + priv.toAbsolutePath());
					System.err.println("Use --force to overwrite it.");
					return 1;
				}
				if (Files.exists(pub)) {
					System.err.println("Error: public key file already exists: " + pub.toAbsolutePath());
					System.err.println("Use --force to overwrite it.");
					return 1;
				}
			}

			LicenseKeyManager keyManager = new LicenseKeyManager(null, null);
			writePem(priv, "PRIVATE KEY", keyManager.getPrivateKeyBase64());
			writePem(pub, "PUBLIC KEY", keyManager.getPublicKeyBase64());
			setOwnerOnlyPermissions(priv);

			System.out.println("Private key written to: " + priv.toAbsolutePath());
			System.out.println("Public key written to: " + pub.toAbsolutePath());
			System.out.println();
			System.out.println("Keep the private key secret. Use --private-key-file <path> or set");
			System.out.println("INKWASH_PRIVATE_KEY when generating licenses.");
			return 0;
		} catch (Exception e) {
			System.err.println("Error generating keys: " + e.getMessage());
			return 1;
		}
	}

	private static void writePem(Path path, String pemType, String base64) throws IOException {
		StringBuilder pem = new StringBuilder("-----BEGIN ").append(pemType).append("-----\n");
		for (int i = 0; i < base64.length(); i += 64) {
			pem.append(base64, i, Math.min(i + 64, base64.length())).append('\n');
		}
		pem.append("-----END ").append(pemType).append("-----\n");
		Files.writeString(path, pem.toString(), StandardCharsets.US_ASCII);
	}

	private static void setOwnerOnlyPermissions(Path path) {
		try {
			var perms = Files.getPosixFilePermissions(path);
			perms.clear();
			perms.add(PosixFilePermission.OWNER_READ);
			perms.add(PosixFilePermission.OWNER_WRITE);
			Files.setPosixFilePermissions(path, perms);
		} catch (UnsupportedOperationException e) {
			// Not a POSIX filesystem (e.g. Windows) - best effort only
		} catch (Exception e) {
			// Best effort
		}
	}
}
