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
import NIO

extension ByteBuffer {
	mutating func readVarint() -> Int? {
		var value: UInt64 = 0
		var shift: UInt64 = 0
		let initialReadIndex = self.readerIndex

		while true {
			guard let c: UInt8 = self.readInteger() else {
				// ran out of bytes. Reset the read pointer and return nil.
				self.moveReaderIndex(to: initialReadIndex)
				return nil
			}

			value |= UInt64(c & 0x7F) << shift
			if c & 0x80 == 0 {
				return Int(value)
			}
			shift += 7
			if shift > 63 {
				fatalError("Invalid varint, requires shift (\(shift)) > 64")
			}
		}
	}

	mutating func writeVarint(_ v: Int) {
		var value = v
		while (true) {
			if ((value & ~0x7F) == 0) {
				// final byte
				self.writeInteger(UInt8(truncatingIfNeeded: value))
				return
			} else {
				self.writeInteger(UInt8(value & 0x7F) | 0x80)
				value = value >> 7
			}
		}
	}
}
