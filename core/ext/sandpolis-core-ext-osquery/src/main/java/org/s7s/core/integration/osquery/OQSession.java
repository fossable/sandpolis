//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.integration.osquery;

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

public record OQSession(Path executable, Process process, BufferedWriter stdin, BufferedReader stdout) {

	public static OQSession of() throws IOException {
		var osqueryi = Installer.locate().orElseThrow(FileNotFoundException::new);

		var process = Runtime.getRuntime().exec(osqueryi.toString());
		var stdin = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
		var stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));

		// Drain initial stdout
		if (stdout.ready())
			stdout.skip(Long.MAX_VALUE);

		return new OQSession(osqueryi, process, stdin, stdout);
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
