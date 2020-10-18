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

#include "com/sandpolis/core/net/message.pb.h"
#include "com/sandpolis/core/net/msg/msg_cvid.pb.h"
#include "com/sandpolis/core/instance/metatypes.pb.h"

// The maximum number of bytes that a single protobuf varint can occupy
#define MAX_VARINT_WIDTH 5

class Sock {

	// The socket file descriptor
	const int sockfd;

	// This endpoint's UUID
	const std::string uuid;

	// This endpoint's CVID
	int cvid;

	// The remote endpoint's CVID
	int remote_cvid;

	// The remote endpoint's UUID
	std::string remote_uuid;

	// Write a protobuf varint32 to the given buffer.
	void WriteVarint32(char *buffer, int value);

	// Read a protobuf varint32 from the given buffer.
	int ReadVarint32(char *buffer);

	// Determine the number of bytes that the value would occupy when encoded as a
	// protobuf varint32.
	int ComputeVarint32Width(int value);

public:
	Sock(std::string u, int sfd) : uuid(u), sockfd(sfd) {
	}

	// Perform a CVID handshake with the remote host.
	bool CvidHandshake();

	// Serialize and send the given message to the remote host.
	bool Send(const core::net::MSG &msg);

	// Receive and parse a message from the remote host.
	bool Recv(core::net::MSG &msg);
};

#endif
