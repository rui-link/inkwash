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
package top.ruilink.inkwash.base.config;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import top.ruilink.inkwash.base.util.ClientInfoUtil;

/**
 * Client information configuration supplying the trusted proxy allowlist as a
 * comma separated list of IPs or CIDRs. Without configuration no proxy header
 * is trusted.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Slf4j
@Component
public class ClientInfoConfig {

	private final Set<String> trustedProxies;

	public ClientInfoConfig(@Value("${server.trusted-proxies:}") String trustedProxies) {
		this.trustedProxies = Arrays.stream(trustedProxies.split(",")).map(String::trim).filter(s -> !s.isEmpty())
				.collect(Collectors.toUnmodifiableSet());
	}

	@PostConstruct
	public void init() {
		ClientInfoUtil.setTrustedProxies(trustedProxies);
		if (trustedProxies.isEmpty()) {
			log.info("未配置可信代理，忽略 X-Forwarded-For 等代理头");
		} else {
			log.info("可信代理名单已配置: {}", trustedProxies);
		}
	}
}
