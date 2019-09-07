//****************************************************************************//
//                                                                            //
//                Copyright © 2015 - 2019 Subterranean Security               //
//                                                                            //
//  Licensed under the Apache License, Version 2.0 (the "License");           //
//  you may not use this file except in compliance with the License.          //
//  You may obtain a copy of the License at                                   //
//                                                                            //
//      http://www.apache.org/licenses/LICENSE-2.0                            //
//                                                                            //
//  Unless required by applicable law or agreed to in writing, software       //
//  distributed under the License is distributed on an "AS IS" BASIS,         //
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  //
//  See the License for the specific language governing permissions and       //
//  limitations under the License.                                            //
//                                                                            //
//****************************************************************************//
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
