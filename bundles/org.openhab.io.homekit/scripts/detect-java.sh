#!/bin/bash

# Function to check if a directory is a valid Java home
is_valid_java_home() {
    local java_home=$1
    if [ -x "${java_home}/bin/java" ]; then
        return 0
    fi
    return 1
}

# Function to get Java version
get_java_version() {
    local java_home=$1
    "${java_home}/bin/java" -version 2>&1 | awk -F '"' '/version/ {print $2}'
}

# Try to find Java in common locations
find_java_home() {
    # Check JAVA_HOME environment variable first
    if [ -n "$JAVA_HOME" ] && is_valid_java_home "$JAVA_HOME"; then
        echo "$JAVA_HOME"
        return 0
    fi

    # Check common installation locations
    local possible_locations=(
        "/Library/Java/JavaVirtualMachines"
        "/usr/lib/jvm"
        "/opt/java"
        "/usr/java"
    )

    for location in "${possible_locations[@]}"; do
        if [ -d "$location" ]; then
            # Find the most recent Java version
            local java_dir=$(ls -d "$location"/* 2>/dev/null | sort -V | tail -n 1)
            if [ -n "$java_dir" ] && is_valid_java_home "$java_dir"; then
                echo "$java_dir"
                return 0
            fi
        fi
    done

    # Try to find java in PATH
    local java_path=$(which java 2>/dev/null)
    if [ -n "$java_path" ]; then
        local java_home=$(dirname "$(dirname "$java_path")")
        if is_valid_java_home "$java_home"; then
            echo "$java_home"
            return 0
        fi
    fi

    return 1
}

# Main execution
java_home=$(find_java_home)

if [ -z "$java_home" ]; then
    echo "Error: Could not find a valid Java installation" >&2
    exit 1
fi

# Get Java version
java_version=$(get_java_version "$java_home")

# Check if Java version is compatible (Java 11 or higher)
major_version=$(echo "$java_version" | cut -d. -f1)
if [ "$major_version" -lt 11 ]; then
    echo "Error: Java version $java_version is not supported. Please use Java 11 or higher." >&2
    exit 1
fi

# Output the results in a format that can be used by tasks.json
echo "JAVA_HOME=$java_home"
echo "JAVA_VERSION=$java_version" 