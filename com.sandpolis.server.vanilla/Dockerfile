FROM adoptopenjdk:13-hotspot

WORKDIR /app

# Install application
COPY build/docker/lib/ /app

EXPOSE 8768

ENTRYPOINT ["java", "-Dpath.lib=/app/libs", "-Dpath.gen=/tmp", "-Dserver.debug_client=false", "-Dnet.ipc.mutex=false", "-Dlogging.net.traffic.raw=false", "-Dlogging.net.traffic.decoded=false", "-Dnet.connection.tls=true", "-Dplugin.enabled=true", "--module-path", "/app", "-m", "com.sandpolis.server.vanilla/com.sandpolis.server.vanilla.Main"]