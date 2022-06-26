from org.s7s.core.instance.msg.msg_cvid_pb2 import RQ_Cvid, RS_Cvid
from org.s7s.core.instance.metatypes_pb2 import InstanceType, InstanceFlavor
from org.s7s.core.instance.message_pb2 import MSG
from enum import Enum, auto
from google.protobuf.internal.decoder import _DecodeVarint32
from google.protobuf.internal.encoder import _VarintBytes
from random import randint
from socket import create_connection
from ssl import SSLContext, PROTOCOL_TLS_CLIENT, CERT_NONE
from threading import Thread, Condition

import logging

log = logging.getLogger("connection")


class ConnectionState(Enum):
    NEW = auto()
    CONNECTED = auto()
    CLOSED = auto()


class Connection:
    def __init__(self, server_host: str, server_port: int):
        self.server_host = server_host
        self.server_port = server_port
        self.response_map = dict()
        self.response_map_cv = Condition()
        self.connection_state = ConnectionState.NEW
        self.connection_state_cv = Condition()

    def connect(self, timeout=10):
        """
        Attempt to connect to the configured server.
        """

        if self.connection_state == ConnectionState.NEW:
            log.info(f"Attempting connection to {self.server_host}:{self.server_port}")

            Thread(target=self.handle_connection).start()

            with self.connection_state_cv:
                if not self.connection_state_cv.wait_for(
                    lambda: self.connection_state != ConnectionState.CONNECTED, timeout
                ):
                    return False

                return self.connection_state == ConnectionState.CONNECTED
        else:
            raise Error("Illegal connection state")

    def handle_connection(self):
        """
        Run connection handling routine.
        """

        context = SSLContext(PROTOCOL_TLS_CLIENT)
        context.check_hostname = False
        context.verify_mode = CERT_NONE

        with create_connection((self.server_host, self.server_port)) as self.socket:
            with context.wrap_socket(
                self.socket, server_hostname=self.server_host
            ) as self.tls:

                # Use this buffer for all reads
                read_buffer = bytearray(4096)

                # Perform SID handshake
                rq = RQ_Cvid()
                rq.uuid = os.environ["S7S_UUID"]
                rq.instance = InstanceType.CLIENT
                rq.instance_flavor = InstanceFlavor.CLIENT_BRIGHTSTONE

                msg = MSG()
                setattr(msg, "payload", rq.SerializeToString())
                setattr(msg, "id", randint(0, 65535))

                self.tls.write(_VarintBytes(msg.ByteSize()))
                self.tls.sendall(msg.SerializeToString())

                read = self.tls.recv_into(read_buffer, 4096)
                if read == 0:
                    raise EOFError("")

                msg_len, msg_start = _DecodeVarint32(read_buffer, 0)

                msg = MSG()
                msg.ParseFromString(read_buffer[msg_start : msg_start + msg_len])

                rs = RS_Cvid()
                rs.ParseFromString(msg.payload)

                self.server_cvid = rs.server_cvid
                self.sid = rs.sid

                # The connection is now connected
                with self.connection_state_cv:
                    self.connection_state = ConnectionState.CONNECTED
                    self.connection_state_cv.notify()

                # Begin accepting messages
                while True:

                    # TODO there may be another message in the read buffer

                    # Read from the socket
                    read = self.tls.recv_into(read_buffer, 4096)
                    if read == 0:
                        raise EOFError("")

                    n = 0
                    while n < read:
                        msg_len, n = _DecodeVarint32(read_buffer, n)

                        msg = MSG()
                        msg.ParseFromString(read_buffer[n : n + msg_len])

                        # Place message in response map
                        with self.response_map_cv:
                            self.response_map[msg.id] = msg
                            self.response_map_cv.notify()

        # The connection is now closed
        with self.connection_state_cv:
            self.connection_state = ConnectionState.CLOSED
            self.connection_state_cv.notify()

    def request(self, rq, to=0, timeout=10):
        """
        Send the given request and return the response it provoked.
        """

        if self.connection_state != ConnectionState.CONNECTED:
            raise Error("Illegal connection state")

        msg = MSG()
        setattr(msg, "payload", rq.SerializeToString())
        setattr(msg, "id", randint(0, 65535))
        if to == 0:
            setattr(msg, "to", self.server_cvid)
        else:
            setattr(msg, "to", to)
        setattr(msg, "from", self.sid)

        # Write the message
        self.tls.write(_VarintBytes(msg.ByteSize()))
        self.tls.sendall(msg.SerializeToString())

        # Wait for response
        with self.response_map_cv:
            if not self.response_map_cv.wait_for(
                lambda: msg.id in self.response_map.keys(), timeout
            ):
                return None

            # Return the response message
            rs = self.response_map[msg.id]
            del self.response_map[msg.id]
            return rs
