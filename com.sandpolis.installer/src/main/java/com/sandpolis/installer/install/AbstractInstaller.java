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
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.Config;
import com.sandpolis.core.soi.Dependency.SO_DependencyMatrix.Artifact;
import com.sandpolis.core.soi.SoiUtil;
import com.sandpolis.core.util.ArtifactUtil;

/**
 * @author cilki
 * @since 5.0.0
 */
public abstract class AbstractInstaller {

	private static final Logger log = LoggerFactory.getLogger(AbstractInstaller.class);

	/**
	 * The installation directory.
	 */
	private Path destination;

	/**
	 * A callback for the current installation status.
	 */
	private Consumer<String> status;

	/**
	 * A callback for the current installation progress (from 0 to 100).
	 */
	private Consumer<Double> progress;

	/**
	 * The current progress.
	 */
	private double progressValue;

	/**
	 * The Sandpolis version that will be installed.
	 */
	private String version;

	public AbstractInstaller(Path destination, Consumer<String> status, Consumer<Double> progress) {
		this.destination = Objects.requireNonNull(destination);
		this.status = Objects.requireNonNull(status);
		this.progress = Objects.requireNonNull(progress);

		if (Config.has("install.version")) {
			version = Config.get("install.version");
		}
	}

	public void installServer(double progressIncrement) throws IOException {

		Files.createDirectories(destination);

		log.info("Installing server");
		install(progressIncrement, "com.sandpolis:sandpolis-server:");
		serverPostInstall();
	}

	public void installViewerJfx(double progressIncrement) throws IOException {

		Files.createDirectories(destination);

		log.info("Installing viewer GUI");
		install(progressIncrement, "com.sandpolis:sandpolis-viewer-jfx:");
		viewerJfxPostInstall();
	}

	public void installViewerCli(double progressIncrement) throws IOException {

		Files.createDirectories(destination);

		log.info("Installing viewer CLI");
		install(progressIncrement, "com.sandpolis:sandpolis-viewer-cli:");
		viewerCliPostInstall();
	}

	public void installClient(double progressIncrement, String key) throws IOException {

		Files.createDirectories(destination);

		log.info("Installing client");
		install(progressIncrement, "com.sandpolis:sandpolis-client-mega:");
		clientPostInstall();
	}

	/**
	 * Installs the given artifact into {@link #destination} and all dependencies
	 * into the lib directory.
	 * 
	 * @param progressIncrement The amount to increase progress if successful
	 * @param coordinate        The artifact to install
	 * @throws IOException
	 */
	private void install(double progressIncrement, String coordinate) throws IOException {

		if (version == null) {
			// Request latest version number
			status.accept("Downloading metadata");
			version = ArtifactUtil.getLatestVersion(coordinate);
		}
		coordinate += version;

		// Download executable
		status.accept("Downloading executable");
		Path executable = ArtifactUtil.download(destination, coordinate);

		// Download dependencies
		Path lib = destination.resolve("lib");
		List<Artifact> dependencies = SoiUtil.readMatrix(executable).getArtifactList();

		for (var artifact : dependencies) {
			if (!Files.exists(lib.resolve(fromCoordinate(artifact.getCoordinates()).filename))) {
				status.accept("Downloading " + artifact.getCoordinates());
				ArtifactUtil.download(lib, artifact.getCoordinates());
			}

			progressValue += progressIncrement / dependencies.size();
			progress.accept(progressValue);
		}
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
