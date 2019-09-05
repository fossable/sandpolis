// DO NOT EDIT.
//
// Generated by the Swift generator plugin for the protocol buffer compiler.
// Source: com/sandpolis/core/proto/pojo/User.proto
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
/// The configuration of a User.
struct Pojo_UserConfig {
  // SwiftProtobuf.Message conformance is added in an extension below. See the
  // `Message` and `Message+*Additions` files in the SwiftProtobuf library for
  // methods supported on all messages.

  /// The primary ID
  var id: Int64 {
    get {return _id ?? 0}
    set {_id = newValue}
  }
  /// Returns true if `id` has been explicitly set.
  var hasID: Bool {return self._id != nil}
  /// Clears the value of `id`. Subsequent reads from it will return its default value.
  mutating func clearID() {self._id = nil}

  /// The username
  var username: String {
    get {return _username ?? String()}
    set {_username = newValue}
  }
  /// Returns true if `username` has been explicitly set.
  var hasUsername: Bool {return self._username != nil}
  /// Clears the value of `username`. Subsequent reads from it will return its default value.
  mutating func clearUsername() {self._username = nil}

  /// The password
  var password: String {
    get {return _password ?? String()}
    set {_password = newValue}
  }
  /// Returns true if `password` has been explicitly set.
  var hasPassword: Bool {return self._password != nil}
  /// Clears the value of `password`. Subsequent reads from it will return its default value.
  mutating func clearPassword() {self._password = nil}

  /// The email address
  var email: String {
    get {return _email ?? String()}
    set {_email = newValue}
  }
  /// Returns true if `email` has been explicitly set.
  var hasEmail: Bool {return self._email != nil}
  /// Clears the value of `email`. Subsequent reads from it will return its default value.
  mutating func clearEmail() {self._email = nil}

  /// The expiration timestamp
  var expiration: Int64 {
    get {return _expiration ?? 0}
    set {_expiration = newValue}
  }
  /// Returns true if `expiration` has been explicitly set.
  var hasExpiration: Bool {return self._expiration != nil}
  /// Clears the value of `expiration`. Subsequent reads from it will return its default value.
  mutating func clearExpiration() {self._expiration = nil}

  var unknownFields = SwiftProtobuf.UnknownStorage()

  init() {}

  fileprivate var _id: Int64? = nil
  fileprivate var _username: String? = nil
  fileprivate var _password: String? = nil
  fileprivate var _email: String? = nil
  fileprivate var _expiration: Int64? = nil
}

///*
/// User statistics.
struct Pojo_UserStats {
  // SwiftProtobuf.Message conformance is added in an extension below. See the
  // `Message` and `Message+*Additions` files in the SwiftProtobuf library for
  // methods supported on all messages.

  /// The creation timestamp
  var ctime: Int64 {
    get {return _ctime ?? 0}
    set {_ctime = newValue}
  }
  /// Returns true if `ctime` has been explicitly set.
  var hasCtime: Bool {return self._ctime != nil}
  /// Clears the value of `ctime`. Subsequent reads from it will return its default value.
  mutating func clearCtime() {self._ctime = nil}

  var unknownFields = SwiftProtobuf.UnknownStorage()

  init() {}

  fileprivate var _ctime: Int64? = nil
}

///*
/// A User container.
struct Pojo_ProtoUser {
  // SwiftProtobuf.Message conformance is added in an extension below. See the
  // `Message` and `Message+*Additions` files in the SwiftProtobuf library for
  // methods supported on all messages.

  var config: Pojo_UserConfig {
    get {return _storage._config ?? Pojo_UserConfig()}
    set {_uniqueStorage()._config = newValue}
  }
  /// Returns true if `config` has been explicitly set.
  var hasConfig: Bool {return _storage._config != nil}
  /// Clears the value of `config`. Subsequent reads from it will return its default value.
  mutating func clearConfig() {_uniqueStorage()._config = nil}

  var stats: Pojo_UserStats {
    get {return _storage._stats ?? Pojo_UserStats()}
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

extension Pojo_UserConfig: SwiftProtobuf.Message, SwiftProtobuf._MessageImplementationBase, SwiftProtobuf._ProtoNameProviding {
  static let protoMessageName: String = _protobuf_package + ".UserConfig"
  static let _protobuf_nameMap: SwiftProtobuf._NameMap = [
    1: .same(proto: "id"),
    2: .same(proto: "username"),
    3: .same(proto: "password"),
    4: .same(proto: "email"),
    5: .same(proto: "expiration"),
  ]

  mutating func decodeMessage<D: SwiftProtobuf.Decoder>(decoder: inout D) throws {
    while let fieldNumber = try decoder.nextFieldNumber() {
      switch fieldNumber {
      case 1: try decoder.decodeSingularInt64Field(value: &self._id)
      case 2: try decoder.decodeSingularStringField(value: &self._username)
      case 3: try decoder.decodeSingularStringField(value: &self._password)
      case 4: try decoder.decodeSingularStringField(value: &self._email)
      case 5: try decoder.decodeSingularInt64Field(value: &self._expiration)
      default: break
      }
    }
  }

  func traverse<V: SwiftProtobuf.Visitor>(visitor: inout V) throws {
    if let v = self._id {
      try visitor.visitSingularInt64Field(value: v, fieldNumber: 1)
    }
    if let v = self._username {
      try visitor.visitSingularStringField(value: v, fieldNumber: 2)
    }
    if let v = self._password {
      try visitor.visitSingularStringField(value: v, fieldNumber: 3)
    }
    if let v = self._email {
      try visitor.visitSingularStringField(value: v, fieldNumber: 4)
    }
    if let v = self._expiration {
      try visitor.visitSingularInt64Field(value: v, fieldNumber: 5)
    }
    try unknownFields.traverse(visitor: &visitor)
  }

  static func ==(lhs: Pojo_UserConfig, rhs: Pojo_UserConfig) -> Bool {
    if lhs._id != rhs._id {return false}
    if lhs._username != rhs._username {return false}
    if lhs._password != rhs._password {return false}
    if lhs._email != rhs._email {return false}
    if lhs._expiration != rhs._expiration {return false}
    if lhs.unknownFields != rhs.unknownFields {return false}
    return true
  }
}

extension Pojo_UserStats: SwiftProtobuf.Message, SwiftProtobuf._MessageImplementationBase, SwiftProtobuf._ProtoNameProviding {
  static let protoMessageName: String = _protobuf_package + ".UserStats"
  static let _protobuf_nameMap: SwiftProtobuf._NameMap = [
    1: .same(proto: "ctime"),
  ]

  mutating func decodeMessage<D: SwiftProtobuf.Decoder>(decoder: inout D) throws {
    while let fieldNumber = try decoder.nextFieldNumber() {
      switch fieldNumber {
      case 1: try decoder.decodeSingularInt64Field(value: &self._ctime)
      default: break
      }
    }
  }

  func traverse<V: SwiftProtobuf.Visitor>(visitor: inout V) throws {
    if let v = self._ctime {
      try visitor.visitSingularInt64Field(value: v, fieldNumber: 1)
    }
    try unknownFields.traverse(visitor: &visitor)
  }

  static func ==(lhs: Pojo_UserStats, rhs: Pojo_UserStats) -> Bool {
    if lhs._ctime != rhs._ctime {return false}
    if lhs.unknownFields != rhs.unknownFields {return false}
    return true
  }
}

extension Pojo_ProtoUser: SwiftProtobuf.Message, SwiftProtobuf._MessageImplementationBase, SwiftProtobuf._ProtoNameProviding {
  static let protoMessageName: String = _protobuf_package + ".ProtoUser"
  static let _protobuf_nameMap: SwiftProtobuf._NameMap = [
    1: .same(proto: "config"),
    2: .same(proto: "stats"),
  ]

  fileprivate class _StorageClass {
    var _config: Pojo_UserConfig? = nil
    var _stats: Pojo_UserStats? = nil

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

  static func ==(lhs: Pojo_ProtoUser, rhs: Pojo_ProtoUser) -> Bool {
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