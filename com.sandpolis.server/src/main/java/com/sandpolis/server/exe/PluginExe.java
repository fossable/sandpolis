/******************************************************************************
 *                                                                            *
 *                    Copyright 2018 Subterranean Security                    *
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
package com.sandpolis.server.exe;

import static com.sandpolis.core.instance.Environment.EnvPath.JLIB;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteSource;
import com.google.protobuf.ByteString;
import com.sandpolis.core.instance.Environment;
import com.sandpolis.core.instance.store.artifact.ArtifactUtil;
import com.sandpolis.core.instance.store.plugin.PluginStore;
import com.sandpolis.core.net.Exelet;
import com.sandpolis.core.net.Sock;
import com.sandpolis.core.proto.net.MCPlugin.RS_ArtifactDownload;
import com.sandpolis.core.proto.net.MCPlugin.RS_PluginList;
import com.sandpolis.core.proto.net.MSG.Message;
import com.sandpolis.core.proto.util.Platform.InstanceFlavor;
import com.sandpolis.core.proto.util.Result.Outcome;
import com.sandpolis.core.util.CertUtil;
import com.sandpolis.core.util.JarUtil;
import com.sandpolis.core.util.NetUtil;
import com.sandpolis.server.store.trust.TrustStore;

/**
 * Message handlers for plugin requests.
 * 
 * @author cilki
 * @since 5.0.0
 */
public class PluginExe extends Exelet {

	private static final Logger log = LoggerFactory.getLogger(PluginExe.class);

	public PluginExe(Sock connector) {
		super(connector);
	}

	@Auth
	public void rq_artifact_download(Message m) {
		var rq = Objects.requireNonNull(m.getRqArtifactDownload());
		var rs = RS_ArtifactDownload.newBuilder();

		PluginStore.getPlugin(rq.getCoordinates()).ifPresentOrElse(plugin -> {
			if (!PluginStore.findComponentTypes(plugin).contains(InstanceFlavor.MEGA)) // TODO hardcoded subtype
				reply(m, Outcome.newBuilder().setResult(false));// TODO message
			else if (rq.getLocation()) {
				rs.setCoordinates(String.format("com.sandpolis:%s:%s", plugin.getId(), plugin.getVersion()));
			} else {
				// Send binary for correct component
				ByteSource component = PluginStore.getPluginComponent(plugin, connector.getRemoteInstance(),
						// TODO hardcoded subtype
						InstanceFlavor.MEGA);

				try (var in = component.openStream()) {
					rs.setBinary(ByteString.readFrom(in));
				} catch (IOException e) {
					// Failed to read plugin
					reply(m, Outcome.newBuilder().setResult(false));// TODO message
				}
			}

			reply(m, rs);
		}, () -> {
			// Check artifacts
			Path artifact = ArtifactUtil.getArtifactFile(rq.getCoordinates());
			if (rq.getLocation()) {
				// TODO send group:artifact:version
			} else {
				if (Files.exists(artifact)) {
					try (var in = Files.newInputStream(artifact)) {
						rs.setBinary(ByteString.readFrom(in));
					} catch (IOException e) {
						// Failed to read artifact
						reply(m, Outcome.newBuilder().setResult(false));// TODO message
					}
				} else {
					// TODO missing artifact
				}
			}
		});
	}

	@Auth
	public void rq_plugin_list(Message m) {
		reply(m, RS_PluginList.newBuilder().addAllPlugin(() -> PluginStore.getPluginDescriptors().iterator()));
	}

	@Auth
	public void rq_plugin_install(Message m) throws Exception {
		var rq = Objects.requireNonNull(m.getRqPluginInstall());

		Path binary = Files.createTempFile("", ".jar");
		switch (rq.getSourceCase()) {
		case PLUGIN_BINARY:
			Files.write(binary, rq.getPluginBinary().toByteArray());
			break;
		case PLUGIN_URL:
			Files.write(binary, NetUtil.download(rq.getPluginUrl()));
			break;
		case PLUGIN_COORDINATES:
			if (!ArtifactUtil.download(binary.getParent(), rq.getPluginCoordinates())) {
				// TODO
				return;
			}
			binary = binary.resolveSibling("TODO");// TODO
			break;
		default:
			// TODO
			return;
		}

		var manifest = JarUtil.getManifest(binary);

		// Read plugin name
		String id = manifest.getValue("Plugin-Id");

		// Read certificate
		var cert = CertUtil.parse(manifest.getValue("Plugin-Cert"));

		// Verify certificate
		if (!TrustStore.verifyPluginCertificate(cert))
			// TODO reply
			return;

		// Move into library directory
		Files.move(binary, Environment.get(JLIB).resolve(id + ".jar"));
	}

}
