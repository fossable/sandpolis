/******************************************************************************
 *                                                                            *
 *                    Copyright 2019 Subterranean Security                    *
 *                                                                            *
 *  Licensed under the Apache License, Version 2.0 (the "License");           *
 *  you may not use this file except in compliance with the License.          *
 *  You may obtain a copy of the License at                                   *
 *                                                                            *
 *      http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                            *
 *  Unless required by applicable law or agreed to in writing, software       *
 *  distributed under the License is distributed on an "AS IS" BASIS,         *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  *
 *  See the License for the specific language governing permissions and       *
 *  limitations under the License.                                            *
 *                                                                            *
 *****************************************************************************/
import NIO
import CryptoSwift
import SwiftProtobuf
import os

/// A utility class for interacting with the Sandpolis backend
public class SandpolisUtil {

    /// The local CVID established via handshake in CvidRequestHandler
    static var cvid: Int32 = 0

    /// The local UUID
    static let uuid = UUID().uuidString

    /// A list of active streams
    private static var streams = [SandpolisStream]()

    /// The client list
    static var profiles = [SandpolisProfile]()

    /// A list of profile listeners
    private static var profileListeners = [(SandpolisProfile) -> Void]()

    /// A list of disconnection listeners
    private static var disconnectListeners = [() -> Void]()

    /// The server connection
    private static var channel: Channel!

    /// The response handler
    private static var responseHandler: ResponseHandler!

    /// The networking I/O loop group
    private static var loopGroup: EventLoopGroup!

    /// An executor for all response handlers
    private static let responseLoop = MultiThreadedEventLoopGroup(numberOfThreads: 1).next()

    /// Test a server's availability by briefly connecting.
    ///
    /// - Parameter server: The server address
    /// - Returns: Whether the connection was successful
    static func testConnect(_ server: String) -> Bool {
        os_log("Probing server: %s", server)

        let testLoopGroup = MultiThreadedEventLoopGroup(numberOfThreads: 1)
        defer {
            try! testLoopGroup.syncShutdownGracefully()
        }

        let bootstrap = ClientBootstrap(group: testLoopGroup).channelOption(ChannelOptions.socket(SocketOptionLevel(SOL_SOCKET), SO_REUSEADDR), value: 1)
        do {
            _ = try bootstrap.connect(host: server, port: 10101).wait()
            return true
        } catch {
            return false
        }
    }

    /// Connect to a server.
    ///
    /// - Parameter server: The server IP address or DNS name
    /// - Returns: A future that is notified when the CVID handshake completes
    static func connect(_ server: String) -> EventLoopFuture<Int32> {
        os_log("Attempting connection to server: %s", server)

        if channel != nil {
            disconnect()
        }

        responseHandler = ResponseHandler()
        loopGroup = MultiThreadedEventLoopGroup(numberOfThreads: 1)

        let cvidHandler = CvidRequestHandler(responseLoop)
        let bootstrap = ClientBootstrap(group: loopGroup)
            .channelOption(ChannelOptions.socket(SocketOptionLevel(SOL_SOCKET), SO_REUSEADDR), value: 1)
            .channelInitializer {
                $0.pipeline.addHandlers([
                    ByteToMessageHandler(VarintFrameDecoder()),
                    ByteToMessageHandler(ProtobufDecoder<Net_Message>()),
                    MessageToByteHandler(VarintLengthFieldPrepender()),
                    MessageToByteHandler(ProtobufEncoder<Net_Message>()),
                    cvidHandler,
                    responseHandler,
                    ClientHandler()])
        }

        let connect = bootstrap.connect(host: server, port: 10101)

        connect.whenFailure { error in
            cvidHandler.handshakePromise.fail(error)
        }
        return connect.flatMap { ch in
            channel = ch
            return cvidHandler.handshakePromise.futureResult
        }
    }

    /// Disconnect from the current server.
    static func disconnect() {
        guard loopGroup != nil else {
            return
        }
        DispatchQueue.main.async {
            for listener in disconnectListeners {
                listener()
            }
        }

        do {
            // Shutdown loop group
            try loopGroup.syncShutdownGracefully()
        } catch {
            // Ignore
        }
        loopGroup = nil

        // Reset everything else
        channel = nil
        cvid = 0
        responseHandler = nil
        profiles.removeAll()
        streams.removeAll()
        profileListeners.removeAll()
    }

    /// A non-blocking method that sends a request and returns a response future.
    ///
    /// - Parameter rq: The request message
    /// - Returns: A response future
    static func request(_ rq: inout Net_Message) -> EventLoopFuture<Net_Message> {
        rq.id = Int32.random(in: 1 ... Int32.max)
        rq.from = cvid

        let p = responseLoop.makePromise(of: Net_Message.self)
        responseHandler.register(rq.id, p)

        channel.writeAndFlush(rq)
        return p.futureResult
    }

    /// Login to the connected server.
    ///
    /// - Parameters:
    ///   - username: The user's username
    ///   - password: The user's plaintext password
    /// - Returns: A response future
    static func login(_ username: String, _ password: String) -> EventLoopFuture<Net_Message> {
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
    static func screenshot(_ target: Int32) -> EventLoopFuture<Net_Message> {
        var rq = Net_Message.with {
            $0.to = target
            $0.rqScreenshot = Net_RQ_Screenshot()
        }

        os_log("Requesting screenshot for client: %d", target)
        return request(&rq)
    }

    /// Request to shutdown the given client.
    ///
    /// - Parameter target: The target client's CVID
    /// - Returns: A response future
    static func poweroff(_ target: Int32) -> EventLoopFuture<Net_Message> {
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
    static func restart(_ target: Int32) -> EventLoopFuture<Net_Message> {
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
    static func fm_list(_ target: Int32, _ path: String, mtimes: Bool, sizes: Bool) -> EventLoopFuture<Net_Message> {
        var rq = Net_Message.with {
            $0.to = target
            $0.rqFileListing = Net_RQ_FileListing.with {
                $0.path = path
                $0.options = Net_FsHandleOptions.with {
                    $0.mtime = mtimes
                    $0.size = sizes
                }
            }
        }

        os_log("Requesting file listing for client: %d, path: %s", target, path)
        return request(&rq)
    }

    /// Delete a file on the given client.
    ///
    /// - Parameter target: The target client's CVID
    /// - Parameter path: The target file
    /// - Returns: A response future
    static func fm_delete(_ target: Int32, _ path: String) -> EventLoopFuture<Net_Message> {
        var rq = Net_Message.with {
            $0.to = target
            $0.rqFileDelete = Net_RQ_FileDelete.with {
                $0.path = path
            }
        }

        os_log("Requesting file deletion for client: %d, path: %s", target, path)
        return request(&rq)
    }

    /// Request to execute the script on the given client.
    ///
    /// - Parameter cvid: The target client's CVID
    /// - Parameter script: The macro script
    /// - Returns: A response future
    static func execute(_ target: Int32, _ script: String) -> EventLoopFuture<Net_Message> {
        var rq = Net_Message.with {
            $0.to = target
            $0.rqExecute = Net_RQ_Execute.with {
                $0.command = script
            }
        }

        os_log("Requesting macro execution for client: %d, script: %s", target, script)
        return request(&rq)
    }

    /// Open a new profile stream.
    ///
    /// - Returns: A response future
    static func openProfileStream() -> EventLoopFuture<Net_Message> {
        var rq = Net_Message.with {
            $0.rqStreamStart = Net_RQ_StreamStart.with {
                $0.param = Net_StreamParam.with {
                    $0.direction = .reverse
                    $0.type = .profile(Net_ProfileStreamParam())
                }
            }
        }

        return request(&rq).map { rs in
            buildProfileStream(rs.rsStreamStart.streamID)
            return rs
        }
    }

    private static func buildProfileStream(_ streamID: Int32) {
        let stream = SandpolisStream(streamID)
        stream.register { (ev: Net_EV_StreamData) -> Void in
            if ev.profile.online {

                // Query metadata
                getClientMetadata(ev.profile.cvid).whenSuccess { rs in
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

                    LocationUtil.queryIpLocation(ev.profile.ip) { json, error in
                        if let json = json, let status = json["status"] as? String, status == "success" {
                            profile.latitude = json["lat"] as? Double
                            profile.longitude = json["lon"] as? Double
                            profile.country = json["country"] as? String
                            profile.countryCode = json["countryCode"] as? String
                            profile.region = json["regionName"] as? String
                            profile.city = json["city"] as? String
                        }

                        profiles.append(profile)
                        for handler in profileListeners {
                            handler(profile)
                        }
                    }
                }
            } else {
                if let index = profiles.firstIndex(where: { $0.cvid == ev.profile.cvid }) {
                    let profile = profiles.remove(at: index)
					profile.online = ev.profile.online
                    for handler in profileListeners {
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
    static func getClientMetadata(_ target: Int32) -> EventLoopFuture<Net_Message> {
        var rq = Net_Message.with {
            $0.to = target
            $0.rqClientMetadata = Net_RQ_ClientMetadata()
        }

        return request(&rq)
    }

    /// Register the given handler to receive profile updates.
    ///
    /// - Parameter handler: The profile handler
    static func registerHostUpdates(_ handler: @escaping (SandpolisProfile) -> Void) {
        profileListeners.append(handler)
    }

    /// Register the given handler to receive disconnection updates.
    ///
    /// - Parameter handler: The disconnect handler
    static func registerDisconnectHandler(_ handler: @escaping () -> Void) {
        disconnectListeners.append(handler)
    }

    /// Handles non-request messages
    private final class ClientHandler: ChannelInboundHandler {
        typealias InboundIn = Net_Message

        func channelRead(context: ChannelHandlerContext, data: NIOAny) {
            let rs = self.unwrapInboundIn(data)

            if rs.msgOneof == nil {
                return
            }

            switch rs.msgOneof! {
            case .evStreamData:
                DispatchQueue.global(qos: .default).async {
                    // Linear search to find stream
                    if let stream = streams.first(where: { $0.id == rs.evStreamData.id }) {
                        stream.consume(rs.evStreamData)
                    } else {
                        // Wait one second and try again before dropping
                        sleep(1)
                        if let stream = streams.first(where: { $0.id == rs.evStreamData.id }) {
                            stream.consume(rs.evStreamData)
                        } else {
                            print("Warning: dropped data for stream:", rs.evStreamData.id)
                        }
                    }
                }
            default:
                print("Warning: Missing handler:", rs.msgOneof!)
            }
        }

        func channelInactive(context: ChannelHandlerContext) {
            SandpolisUtil.disconnect()
        }

        func errorCaught(context: ChannelHandlerContext, error: Error) {
            SandpolisUtil.disconnect()
        }
    }
}
