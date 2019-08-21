// DO NOT EDIT.
//
// Generated by the Swift generator plugin for the protocol buffer compiler.
// Source: com/sandpolis/core/proto/net/MC_Cvid.proto
//
// For information on using the generated types, please see the documenation:
//   https://github.com/apple/swift-protobuf/

import Foundation
import SwiftProtobuf

// If the compiler emits an error on this type, it is because this file
// was generated by a version of the `protoc` Swift plug-in that is
// incompatible with the version of SwiftProtobuf to which you are linking.
// Please ensure that your are building against the same version of the API
// that was used to generate this file.
fileprivate struct _GeneratedWithProtocGenSwiftVersion: SwiftProtobuf.ProtobufAPIVersionCheck {
  struct _2: SwiftProtobuf.ProtobufAPIVersion_2 {}
  typealias Version = _2
}

///*
/// A CVID request which must occur before authentication.
struct Net_RQ_Cvid {
  // SwiftProtobuf.Message conformance is added in an extension below. See the
  // `Message` and `Message+*Additions` files in the SwiftProtobuf library for
  // methods supported on all messages.

  /// The UUID of the requesting instance
  var uuid: String = String()

  /// The instance type of the requesting instance
  var instance: Util_Instance = .charcoal

  /// The instance flavor of the requesting instance
  var instanceFlavor: Util_InstanceFlavor = .none

  var unknownFields = SwiftProtobuf.UnknownStorage()

  init() {}
}

///*
/// The response to a CVID request.
struct Net_RS_Cvid {
  // SwiftProtobuf.Message conformance is added in an extension below. See the
  // `Message` and `Message+*Additions` files in the SwiftProtobuf library for
  // methods supported on all messages.

  /// A CVID for the requesting instance
  var cvid: Int32 = 0

  /// The CVID of the server
  var serverCvid: Int32 = 0

  /// The UUID of the server 
  var serverUuid: String = String()

  var unknownFields = SwiftProtobuf.UnknownStorage()

  init() {}
}

// MARK: - Code below here is support for the SwiftProtobuf runtime.

fileprivate let _protobuf_package = "net"

extension Net_RQ_Cvid: SwiftProtobuf.Message, SwiftProtobuf._MessageImplementationBase, SwiftProtobuf._ProtoNameProviding {
  static let protoMessageName: String = _protobuf_package + ".RQ_Cvid"
  static let _protobuf_nameMap: SwiftProtobuf._NameMap = [
    1: .same(proto: "uuid"),
    2: .same(proto: "instance"),
    3: .standard(proto: "instance_flavor"),
  ]

  mutating func decodeMessage<D: SwiftProtobuf.Decoder>(decoder: inout D) throws {
    while let fieldNumber = try decoder.nextFieldNumber() {
      switch fieldNumber {
      case 1: try decoder.decodeSingularStringField(value: &self.uuid)
      case 2: try decoder.decodeSingularEnumField(value: &self.instance)
      case 3: try decoder.decodeSingularEnumField(value: &self.instanceFlavor)
      default: break
      }
    }
  }

  func traverse<V: SwiftProtobuf.Visitor>(visitor: inout V) throws {
    if !self.uuid.isEmpty {
      try visitor.visitSingularStringField(value: self.uuid, fieldNumber: 1)
    }
    if self.instance != .charcoal {
      try visitor.visitSingularEnumField(value: self.instance, fieldNumber: 2)
    }
    if self.instanceFlavor != .none {
      try visitor.visitSingularEnumField(value: self.instanceFlavor, fieldNumber: 3)
    }
    try unknownFields.traverse(visitor: &visitor)
  }

  static func ==(lhs: Net_RQ_Cvid, rhs: Net_RQ_Cvid) -> Bool {
    if lhs.uuid != rhs.uuid {return false}
    if lhs.instance != rhs.instance {return false}
    if lhs.instanceFlavor != rhs.instanceFlavor {return false}
    if lhs.unknownFields != rhs.unknownFields {return false}
    return true
  }
}

extension Net_RS_Cvid: SwiftProtobuf.Message, SwiftProtobuf._MessageImplementationBase, SwiftProtobuf._ProtoNameProviding {
  static let protoMessageName: String = _protobuf_package + ".RS_Cvid"
  static let _protobuf_nameMap: SwiftProtobuf._NameMap = [
    1: .same(proto: "cvid"),
    2: .standard(proto: "server_cvid"),
    3: .standard(proto: "server_uuid"),
  ]

  mutating func decodeMessage<D: SwiftProtobuf.Decoder>(decoder: inout D) throws {
    while let fieldNumber = try decoder.nextFieldNumber() {
      switch fieldNumber {
      case 1: try decoder.decodeSingularInt32Field(value: &self.cvid)
      case 2: try decoder.decodeSingularInt32Field(value: &self.serverCvid)
      case 3: try decoder.decodeSingularStringField(value: &self.serverUuid)
      default: break
      }
    }
  }

  func traverse<V: SwiftProtobuf.Visitor>(visitor: inout V) throws {
    if self.cvid != 0 {
      try visitor.visitSingularInt32Field(value: self.cvid, fieldNumber: 1)
    }
    if self.serverCvid != 0 {
      try visitor.visitSingularInt32Field(value: self.serverCvid, fieldNumber: 2)
    }
    if !self.serverUuid.isEmpty {
      try visitor.visitSingularStringField(value: self.serverUuid, fieldNumber: 3)
    }
    try unknownFields.traverse(visitor: &visitor)
  }

  static func ==(lhs: Net_RS_Cvid, rhs: Net_RS_Cvid) -> Bool {
    if lhs.cvid != rhs.cvid {return false}
    if lhs.serverCvid != rhs.serverCvid {return false}
    if lhs.serverUuid != rhs.serverUuid {return false}
    if lhs.unknownFields != rhs.unknownFields {return false}
    return true
  }
}
