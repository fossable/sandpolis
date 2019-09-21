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
			return false
		}
	}
}
