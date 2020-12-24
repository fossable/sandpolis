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
package com.sandpolis.plugin.sysinfo.agent.vanilla.osquery;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class OQSession {

	private final Path binary;
	private final Process process;
	private final BufferedWriter stdin;
	private final BufferedReader stdout;

	public OQSession() throws IOException {
		binary = Installer.getBinary().orElseThrow(FileNotFoundException::new);

		process = Runtime.getRuntime().exec(binary.toString());
		stdin = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
		stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));

		// Drain initial stdout
		if (stdout.ready())
			stdout.skip(Long.MAX_VALUE);
	}

	public synchronized String[][] query(String[] columns, String table) throws IOException {

		try {
			// Add select operation
			stdin.write("SELECT ");

			// Add select columns
			stdin.write(Arrays.stream(columns).collect(Collectors.joining(",")));

			// Add table
			stdin.write(" FROM ");
			stdin.write(table);

			// Execute query
			stdin.write(";\n");
			stdin.flush();

			// Read header
			String line = stdout.readLine();
			if (line == null || !line.startsWith("+")) {
				throw new IOException("Unexpected header line: " + line);
			}
			line = stdout.readLine();
			if (line == null || !line.startsWith("|")) {
				throw new IOException("Unexpected header line: " + line);
			}
			line = stdout.readLine();
			if (line == null || !line.startsWith("+")) {
				throw new IOException("Unexpected header line: " + line);
			}

			// Read rows
			var rows = new ArrayList<String[]>();
			while ((line = stdout.readLine()) != null) {
				if (line.startsWith("|")) {
					rows.add(Arrays.stream(line.split("\\|")).map(String::trim).filter(Predicate.not(String::isEmpty))
							.toArray(String[]::new));
				} else {
					break;
				}
			}

			return rows.toArray(String[][]::new);
		} finally {
			// Drain stdout
			if (stdout.ready())
				stdout.skip(Long.MAX_VALUE);
		}
	}
}
