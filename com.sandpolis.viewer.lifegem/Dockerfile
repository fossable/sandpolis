FROM adoptopenjdk:13-hotspot

# Install dependencies
RUN apt-get update && apt-get install -y libgtk-3-0 libglu1-mesa && apt-get clean && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Install application
COPY build/docker/lib/ /app

ENTRYPOINT ["java", "-Dlogging.net.traffic.raw=false", "-Dlogging.net.traffic.decoded=false", "-Dnet.connection.tls=true", "-Dplugin.enabled=true", "-Dnet.ipc.mutex=false", "--module-path", "/app", "-m", "com.sandpolis.viewer.lifegem/com.sandpolis.viewer.lifegem.Main"]