//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.plugin.shell.agent.java;

import static com.google.common.base.Preconditions.checkArgument;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.s7s.core.foundation.S7SProcess;
import org.s7s.plugin.shell.Messages.ShellCapability;

public record Shell(Path executable, Set<ShellCapability> capabilities, String version) {

	private static final Map<String, Set<ShellCapability>> knownPaths = Map.of(
			// Bash
			"/bin/bash", Set.of(ShellCapability.BASH),
			// Windows CMD
			"C:/Windows/System32/cmd.exe", Set.of(ShellCapability.CMD),
			// Powershell
			"/usr/bin/pwsh", Set.of(ShellCapability.PWSH),
			// Powershell
			"C:/Windows/System32/WindowsPowerShell/v1.0/powershell.exe", Set.of(ShellCapability.PWSH),
			// Powershell
			"C:/Windows/SysWOW64/WindowsPowerShell/v1.0/powershell.exe", Set.of(ShellCapability.PWSH),
			// ZSH
			"/usr/bin/zsh", Set.of(ShellCapability.ZSH));

	public static List<Shell> discoverShells() {
		return knownPaths.keySet().stream().map(Shell::of).collect(Collectors.toList());
	}

	public static Shell of(String executable) {
		return of(Paths.get(executable));
	}

	public static Shell of(Path executable) {
		checkArgument(Files.isExecutable(executable));

		var capabilities = new HashSet<ShellCapability>();
		String version = null;

		// Check version output
		S7SProcess.exec(executable, "--version").complete((exit, stdout, stderr) -> {
			if (exit == 0) {
				if (stdout.startsWith("zsh")) {
					capabilities.add(ShellCapability.ZSH);
					capabilities.add(ShellCapability.BASH);
					capabilities.add(ShellCapability.SH);
				}

				if (stdout.startsWith("GNU bash,")) {
					capabilities.add(ShellCapability.BASH);
					capabilities.add(ShellCapability.SH);
				}
			}
		});

		// Check help output if we need to
		S7SProcess.exec(executable, "--help").complete((exit, stdout, stderr) -> {
			if (exit == 0) {
				if (stdout.startsWith("Usage: zsh")) {
					capabilities.add(ShellCapability.ZSH);
					capabilities.add(ShellCapability.BASH);
					capabilities.add(ShellCapability.SH);
				}

				if (stdout.startsWith("GNU bash,")) {
					capabilities.add(ShellCapability.BASH);
					capabilities.add(ShellCapability.SH);
				}
			}
		});

		return new Shell(executable, Collections.unmodifiableSet(capabilities), version);
	}

	public ProcessBuilder execute(String command) {
		if (capabilities.contains(ShellCapability.SH)) {
			return new ProcessBuilder(executable.toString(), "-c",
					"echo " + Base64.getEncoder().encodeToString(command.getBytes()) + " | base64 --decode | "
							+ executable.toString());
		}

		if (capabilities.contains(ShellCapability.CMD)) {
			return new ProcessBuilder(executable.toString(), "/C", command);
		}

		if (capabilities.contains(ShellCapability.PWSH)) {
			return new ProcessBuilder(executable.toString(), "-encodedCommand",
					Base64.getEncoder().encodeToString(command.getBytes(StandardCharsets.UTF_16LE)));
		}

		throw new UnsupportedOperationException();
	}

	public ProcessBuilder newSession() {
		if (capabilities.contains(ShellCapability.SH)) {
			return new ProcessBuilder(executable.toString(), "-i");
		}

		return new ProcessBuilder(executable.toString());
	}
}
