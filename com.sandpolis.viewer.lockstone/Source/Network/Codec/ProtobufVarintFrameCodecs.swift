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

public class VarintFrameDecoder: ByteToMessageDecoder {
    public typealias InboundOut = ByteBuffer
    
    private var messageLength: Int? = nil
    
    public init() {}
    
    public func decode(context: ChannelHandlerContext, buffer: inout ByteBuffer) throws -> DecodingState {
        // If we don't have a length, we need to read one
        if self.messageLength == nil {
            self.messageLength = buffer.readVarint()
        }
        guard let length = self.messageLength else {
            // Not enough bytes to read the message length. Ask for more.
            return .needMoreData
        }
        
        // See if we can read this amount of data.
        guard let messageBytes = buffer.readSlice(length: length) else {
            // not enough bytes in the buffer to satisfy the read. Ask for more.
            return .needMoreData
        }
        
        // We don't need the length now.
        self.messageLength = nil
        
        // The message's bytes up the pipeline to the next handler.
        context.fireChannelRead(self.wrapInboundOut(messageBytes))
        
        // We can keep going if you have more data.
        return .continue
    }
    
    public func decodeLast(context: ChannelHandlerContext, buffer: inout ByteBuffer, seenEOF: Bool) throws -> DecodingState {
        return try decode(context: context, buffer: &buffer)
    }
}

public class VarintLengthFieldPrepender: MessageToByteEncoder {
    public typealias OutboundIn = ByteBuffer
    
    public init() {}
    
    public func encode(data: ByteBuffer, out: inout ByteBuffer) throws {
        let bodyLen = data.readableBytes
        out.writeVarint(bodyLen)
        out.writeBytes(data.readableBytesView)
    }
}
