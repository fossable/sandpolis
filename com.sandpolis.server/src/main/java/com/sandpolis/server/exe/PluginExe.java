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

import com.google.protobuf.ByteString;
import com.sandpolis.core.instance.Environment;
import com.sandpolis.core.instance.store.artifact.ArtifactStore;
import com.sandpolis.core.instance.store.plugin.PluginStore;
import com.sandpolis.core.net.Exelet;
import com.sandpolis.core.net.Sock;
import com.sandpolis.core.proto.net.MCPlugin.RS_PluginList;
import com.sandpolis.core.proto.net.MCPlugin.RS_PluginSync;
import com.sandpolis.core.proto.net.MSG.Message;
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
	public void rq_plugin_sync(Message m) {
		var rq = Objects.requireNonNull(m.getRqPluginSync());
		var rs = RS_PluginSync.newBuilder();

		PluginStore.getPlugins().stream()
				// Skip plugins that are up to date
				.filter(plugin -> {
					return rq.getPluginList().stream().filter(descriptor -> {
						return plugin.getId().equals(descriptor.getId())
								&& plugin.getVersion().equals(descriptor.getVersion());
					}).findAny().isEmpty();
				}).map(PluginStore::getArtifact).map(PluginExe::tempRead).filter(Objects::nonNull)
				.map(ByteString::copyFrom).forEach(rs::addPluginBinary);

		reply(m, rs);
	}

	@Auth
	public void rq_plugin_list(Message m) {
		reply(m, RS_PluginList.newBuilder().addAllPlugin(PluginStore.getPluginDescriptors()));
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
			if (!ArtifactStore.download(binary.getParent().toFile(), rq.getPluginCoordinates())) {
				// TODO
				return;
			}
			binary = binary.resolveSibling("TODO");// TODO
			break;
		default:
			// TODO
			return;
		}

		var manifest = JarUtil.getManifest(binary.toFile());

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

	// TODO
	public static byte[] tempRead(Path path) {
		try {
			return Files.readAllBytes(path);
		} catch (IOException e) {
			return null;
		}
	}

}
