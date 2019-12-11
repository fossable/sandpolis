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
