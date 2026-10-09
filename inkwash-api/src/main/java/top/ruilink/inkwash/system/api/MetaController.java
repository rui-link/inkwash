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
package top.ruilink.inkwash.system.api;

import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;
import top.ruilink.inkwash.system.api.view.SysMetaView;

/**
 * Application build metadata and about REST endpoints.
 *
 * <p>
 * Both endpoints answer from {@code BuildProperties}, which is populated by the
 * {@code build-info} goal at package time. They used to return the
 * {@code SysMeta} domain object; they now return {@link SysMetaView} so the
 * public response shape is declared here rather than inherited from the
 * persistence layer (ISS-047).
 *
 * @author Dyllon
 * @since 0.5.1
 */
@RestController
@RequestMapping("/api/system")
@Validated
public class MetaController {

	private final BuildProperties buildProps;

	public MetaController(BuildProperties buildProps) {
		this.buildProps = buildProps;
	}

	/**
	 * Public build metadata. Consumed by the frontend {@code useSiteStore} to title
	 * the app, so it stays unauthenticated and carries no license information.
	 */
	@GetMapping("/meta")
	public ResponseEntity<SysMetaView> getAppInfo() {
		return ResponseEntity.ok(buildBaseView());
	}

	@GetMapping("/about")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<SysMetaView> getAbout() {
		SysMetaView view = buildBaseView();
		view.setSpringBoot(SpringBootVersion.getVersion());
		view.setLicense(buildProps.get("license"));
		return ResponseEntity.ok(view);
	}

	/**
	 * The eleven fields both endpoints share. Previously the two methods each
	 * repeated the same eleven setter calls; {@code description} in particular was
	 * set in both, and any new build property would have had to be added in two
	 * places.
	 */
	private SysMetaView buildBaseView() {
		SysMetaView view = new SysMetaView();
		view.setGroupId(buildProps.getGroup());
		view.setArtifactId(buildProps.getArtifact());
		view.setProjectName(buildProps.getName());
		view.setVersion(buildProps.getVersion());
		view.setBuildTime(buildProps.getTime());
		view.setShortName(buildProps.get("short.name"));
		view.setFormalName(buildProps.get("formal.name"));
		view.setDescription(buildProps.get("description"));
		view.setCopyright(buildProps.get("copyright"));
		view.setAuthor(buildProps.get("author"));
		view.setJavaVersion(buildProps.get("java.version"));
		return view;
	}
}