/******************************************************************************
 *                                                                            *
 *                    Copyright 2019 Subterranean Security                    *
 *                                                                            *
 *  Licensed under the Apache License, Version 2.0 (the "License");           *
 *  you may not use this file except in compliance with the License.          *
 *  You may obtain a copy of the License at                                   *
 *                                                                            *
 *      http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                            *
 *  Unless required by applicable law or agreed to in writing, software       *
 *  distributed under the License is distributed on an "AS IS" BASIS,         *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  *
 *  See the License for the specific language governing permissions and       *
 *  limitations under the License.                                            *
 *                                                                            *
 *****************************************************************************/
package com.sandpolis.installer.install;

import static com.sandpolis.core.util.ArtifactUtil.ParsedCoordinate.fromCoordinate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.Config;
import com.sandpolis.core.instance.PlatformUtil;
import com.sandpolis.core.soi.Dependency.SO_DependencyMatrix;
import com.sandpolis.core.soi.Dependency.SO_DependencyMatrix.Artifact;
import com.sandpolis.core.soi.SoiUtil;
import com.sandpolis.core.util.ArtifactUtil;

import javafx.concurrent.Task;

/**
 * @author cilki
 * @since 5.0.0
 */
public abstract class AbstractInstaller extends Task<Void> {

	private static final Logger log = LoggerFactory.getLogger(AbstractInstaller.class);

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
	private String version;

	/**
	 * The server username.
	 */
	protected String username;

	/**
	 * The server password.
	 */
	protected String password;

	/**
	 * The client sync key.
	 */
	protected String key;

	/**
	 * Whether the installation completed successfully.
	 */
	private boolean completed;

	/**
	 * The installation progress.
	 */
	private double progress;

	/**
	 * A post-installation hook.
	 */
	private Runnable postHook;

	protected AbstractInstaller(Path destination) {
		this.destination = Objects.requireNonNull(destination);
		updateProgress(0, 1);

		if (Config.has("install.version")) {
			version = Config.get("install.version");
		}
	}

	private static AbstractInstaller newInstaller() {
		switch (PlatformUtil.queryOsType()) {
		case LINUX:
			return new LinuxInstaller();
		case MACOS:
			return new LinuxInstaller();
		case WINDOWS:
			return new WindowsInstaller();
		default:
			throw new RuntimeException("Unsupported platform");
		}
	}

	public static AbstractInstaller newServerInstaller(String username, String password) {
		AbstractInstaller installer = newInstaller();
		installer.coordinate = "com.sandpolis:sandpolis-server:";
		installer.postHook = installer::serverPostInstall;
		installer.username = username;
		installer.password = password;
		return installer;
	}

	public static AbstractInstaller newViewerJfxInstaller() {
		AbstractInstaller installer = newInstaller();
		installer.coordinate = "com.sandpolis:sandpolis-viewer-jfx:";
		installer.postHook = installer::viewerJfxPostInstall;
		return installer;
	}

	public static AbstractInstaller newViewerCliInstaller() {
		AbstractInstaller installer = newInstaller();
		installer.coordinate = "com.sandpolis:sandpolis-viewer-cli:";
		installer.postHook = installer::viewerCliPostInstall;
		return installer;
	}

	public static AbstractInstaller newClientInstaller(String key) {
		AbstractInstaller installer = newInstaller();
		installer.coordinate = "com.sandpolis:sandpolis-client-mega:";
		installer.postHook = installer::clientPostInstall;
		installer.key = key;
		return installer;
	}

	@Override
	protected Void call() throws Exception {

		if (version == null) {
			// Request latest version number
			updateMessage("Downloading metadata");
			version = ArtifactUtil.getLatestVersion(coordinate);
		}
		coordinate += version;

		Files.createDirectories(destination);

		// Download executable
		updateMessage("Downloading " + coordinate);
		Path executable = ArtifactUtil.download(destination, coordinate);

		// Download dependencies
		Path lib = destination.resolve("lib");
		Files.createDirectories(lib);

		SO_DependencyMatrix matrix = SoiUtil.readMatrix(executable);
		download(matrix, lib, coordinate, 1.0);

		postHook.run();
		completed = true;
		return null;
	}

	/**
	 * Download all dependencies of the artifact corresponding to the given
	 * coordinate.
	 * 
	 * @param matrix     The dependency matrix
	 * @param lib        The output directory
	 * @param coordinate The coordinate
	 * @param percent    The percentage that the progress property should increase
	 *                   after the current invocation is complete
	 * @throws IOException
	 */
	private void download(SO_DependencyMatrix matrix, Path lib, String coordinate, double percent) throws IOException {

		List<Artifact> dependencies = matrix.getArtifactList().stream()
				.filter(a -> a.getCoordinates().equals(coordinate)).findFirst().get().getDependencyList().stream()
				.map(matrix.getArtifactList()::get).collect(Collectors.toList());

		for (var artifact : dependencies) {
			Path dependency = lib.resolve(fromCoordinate(artifact.getCoordinates()).filename);
			if (!Files.exists(dependency)) {
				updateMessage("Downloading " + artifact.getCoordinates());
				ArtifactUtil.download(lib, artifact.getCoordinates());
			}

			download(matrix, lib, artifact.getCoordinates(), percent / dependencies.size());
		}

		if (dependencies.size() == 0) {
			progress += percent;
			updateProgress(progress, 1.0);
		}
	}

	public boolean isCompleted() {
		return completed;
	}

	/**
	 * A hook invoked after the server has been successfully installed.
	 */
	protected void serverPostInstall() {
		// no op
	}

	/**
	 * A hook invoked after the viewer has been successfully installed.
	 */
	protected void viewerJfxPostInstall() {
		// no op
	}

	/**
	 * A hook invoked after the viewer cli has been successfully installed.
	 */
	protected void viewerCliPostInstall() {
		// no op
	}

	/**
	 * A hook invoked after the client has been successfully installed.
	 */
	protected void clientPostInstall() {
		// no op
	}

}
