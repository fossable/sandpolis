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
#ifndef UUID_H
#define UUID_H

#include <random>

// Generate a new random UUID.
std::string generate_uuid() {

	// Setup new random number generator on every call to save memory
	std::random_device device;
	std::mt19937 rng(device());
	std::uniform_int_distribution<int> dist(0, 15);

	const char *domain = "0123456789abcdef";

	std::string uuid;
	for (int i = 0; i < 8; ++i)
		uuid += domain[dist(rng)];
	uuid += "-";

	for (int i = 0; i < 4; ++i)
		uuid += domain[dist(rng)];
	uuid += "-";

	for (int i = 0; i < 4; ++i)
		uuid += domain[dist(rng)];
	uuid += "-";

	for (int i = 0; i < 4; ++i)
		uuid += domain[dist(rng)];
	uuid += "-";

	for (int i = 0; i < 12; ++i)
		uuid += domain[dist(rng)];

	return uuid;
}

#endif
