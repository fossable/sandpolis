FROM eclipse-temurin:17

# Set application directory
WORKDIR /app

# Set application entry
ENTRYPOINT ["java", "--module-path", "/app/lib", "-m", "org.s7s.instance.server.java/org.s7s.instance.server.java.Main"]

# Default listening port
EXPOSE 8768

# Set environment
ENV S7S_RUNTIME_RESIDENCY     "container"
ENV S7S_PATH_GEN              "/tmp"
ENV S7S_PATH_LIB              "/app/lib"
ENV S7S_PATH_PLUGIN           "/app/plugin"

# Install application
COPY build/lib /app/lib
