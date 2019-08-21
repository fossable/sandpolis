// DO NOT EDIT.
//
// Generated by the Swift generator plugin for the protocol buffer compiler.
// Source: com/sandpolis/core/proto/pojo/Stream.proto
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
struct Pojo_StreamConfig {
  // SwiftProtobuf.Message conformance is added in an extension below. See the
  // `Message` and `Message+*Additions` files in the SwiftProtobuf library for
  // methods supported on all messages.

  /// The stream ID
  var id: Int32 {
    get {return _id ?? 0}
    set {_id = newValue}
  }
  /// Returns true if `id` has been explicitly set.
  var hasID: Bool {return self._id != nil}
  /// Clears the value of `id`. Subsequent reads from it will return its default value.
  mutating func clearID() {self._id = nil}

  /// The stream direction
  var direction: Pojo_StreamConfig.Direction {
    get {return _direction ?? .forward}
    set {_direction = newValue}
  }
  /// Returns true if `direction` has been explicitly set.
  var hasDirection: Bool {return self._direction != nil}
  /// Clears the value of `direction`. Subsequent reads from it will return its default value.
  mutating func clearDirection() {self._direction = nil}

  var upstreamEndpoint: [String] = []

  var downstreamEndpoint: [String] = []

  var unknownFields = SwiftProtobuf.UnknownStorage()

  enum Direction: SwiftProtobuf.Enum {
    typealias RawValue = Int

    /// Data moves into the target
    case forward // = 0

    /// Data moves from the target
    case reverse // = 1

    init() {
      self = .forward
    }

    init?(rawValue: Int) {
      switch rawValue {
      case 0: self = .forward
      case 1: self = .reverse
      default: return nil
      }
    }

    var rawValue: Int {
      switch self {
      case .forward: return 0
      case .reverse: return 1
      }
    }

  }

  init() {}

  fileprivate var _id: Int32? = nil
  fileprivate var _direction: Pojo_StreamConfig.Direction? = nil
}

#if swift(>=4.2)

extension Pojo_StreamConfig.Direction: CaseIterable {
  // Support synthesized by the compiler.
}

#endif  // swift(>=4.2)

struct Pojo_StreamStats {
  // SwiftProtobuf.Message conformance is added in an extension below. See the
  // `Message` and `Message+*Additions` files in the SwiftProtobuf library for
  // methods supported on all messages.

  var unknownFields = SwiftProtobuf.UnknownStorage()

  init() {}
}

struct Pojo_ProtoStream {
  // SwiftProtobuf.Message conformance is added in an extension below. See the
  // `Message` and `Message+*Additions` files in the SwiftProtobuf library for
  // methods supported on all messages.

  var config: Pojo_StreamConfig {
    get {return _storage._config ?? Pojo_StreamConfig()}
    set {_uniqueStorage()._config = newValue}
  }
  /// Returns true if `config` has been explicitly set.
  var hasConfig: Bool {return _storage._config != nil}
  /// Clears the value of `config`. Subsequent reads from it will return its default value.
  mutating func clearConfig() {_uniqueStorage()._config = nil}

  var stats: Pojo_StreamStats {
    get {return _storage._stats ?? Pojo_StreamStats()}
    set {_uniqueStorage()._stats = newValue}
  }
  /// Returns true if `stats` has been explicitly set.
  var hasStats: Bool {return _storage._stats != nil}
  /// Clears the value of `stats`. Subsequent reads from it will return its default value.
  mutating func clearStats() {_uniqueStorage()._stats = nil}

  var unknownFields = SwiftProtobuf.UnknownStorage()

  init() {}

  fileprivate var _storage = _StorageClass.defaultInstance
}

// MARK: - Code below here is support for the SwiftProtobuf runtime.

fileprivate let _protobuf_package = "pojo"

extension Pojo_StreamConfig: SwiftProtobuf.Message, SwiftProtobuf._MessageImplementationBase, SwiftProtobuf._ProtoNameProviding {
  static let protoMessageName: String = _protobuf_package + ".StreamConfig"
  static let _protobuf_nameMap: SwiftProtobuf._NameMap = [
    1: .same(proto: "id"),
    2: .same(proto: "direction"),
    3: .standard(proto: "upstream_endpoint"),
    4: .standard(proto: "downstream_endpoint"),
  ]

  mutating func decodeMessage<D: SwiftProtobuf.Decoder>(decoder: inout D) throws {
    while let fieldNumber = try decoder.nextFieldNumber() {
      switch fieldNumber {
      case 1: try decoder.decodeSingularInt32Field(value: &self._id)
      case 2: try decoder.decodeSingularEnumField(value: &self._direction)
      case 3: try decoder.decodeRepeatedStringField(value: &self.upstreamEndpoint)
      case 4: try decoder.decodeRepeatedStringField(value: &self.downstreamEndpoint)
      default: break
      }
    }
  }

  func traverse<V: SwiftProtobuf.Visitor>(visitor: inout V) throws {
    if let v = self._id {
      try visitor.visitSingularInt32Field(value: v, fieldNumber: 1)
    }
    if let v = self._direction {
      try visitor.visitSingularEnumField(value: v, fieldNumber: 2)
    }
    if !self.upstreamEndpoint.isEmpty {
      try visitor.visitRepeatedStringField(value: self.upstreamEndpoint, fieldNumber: 3)
    }
    if !self.downstreamEndpoint.isEmpty {
      try visitor.visitRepeatedStringField(value: self.downstreamEndpoint, fieldNumber: 4)
    }
    try unknownFields.traverse(visitor: &visitor)
  }

  static func ==(lhs: Pojo_StreamConfig, rhs: Pojo_StreamConfig) -> Bool {
    if lhs._id != rhs._id {return false}
    if lhs._direction != rhs._direction {return false}
    if lhs.upstreamEndpoint != rhs.upstreamEndpoint {return false}
    if lhs.downstreamEndpoint != rhs.downstreamEndpoint {return false}
    if lhs.unknownFields != rhs.unknownFields {return false}
    return true
  }
}

extension Pojo_StreamConfig.Direction: SwiftProtobuf._ProtoNameProviding {
  static let _protobuf_nameMap: SwiftProtobuf._NameMap = [
    0: .same(proto: "FORWARD"),
    1: .same(proto: "REVERSE"),
  ]
}

extension Pojo_StreamStats: SwiftProtobuf.Message, SwiftProtobuf._MessageImplementationBase, SwiftProtobuf._ProtoNameProviding {
  static let protoMessageName: String = _protobuf_package + ".StreamStats"
  static let _protobuf_nameMap = SwiftProtobuf._NameMap()

  mutating func decodeMessage<D: SwiftProtobuf.Decoder>(decoder: inout D) throws {
    while let _ = try decoder.nextFieldNumber() {
    }
  }

  func traverse<V: SwiftProtobuf.Visitor>(visitor: inout V) throws {
    try unknownFields.traverse(visitor: &visitor)
  }

  static func ==(lhs: Pojo_StreamStats, rhs: Pojo_StreamStats) -> Bool {
    if lhs.unknownFields != rhs.unknownFields {return false}
    return true
  }
}

extension Pojo_ProtoStream: SwiftProtobuf.Message, SwiftProtobuf._MessageImplementationBase, SwiftProtobuf._ProtoNameProviding {
  static let protoMessageName: String = _protobuf_package + ".ProtoStream"
  static let _protobuf_nameMap: SwiftProtobuf._NameMap = [
    1: .same(proto: "config"),
    2: .same(proto: "stats"),
  ]

  fileprivate class _StorageClass {
    var _config: Pojo_StreamConfig? = nil
    var _stats: Pojo_StreamStats? = nil

    static let defaultInstance = _StorageClass()

    private init() {}

    init(copying source: _StorageClass) {
      _config = source._config
      _stats = source._stats
    }
  }

  fileprivate mutating func _uniqueStorage() -> _StorageClass {
    if !isKnownUniquelyReferenced(&_storage) {
      _storage = _StorageClass(copying: _storage)
    }
    return _storage
  }

  mutating func decodeMessage<D: SwiftProtobuf.Decoder>(decoder: inout D) throws {
    _ = _uniqueStorage()
    try withExtendedLifetime(_storage) { (_storage: _StorageClass) in
      while let fieldNumber = try decoder.nextFieldNumber() {
        switch fieldNumber {
        case 1: try decoder.decodeSingularMessageField(value: &_storage._config)
        case 2: try decoder.decodeSingularMessageField(value: &_storage._stats)
        default: break
        }
      }
    }
  }

  func traverse<V: SwiftProtobuf.Visitor>(visitor: inout V) throws {
    try withExtendedLifetime(_storage) { (_storage: _StorageClass) in
      if let v = _storage._config {
        try visitor.visitSingularMessageField(value: v, fieldNumber: 1)
      }
      if let v = _storage._stats {
        try visitor.visitSingularMessageField(value: v, fieldNumber: 2)
      }
    }
    try unknownFields.traverse(visitor: &visitor)
  }

  static func ==(lhs: Pojo_ProtoStream, rhs: Pojo_ProtoStream) -> Bool {
    if lhs._storage !== rhs._storage {
      let storagesAreEqual: Bool = withExtendedLifetime((lhs._storage, rhs._storage)) { (_args: (_StorageClass, _StorageClass)) in
        let _storage = _args.0
        let rhs_storage = _args.1
        if _storage._config != rhs_storage._config {return false}
        if _storage._stats != rhs_storage._stats {return false}
        return true
      }
      if !storagesAreEqual {return false}
    }
    if lhs.unknownFields != rhs.unknownFields {return false}
    return true
  }
}
