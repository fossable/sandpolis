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
#include <chrono>
#include <fstream>
#include <iostream>
#include <istream>
#include <string>
#include <thread>

#include "sock.h"
#include "util/net.h"
#include "util/resources.h"
#include "util/uuid.h"

#include "com/sandpolis/core/proto/util/generator.pb.h"
#include "com/sandpolis/core/soi/build.pb.h"

int main(int argc, char **argv) {
	soi::SO_Build so_build;
	util::MicroConfig config;

	// Load build metadata from resource file
	if (!so_build.ParseFromArray(soi_build, soi_build_len)) {
		std::cout << "Failed to read embedded metadata!" << std::endl;
		return 1;
	}

	// Load client configuration from resource file
	if (!config.ParseFromArray(soi_client, soi_client_len)) {
		std::cout << "Failed to read embedded configuration!" << std::endl;
		return 1;
	}

	printf("Launching Sandpolis Micro Client (%s)\n", so_build.version());
	printf("Build Environment:");
	printf("   Platform: %s\n", so_build.platform());
	printf("     Gradle: %s\n", so_build.gradle_version());
	printf("       Java: %s\n", so_build.java_version());

	// Begin connection routine
	long iteration = 0;
	const util::LoopConfig &loop_config = config.network().loop_config();
	while (iteration < loop_config.iteration_limit()
			|| loop_config.iteration_limit() == 0) {

		for (int i = 0; i < loop_config.target_size(); ++i) {
			int sfd = OpenConnection(loop_config.target(i).address(),
					loop_config.target(i).port());
			if (sfd > 0) {
				Sock sock(sfd);
				// TODO enter sock event loop

				iteration = 0;
				break;
			}

			iteration++;
			std::this_thread::sleep_for(
					std::chrono::milliseconds(loop_config.cooldown()));
		}
	}
}
