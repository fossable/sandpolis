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
import NIOSSL
import CryptoSwift
import SwiftProtobuf
import os

/// Represents a connection to a Sandpolis server
public class SandpolisConnection {

	/// The global application trust store
	private static let trustRoots: NIOSSLTrustRoots = {
		if let ca = NSDataAsset(name: "cert/ca"), let server = NSDataAsset(name: "cert/int_server") {
			return .certificates([
				// The root CA certificate
				try! NIOSSLCertificate(bytes: ca.data.bytes, format: .pem),
				// The server intermediate certificate
				try! NIOSSLCertificate(bytes: server.data.bytes, format: .pem)
			])
		}

		os_log("Using default trust store")
		return .default
	}()

	/// The server connection
	var channel: Channel!

	/// The local CVID established via handshake
	var cvid: Int32!

	/// A list of active streams
	var streams = [SandpolisStream]()

	/// A list of client profiles
	var profiles = [SandpolisProfile]()

	/// A promise that's notified when the connection completes
	let connectionPromise: EventLoopPromise<Void>
	let connectionFuture: EventLoopFuture<Void>

	/// Whether the CVID handshake has completed successfully
	var handshakeCompleted = false

	/// A list of profile listeners
	private var profileListeners = [(SandpolisProfile) -> Void]()

	/// A list of disconnection listeners
	private var disconnectListeners = [() -> Void]()

	/// The response handler
	private var responseHandler = ResponseHandler()

	/// The networking I/O loop group
	private let loopGroup = MultiThreadedEventLoopGroup(numberOfThreads: 1)

	/// An executor for all response handlers
	private let responseLoop = MultiThreadedEventLoopGroup(numberOfThreads: 1).next()

	/// Connect to a server.
	///
	/// - Parameter server: The server IP address or DNS name
	/// - Parameter port The server port number
	/// - Parameter certificateVerification: The type of certificate verification to perform
	init(_ server: String, _ port: Int, certificateVerification: CertificateVerification = .fullVerification) {
		os_log("Attempting connection to server: %s", server)

		connectionPromise = responseLoop.makePromise()
		connectionFuture = connectionPromise.futureResult

		let sslContext = try! NIOSSLContext(configuration: TLSConfiguration.forClient(
			//minimumTLSVersion: .tlsv13,
			certificateVerification: certificateVerification,
			trustRoots: SandpolisConnection.trustRoots
		))

		let bootstrap = ClientBootstrap(group: loopGroup)
			.channelOption(ChannelOptions.socket(SocketOptionLevel(SOL_SOCKET), SO_REUSEADDR), value: 1)
			.channelInitializer {
				$0.pipeline.addHandlers([
					self.buildSslHandler(sslContext, server),
					ByteToMessageHandler(VarintFrameDecoder()),
					ByteToMessageHandler(ProtobufDecoder<Net_MSG>()),
					MessageToByteHandler(VarintLengthFieldPrepender()),
					MessageToByteHandler(ProtobufEncoder<Net_MSG>()),
					CvidRequestHandler(self),
					self.responseHandler,
					ClientHandler(self),
					ManagementHandler(self)
				])
		}

		let connect = bootstrap.connect(host: server, port: port)

		connect.whenFailure { error in
			self.connectionPromise.fail(error)
		}
		connect.whenSuccess { ch in
			self.channel = ch
		}
	}

	private func buildSslHandler(_ context: NIOSSLContext, _ hostname: String) -> NIOSSLClientHandler {
		do {
			return try NIOSSLClientHandler(context: context, serverHostname: hostname)
		} catch {
			// Probably because hostname is an IP address
			return try! NIOSSLClientHandler(context: context, serverHostname: nil)
		}
	}

	func isConnected() -> Bool {
		return channel != nil && channel.isActive && handshakeCompleted
	}

	/// Disconnect from the current server.
	func disconnect() {
		DispatchQueue.main.async {
			for listener in self.disconnectListeners {
				listener()
			}
		}

		_ = channel.close()

		do {
			// Shutdown loop group
			try loopGroup.syncShutdownGracefully()
		} catch {
			// Ignore
		}
	}

	/// A non-blocking method that sends a request and returns a response future. If the timeout elapses before a response is received, the response future fails.
	///
	/// - Parameters:
	///   - rq: The request message
	///   - timeout: The request timeout in seconds
	/// - Returns: A response future
	func request(_ rq: inout Net_MSG, _ timeout: TimeInterval = 8.0) -> EventLoopFuture<Net_MSG> {
		rq.id = SandpolisUtil.rq()
		rq.from = cvid

		let p = responseLoop.makePromise(of: Net_MSG.self)
		responseHandler.register(rq.id, p, timeout)

		_ = channel.writeAndFlush(rq)
		return p.futureResult
	}

	/// A non-blocking method that sends an event.
	///
	/// - Parameter rq: The event message
	func send(_ ev: inout Net_MSG) {
		_ = channel.writeAndFlush(ev)
	}

	/// Login to the connected server.
	///
	/// - Parameters:
	///   - username: The user's username
	///   - password: The user's plaintext password
	/// - Returns: A response future
	func login(_ username: String, _ password: String) -> EventLoopFuture<Net_MSG> {
		var rq = Net_MSG.with {
			$0.rqLogin = Net_RQ_Login.with {
				$0.username = username
				$0.password = password.bytes.sha256().toHexString()
			}
		}

		os_log("Requesting login for username: %s", username)
		return request(&rq)
	}

	/// Login to the connected server.
	///
	/// - Parameters:
	///   - target: The target profile
	///   - attribute: The attribute to query
	/// - Returns: A response future
	func query(_ target: SandpolisProfile, _ attribute: String) -> EventLoopFuture<Net_MSG> {
		var rq = Net_MSG.with {
			$0.to = target.cvid
			$0.rqAttributeQuery = Net_RQ_AttributeQuery.with {
				$0.path = [attribute]
			}
		}

		os_log("Requesting attribute: %s", attribute)
		return request(&rq).map { rs in
			//target.merge(rs.rsAttributeQuery.result)
			return rs
		}
	}

	/// Open a new profile stream.
	///
	/// - Returns: A response future
	func openProfileStream() -> EventLoopFuture<Net_MSG> {
		let stream = SandpolisStream(self, SandpolisUtil.stream())
		stream.register { (m: Net_MSG) -> Void in
			let ev = m.evProfileStream
			if ev.online {

				let profile = SandpolisProfile(
					uuid: ev.uuid,
					cvid: ev.cvid,
					hostname: ev.hostname,
					ipAddress: ev.ip,
					platform: ev.platform,
					online: ev.online
				)

				if ev.hasLocation {
					profile.location = ev.location
				}

				self.profiles.append(profile)
				for handler in self.profileListeners {
					handler(profile)
				}
			} else {
				if let index = self.profiles.firstIndex(where: { $0.cvid == ev.cvid }) {
					let profile = self.profiles.remove(at: index)
					profile.online = ev.online
					for handler in self.profileListeners {
						handler(profile)
					}
				}
			}
		}
		streams.append(stream)

		var rq = Net_MSG.with {
			$0.rqProfileStream = Net_RQ_ProfileStream.with {
				$0.id = stream.id
			}
		}

		return request(&rq)
	}

	/// Register the given handler to receive profile updates.
	///
	/// - Parameter handler: The profile handler
	func registerHostUpdates(_ handler: @escaping (SandpolisProfile) -> Void) {
		profileListeners.append(handler)
	}

	/// Register the given handler to receive disconnection updates.
	///
	/// - Parameter handler: The disconnect handler
	func registerDisconnectHandler(_ handler: @escaping () -> Void) {
		disconnectListeners.append(handler)
	}
}
