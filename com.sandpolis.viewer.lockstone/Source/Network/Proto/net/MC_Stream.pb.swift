// DO NOT EDIT.
//
// Generated by the Swift generator plugin for the protocol buffer compiler.
// Source: com/sandpolis/core/proto/net/MC_Stream.proto
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

struct Net_ProfileStreamParam {
  // SwiftProtobuf.Message conformance is added in an extension below. See the
  // `Message` and `Message+*Additions` files in the SwiftProtobuf library for
  // methods supported on all messages.

  var unknownFields = SwiftProtobuf.UnknownStorage()

  init() {}
}

struct Net_StreamParam {
  // SwiftProtobuf.Message conformance is added in an extension below. See the
  // `Message` and `Message+*Additions` files in the SwiftProtobuf library for
  // methods supported on all messages.

  /// The stream direction
  var direction: Net_StreamParam.Direction {
    get {return _storage._direction}
    set {_uniqueStorage()._direction = newValue}
  }

  var type: OneOf_Type? {
    get {return _storage._type}
    set {_uniqueStorage()._type = newValue}
  }

  var profile: Net_ProfileStreamParam {
    get {
      if case .profile(let v)? = _storage._type {return v}
      return Net_ProfileStreamParam()
    }
    set {_uniqueStorage()._type = .profile(newValue)}
  }

  var unknownFields = SwiftProtobuf.UnknownStorage()

  enum OneOf_Type: Equatable {
    case profile(Net_ProfileStreamParam)

  #if !swift(>=4.1)
    static func ==(lhs: Net_StreamParam.OneOf_Type, rhs: Net_StreamParam.OneOf_Type) -> Bool {
      switch (lhs, rhs) {
      case (.profile(let l), .profile(let r)): return l == r
      }
    }
  #endif
  }

  /// Temporary
  enum Direction: SwiftProtobuf.Enum {
    typealias RawValue = Int
    case forward // = 0
    case reverse // = 1
    case UNRECOGNIZED(Int)

    init() {
      self = .forward
    }

    init?(rawValue: Int) {
      switch rawValue {
      case 0: self = .forward
      case 1: self = .reverse
      default: self = .UNRECOGNIZED(rawValue)
      }
    }

    var rawValue: Int {
      switch self {
      case .forward: return 0
      case .reverse: return 1
      case .UNRECOGNIZED(let i): return i
      }
    }

  }

  init() {}

  fileprivate var _storage = _StorageClass.defaultInstance
}

#if swift(>=4.2)

extension Net_StreamParam.Direction: CaseIterable {
  // The compiler won't synthesize support with the UNRECOGNIZED case.
  static var allCases: [Net_StreamParam.Direction] = [
    .forward,
    .reverse,
  ]
}

#endif  // swift(>=4.2)

///*
/// Request: Begin a new Stream
struct Net_RQ_StreamStart {
  // SwiftProtobuf.Message conformance is added in an extension below. See the
  // `Message` and `Message+*Additions` files in the SwiftProtobuf library for
  // methods supported on all messages.

  var param: Net_StreamParam {
    get {return _storage._param ?? Net_StreamParam()}
    set {_uniqueStorage()._param = newValue}
  }
  /// Returns true if `param` has been explicitly set.
  var hasParam: Bool {return _storage._param != nil}
  /// Clears the value of `param`. Subsequent reads from it will return its default value.
  mutating func clearParam() {_uniqueStorage()._param = nil}

  var unknownFields = SwiftProtobuf.UnknownStorage()

  init() {}

  fileprivate var _storage = _StorageClass.defaultInstance
}

///*
/// Request: rq_stream_start
/// Response:
struct Net_RS_StreamStart {
  // SwiftProtobuf.Message conformance is added in an extension below. See the
  // `Message` and `Message+*Additions` files in the SwiftProtobuf library for
  // methods supported on all messages.

  var streamID: Int32 = 0

  var unknownFields = SwiftProtobuf.UnknownStorage()

  init() {}
}

///*
/// Request: Stop a Stream
/// Response: rs_outcome
struct Net_RQ_StreamStop {
  // SwiftProtobuf.Message conformance is added in an extension below. See the
  // `Message` and `Message+*Additions` files in the SwiftProtobuf library for
  // methods supported on all messages.

  /// The ID of the stream to stop
  var streamID: Int32 = 0

  var unknownFields = SwiftProtobuf.UnknownStorage()

  init() {}
}

struct Net_ProfileStreamData {
  // SwiftProtobuf.Message conformance is added in an extension below. See the
  // `Message` and `Message+*Additions` files in the SwiftProtobuf library for
  // methods supported on all messages.

  var cvid: Int32 = 0

  var uuid: String = String()

  /// Temporary
  var ip: String = String()

  var online: Bool = false

  var unknownFields = SwiftProtobuf.UnknownStorage()

  init() {}
}

///*
struct Net_EV_StreamData {
  // SwiftProtobuf.Message conformance is added in an extension below. See the
  // `Message` and `Message+*Additions` files in the SwiftProtobuf library for
  // methods supported on all messages.

  /// The stream ID
  var id: Int32 {
    get {return _storage._id}
    set {_uniqueStorage()._id = newValue}
  }

  var data: OneOf_Data? {
    get {return _storage._data}
    set {_uniqueStorage()._data = newValue}
  }

  /// Plugin stream data
  var plugin: Data {
    get {
      if case .plugin(let v)? = _storage._data {return v}
      return SwiftProtobuf.Internal.emptyData
    }
    set {_uniqueStorage()._data = .plugin(newValue)}
  }

  var profile: Net_ProfileStreamData {
    get {
      if case .profile(let v)? = _storage._data {return v}
      return Net_ProfileStreamData()
    }
    set {_uniqueStorage()._data = .profile(newValue)}
  }

  var unknownFields = SwiftProtobuf.UnknownStorage()

  enum OneOf_Data: Equatable {
    /// Plugin stream data
    case plugin(Data)
    case profile(Net_ProfileStreamData)

  #if !swift(>=4.1)
    static func ==(lhs: Net_EV_StreamData.OneOf_Data, rhs: Net_EV_StreamData.OneOf_Data) -> Bool {
      switch (lhs, rhs) {
      case (.plugin(let l), .plugin(let r)): return l == r
      case (.profile(let l), .profile(let r)): return l == r
      default: return false
      }
    }
  #endif
  }

  init() {}

  fileprivate var _storage = _StorageClass.defaultInstance
}

// MARK: - Code below here is support for the SwiftProtobuf runtime.

fileprivate let _protobuf_package = "net"

extension Net_ProfileStreamParam: SwiftProtobuf.Message, SwiftProtobuf._MessageImplementationBase, SwiftProtobuf._ProtoNameProviding {
  static let protoMessageName: String = _protobuf_package + ".ProfileStreamParam"
  static let _protobuf_nameMap = SwiftProtobuf._NameMap()

  mutating func decodeMessage<D: SwiftProtobuf.Decoder>(decoder: inout D) throws {
    while let _ = try decoder.nextFieldNumber() {
    }
  }

  func traverse<V: SwiftProtobuf.Visitor>(visitor: inout V) throws {
    try unknownFields.traverse(visitor: &visitor)
  }

  static func ==(lhs: Net_ProfileStreamParam, rhs: Net_ProfileStreamParam) -> Bool {
    if lhs.unknownFields != rhs.unknownFields {return false}
    return true
  }
}

extension Net_StreamParam: SwiftProtobuf.Message, SwiftProtobuf._MessageImplementationBase, SwiftProtobuf._ProtoNameProviding {
  static let protoMessageName: String = _protobuf_package + ".StreamParam"
  static let _protobuf_nameMap: SwiftProtobuf._NameMap = [
    2: .same(proto: "direction"),
    3: .same(proto: "profile"),
  ]

  fileprivate class _StorageClass {
    var _direction: Net_StreamParam.Direction = .forward
    var _type: Net_StreamParam.OneOf_Type?

    static let defaultInstance = _StorageClass()

    private init() {}

    init(copying source: _StorageClass) {
      _direction = source._direction
      _type = source._type
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
        case 2: try decoder.decodeSingularEnumField(value: &_storage._direction)
        case 3:
          var v: Net_ProfileStreamParam?
          if let current = _storage._type {
            try decoder.handleConflictingOneOf()
            if case .profile(let m) = current {v = m}
          }
          try decoder.decodeSingularMessageField(value: &v)
          if let v = v {_storage._type = .profile(v)}
        default: break
        }
      }
    }
  }

  func traverse<V: SwiftProtobuf.Visitor>(visitor: inout V) throws {
    try withExtendedLifetime(_storage) { (_storage: _StorageClass) in
      if _storage._direction != .forward {
        try visitor.visitSingularEnumField(value: _storage._direction, fieldNumber: 2)
      }
      if case .profile(let v)? = _storage._type {
        try visitor.visitSingularMessageField(value: v, fieldNumber: 3)
      }
    }
    try unknownFields.traverse(visitor: &visitor)
  }

  static func ==(lhs: Net_StreamParam, rhs: Net_StreamParam) -> Bool {
    if lhs._storage !== rhs._storage {
      let storagesAreEqual: Bool = withExtendedLifetime((lhs._storage, rhs._storage)) { (_args: (_StorageClass, _StorageClass)) in
        let _storage = _args.0
        let rhs_storage = _args.1
        if _storage._direction != rhs_storage._direction {return false}
        if _storage._type != rhs_storage._type {return false}
        return true
      }
      if !storagesAreEqual {return false}
    }
    if lhs.unknownFields != rhs.unknownFields {return false}
    return true
  }
}

extension Net_StreamParam.Direction: SwiftProtobuf._ProtoNameProviding {
  static let _protobuf_nameMap: SwiftProtobuf._NameMap = [
    0: .same(proto: "FORWARD"),
    1: .same(proto: "REVERSE"),
  ]
}

extension Net_RQ_StreamStart: SwiftProtobuf.Message, SwiftProtobuf._MessageImplementationBase, SwiftProtobuf._ProtoNameProviding {
  static let protoMessageName: String = _protobuf_package + ".RQ_StreamStart"
  static let _protobuf_nameMap: SwiftProtobuf._NameMap = [
    1: .same(proto: "param"),
  ]

  fileprivate class _StorageClass {
    var _param: Net_StreamParam? = nil

    static let defaultInstance = _StorageClass()

    private init() {}

    init(copying source: _StorageClass) {
      _param = source._param
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
        case 1: try decoder.decodeSingularMessageField(value: &_storage._param)
        default: break
        }
      }
    }
  }

  func traverse<V: SwiftProtobuf.Visitor>(visitor: inout V) throws {
    try withExtendedLifetime(_storage) { (_storage: _StorageClass) in
      if let v = _storage._param {
        try visitor.visitSingularMessageField(value: v, fieldNumber: 1)
      }
    }
    try unknownFields.traverse(visitor: &visitor)
  }

  static func ==(lhs: Net_RQ_StreamStart, rhs: Net_RQ_StreamStart) -> Bool {
    if lhs._storage !== rhs._storage {
      let storagesAreEqual: Bool = withExtendedLifetime((lhs._storage, rhs._storage)) { (_args: (_StorageClass, _StorageClass)) in
        let _storage = _args.0
        let rhs_storage = _args.1
        if _storage._param != rhs_storage._param {return false}
        return true
      }
      if !storagesAreEqual {return false}
    }
    if lhs.unknownFields != rhs.unknownFields {return false}
    return true
  }
}

extension Net_RS_StreamStart: SwiftProtobuf.Message, SwiftProtobuf._MessageImplementationBase, SwiftProtobuf._ProtoNameProviding {
  static let protoMessageName: String = _protobuf_package + ".RS_StreamStart"
  static let _protobuf_nameMap: SwiftProtobuf._NameMap = [
    1: .same(proto: "streamID"),
  ]

  mutating func decodeMessage<D: SwiftProtobuf.Decoder>(decoder: inout D) throws {
    while let fieldNumber = try decoder.nextFieldNumber() {
      switch fieldNumber {
      case 1: try decoder.decodeSingularInt32Field(value: &self.streamID)
      default: break
      }
    }
  }

  func traverse<V: SwiftProtobuf.Visitor>(visitor: inout V) throws {
    if self.streamID != 0 {
      try visitor.visitSingularInt32Field(value: self.streamID, fieldNumber: 1)
    }
    try unknownFields.traverse(visitor: &visitor)
  }

  static func ==(lhs: Net_RS_StreamStart, rhs: Net_RS_StreamStart) -> Bool {
    if lhs.streamID != rhs.streamID {return false}
    if lhs.unknownFields != rhs.unknownFields {return false}
    return true
  }
}

extension Net_RQ_StreamStop: SwiftProtobuf.Message, SwiftProtobuf._MessageImplementationBase, SwiftProtobuf._ProtoNameProviding {
  static let protoMessageName: String = _protobuf_package + ".RQ_StreamStop"
  static let _protobuf_nameMap: SwiftProtobuf._NameMap = [
    1: .same(proto: "streamID"),
  ]

  mutating func decodeMessage<D: SwiftProtobuf.Decoder>(decoder: inout D) throws {
    while let fieldNumber = try decoder.nextFieldNumber() {
      switch fieldNumber {
      case 1: try decoder.decodeSingularInt32Field(value: &self.streamID)
      default: break
      }
    }
  }

  func traverse<V: SwiftProtobuf.Visitor>(visitor: inout V) throws {
    if self.streamID != 0 {
      try visitor.visitSingularInt32Field(value: self.streamID, fieldNumber: 1)
    }
    try unknownFields.traverse(visitor: &visitor)
  }

  static func ==(lhs: Net_RQ_StreamStop, rhs: Net_RQ_StreamStop) -> Bool {
    if lhs.streamID != rhs.streamID {return false}
    if lhs.unknownFields != rhs.unknownFields {return false}
    return true
  }
}

extension Net_ProfileStreamData: SwiftProtobuf.Message, SwiftProtobuf._MessageImplementationBase, SwiftProtobuf._ProtoNameProviding {
  static let protoMessageName: String = _protobuf_package + ".ProfileStreamData"
  static let _protobuf_nameMap: SwiftProtobuf._NameMap = [
    1: .same(proto: "cvid"),
    2: .same(proto: "uuid"),
    3: .same(proto: "ip"),
    4: .same(proto: "online"),
  ]

  mutating func decodeMessage<D: SwiftProtobuf.Decoder>(decoder: inout D) throws {
    while let fieldNumber = try decoder.nextFieldNumber() {
      switch fieldNumber {
      case 1: try decoder.decodeSingularInt32Field(value: &self.cvid)
      case 2: try decoder.decodeSingularStringField(value: &self.uuid)
      case 3: try decoder.decodeSingularStringField(value: &self.ip)
      case 4: try decoder.decodeSingularBoolField(value: &self.online)
      default: break
      }
    }
  }

  func traverse<V: SwiftProtobuf.Visitor>(visitor: inout V) throws {
    if self.cvid != 0 {
      try visitor.visitSingularInt32Field(value: self.cvid, fieldNumber: 1)
    }
    if !self.uuid.isEmpty {
      try visitor.visitSingularStringField(value: self.uuid, fieldNumber: 2)
    }
    if !self.ip.isEmpty {
      try visitor.visitSingularStringField(value: self.ip, fieldNumber: 3)
    }
    if self.online != false {
      try visitor.visitSingularBoolField(value: self.online, fieldNumber: 4)
    }
    try unknownFields.traverse(visitor: &visitor)
  }

  static func ==(lhs: Net_ProfileStreamData, rhs: Net_ProfileStreamData) -> Bool {
    if lhs.cvid != rhs.cvid {return false}
    if lhs.uuid != rhs.uuid {return false}
    if lhs.ip != rhs.ip {return false}
    if lhs.online != rhs.online {return false}
    if lhs.unknownFields != rhs.unknownFields {return false}
    return true
  }
}

extension Net_EV_StreamData: SwiftProtobuf.Message, SwiftProtobuf._MessageImplementationBase, SwiftProtobuf._ProtoNameProviding {
  static let protoMessageName: String = _protobuf_package + ".EV_StreamData"
  static let _protobuf_nameMap: SwiftProtobuf._NameMap = [
    1: .same(proto: "id"),
    2: .same(proto: "plugin"),
    3: .same(proto: "profile"),
  ]

  fileprivate class _StorageClass {
    var _id: Int32 = 0
    var _data: Net_EV_StreamData.OneOf_Data?

    static let defaultInstance = _StorageClass()

    private init() {}

    init(copying source: _StorageClass) {
      _id = source._id
      _data = source._data
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
        case 1: try decoder.decodeSingularInt32Field(value: &_storage._id)
        case 2:
          if _storage._data != nil {try decoder.handleConflictingOneOf()}
          var v: Data?
          try decoder.decodeSingularBytesField(value: &v)
          if let v = v {_storage._data = .plugin(v)}
        case 3:
          var v: Net_ProfileStreamData?
          if let current = _storage._data {
            try decoder.handleConflictingOneOf()
            if case .profile(let m) = current {v = m}
          }
          try decoder.decodeSingularMessageField(value: &v)
          if let v = v {_storage._data = .profile(v)}
        default: break
        }
      }
    }
  }

  func traverse<V: SwiftProtobuf.Visitor>(visitor: inout V) throws {
    try withExtendedLifetime(_storage) { (_storage: _StorageClass) in
      if _storage._id != 0 {
        try visitor.visitSingularInt32Field(value: _storage._id, fieldNumber: 1)
      }
      switch _storage._data {
      case .plugin(let v)?:
        try visitor.visitSingularBytesField(value: v, fieldNumber: 2)
      case .profile(let v)?:
        try visitor.visitSingularMessageField(value: v, fieldNumber: 3)
      case nil: break
      }
    }
    try unknownFields.traverse(visitor: &visitor)
  }

  static func ==(lhs: Net_EV_StreamData, rhs: Net_EV_StreamData) -> Bool {
    if lhs._storage !== rhs._storage {
      let storagesAreEqual: Bool = withExtendedLifetime((lhs._storage, rhs._storage)) { (_args: (_StorageClass, _StorageClass)) in
        let _storage = _args.0
        let rhs_storage = _args.1
        if _storage._id != rhs_storage._id {return false}
        if _storage._data != rhs_storage._data {return false}
        return true
      }
      if !storagesAreEqual {return false}
    }
    if lhs.unknownFields != rhs.unknownFields {return false}
    return true
  }
}
