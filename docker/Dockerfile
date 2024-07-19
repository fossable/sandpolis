FROM rust as build

WORKDIR /build
COPY . .
RUN cargo build --release --all-features

FROM couchdb

COPY --from=build /build/target/release/sandpolis /usr/bin/sandpolis
ENTRYPOINT ["/usr/bin/sandpolis"]
