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
#include "sock.h"

bool Sock::Send(net::MSG &msg) {

	int payload_size = msg.ByteSize();
	int header_size = computeVarint32Width(payload_size);

	// Reserve enough space for the payload and the length header
	char buffer[payload_size + header_size];

	// Write header
	writeVarint32(buffer, payload_size);

	// Write payload
	msg.SerializeToArray(buffer + header_size, payload_size);

	// Send payload
	for (int sent = 0; sent < (payload_size + header_size);) {
		int s = send(sock, buffer + sent, payload_size - sent, 0);
		if (s <= 0) {
			return false;
		}

		sent += s;
	}

	return true;
}
