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
					ByteToMessageHandler(ProtobufDecoder<Net_Message>()),
					MessageToByteHandler(VarintLengthFieldPrepender()),
					MessageToByteHandler(ProtobufEncoder<Net_Message>()),
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

	/// A non-blocking method that sends a request and returns a response future.
	///
	/// - Parameter rq: The request message
	/// - Returns: A response future
	func request(_ rq: inout Net_Message) -> EventLoopFuture<Net_Message> {
		rq.id = Int32.random(in: 1 ... Int32.max)
		rq.from = cvid

		let p = responseLoop.makePromise(of: Net_Message.self)
		responseHandler.register(rq.id, p)

		_ = channel.writeAndFlush(rq)
		return p.futureResult
	}

	/// Login to the connected server.
	///
	/// - Parameters:
	///   - username: The user's username
	///   - password: The user's plaintext password
	/// - Returns: A response future
	func login(_ username: String, _ password: String) -> EventLoopFuture<Net_Message> {
		var rq = Net_Message.with {
			$0.rqLogin = Net_RQ_Login.with {
				$0.username = username
				$0.password = password.bytes.sha256().toHexString()
			}
		}

		os_log("Requesting login for username: %s", username)
		return request(&rq)
	}

	/// Request a screenshot from the given client.
	///
	/// - Parameter target: The target client's CVID
	/// - Returns: A response future
	func screenshot(_ target: Int32) -> EventLoopFuture<Net_Message> {
		var rq = Net_Message.with {
			$0.to = target
			$0.plugin = try! Google_Protobuf_Any(message: Net_RQ_Screenshot(), typePrefix: "com.sandpolis.plugin.desktop")
		}

		os_log("Requesting screenshot for client: %d", target)
		return request(&rq)
	}

	/// Request to shutdown the given client.
	///
	/// - Parameter target: The target client's CVID
	/// - Returns: A response future
	func poweroff(_ target: Int32) -> EventLoopFuture<Net_Message> {
		var rq = Net_Message.with {
			$0.to = target
			$0.rqPowerChange = Net_RQ_PowerChange.with {
				$0.change = .poweroff
			}
		}

		os_log("Requesting poweroff for client: %d", target)
		return request(&rq)
	}

	/// Request to restart the given client.
	///
	/// - Parameter target: The target client's CVID
	/// - Returns: A response future
	func restart(_ target: Int32) -> EventLoopFuture<Net_Message> {
		var rq = Net_Message.with {
			$0.to = target
			$0.rqPowerChange = Net_RQ_PowerChange.with {
				$0.change = .restart
			}
		}

		os_log("Requesting restart for client: %d", target)
		return request(&rq)
	}

	/// Request a file listing from the given client.
	///
	/// - Parameters:
	///   - target: The target client's CVID
	///   - path: The target path
	///   - mtimes: Whether modification times will be returned
	///   - sizes: Whether file sizes will be returned
	/// - Returns: A response future
	func fm_list(_ target: Int32, _ path: String, mtimes: Bool, sizes: Bool) -> EventLoopFuture<Net_Message> {
		var rq = Net_Message.with {
			$0.to = target
			$0.plugin = try! Google_Protobuf_Any(message: Net_RQ_FileListing.with {
				$0.path = path
				$0.options = Net_FsHandleOptions.with {
					$0.mtime = mtimes
					$0.size = sizes
				}
			}, typePrefix: "com.sandpolis.plugin.filesys")
		}

		os_log("Requesting file listing for client: %d, path: %s", target, path)
		return request(&rq)
	}

	/// Delete a file on the given client.
	///
	/// - Parameter target: The target client's CVID
	/// - Parameter path: The target file
	/// - Returns: A response future
	func fm_delete(_ target: Int32, _ path: String) -> EventLoopFuture<Net_Message> {
		var rq = Net_Message.with {
			$0.to = target
			$0.plugin = try! Google_Protobuf_Any(message: Net_RQ_FileDelete.with {
				$0.path = path
			}, typePrefix: "com.sandpolis.plugin.filesys")
		}

		os_log("Requesting file deletion for client: %d, path: %s", target, path)
		return request(&rq)
	}

	/// Request to execute the script on the given client.
	///
	/// - Parameter cvid: The target client's CVID
	/// - Parameter script: The macro script
	/// - Returns: A response future
	func execute(_ target: Int32, _ script: String) -> EventLoopFuture<Net_Message> {
		var rq = Net_Message.with {
			$0.to = target
			$0.plugin = try! Google_Protobuf_Any(message: Net_RQ_Execute.with {
				$0.command = script
			}, typePrefix: "com.sandpolis.plugin.shell")
		}

		os_log("Requesting macro execution for client: %d, script: %s", target, script)
		return request(&rq)
	}

	/// Open a new profile stream.
	///
	/// - Returns: A response future
	func openProfileStream() -> EventLoopFuture<Net_Message> {
		var rq = Net_Message.with {
			$0.rqStreamStart = Net_RQ_StreamStart.with {
				$0.param = Net_StreamParam.with {
					$0.direction = .reverse
					$0.type = .profile(Net_ProfileStreamParam())
				}
			}
		}

		return request(&rq).map { rs in
			self.buildProfileStream(rs.rsStreamStart.streamID)
			return rs
		}
	}

	private func buildProfileStream(_ streamID: Int32) {
		let stream = SandpolisStream(self, streamID)
		stream.register { (ev: Net_EV_StreamData) -> Void in
			if ev.profile.online {

				// Query metadata
				self.getClientMetadata(ev.profile.cvid).whenSuccess { rs in
					let metadata = rs.rsClientMetadata
					var profile = SandpolisProfile(
						uuid: ev.profile.uuid,
						cvid: ev.profile.cvid,
						hostname: metadata.hostname,
						ipAddress: ev.profile.ip,
						uploadTotal: metadata.upload,
						downloadTotal: metadata.download,
						platform: metadata.osType,
						osVersion: metadata.osVersion,
						username: metadata.username,
						userhome: metadata.userhome,
						startTime: metadata.startTimestamp,
						timezone: TimeZone(identifier: metadata.timezone),
						online: ev.profile.online
					)

					self.profiles.append(profile)
					for handler in self.profileListeners {
						handler(profile)
					}
				}
			} else {
				if let index = self.profiles.firstIndex(where: { $0.cvid == ev.profile.cvid }) {
					let profile = self.profiles.remove(at: index)
					profile.online = ev.profile.online
					for handler in self.profileListeners {
						handler(profile)
					}
				}
			}
		}
		streams.append(stream)
	}

	/// Query client metadata.
	///
	/// - Parameter target: The target client's CVID
	/// - Returns: A response future
	func getClientMetadata(_ target: Int32) -> EventLoopFuture<Net_Message> {
		var rq = Net_Message.with {
			$0.to = target
			$0.rqClientMetadata = Net_RQ_ClientMetadata()
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
