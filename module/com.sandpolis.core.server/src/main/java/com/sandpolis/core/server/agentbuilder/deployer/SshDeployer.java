//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
package com.sandpolis.core.server.agentbuilder.deployer;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Optional;

import com.sandpolis.core.clientserver.msg.MsgAgentbuilder.DeploymentOptions;
import com.sandpolis.core.foundation.Platform.OsType;
import com.sandpolis.core.foundation.util.TextUtil;
import com.sandpolis.core.server.agentbuilder.packager.PackagedAgent;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import net.schmizz.sshj.xfer.InMemorySourceFile;

public class SshDeployer implements AgentDeployer {

	private DeploymentOptions options;

	public SshDeployer(DeploymentOptions options) {
		this.options = options;
	}

	private SSHClient connectSsh() throws IOException {
		var ssh = new SSHClient();

		if (options.getSshPrivateKey().isEmpty()) {
			ssh.authPassword(options.getSshUsername(), options.getSshPassword());
		} else {
			// TODO
		}

		ssh.connect(options.getSshHost());
		return ssh;
	}

	@Override
	public void run(PackagedAgent agent) throws Exception {
		this.options = options;

		try (var ssh = connectSsh()) {
			var os = probeOs(ssh);
			var javaVersion = probeJava(ssh);

			if (javaVersion.isEmpty() || TextUtil.compareVersion(javaVersion.get(), "") < 0) {
				// We need a runtime
				// TODO
			}

			upload(os, ssh, null, null);
		} catch (IOException e) {

		}
	}

	/**
	 * Determine the remote host's Java version if available.
	 *
	 * @param ssh The SSH client
	 * @return The ascertained Java version
	 * @throws IOException
	 */
	private Optional<String> probeJava(SSHClient ssh) throws IOException {

		try (var session = ssh.startSession()) {
			Command cmd;
			String version;

			// 1: Try "java --version"
			cmd = session.exec("java --version");
			cmd.join(5, SECONDS);

			version = TextUtil.parseJavaVersion(IOUtils.readFully(cmd.getInputStream()).toString());
			if (version != null) {
				return Optional.of(version);
			}

			// 2: Try "${JAVA_HOME}/bin/java --version"
			// TODO
		}

		return Optional.empty();
	}

	/**
	 * Determine the remote host's OS by observing the results of a series of test
	 * commands.
	 *
	 * @param ssh The SSH client
	 * @return The ascertained OS
	 * @throws IOException
	 */
	private OsType probeOs(SSHClient ssh) throws IOException {

		try (var session = ssh.startSession()) {
			Command cmd;

			// 1: Try "uname"
			cmd = session.exec("/usr/bin/uname");
			cmd.join(5, SECONDS);

			if (IOUtils.readFully(cmd.getInputStream()).toString().contains("Linux")) {
				return OsType.LINUX;
			}
		}

		return OsType.UNKNOWN;
	}

	private void upload(OsType os, SSHClient ssh, byte[] content, String path) throws IOException {
		try {
			ssh.newSCPFileTransfer().upload(new InMemorySourceFile() {

				@Override
				public InputStream getInputStream() throws IOException {
					return new ByteArrayInputStream(content);
				}

				@Override
				public long getLength() {
					return content.length;
				}

				@Override
				public String getName() {
					return Paths.get(path).getFileName().toString();
				}
			}, Paths.get(path).getParent().toString());
			return;
		} catch (IOException e) {
			// Try fallback method next
		}

		// SCP upload failed, so try some workarounds according to OS
		try (var session = ssh.startSession()) {
			switch (os) {
			case LINUX:
				try (var cmd = session.exec("base64 -d >" + path)) {
					try (var out = cmd.getOutputStream()) {
						out.write(Base64.getEncoder().encode(content));
						out.flush();
					}
				}
				break;
			default:
				break;
			}
		}
	}

	private void configureSystemd() {

	}

}
