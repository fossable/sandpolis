FROM adoptopenjdk:14-hotspot

# Set application root directory
WORKDIR /app

# Install dependencies
RUN apt-get update && apt-get install -y libgtk-3-0 libglu1-mesa && apt-get clean && rm -rf /var/lib/apt/lists/*

# Setup environment
ENV SANDPOLIS_IPC_MUTEX             "false"
ENV SANDPOLIS_NET_CONNECTION_TLS    "true"
ENV SANDPOLIS_NET_LOGGING_DECODED   "false"
ENV SANDPOLIS_NET_LOGGING_RAW       "false"
ENV SANDPOLIS_PATH_LIB              "/app/lib"
ENV SANDPOLIS_PATH_PLUGIN           "/app/plugin"
ENV SANDPOLIS_PLUGINS_ENABLED       "true"

# Install application
COPY build/docker/lib/ /app/lib

ENTRYPOINT ["java", "--module-path", "/app/lib", "-m", "com.sandpolis.viewer.lifegem/com.sandpolis.viewer.lifegem.Main"]