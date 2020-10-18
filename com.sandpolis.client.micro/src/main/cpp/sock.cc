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
#include "sock.hh"

void Sock::WriteVarint32(char *buffer, int value) {
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

int Sock::ReadVarint32(char *buffer) {

	char tmp = buffer[0];
	if (tmp >= 0) {
		return tmp;
	} else {
		int result = tmp & 127;
		if ((tmp = buffer[1]) >= 0) {
			result |= tmp << 7;
		} else {
			result |= (tmp & 127) << 7;
			if ((tmp = buffer[2]) >= 0) {
				result |= tmp << 14;
			} else {
				result |= (tmp & 127) << 14;
				if ((tmp = buffer[3]) >= 0) {
					result |= tmp << 21;
				} else {
					result |= (tmp & 127) << 21;
					result |= (tmp = buffer[4]) << 28;
					if (tmp < 0) {
						return -1;
					}
				}
			}
		}
		return result;
	}
}

int Sock::ComputeVarint32Width(int value) {
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

bool Sock::CvidHandshake() {

	core::net::MSG rq;
	core::net::MSG rs;
	core::net::msg::RQ_Cvid rq_cvid;
	core::net::msg::RS_Cvid rs_cvid;

	rq_cvid.set_instance(core::instance::InstanceType::CLIENT);
	rq_cvid.set_instance_flavor(core::instance::InstanceFlavor::MICRO);
	rq_cvid.set_uuid(uuid);
	rq.mutable_payload()->PackFrom(rq_cvid);
	if (!Send(rq)) {
		return false;
	}

	if (!Recv(rs)) {
		return false;
	}

	if (!rs.payload().UnpackTo(&rs_cvid)) {
		return false;
	}
	remote_cvid = rs_cvid.server_cvid();
	remote_uuid = rs_cvid.server_uuid();
	cvid = rs_cvid.cvid();

	std::cout << "CVID Handshake completed:" << std::endl;
	std::cout << "    Server UUID: " << remote_uuid << std::endl;
	std::cout << "    Server CVID: " << remote_cvid << std::endl;
	std::cout << "  Assigned CVID: " << cvid << std::endl;

	return true;
}

bool Sock::Send(const core::net::MSG &msg) {

	int payload_size = msg.ByteSize();
	int header_size = ComputeVarint32Width(payload_size);

	// Reserve enough space for the payload and the length header
	char buffer[payload_size + header_size];

	// Write header
	WriteVarint32(buffer, payload_size);

	// Write payload
	msg.SerializeToArray(buffer + header_size, payload_size);

	// Send payload
	for (int sent = 0; sent < (payload_size + header_size);) {
		int s = send(sockfd, buffer + sent, payload_size + header_size - sent,
				0);
		if (s <= 0) {
			return false;
		}

		sent += s;
	}

	return true;
}

bool Sock::Recv(core::net::MSG &msg) {
	char buffer[1024];

	int r = recv(sockfd, buffer, 1024, 0);
	int len = ReadVarint32(buffer);
	return msg.ParseFromArray(buffer + ComputeVarint32Width(len), len);
}
