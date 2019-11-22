/*******************************************************************************
 *                                                                             *
 *                Copyright © 2015 - 2019 Subterranean Security                *
 *                                                                             *
 *  Licensed under the Apache License, Version 2.0 (the "License");            *
 *  you may not use this file except in compliance with the License.           *
 *  You may obtain a copy of the License at                                    *
 *                                                                             *
 *      http://www.apache.org/licenses/LICENSE-2.0                             *
 *                                                                             *
 *  Unless required by applicable law or agreed to in writing, software        *
 *  distributed under the License is distributed on an "AS IS" BASIS,          *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 *  See the License for the specific language governing permissions and        *
 *  limitations under the License.                                             *
 *                                                                             *
 ******************************************************************************/
package com.sandpolis.installer;

import com.sandpolis.core.soi.SoiUtil;
import com.sandpolis.core.util.ArtifactUtil;
import com.sandpolis.installer.util.InstallUtil;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.sandpolis.core.util.ArtifactUtil.ParsedCoordinate.fromCoordinate;

/**
 * @author cilki
 * @since 5.0.0
 */
public class JavafxInstaller extends Task<Void> {

	private static final Logger log = LoggerFactory.getLogger(JavafxInstaller.class);

	/**
	 * The installation directory.
	 */
	private Path destination;

	/**
	 * The Sandpolis instance that will be installed.
	 */
	private String coordinate;

	/**
	 * The Sandpolis version that will be installed.
	 */
	private String version = System.getProperty("version");

	/**
	 * The client configuration.
	 */
	protected String config;

	/**
	 * Whether the installation completed successfully.
	 */
	private boolean completed;

	/**
	 * A list of installation extensions.
	 */
	private List<Runnable> extensions;

	protected JavafxInstaller(Path destination) {
		this.destination = Objects.requireNonNull(destination);
		updateProgress(0, 1);
	}

	public static JavafxInstaller newServerInstaller(Path destination) {
		JavafxInstaller installer = new JavafxInstaller(destination);
		installer.coordinate = "com.sandpolis:sandpolis-server-vanilla:";
		return installer;
	}

	public static JavafxInstaller newViewerJfxInstaller(Path destination) {
		JavafxInstaller installer = new JavafxInstaller(destination);
		installer.coordinate = "com.sandpolis:sandpolis-viewer-jfx:";
		return installer;
	}

	public static JavafxInstaller newViewerCliInstaller(Path destination) {
		JavafxInstaller installer = new JavafxInstaller(destination);
		installer.coordinate = "com.sandpolis:sandpolis-viewer-cli:";
		return installer;
	}

	public static JavafxInstaller newClientInstaller(Path destination, String config) {
		JavafxInstaller installer = new JavafxInstaller(destination);
		installer.coordinate = "com.sandpolis:sandpolis-client-mega:";
		installer.config = config;
		return installer;
	}

	@Override
	protected Void call() throws Exception {
		log.debug("Executing installation for " + coordinate);

		if (version == null) {
			// Request latest version number
			updateMessage("Downloading metadata");
			version = ArtifactUtil.getLatestVersion(coordinate);
		}
		coordinate += version;

		// Create directories
		Path lib = destination.resolve("lib");
		Files.createDirectories(lib);

		// Download executable
		updateMessage("Downloading " + coordinate);
		Path executable = ArtifactUtil.download(lib, coordinate);

		// Calculate dependencies
		Set<String> dependencies = InstallUtil.computeDependencies(SoiUtil.readMatrix(executable), coordinate);

		double progress = 0;
		for (String dep : dependencies) {
			var coordinate = fromCoordinate(dep);
			Path dependency = lib.resolve(coordinate.filename);
			if (!Files.exists(dependency)) {
				InputStream in = JavafxInstaller.class.getResourceAsStream("/" + coordinate.filename);
				if (in != null) {
					updateMessage("Extracting " + dep);
					try (in) {
						Files.copy(in, dependency);
					}
				} else {
					updateMessage("Downloading " + dep);
					ArtifactUtil.download(lib, dep);
				}
			}

			progress++;
			updateProgress(progress, dependencies.size());
		}

		completed = true;
		return null;
	}

	public boolean isCompleted() {
		return completed;
	}

}
