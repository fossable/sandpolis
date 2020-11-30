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

// Take/restore disk snapshots

bool snapshot_block_read(char *device) {
	int fd = open(device, O_RDONLY);
	if (fd <= 0) {
		std::cout("Failed to open device");
		return false;
	}

	size_t device_size = 0;
	if (device_size <= 0) {
		std::cout("Failed to get device size");
		return false;
	}

	void *blocks = mmap(nullptr, device_size, PROT_READ, MAP_PRIVATE, fd, 0);
	if (blocks == nullptr) {
		std::cout("Failed to map device");
		return false;
	}

	void *block;
	for(unsigned long i = 0; i < device_size; i += SNAPSHOT_BLOCK_SIZE) {
		block = blocks + i;

		// TODO: hash
		// TODO: egress
	}
}

void snapshot_block_write(char *device) {
	int fd = open(device, O_WRONLY);
	if (fd <= 0) {
		std::cout("Failed to open device");
		return false;
	}

	mmap();
}
