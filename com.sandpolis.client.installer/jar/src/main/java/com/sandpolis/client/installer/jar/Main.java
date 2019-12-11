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
package com.sandpolis.client.installer.jar;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class Main {

	public static void main(String[] args) throws IOException {
		Properties config = new Properties();
		config.load(Main.class.getResourceAsStream("/config.properties"));

		Path base = Paths.get(config.getProperty("path." + System.getProperty("os.name"),
				System.getProperty("user.home") + "/.sandpolis"));
		Path lib = base.resolve("lib");

		try {
			Files.createDirectories(lib);
		} catch (IOException e) {
			// Force install if enabled
			// TODO
			e.printStackTrace();
		}

		// Install dependencies
		for (var gav : config.getProperty("modules").split(" ")) {
			String filename = String.format("%s-%s.jar", gav.split(":")[1], gav.split(":")[2]);

			var in = Main.class.getResourceAsStream("/lib/" + filename);
			if (in != null) {
				Files.copy(in, lib.resolve(filename), REPLACE_EXISTING);
			} else {
				// Download from Maven Central
				URL url = new URL(String.format("https://repo1.maven.org/maven2/%s/%s/%s/%s",
						gav.split(":")[0].replaceAll("\\.", "/"), gav.split(":")[1], gav.split(":")[2], filename));

				try (var in2 = url.openConnection().getInputStream();
						var out = Files.newOutputStream(lib.resolve(filename))) {
					in2.transferTo(out);
				}
			}
		}

		String session = config.getProperty("screen.session");
		if (session != null) {
			Runtime.getRuntime().exec(
					new String[] { "screen", "-S", session, "-X", "stuff",
							"clear && java --module-path " + lib.toString()
									+ " -m com.sandpolis.client.mega/com.sandpolis.client.mega.Main\n" },
					null, lib.toFile());
		} else {
			Runtime.getRuntime().exec(new String[] { "java", "--module-path", lib.toString(), "-m",
					"com.sandpolis.client.mega/com.sandpolis.client.mega.Main" }, null, lib.toFile());
		}

		System.exit(0);
	}
}
