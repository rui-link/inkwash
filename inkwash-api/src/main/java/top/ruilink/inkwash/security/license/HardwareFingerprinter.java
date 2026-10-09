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
package top.ruilink.inkwash.security.license;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

/**
 * NOTE: identical copy maintained in inkwash-api …security/license and
 * inkwash-cmd …cmd/license — keep in sync. Deterministic, cross-platform
 * hardware fingerprinting shared by the license generator (inkwash-cmd) and the
 * backend verifier (inkwash-api) so both sides derive the SAME machine code for
 * the same host.
 *
 * <p>
 * Design rules:
 * <ul>
 * <li>No timestamps, counters, randomness, or wall-clock state anywhere - the
 * output must be byte-identical across JVM restarts on the same host.</li>
 * <li>Stable identifiers are collected in a FIXED order into one canonical
 * string, so the hash input is stable even when the platform enumerates network
 * interfaces in a different order.</li>
 * <li>JVM vendor/version are deliberately NOT included: the cmd generator and
 * the api verifier usually run in different JVMs (often different JDKs) on the
 * same host, so JVM identity would break cross-JVM determinism - the exact
 * failure mode this shared implementation exists to remove.</li>
 * <li>A {@code v1|} marker prefixes the canonical string so a future algorithm
 * change can be compared by version instead of silently breaking.</li>
 * </ul>
 *
 * @author Dyllon
 * @since 0.5.1
 */
public final class HardwareFingerprinter {

	/** Version marker: bump whenever the canonical join or source set changes. */
	private static final String VERSION_MARKER = "v1|";

	/** Stable, role-independent join separator between canonical parts. */
	private static final String SEP = "|";

	/**
	 * Interface name / display-name tokens identifying virtual, VPN, bridge or
	 * container adapters that must not be used as a physical machine identity.
	 */
	private static final List<String> VIRTUAL_NAME_TOKENS = List.of("vethernet", "docker", "br-", "virbr", "vmnet",
			"vmware", "vbox", "virtualbox", "tailscale", "wsl", "hyper-v", "hyperv", "vm");

	private HardwareFingerprinter() {
	}

	/**
	 * Canonical description of this host's stable identifiers. Deterministic per
	 * host; never contains timestamps or randomness.
	 */
	public static FingerprintResult fingerprint() {
		List<String> macs = collectMacAddresses();
		String osName = safeProperty("os.name");
		String osVersion = safeProperty("os.version");
		String osArch = safeProperty("os.arch");
		String hostname = hostnameOrEmpty();

		String macPart = macs.isEmpty() ? "" : String.join(",", macs);
		String canonical = VERSION_MARKER + String.join(SEP, macPart, osName, osVersion, osArch, hostname);

		String source = "macs=" + macs.size() + ",os=" + osName + " " + osVersion + " " + osArch + ",hostname="
				+ (hostname.isEmpty() ? "-" : hostname);
		return new FingerprintResult(canonical, source);
	}

	/**
	 * SHA-256 of {@link #fingerprint()} truncated to its first 16 bytes and
	 * rendered as 32 lowercase hex chars. The width/format match what the cmd tool
	 * historically printed, so license-binding semantics stay stable.
	 */
	public static String machineCode() {
		String canonical = fingerprint().fingerprint();
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder(32);
			for (int i = 0; i < 16; i++) {
				sb.append(String.format(Locale.ROOT, "%02x", hash[i]));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 not available", e);
		}
	}

	/**
	 * Physical NIC MAC addresses in stable (sorted, normalized) order, virtual and
	 * loopback adapters filtered out. Empty when no physical NIC is visible.
	 */
	static List<String> collectMacAddresses() {
		List<IfEntry> entries = new ArrayList<>();
		try {
			Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
			while (ifaces != null && ifaces.hasMoreElements()) {
				NetworkInterface ni = ifaces.nextElement();
				try {
					boolean skip = ni.isLoopback() || ni.isVirtual() || ni.isPointToPoint();
					entries.add(new IfEntry(ni.getName(), ni.getDisplayName(), skip, ni.getHardwareAddress()));
				} catch (SocketException e) {
					// Per-interface inspection failed - skip this interface.
				}
			}
		} catch (SocketException e) {
			// No interface info visible at all - weaker hostname+os fingerprint.
		}
		return selectMacAddresses(entries);
	}

	/**
	 * Filters virtual/loopback adapters out of a list of interface descriptors and
	 * returns the surviving MAC addresses sorted by their normalized string form.
	 * Package-visible so tests can pin the deterministic order with a stub list.
	 */
	static List<String> selectMacAddresses(List<IfEntry> interfaces) {
		List<String> macs = new ArrayList<>();
		for (IfEntry entry : interfaces) {
			if (entry.skip()) {
				continue;
			}
			if (isVirtualName(entry.name()) || isVirtualName(entry.displayName())) {
				continue;
			}
			byte[] hw = entry.hardwareAddress();
			if (hw == null || hw.length == 0) {
				continue;
			}
			macs.add(formatMac(hw));
		}
		Collections.sort(macs);
		return macs;
	}

	/** Synthetic view of a {@link NetworkInterface} usable as a pure data input. */
	record IfEntry(String name, String displayName, boolean skip, byte[] hardwareAddress) {
	}

	private static boolean isVirtualName(String name) {
		if (name == null) {
			return false;
		}
		String lower = name.toLowerCase(Locale.ROOT);
		for (String token : VIRTUAL_NAME_TOKENS) {
			if (lower.contains(token)) {
				return true;
			}
		}
		return false;
	}

	private static String formatMac(byte[] mac) {
		StringBuilder sb = new StringBuilder(mac.length * 3 - 1);
		for (int i = 0; i < mac.length; i++) {
			if (i > 0) {
				sb.append(':');
			}
			sb.append(String.format(Locale.ROOT, "%02x", mac[i]));
		}
		return sb.toString();
	}

	private static String hostnameOrEmpty() {
		try {
			return String.valueOf(InetAddress.getLocalHost().getHostName());
		} catch (Exception e) {
			// UnknownHostException, SecurityException, ... - hostname is optional.
			return "";
		}
	}

	private static String safeProperty(String key) {
		try {
			String value = System.getProperty(key);
			return value == null ? "" : value.trim();
		} catch (SecurityException e) {
			return "";
		}
	}
}
