FROM adoptopenjdk:13-hotspot

WORKDIR /app

# Install application
COPY build/docker/lib/ /app

ENTRYPOINT ["java", "-Dsandpolis.ipc.mutex=false", "-Dsandpolis.net.logging.raw=false", "-Dlsandpolis.net.logging.decoded=false", "-Dsandpolis.net.connection.tls=true", "-Dsandpolis.plugins.enabled=true", "--module-path", "/app", "-m", "com.sandpolis.client.mega/com.sandpolis.client.mega.Main"]