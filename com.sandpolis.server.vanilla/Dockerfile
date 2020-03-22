FROM adoptopenjdk:14-hotspot

# Set application root directory
WORKDIR /app

# Default listening port
EXPOSE 8768

# Setup environment
ENV SANDPOLIS_DEBUG_GENERATE_CLIENT = "true"
ENV SANDPOLIS_IPC_MUTEX             = "false"
ENV SANDPOLIS_NET_CONNECTION_TLS    = "true"
ENV SANDPOLIS_NET_LOGGING_DECODED   = "false"
ENV SANDPOLIS_NET_LOGGING_RAW       = "false"
ENV SANDPOLIS_PATH_GEN              = "/tmp"
ENV SANDPOLIS_PATH_LIB              = "/app/lib"
ENV SANDPOLIS_PATH_PLUGIN           = "/app/plugin"
ENV SANDPOLIS_PLUGINS_ENABLED       = "true"

# Install application
COPY build/docker/lib/ /app/lib

ENTRYPOINT ["java", "--module-path", "/app/lib", "-m", "com.sandpolis.server.vanilla/com.sandpolis.server.vanilla.Main"]