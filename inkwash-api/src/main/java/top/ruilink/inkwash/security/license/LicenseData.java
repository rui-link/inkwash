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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * License data model Contains all information needed for license validation
 * Compatible with inkwash-cmd LicenseData record structure
 *
 * @author Dyllon
 * @since 0.5.1
 */
public class LicenseData {

	/**
	 * License subject (software name)
	 */
	private String subject;

	/**
	 * License holder/owner
	 */
	private String holder;

	/**
	 * License holder's email
	 */
	private String email;

	/**
	 * Issue date
	 */
	private LocalDateTime issuedDate;

	/**
	 * Expiration date recorded for reference; never checked during verification
	 */
	private LocalDateTime expirationDate;

	/**
	 * Machine code for hardware binding, required for PERMANENT and optional for
	 * TRIAL
	 */
	private String machineCode;

	/**
	 * License type: PERMANENT or TRIAL
	 */
	private LicenseType type;

	/**
	 * Maximum allowed users (0 = unlimited)
	 */
	private Integer maxUsers;

	/**
	 * License edition: personal, professional, enterprise
	 */
	private String edition;

	/**
	 * Custom license properties (version, features, etc.)
	 */
	private Map<String, Object> extra;

	/**
	 * Allowed modules (system, cms, monitor)
	 */
	private List<String> modules;

	/**
	 * License version for future compatibility
	 */
	private Integer version;

	/**
	 * License type enum
	 */
	public enum LicenseType {
		/**
		 * Permanent license, must carry a machine code
		 */
		PERMANENT,
		/**
		 * Trial license, machine code optional
		 */
		TRIAL
	}

	@JsonCreator
	public LicenseData(@JsonProperty("subject") String subject, @JsonProperty("holder") String holder,
			@JsonProperty("email") String email, @JsonProperty("issuedDate") LocalDateTime issuedDate,
			@JsonProperty("expirationDate") LocalDateTime expirationDate,
			@JsonProperty("machineCode") String machineCode, @JsonProperty("type") LicenseType type,
			@JsonProperty("maxUsers") Integer maxUsers, @JsonProperty("edition") String edition,
			@JsonProperty("extra") Map<String, Object> extra, @JsonProperty("modules") List<String> modules,
			@JsonProperty("version") Integer version) {
		this.subject = subject;
		this.holder = holder;
		this.email = email;
		this.issuedDate = issuedDate;
		this.expirationDate = expirationDate;
		this.machineCode = machineCode;
		this.type = type;
		this.maxUsers = maxUsers;
		this.edition = edition;
		this.extra = extra;
		this.modules = modules;
		this.version = version != null && version > 0 ? version : 1;
	}

	// Default constructor for Jackson
	public LicenseData() {
		this.version = 1;
	}

	// Getters and Setters
	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getHolder() {
		return holder;
	}

	public void setHolder(String holder) {
		this.holder = holder;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public LocalDateTime getIssuedDate() {
		return issuedDate;
	}

	public void setIssuedDate(LocalDateTime issuedDate) {
		this.issuedDate = issuedDate;
	}

	public LocalDateTime getExpirationDate() {
		return expirationDate;
	}

	public void setExpirationDate(LocalDateTime expirationDate) {
		this.expirationDate = expirationDate;
	}

	public String getMachineCode() {
		return machineCode;
	}

	public void setMachineCode(String machineCode) {
		this.machineCode = machineCode;
	}

	public LicenseType getType() {
		return type;
	}

	public void setType(LicenseType type) {
		this.type = type;
	}

	public Integer getMaxUsers() {
		return maxUsers;
	}

	public void setMaxUsers(Integer maxUsers) {
		this.maxUsers = maxUsers;
	}

	public String getEdition() {
		return edition;
	}

	public void setEdition(String edition) {
		this.edition = edition;
	}

	public Map<String, Object> getExtra() {
		return extra;
	}

	public void setExtra(Map<String, Object> extra) {
		this.extra = extra;
	}

	public List<String> getModules() {
		return modules;
	}

	public void setModules(List<String> modules) {
		this.modules = modules;
	}

	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version != null && version > 0 ? version : 1;
	}

	/**
	 * Check if license is bound to a specific machine
	 */
	@JsonIgnore
	public boolean isMachineBound() {
		return machineCode != null && !machineCode.isEmpty();
	}

	/**
	 * Verify machine code matches
	 */
	@JsonIgnore
	public boolean verifyMachineCode(String code) {
		if (!isMachineBound()) {
			return true;
		}
		return machineCode.equals(code);
	}
}
