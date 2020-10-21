FROM adoptopenjdk:15-hotspot

# Set application root directory
WORKDIR /app

# Default listening port
EXPOSE 8768

# Setup environment
ENV SANDPOLIS_STORAGE_PROVIDER      "ephemeral"
ENV SANDPOLIS_DEBUG_GENERATE_CLIENT "true"
ENV SANDPOLIS_NET_CONNECTION_TLS    "true"
ENV SANDPOLIS_NET_LOGGING_DECODED   "false"
ENV SANDPOLIS_NET_LOGGING_RAW       "false"
ENV SANDPOLIS_PATH_GEN              "/tmp"
ENV SANDPOLIS_PATH_LIB              "/app/lib"
ENV SANDPOLIS_PATH_PLUGIN           "/app/plugin"
ENV SANDPOLIS_PLUGINS_ENABLED       "true"

# Enable JVM debugging
#ENV JAVA_TOOL_OPTIONS "-agentlib:jdwp=transport=dt_socket,address=0.0.0.0:7000,server=y,suspend=y"

# Install application
COPY build/docker/lib/ /app/lib

# Module-path invocation
ENTRYPOINT ["java", "--module-path", "/app/lib", "-m", "com.sandpolis.server.vanilla/com.sandpolis.server.vanilla.Main"]

# Class-path invocation
#ENTRYPOINT ["java", "-cp", "/app/lib/*", "com.sandpolis.server.vanilla.Main"]