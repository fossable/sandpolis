FROM rust:slim-bullseye as build

WORKDIR /build
COPY sandpolis .
RUN cargo build --release --features server

FROM couchdb
EXPOSE 8768

COPY --from=build /build/target/release/sandpolis /usr/bin/sandpolis-server
ENTRYPOINT ["/usr/bin/sandpolis-server"]
