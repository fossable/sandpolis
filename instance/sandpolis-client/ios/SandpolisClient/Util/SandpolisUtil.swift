//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
import NIO
import os

final class SandpolisUtil {

	/// An event loop for utilities in this class only
	private static let internalLoop = MultiThreadedEventLoopGroup(numberOfThreads: 1)

	/// The application's current connection
	static var connection: SandpolisConnection!

	/// Test a server's availability by briefly connecting to it.
	///
	/// - Parameter server: The server address
	/// - Parameter port: The server port
	/// - Returns: Whether the connection was successful
	static func testConnect(_ server: String, _ port: Int) -> Bool {
		os_log("Probing server: %s", server)

		let bootstrap = ClientBootstrap(group: internalLoop).channelOption(ChannelOptions.socket(SocketOptionLevel(SOL_SOCKET), SO_REUSEADDR), value: 1)
		do {
			_ = try bootstrap.connect(host: server, port: port).wait()
			return true
		} catch {
			os_log("Failed to probe: \(error as NSObject)")
			return false
		}
	}

	static func rq() -> Int32 {
		return Int32.random(in: Int32.min ... Int32.max) << 1
	}

	static func stream() -> Int32 {
		return Int32.random(in: Int32.min ... Int32.max) | 0x01
	}
}
