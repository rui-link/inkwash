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

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * License data model Contains all information needed for license generation
 * Structure must stay compatible with
 * top.ruilink.inkwash.security.license.LicenseData
 *
 * @author Dyllon
 * @since 0.5.1
 */
public record LicenseData(String subject, String holder, String email, LocalDateTime issuedDate,
		LocalDateTime expirationDate, String machineCode, LicenseType type, Integer maxUsers, String edition,
		List<String> modules, Map<String, Serializable> extra, int version) {
	public LicenseData {
		if (version <= 0)
			version = 1;
	}

	public enum LicenseType {
		PERMANENT, TRIAL
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private String subject;
		private String holder;
		private String email;
		private LocalDateTime issuedDate;
		private LocalDateTime expirationDate;
		private String machineCode;
		private LicenseType type;
		private Integer maxUsers;
		private String edition;
		private List<String> modules;
		private Map<String, Serializable> extra;
		private int version = 1;

		public Builder subject(String subject) {
			this.subject = subject;
			return this;
		}

		public Builder holder(String holder) {
			this.holder = holder;
			return this;
		}

		public Builder email(String email) {
			this.email = email;
			return this;
		}

		public Builder issuedDate(LocalDateTime issuedDate) {
			this.issuedDate = issuedDate;
			return this;
		}

		public Builder expirationDate(LocalDateTime expirationDate) {
			this.expirationDate = expirationDate;
			return this;
		}

		public Builder machineCode(String machineCode) {
			this.machineCode = machineCode;
			return this;
		}

		public Builder type(LicenseType type) {
			this.type = type;
			return this;
		}

		public Builder maxUsers(Integer maxUsers) {
			this.maxUsers = maxUsers;
			return this;
		}

		public Builder edition(String edition) {
			this.edition = edition;
			return this;
		}

		public Builder modules(List<String> modules) {
			this.modules = modules;
			return this;
		}

		public Builder extra(Map<String, Serializable> extra) {
			this.extra = extra;
			return this;
		}

		public Builder version(int version) {
			this.version = version;
			return this;
		}

		public LicenseData build() {
			return new LicenseData(subject, holder, email, issuedDate, expirationDate, machineCode, type, maxUsers,
					edition, modules, extra, version);
		}
	}
}
