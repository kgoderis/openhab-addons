#!/bin/bash
# dev-workflow.sh

# Get the workspace root directory
WORKSPACE_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CORE_DIR="${WORKSPACE_ROOT}/openhab-core"
ADDONS_DIR="${WORKSPACE_ROOT}/openhab-addons"
HOMEKIT_DIR="${ADDONS_DIR}/bundles/org.openhab.io.homekit"
RUNTIME_DIR="${HOMEKIT_DIR}/runtime"

# Build core modules
cd "${CORE_DIR}"
mvn clean install -DskipTests

# Build HomeKit bundle
cd "${HOMEKIT_DIR}"
mvn clean install -DskipTests

# Deploy HomeKit bundle
cp target/org.openhab.io.homekit-*.jar "${RUNTIME_DIR}/addons/"

# Start OpenHAB in debug mode
cd "${RUNTIME_DIR}"
./start_debug.sh