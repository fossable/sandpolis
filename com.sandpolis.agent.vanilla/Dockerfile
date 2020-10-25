FROM adoptopenjdk:15-hotspot

# Set application root directory
WORKDIR /app

# Setup environment
ENV SANDPOLIS_IPC_MUTEX             "false"
ENV SANDPOLIS_NET_CONNECTION_TLS    "true"
ENV SANDPOLIS_NET_LOGGING_DECODED   "false"
ENV SANDPOLIS_NET_LOGGING_RAW       "false"
ENV SANDPOLIS_PATH_LIB              "/app/lib"
ENV SANDPOLIS_PATH_PLUGIN           "/app/plugin"
ENV SANDPOLIS_PLUGINS_ENABLED       "true"

# Enable JVM debugging
#ENV JAVA_TOOL_OPTIONS "-agentlib:jdwp=transport=dt_socket,address=0.0.0.0:7000,server=y,suspend=y"

# Install application
COPY build/docker/lib/ /app/lib

ENTRYPOINT ["java", "--module-path", "/app/lib", "-m", "com.sandpolis.agent.vanilla/com.sandpolis.agent.vanilla.Main"]