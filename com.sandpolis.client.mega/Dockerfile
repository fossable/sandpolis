FROM adoptopenjdk:13-hotspot

WORKDIR /app

# Install application
COPY build/docker/lib/ /app

ENTRYPOINT ["java", "-Dlogging.net.traffic.raw=false", "-Dlogging.net.traffic.decoded=false", "-Dnet.connection.tls=true", "-Dplugin.enabled=true", "-Dnet.ipc.mutex=false", "--module-path", "/app", "-m", "com.sandpolis.client.mega/com.sandpolis.client.mega.Main"]