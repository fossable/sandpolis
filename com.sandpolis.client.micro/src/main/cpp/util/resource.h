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
#ifndef RESOURCE_H
#define RESOURCE_H

#include <streambuf>

// A helper class that converts a string to an istream.
struct ResourceBuffer: std::streambuf, public std::istream {

	explicit ResourceBuffer(char *begin, size_t length) {
		// Configure the std::streambuf aspect
		this->setg(begin, begin, begin + length);

		// Configure the std::istream aspect
		this->rdbuf(this);
	}
};

#endif
