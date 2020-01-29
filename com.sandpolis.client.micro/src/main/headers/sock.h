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
#ifndef SOCK_H
#define SOCK_H

#include <sys/socket.h>
#include <sys/types.h>

#include "com/sandpolis/core/proto/net/message.pb.h"

// The maximum number of bytes that a single protobuf varint can occupy
#define MAX_VARINT_WIDTH 5

class Sock {

	// The socket file descriptor
	const int sock;

	// The remote endpoint's CVID
	int remote_cvid;

	// The remote endpoint's UUID
	std::string remote_uuid;

	// Write a protobuf varint to the given buffer.
	void writeVarint32(char *buffer, int value) {
		for (int i = 0; i < MAX_VARINT_WIDTH; ++i) {
			if ((value & ~0x7f) == 0) {
				buffer[i] = value;
				return;
			} else {
				buffer[i] = ((value & 0x7f) | 0x80) & 0xff;
				value = ((unsigned int) value) >> 7;
			}
		}
	}

	// Determines the number of bytes that the value would occupy when encoded as a
	// protobuf varint32.
	int computeVarint32Width(int value) {
		if ((value & (0xffffffff << 7)) == 0)
			return 1;
		if ((value & (0xffffffff << 14)) == 0)
			return 2;
		if ((value & (0xffffffff << 21)) == 0)
			return 3;
		if ((value & (0xffffffff << 28)) == 0)
			return 4;

		return MAX_VARINT_WIDTH;
	}

	// Perform a CVID handshake with the remote host.
	int cvid_handshake() {

		// Send a cvid request containing instance information
		net::RQ_Cvid rq;
		rq.set_instance(util::Instance::CLIENT);
		rq.set_instance_flavor(util::InstanceFlavor::MICRO);
		//rq.set_uuid(uuid);

		//send(sock, &rq);
		net::RS_Cvid rs;
		remote_cvid = rs.server_cvid();
		remote_uuid = rs.server_uuid();
	}

public:
	Sock(int sfd) : sock(sfd) {
		cvid_handshake();
	}

	bool Send(net::MSG &msg);
};

#endif
