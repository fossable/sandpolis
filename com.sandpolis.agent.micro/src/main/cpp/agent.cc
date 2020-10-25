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

#include "sock.hh"
#include "util/net.hh"
#include "util/resources.hh"
#include "util/uuid.hh"

#include "com/sandpolis/core/instance/generator.pb.h"
#include "com/sandpolis/core/foundation/soi/build.pb.h"

int main(int argc, char **argv) {
	core::foundation::soi::SO_Build so_build;
	core::instance::MicroConfig config;

	// Load build metadata from resource file
	if (!so_build.ParseFromArray(resource_body(soi_build),
			resource_length(soi_build))) {
		std::cout << "Failed to read embedded metadata!" << std::endl;
		return 1;
	}

	// Load configuration from resource file
	if (!config.ParseFromArray(resource_body(soi_agent),
			resource_length(soi_agent))) {
		std::cout << "Failed to read embedded configuration!" << std::endl;
		return 1;
	}

	std::cout << "Launching Sandpolis Micro Agent (" << so_build.version()
			<< ")" << std::endl;
	std::cout << "Build Environment:" << std::endl;
	std::cout << "   Platform: " << so_build.platform() << std::endl;
	std::cout << "     Gradle: " << so_build.gradle_version() << std::endl;
	std::cout << "       Java: " << so_build.java_version() << std::endl;

	// Load UUID
	std::string uuid = generate_uuid();

	// Begin connection routine
	long iteration = 0;
	const core::instance::LoopConfig &loop_config = config.network().loop_config();
	while (iteration < loop_config.iteration_limit()
			|| loop_config.iteration_limit() == 0) {

		for (int i = 0; i < loop_config.target_size(); ++i) {
			std::cout << "Attempting connection: "
					<< loop_config.target(i).address() << std::endl;
			int sfd = OpenConnection(loop_config.target(i).address(),
					loop_config.target(i).port());
			if (sfd > 0) {
				Sock sock(uuid, sfd);

				if (sock.CvidHandshake()) {
					// TODO enter sock event loop
					return 0;
				}

				iteration = 0;
				break;
			}

			iteration++;
			std::this_thread::sleep_for(
					std::chrono::milliseconds(loop_config.cooldown()));
		}
	}
}
