FROM adoptopenjdk:13-hotspot

WORKDIR /app

# Install application
COPY build/docker/lib/ /app

EXPOSE 8768

ENTRYPOINT ["java", "-Dsandpolis.path.lib=/app/libs", "-Dsandpolis.path.gen=/tmp", "-Dsandpolis.debug.generate_client=false", "-Dsandpolis.ipc.mutex=false", "-Dsandpolis.net.logging.raw=false", "-Dlsandpolis.net.logging.decoded=false", "-Dsandpolis.net.connection.tls=true", "-Dsandpolis.plugins.enabled=true", "--module-path", "/app", "-m", "com.sandpolis.server.vanilla/com.sandpolis.server.vanilla.Main"]