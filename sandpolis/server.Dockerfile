FROM rust:alpine3.21 AS builder
RUN apk add build-base gcc openssl-dev alsa-lib-dev eudev-dev cmake
WORKDIR /build

COPY . .
RUN cd sandpolis && cargo build --release --features server

FROM alpine:3.21
COPY --from=builder /build/sandpolis/target/release/sandpolis /bin/sandpolis
ENTRYPOINT [ "/bin/sandpolis" ]
