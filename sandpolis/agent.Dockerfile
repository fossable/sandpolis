FROM rust:alpine3.21 AS builder
RUN apk add build-base gcc openssl-dev alsa-lib-dev eudev-dev cmake
WORKDIR /build

COPY . .
RUN cargo build --release --bin sandpolis --features agent

FROM alpine:3.21
COPY --from=builder /build/target/release/sandpolis /bin/sandpolis
ENTRYPOINT [ "/bin/sandpolis" ]
