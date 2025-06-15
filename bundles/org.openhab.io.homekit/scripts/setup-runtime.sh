#!/bin/bash

# Configuration
OPENHAB_VERSION="5.0.0-SNAPSHOT"
RUNTIME_DIR="/Users/kgoderis/Development/openhab/runtime"
DOWNLOAD_URL="https://ci.openhab.org/job/openHAB-Distribution/lastSuccessfulBuild/artifact/distributions/openhab/target/openhab-${OPENHAB_VERSION}.zip"

# Create runtime directory if it doesn't exist
mkdir -p "${RUNTIME_DIR}"

# Download OpenHAB distribution
echo "Downloading OpenHAB ${OPENHAB_VERSION}..."
curl -L "${DOWNLOAD_URL}" -o openhab.zip

# Extract the distribution
echo "Extracting OpenHAB to ${RUNTIME_DIR}..."
unzip -q openhab.zip -d "${RUNTIME_DIR}"

# Clean up
rm openhab.zip

# Create necessary directories if they don't exist
mkdir -p "${RUNTIME_DIR}/conf"
mkdir -p "${RUNTIME_DIR}/userdata"
mkdir -p "${RUNTIME_DIR}/addons"

# Create a basic configuration
# cat > "${RUNTIME_DIR}/conf/services/addons.cfg" << EOL
# # Add-on configuration
# package = standard
# binding = 
# ui = 
# transformation = 
# voice = 
# misc = 
# EOL

# Create a basic logging configuration
# cat > "${RUNTIME_DIR}/conf/logback.xml" << EOL
# <?xml version="1.0" encoding="UTF-8"?>
# <configuration>
#     <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
#         <encoder>
#             <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
#         </encoder>
#     </appender>

#     <logger name="org.openhab.io.homekit" level="DEBUG" additivity="false">
#         <appender-ref ref="STDOUT"/>
#     </logger>

#     <root level="INFO">
#         <appender-ref ref="STDOUT"/>
#     </root>
# </configuration>
# EOL

# Make start script executable
chmod +x "${RUNTIME_DIR}/start_debug.sh"

echo "OpenHAB runtime setup complete!"
echo "You can now start OpenHAB in debug mode using: ./${RUNTIME_DIR}/start_debug.sh" 