//===----------------------------------------------------------------------===//
//
// This source file is part of the SwiftProtobuf open source project
//
// Copyright (c) 2019 Circuit Dragon, Ltd.
// Licensed under Apache License v2.0
//
// See LICENSE.txt for license information
//
// SPDX-License-Identifier: Apache-2.0
//
//===----------------------------------------------------------------------===//
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
