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
import CryptoKit
import NIO
import NIOProtobuf
import NIOSSL
import SwiftProtobuf
import SwiftEventBus
import Foundation
import UIKit
import os

/// Represents a connection to a Sandpolis server
public class SandpolisConnection {

	/// The global application trust store
	private static let trustRoots: NIOSSLTrustRoots = {
		if let ca = NSDataAsset(name: "cert/ca"), let server = NSDataAsset(name: "cert/int_server") {
			return .certificates([
				// The root CA certificate
				try! NIOSSLCertificate(bytes: [UInt8](ca.data), format: .pem),
				// The server intermediate certificate
				try! NIOSSLCertificate(bytes: [UInt8](server.data), format: .pem)
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
					ByteToMessageHandler(ProtobufDecoder<Core_Net_MSG>()),
					MessageToByteHandler(VarintLengthFieldPrepender()),
					MessageToByteHandler(ProtobufEncoder<Core_Net_MSG>()),
					CvidRequestHandler(self),
					self.responseHandler,
					ClientHandler(self),
					ManagementHandler(self)
				])
		}

		bootstrap.connect(host: server, port: port).whenComplete { result in
			switch result {
			case .success(let ch):
				self.channel = ch
			case .failure(let error):
				self.connectionPromise.fail(error)
			}
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
		SwiftEventBus.post("connectionLostEvent")

		_ = channel.close()

		// Shutdown loop group
		loopGroup.shutdownGracefully { _ in}
	}

	/// A non-blocking method that sends a request and returns a response future. If the timeout elapses before a response is received, the response future fails.
	///
	/// - Parameters:
	///   - rq: The request message
	///   - timeout: The request timeout in seconds
	/// - Returns: A response future
	func request(_ rq: inout Core_Net_MSG, _ timeout: TimeInterval = 8.0) -> EventLoopFuture<Core_Net_MSG> {
		rq.id = SandpolisUtil.rq()
		rq.from = cvid

		let p = responseLoop.makePromise(of: Core_Net_MSG.self)
		responseHandler.register(rq.id, p, timeout)

		_ = channel.writeAndFlush(rq)
		return p.futureResult
	}

	/// A non-blocking method that sends an event.
	///
	/// - Parameter rq: The event message
	func send(_ ev: inout Core_Net_MSG) {
		_ = channel.writeAndFlush(ev)
	}

	/// Login to the connected server.
	///
	/// - Parameters:
	///   - username: The user's username
	///   - password: The user's plaintext password
	/// - Returns: A response future
	func login(_ username: String, _ password: String) -> EventLoopFuture<Core_Net_MSG> {
		var rq = Core_Net_MSG.with {
			$0.payload = try! Core_Clientserver_Msg_RQ_Login.with {
				$0.username = username
				$0.password = SHA256.hash(data: password.data(using: .utf8)!).map { String(format: "%02hhx", $0) }.joined()
            }.serializedData()
		}

		os_log("Requesting login for username: %s", username)
		return request(&rq)
	}

	/// Request an ST snapshot.
	///
	/// - Parameters:
	///   - target: The target profile
	///   - attribute: The target OID
	/// - Returns: A response future
	func snapshot_collection(_ target: SandpolisProfile, _ oid: String) -> EventLoopFuture<Core_Instance_ProtoCollection> {
		var rq = Core_Net_MSG.with {
			$0.to = target.cvid
			$0.payload = try! Core_Net_Msg_RQ_STSnapshot.with {
				$0.oid = oid
            }.serializedData()
		}

		os_log("Requesting snapshot: %s", oid)
		return request(&rq).map { rs in
			do {
				return try Core_Instance_ProtoCollection.init(serializedData: rs.payload)
			} catch {
				return Core_Instance_ProtoCollection.init()
			}
		}
	}
}
