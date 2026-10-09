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
package top.ruilink.inkwash.cmd.license;

import lombok.extern.slf4j.Slf4j;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * RSA Key Manager for license generation Generates and manages RSA key pairs
 * for license signing
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Slf4j
public class LicenseKeyManager {

	private static final int KEY_SIZE = 2048;

	private String privateKeyBase64;
	private String publicKeyBase64;

	private PrivateKey privateKey;
	private PublicKey publicKey;

	public LicenseKeyManager(String privateKeyBase64, String publicKeyBase64) {
		this.privateKeyBase64 = privateKeyBase64;
		this.publicKeyBase64 = publicKeyBase64;
		init();
	}

	private void init() {
		try {
			boolean hasPrivate = privateKeyBase64 != null && !privateKeyBase64.isEmpty();
			boolean hasPublic = publicKeyBase64 != null && !publicKeyBase64.isEmpty();

			if (hasPrivate && hasPublic) {
				privateKey = loadPrivateKey(privateKeyBase64);
				publicKey = loadPublicKey(publicKeyBase64);
				log.info("License RSA keys loaded from configuration");
			} else if (hasPrivate) {
				privateKey = loadPrivateKey(privateKeyBase64);
				publicKey = derivePublicKeyFromPrivate(privateKey);
				log.info("Public key derived from provided private key");
			} else if (hasPublic) {
				publicKey = loadPublicKey(publicKeyBase64);
				log.info("License public key loaded from configuration");
			} else {
				generateKeyPair();
				log.warn("Generated new RSA key pair for development. "
						+ "For production, provide license.private-key and license.public-key.");
			}
		} catch (Exception e) {
			log.error("Failed to initialize license keys", e);
			throw new RuntimeException("License key initialization failed", e);
		}
	}

	private PublicKey derivePublicKeyFromPrivate(PrivateKey privKey) throws Exception {
		java.math.BigInteger modulus;
		java.math.BigInteger publicExponent;

		if (privKey instanceof java.security.interfaces.RSAPrivateCrtKey rsaPrivKey) {
			modulus = rsaPrivKey.getModulus();
			publicExponent = rsaPrivKey.getPublicExponent();
		} else {
			modulus = ((java.security.interfaces.RSAPrivateKey) privKey).getModulus();
			publicExponent = new java.math.BigInteger("65537");
		}

		java.security.spec.RSAPublicKeySpec pubSpec = new java.security.spec.RSAPublicKeySpec(modulus, publicExponent);
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		return keyFactory.generatePublic(pubSpec);
	}

	public void generateKeyPair() {
		try {
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
			keyGen.initialize(KEY_SIZE);
			KeyPair pair = keyGen.generateKeyPair();
			this.privateKey = pair.getPrivate();
			this.publicKey = pair.getPublic();

			// String privateKeyStr =
			// Base64.getEncoder().encodeToString(privateKey.getEncoded());
			String publicKeyStr = Base64.getEncoder().encodeToString(publicKey.getEncoded());

			log.debug("=== Generated RSA Key Pair ===");
			log.debug("Private Key fingerprint: {}", java.util.Base64.getEncoder().encodeToString(java.util.Arrays
					.copyOf(java.security.MessageDigest.getInstance("SHA-256").digest(privateKey.getEncoded()), 8)));
			log.debug("Public Key: {}", publicKeyStr);
			log.debug("==============================");

		} catch (Exception e) {
			throw new RuntimeException("Failed to generate RSA key pair", e);
		}
	}

	private PrivateKey loadPrivateKey(String base64Key) throws Exception {
		// Handle PEM format (with headers) - strip headers and decode
		String cleanedKey = stripPemHeaders(base64Key, "PRIVATE KEY", "RSA PRIVATE KEY");
		byte[] keyBytes = Base64.getDecoder().decode(cleanedKey);

		// Try PKCS#8 first (standard format)
		try {
			PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			return keyFactory.generatePrivate(keySpec);
		} catch (java.security.spec.InvalidKeySpecException e) {
			// Fall back to PKCS#1 format (legacy "-----BEGIN RSA PRIVATE KEY-----")
			log.debug("PKCS#8 parsing failed, trying PKCS#1 format");
			return loadPkcs1PrivateKey(keyBytes);
		}
	}

	private PrivateKey loadPkcs1PrivateKey(byte[] pkcs1KeyBytes) throws Exception {
		// PKCS#1 RSAPrivateKey structure (ASN.1)
		// We need to parse it manually or use BouncyCastle
		// For simplicity, try to construct RSAPrivateKeySpec from the raw bytes
		// PKCS#1 format: SEQUENCE { version, modulus, publicExponent, privateExponent,
		// prime1, prime2, exponent1, exponent2, coefficient }
		try {
			// Use ASN.1 parsing via KeyFactory with PKCS8EncodedKeySpec won't work for
			// PKCS#1
			// Try using BouncyCastle if available, otherwise manually parse
			return parsePkcs1Manually(pkcs1KeyBytes);
		} catch (Exception ex) {
			throw new RuntimeException(
					"Failed to parse PKCS#1 private key. Please convert to PKCS#8 format using: openssl pkcs8 -topk8 -inform PEM -in key.pem -outform PEM -nocrypt",
					ex);
		}
	}

	private PrivateKey parsePkcs1Manually(byte[] pkcs1KeyBytes) throws Exception {
		// Manual ASN.1 parsing for PKCS#1 RSAPrivateKey
		// SEQUENCE { version INTEGER, modulus INTEGER, publicExponent INTEGER,
		// privateExponent INTEGER,
		// prime1 INTEGER, prime2 INTEGER, exponent1 INTEGER, exponent2 INTEGER,
		// coefficient INTEGER }
		// Malformed input must fail with a clean parse error, never an index/length
		// exception.

		int offset = 0;
		if (pkcs1KeyBytes.length < 2) {
			throw new IllegalArgumentException("Invalid PKCS#1 format: key data too short");
		}
		if (pkcs1KeyBytes[offset] != 0x30) { // SEQUENCE tag
			throw new IllegalArgumentException("Invalid PKCS#1 format: not a SEQUENCE");
		}
		offset++;

		// Parse length
		if (offset >= pkcs1KeyBytes.length) {
			throw new IllegalArgumentException("Invalid PKCS#1 format: truncated SEQUENCE length");
		}
		int length = 0;
		if ((pkcs1KeyBytes[offset] & 0x80) == 0) {
			length = pkcs1KeyBytes[offset++] & 0xFF;
		} else {
			int lenBytes = pkcs1KeyBytes[offset++] & 0xFF;
			lenBytes &= 0x7F;
			if (lenBytes == 0 || lenBytes > 4 || offset + lenBytes > pkcs1KeyBytes.length) {
				throw new IllegalArgumentException("Invalid PKCS#1 format: invalid SEQUENCE length encoding");
			}
			for (int i = 0; i < lenBytes; i++) {
				length = (length << 8) | (pkcs1KeyBytes[offset++] & 0xFF);
			}
		}
		if (length <= 0 || offset + length > pkcs1KeyBytes.length) {
			throw new IllegalArgumentException("Invalid PKCS#1 format: SEQUENCE length out of range");
		}

		// Now parse the 9 INTEGER values
		java.math.BigInteger[] values = new java.math.BigInteger[9];
		for (int i = 0; i < 9; i++) {
			if (offset >= pkcs1KeyBytes.length || pkcs1KeyBytes[offset] != 0x02) { // INTEGER tag
				throw new IllegalArgumentException("Invalid PKCS#1 format: expected INTEGER at position " + i);
			}
			offset++;

			// Parse integer length
			if (offset >= pkcs1KeyBytes.length) {
				throw new IllegalArgumentException("Invalid PKCS#1 format: truncated INTEGER length at position " + i);
			}
			int intLen = 0;
			if ((pkcs1KeyBytes[offset] & 0x80) == 0) {
				intLen = pkcs1KeyBytes[offset++] & 0xFF;
			} else {
				int lenBytes = pkcs1KeyBytes[offset++] & 0xFF;
				lenBytes &= 0x7F;
				if (lenBytes == 0 || lenBytes > 4 || offset + lenBytes > pkcs1KeyBytes.length) {
					throw new IllegalArgumentException(
							"Invalid PKCS#1 format: invalid INTEGER length encoding at position " + i);
				}
				for (int j = 0; j < lenBytes; j++) {
					intLen = (intLen << 8) | (pkcs1KeyBytes[offset++] & 0xFF);
				}
			}
			if (intLen <= 0 || offset + intLen > pkcs1KeyBytes.length) {
				throw new IllegalArgumentException(
						"Invalid PKCS#1 format: INTEGER length out of range at position " + i);
			}

			// Read integer value (skip leading zero if present for positive numbers)
			byte[] intBytes = new byte[intLen];
			System.arraycopy(pkcs1KeyBytes, offset, intBytes, 0, intLen);
			offset += intLen;
			values[i] = new java.math.BigInteger(1, intBytes); // Use positive magnitude
		}

		// values[0] = version, values[1] = modulus (n), values[2] = publicExponent (e)
		// values[3] = privateExponent (d), values[4] = prime1 (p), values[5] = prime2
		// (q)
		// values[6] = exponent1 (d mod p-1), values[7] = exponent2 (d mod q-1),
		// values[8] = coefficient (q^-1 mod p)

		java.math.BigInteger modulus = values[1];
		java.math.BigInteger privateExponent = values[3];

		RSAPrivateKeySpec keySpec = new RSAPrivateKeySpec(modulus, privateExponent);
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		return keyFactory.generatePrivate(keySpec);
	}

	private PublicKey loadPublicKey(String base64Key) throws Exception {
		// Handle PEM format (with headers) - strip headers and decode
		String cleanedKey = stripPemHeaders(base64Key, "PUBLIC KEY");
		byte[] keyBytes = Base64.getDecoder().decode(cleanedKey);

		X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		return keyFactory.generatePublic(keySpec);
	}

	private String stripPemHeaders(String key, String... headerTypes) {
		String cleaned = key.trim();
		// Remove PEM headers/footers if present
		for (String headerType : headerTypes) {
			String beginMarker = "-----BEGIN " + headerType + "-----";
			String endMarker = "-----END " + headerType + "-----";
			if (cleaned.contains(beginMarker)) {
				cleaned = cleaned.substring(cleaned.indexOf(beginMarker) + beginMarker.length());
				if (cleaned.contains(endMarker)) {
					cleaned = cleaned.substring(0, cleaned.indexOf(endMarker));
				}
			}
		}
		// Remove whitespace (newlines, spaces)
		return cleaned.replaceAll("\\s+", "");
	}

	public PrivateKey getPrivateKey() {
		return privateKey;
	}

	public PublicKey getPublicKey() {
		return publicKey;
	}

	public String getPublicKeyBase64() {
		return Base64.getEncoder().encodeToString(publicKey.getEncoded());
	}

	public String getPrivateKeyBase64() {
		return Base64.getEncoder().encodeToString(privateKey.getEncoded());
	}
}
