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

import static com.sandpolis.core.instance.store.artifact.ArtifactUtil.ParsedCoordinate.fromCoordinate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.Config;
import com.sandpolis.core.instance.store.artifact.ArtifactUtil;
import com.sandpolis.core.soi.SoiUtil;

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

	public AbstractInstaller(Path destination, Consumer<String> status, Consumer<Double> progress) {
		this.destination = Objects.requireNonNull(destination);
		this.status = Objects.requireNonNull(status);
		this.progress = Objects.requireNonNull(progress);
	}

	public void install(boolean serverInstall, boolean viewerJfxInstall, boolean viewerCliInstall) throws IOException {

		Files.createDirectories(destination);

		if (serverInstall) {
			log.info("Installing server");
			install("com.sandpolis:server:");
			serverPostInstall();
		}
		if (viewerJfxInstall) {
			log.info("Installing viewer GUI");
			install("com.sandpolis:viewer-jfx:");
			viewerJfxPostInstall();
		}
		if (viewerCliInstall) {
			log.info("Installing viewer CLI");
			install("com.sandpolis:viewer-cli:");
			viewerCliPostInstall();
		}
	}

	/**
	 * Installs the given artifact into {@link #destination} and all dependencies
	 * into the lib directory.
	 * 
	 * @param coordinate The artifact to install
	 * @throws IOException
	 */
	private void install(String coordinate) throws IOException {

		if (Config.has("install.version")) {
			coordinate += Config.get("install.version");
		} else {
			// Request latest version number
			status.accept("Downloading metadata");
			coordinate += ArtifactUtil.getLatestVersion(coordinate);
		}

		// Download executable
		ArtifactUtil.download(destination, coordinate);

		// Download dependencies
		Path lib = destination.resolve("lib");
		for (var artifact : SoiUtil.readMatrix(destination.resolve(fromCoordinate(coordinate).filename))
				.getArtifactList()) {
			if (!Files.exists(lib.resolve(fromCoordinate(artifact.getCoordinates()).filename))) {
				status.accept("Downloading " + artifact.getCoordinates());
				progress.accept(0.0);
				ArtifactUtil.download(lib, artifact.getCoordinates());
			}
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

}
