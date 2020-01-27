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
#include <fstream>
#include <iostream>
#include <istream>
#include <string>

#include "util/resource.h"
#include "util/uuid.h"

#include "generator.pb.h"

int main(int argc, char **argv) {
	std::cout << "Launching Sandpolis Micro Client" << std::endl;

	util::MicroConfig config;

	// Load build metadata from resource file
	// TODO

	// Load client configuration from resource file
	ResourceBuffer config_buffer((char*) soi_client_bin, soi_client_bin_len);
	if (!config.ParseFromIstream(&config_buffer)) {
		return 1;
	}

	// Begin connection routine
	//int sock = OpenConnection("127.0.0.1", "8768");
	// TODO: Write RQ_Cvid with instance, flavor, and uuid
	// TODO: Read RS_Cvid with cvid, server cvid, and server uuid
}
