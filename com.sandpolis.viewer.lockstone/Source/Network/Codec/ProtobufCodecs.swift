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
import SwiftProtobuf
import Foundation

public class ProtobufDecoder<T: Message>: ByteToMessageDecoder {
    public typealias InboundOut = T

    let extensionMap: ExtensionMap?
    let decodingOptions: BinaryDecodingOptions

    public init(extensionMap: ExtensionMap? = nil, options: BinaryDecodingOptions = BinaryDecodingOptions()) {
        self.extensionMap = extensionMap
        self.decodingOptions = options
    }

    public func decode(context: ChannelHandlerContext, buffer: inout ByteBuffer) throws -> DecodingState {
        do {
            let message = try buffer.withUnsafeMutableReadableBytes { bufPtr -> T in
                let data = Data(bytesNoCopy: bufPtr.baseAddress!, count: bufPtr.count, deallocator: .none)
                return try T(serializedData: data, extensions: self.extensionMap, options: self.decodingOptions)
            }
            context.fireChannelRead(NIOAny(message))
            // don't forget to consume the bytes in the buffer
            buffer.moveReaderIndex(forwardBy: buffer.readableBytes)
        } catch BinaryDecodingError.truncated {
            return .needMoreData
        }
        return .continue
    }

    public func decodeLast(context: ChannelHandlerContext, buffer: inout ByteBuffer, seenEOF: Bool) throws -> DecodingState {
        return try decode(context: context, buffer: &buffer)
    }
}

public class ProtobufEncoder<T: Message>: MessageToByteEncoder {
    public typealias OutboundIn = T

    public init() {}

    public func encode(data: T, out: inout ByteBuffer) throws {
        out.writeBytes(try data.serializedData())
    }
}
