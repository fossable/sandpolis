FROM adoptopenjdk:13-hotspot

# Install dependencies
RUN apt-get update && apt-get install -y libgtk-3-0 libglu1-mesa && apt-get clean && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Install application
COPY build/docker/lib/ /app

ENTRYPOINT ["java", "-Dsandpolis.path.lib=/app/libs", "-Dsandpolis.path.gen=/tmp", "-Dsandpolis.ipc.mutex=false", "-Dsandpolis.net.logging.raw=false", "-Dlsandpolis.net.logging.decoded=false", "-Dsandpolis.net.connection.tls=true", "-Dsandpolis.plugins.enabled=true", "--module-path", "/app", "-m", "com.sandpolis.viewer.lifegem/com.sandpolis.viewer.lifegem.Main"]