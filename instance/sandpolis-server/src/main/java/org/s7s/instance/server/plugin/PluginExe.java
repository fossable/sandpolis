//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.server.plugin;

import static org.s7s.core.instance.plugin.PluginStore.PluginStore;
import static org.s7s.core.server.trust.TrustStore.TrustStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteSource;
import com.google.protobuf.ByteString;
import org.s7s.core.foundation.S7SCertificate;
import org.s7s.core.foundation.S7SJarFile;
import org.s7s.core.foundation.S7SMavenArtifact;
import org.s7s.core.instance.InstanceContext;
import org.s7s.core.protocol.Plugin.RQ_DownloadArtifact;
import org.s7s.core.protocol.Plugin.RQ_InstallPlugin;
import org.s7s.core.protocol.Plugin.RS_DownloadArtifact;
import org.s7s.core.protocol.Plugin.RS_InstallPlugin;
import org.s7s.core.foundation.Instance.InstanceFlavor;
import org.s7s.core.instance.state.InstanceOids.ProfileOid.ConnectionOid;
import org.s7s.core.instance.state.InstanceOids.ProfileOid.PluginOid;
import org.s7s.core.instance.exelet.Exelet;
import org.s7s.core.instance.exelet.ExeletContext;

/**
 * Message handlers for plugin requests.
 *
 * @author cilki
 * @since 5.0.0
 */
public final class PluginExe extends Exelet {

	private static final Logger log = LoggerFactory.getLogger(PluginExe.class);

	@Handler(auth = true)
	public static void rq_download_artifact(ExeletContext context, RQ_DownloadArtifact rq) {
		var rs = RS_DownloadArtifact.newBuilder();

		var _artifact = S7SMavenArtifact.of(rq.getCoordinates());
		log.debug("Received artifact request: " + _artifact.filename());

		PluginStore.getByPackageId(_artifact.artifactId()).ifPresentOrElse(plugin -> {
			if (!PluginStore.findComponentTypes(plugin)
					.contains(context.connector.get(ConnectionOid.REMOTE_INSTANCE_FLAVOR).asInstanceFlavor()))
				context.reply(null);// TODO message
			else if (rq.getLocation()) {
				context.reply(rs.setCoordinates(
						String.format(":%s:%s", plugin.get(PluginOid.PACKAGE_ID).asString(), plugin.getVersion())));
			} else {
				// Send binary for correct component
				ByteSource component = PluginStore.getPluginComponent(plugin,
						context.connector.get(ConnectionOid.REMOTE_INSTANCE).asInstanceType(),
						// TODO hardcoded subtype
						InstanceFlavor.AGENT_JAVA);

				try (var in = component.openStream()) {
					context.reply(rs.setBinary(ByteString.readFrom(in)));
				} catch (IOException e) {
					// Failed to read plugin
					context.reply(null);// TODO message
				}
			}
		}, () -> {
			// Check regular artifacts
			Path artifact = _artifact.getArtifactFile(InstanceContext.PATH_LIB.get());

			if (!Files.exists(artifact)) {
				// Try to find a suitable artifact
//				try (Stream<Path> artifacts = ArtifactUtil.findArtifactFile(Environment.LIB.path(),
//						coordinate.artifactId)) {
//					artifact = artifacts.findAny().orElse(null);
//				} catch (IOException e1) {
//					// TODO Auto-generated catch block
//					e1.printStackTrace();
//				}
			}

			if (artifact != null) {
				if (rq.getLocation()) {
					context.reply(rs.setCoordinates("TODO"));
				} else {
					try (var in = Files.newInputStream(artifact)) {
						context.reply(rs.setBinary(ByteString.readFrom(in)));
					} catch (IOException e) {
						// Failed to read artifact
						context.reply(null);// TODO message
					}
				}
			} else if (rq.getLocation()) {
				context.reply(rs.setCoordinates("TODO"));
			} else {
				// No artifact could be found or located
				context.reply(null);// TODO message
			}
		});
	}

	@Handler(auth = true)
	public static RS_InstallPlugin rq_install_plugin(RQ_InstallPlugin rq) throws Exception {

		if (!InstanceContext.PLUGIN_ENABLED.get())
			return RS_InstallPlugin.PLUGIN_INSTALL_FAILED_DISABLED;

		Path binary = Files.createTempFile("", ".jar");
		switch (rq.getSourceCase()) {
		case PLUGIN_BINARY:
			Files.write(binary, rq.getPluginBinary().toByteArray());
			break;
		case PLUGIN_URL:
			break;
		case PLUGIN_COORDINATES:
			try (var out = Files.newOutputStream(binary)) {
				S7SMavenArtifact.of(rq.getPluginCoordinates()).download().transferTo(out);
			}

			binary = binary.resolveSibling("TODO");// TODO
			break;
		default:
			return RS_InstallPlugin.PLUGIN_INSTALL_INVALID;
		}

		// Read plugin name
		String id = S7SJarFile.of(binary).getManifestValue("Plugin-Id").get();

		// Read certificate
		var cert = S7SCertificate.of(S7SJarFile.of(binary).getManifestValue("Plugin-Cert").get());

		// Verify certificate
		if (!TrustStore.verifyPluginCertificate(cert.certificate()))
			return RS_InstallPlugin.PLUGIN_INSTALL_FAILED_CERTIFICATE;

		// Move into library directory
		Files.move(binary, InstanceContext.PATH_LIB.get().resolve(id + ".jar"));
		return RS_InstallPlugin.PLUGIN_INSTALL_OK;
	}

	private PluginExe() {
	}
}
