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
#ifndef NET_H
#define NET_H

#include <errno.h>
#include <netdb.h>
#include <resolv.h>
#include <string>
#include <unistd.h>
//#include <openssl/ssl.h>
//#include <openssl/err.h>

// Connect a socket to the remote host and return the file descriptor or -1 to
// indicate failure.
int OpenConnection(const std::string hostname, const int port);

#endif
