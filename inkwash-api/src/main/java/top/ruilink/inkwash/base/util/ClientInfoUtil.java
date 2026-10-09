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
package top.ruilink.inkwash.base.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Client information helper extracting the client IP from the request and
 * parsing the browser, operating system and login device from the User-Agent
 * header.
 * 
 * @author Dyllon
 * @since 0.5.1
 */
public final class ClientInfoUtil {

	private ClientInfoUtil() {
	}

	/**
	 * Trusted proxy allowlist of IPs or CIDRs; empty by default so no proxy header
	 * is trusted.
	 */
	private static volatile Set<String> trustedProxies = Set.of();

	public static void setTrustedProxies(Set<String> proxies) {
		trustedProxies = proxies == null ? Set.of() : Set.copyOf(proxies);
	}

	/**
	 * Resolves the client IP, trusting proxy headers only for configured proxies to
	 * avoid forged X-Forwarded-For.
	 */
	public static String getClientIp(HttpServletRequest request) {
		if (request == null) {
			return null;
		}
		String remoteAddr = request.getRemoteAddr();
		if (!isTrustedProxy(remoteAddr)) {
			return remoteAddr;
		}
		String ip = request.getHeader("X-Forwarded-For");
		if (isUnknown(ip)) {
			ip = request.getHeader("Proxy-Client-IP");
		}
		if (isUnknown(ip)) {
			ip = request.getHeader("X-Real-IP");
		}
		if (isUnknown(ip)) {
			return remoteAddr;
		}
		return ip.contains(",") ? ip.split(",")[0].trim() : ip;
	}

	private static boolean isUnknown(String ip) {
		return ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip);
	}

	private static boolean isTrustedProxy(String remoteAddr) {
		if (remoteAddr == null) {
			return false;
		}
		Set<String> proxies = trustedProxies;
		if (proxies.isEmpty()) {
			return false;
		}
		for (String entry : proxies) {
			if (entry.contains("/")) {
				if (cidrContains(remoteAddr, entry)) {
					return true;
				}
			} else if (hostMatches(remoteAddr, entry)) {
				return true;
			}
		}
		return false;
	}

	private static boolean hostMatches(String remoteAddr, String host) {
		if (remoteAddr.equals(host)) {
			return true;
		}
		try {
			return InetAddress.getByName(remoteAddr).equals(InetAddress.getByName(host));
		} catch (UnknownHostException e) {
			return false;
		}
	}

	private static boolean cidrContains(String ip, String cidr) {
		String[] parts = cidr.split("/", 2);
		if (parts.length != 2) {
			return false;
		}
		int prefixLen;
		try {
			prefixLen = Integer.parseInt(parts[1]);
		} catch (NumberFormatException e) {
			return false;
		}
		try {
			byte[] addr = InetAddress.getByName(ip).getAddress();
			byte[] network = InetAddress.getByName(parts[0]).getAddress();
			if (addr.length != network.length) {
				return false;
			}
			if (prefixLen < 0 || prefixLen > addr.length * 8) {
				return false;
			}
			int fullBytes = prefixLen / 8;
			int remBits = prefixLen % 8;
			for (int i = 0; i < fullBytes; i++) {
				if (addr[i] != network[i]) {
					return false;
				}
			}
			if (remBits > 0) {
				int mask = 0xFF << (8 - remBits);
				if ((addr[fullBytes] & mask) != (network[fullBytes] & mask)) {
					return false;
				}
			}
			return true;
		} catch (UnknownHostException e) {
			return false;
		}
	}

	/**
	 * Parses the browser name from the User-Agent header.
	 */
	public static String parseBrowser(String userAgent) {
		if (userAgent == null || userAgent.isBlank()) {
			return "";
		}
		String ua = userAgent.toLowerCase();
		if (ua.contains("micromessenger")) {
			return "微信";
		}
		if (ua.contains("edg/")) {
			return "Edge";
		}
		if (ua.contains("opr/") || ua.contains("opera")) {
			return "Opera";
		}
		if (ua.contains("firefox")) {
			return "Firefox";
		}
		if (ua.contains("chrome")) {
			return "Chrome";
		}
		if (ua.contains("safari")) {
			return "Safari";
		}
		if (ua.contains("360se") || ua.contains("360ee")) {
			return "360浏览器";
		}
		return "其他";
	}

	/**
	 * Parses the operating system name from the User-Agent header.
	 */
	public static String parseOs(String userAgent) {
		if (userAgent == null || userAgent.isBlank()) {
			return "";
		}
		String ua = userAgent.toLowerCase();
		if (ua.contains("windows nt 11")) {
			return "Windows 11";
		}
		if (ua.contains("windows nt 10")) {
			return "Windows 10";
		}
		if (ua.contains("windows nt 6.1")) {
			return "Windows 7";
		}
		if (ua.contains("windows nt 6.2") || ua.contains("windows nt 6.3")) {
			return "Windows 8";
		}
		if (ua.contains("windows")) {
			return "Windows";
		}
		if (ua.contains("android")) {
			return "Android";
		}
		if (ua.contains("iphone") || ua.contains("ipod") || ua.contains("ipad")) {
			return "iOS";
		}
		if (ua.contains("mac os x") || ua.contains("macintosh")) {
			return "macOS";
		}
		if (ua.contains("linux")) {
			return "Linux";
		}
		return "其他";
	}

	/**
	 * Parses the login device from the User-Agent header, shown when the login
	 * location is unavailable.
	 */
	public static String parseDevice(String userAgent) {
		if (userAgent == null || userAgent.isBlank()) {
			return "";
		}
		String ua = userAgent.toLowerCase();
		if (ua.contains("ipad") || ua.contains("tablet")) {
			return "平板";
		}
		if (ua.contains("iphone") || ua.contains("ipod")) {
			return "iphone";
		}
		if (ua.contains("android")) {
			return "安卓";
		}
		if (ua.contains("windows") || ua.contains("macintosh") || ua.contains("x11") || ua.contains("linux")) {
			return "电脑端";
		}
		return "其他";
	}
}
