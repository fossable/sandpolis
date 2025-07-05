FROM rust:alpine3.21 AS builder
RUN apk add build-base gcc openssl-dev alsa-lib-dev eudev-dev cmake
WORKDIR /build

COPY . .
RUN cargo +nightly build --release --bin sandpolis --all-features

FROM alpine:3.21
COPY --from=builder /build/target/release/sandpolis /bin/sandpolis
ENTRYPOINT [ "/bin/sandpolis" ]
