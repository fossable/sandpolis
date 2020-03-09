//============================================================================//
//                                                                            //
//                Copyright © 2015 - 2020 Subterranean Security               //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation at:                                //
//                                                                            //
//    https://mozilla.org/MPL/2.0                                             //
//                                                                            //
//=========================================================S A N D P O L I S==//
package com.sandpolis.server.vanilla.exe;

import static com.sandpolis.core.instance.store.plugin.PluginStore.PluginStore;
import static com.sandpolis.core.instance.util.ProtoUtil.begin;
import static com.sandpolis.core.instance.util.ProtoUtil.failure;
import static com.sandpolis.core.instance.util.ProtoUtil.success;
import static com.sandpolis.core.util.ArtifactUtil.ParsedCoordinate.fromCoordinate;
import static com.sandpolis.server.vanilla.store.trust.TrustStore.TrustStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteSource;
import com.google.protobuf.ByteString;
import com.google.protobuf.MessageOrBuilder;
import com.sandpolis.core.instance.Config;
import com.sandpolis.core.instance.Environment;
import com.sandpolis.core.net.command.Exelet;
import com.sandpolis.core.net.handler.exelet.ExeletContext;
import com.sandpolis.core.proto.net.Message.MSG;
import com.sandpolis.core.proto.net.MsgPlugin.RQ_ArtifactDownload;
import com.sandpolis.core.proto.net.MsgPlugin.RQ_PluginInstall;
import com.sandpolis.core.proto.net.MsgPlugin.RQ_PluginList;
import com.sandpolis.core.proto.net.MsgPlugin.RS_ArtifactDownload;
import com.sandpolis.core.proto.net.MsgPlugin.RS_PluginList;
import com.sandpolis.core.proto.util.Platform.InstanceFlavor;
import com.sandpolis.core.proto.util.Result.Outcome;
import com.sandpolis.core.util.ArtifactUtil;
import com.sandpolis.core.util.ArtifactUtil.ParsedCoordinate;
import com.sandpolis.core.util.CertUtil;
import com.sandpolis.core.util.JarUtil;
import com.sandpolis.core.util.NetUtil;

/**
 * Message handlers for plugin requests.
 *
 * @author cilki
 * @since 5.0.0
 */
public final class PluginExe extends Exelet {

	private static final Logger log = LoggerFactory.getLogger(PluginExe.class);

	@Auth
	@Handler(tag = MSG.RQ_ARTIFACT_DOWNLOAD_FIELD_NUMBER)
	public static void rq_artifact_download(ExeletContext context, RQ_ArtifactDownload rq) {
		var rs = RS_ArtifactDownload.newBuilder();

		ParsedCoordinate coordinate = fromCoordinate(rq.getCoordinates());
		log.debug("Received artifact request: " + coordinate.coordinate);

		PluginStore.get(coordinate.artifactId).ifPresentOrElse(plugin -> {
			if (!PluginStore.findComponentTypes(plugin).contains(context.connector.getRemoteInstanceFlavor()))
				context.reply(Outcome.newBuilder().setResult(false));// TODO message
			else if (rq.getLocation()) {
				context.reply(rs.setCoordinates(String.format(":%s:%s", plugin.getId(), plugin.getVersion())));
			} else {
				// Send binary for correct component
				ByteSource component = PluginStore.getPluginComponent(plugin, context.connector.getRemoteInstance(),
						// TODO hardcoded subtype
						InstanceFlavor.MEGA);

				try (var in = component.openStream()) {
					context.reply(rs.setBinary(ByteString.readFrom(in)));
				} catch (IOException e) {
					// Failed to read plugin
					context.reply(Outcome.newBuilder().setResult(false));// TODO message
				}
			}
		}, () -> {
			// Check regular artifacts
			Path artifact = ArtifactUtil.getArtifactFile(Environment.LIB.path(), coordinate.coordinate);

			if (!Files.exists(artifact)) {
				// Try to find a suitable artifact
				try (Stream<Path> artifacts = ArtifactUtil.findArtifactFile(Environment.LIB.path(),
						coordinate.artifactId)) {
					artifact = artifacts.findAny().orElse(null);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}

			if (artifact != null) {
				if (rq.getLocation()) {
					context.reply(rs.setCoordinates("TODO"));
				} else {
					try (var in = Files.newInputStream(artifact)) {
						context.reply(rs.setBinary(ByteString.readFrom(in)));
					} catch (IOException e) {
						// Failed to read artifact
						context.reply(Outcome.newBuilder().setResult(false));// TODO message
					}
				}
			} else if (rq.getLocation()) {
				context.reply(rs.setCoordinates("TODO"));
			} else {
				// No artifact could be found or located
				context.reply(Outcome.newBuilder().setResult(false));// TODO message
			}
		});
	}

	@Auth
	@Handler(tag = MSG.RQ_PLUGIN_LIST_FIELD_NUMBER)
	public static MessageOrBuilder rq_plugin_list(RQ_PluginList rq) {
		if (!Config.PLUGIN_ENABLED.value().orElse(true))
			return failure(begin());

		return RS_PluginList.newBuilder().addAllPlugin(() -> PluginStore.getPluginDescriptors().iterator());
	}

	@Auth
	@Handler(tag = MSG.RQ_PLUGIN_INSTALL_FIELD_NUMBER)
	public static MessageOrBuilder rq_plugin_install(RQ_PluginInstall rq) throws Exception {
		var outcome = begin();
		if (!Config.PLUGIN_ENABLED.value().orElse(true))
			return failure(outcome);

		Path binary = Files.createTempFile("", ".jar");
		switch (rq.getSourceCase()) {
		case PLUGIN_BINARY:
			Files.write(binary, rq.getPluginBinary().toByteArray());
			break;
		case PLUGIN_URL:
			Files.write(binary, NetUtil.download(rq.getPluginUrl()));
			break;
		case PLUGIN_COORDINATES:
			ArtifactUtil.download(binary.getParent(), rq.getPluginCoordinates());

			binary = binary.resolveSibling("TODO");// TODO
			break;
		default:
			return failure(outcome);
		}

		var manifest = JarUtil.getManifest(binary);

		// Read plugin name
		String id = manifest.getValue("Plugin-Id");

		// Read certificate
		var cert = CertUtil.parseCert(manifest.getValue("Plugin-Cert"));

		// Verify certificate
		if (!TrustStore.verifyPluginCertificate(cert))
			return failure(outcome);

		// Move into library directory
		Files.move(binary, Environment.LIB.path().resolve(id + ".jar"));
		return success(outcome);
	}

	private PluginExe() {
	}
}
