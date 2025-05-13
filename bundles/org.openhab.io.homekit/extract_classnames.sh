#!/bin/bash

# Download ServiceDefinitions.ts if it doesn't exist
if [ ! -f ServiceDefinitions.ts ]; then
    echo "Downloading ServiceDefinitions.ts..."
    curl -o ServiceDefinitions.ts https://raw.githubusercontent.com/homebridge/HAP-NodeJS/refs/heads/latest/src/lib/definitions/ServiceDefinitions.ts
fi

# Create output directory if it doesn't exist
mkdir -p output

# Extract class names and their UUIDs
awk '
/export class/ {
    class_name = $3
    while (getline) {
        if ($0 ~ /public static readonly UUID: string = "[0-9A-F-]{36}"/) {
            match($0, /UUID: string = "([0-9A-F-]{36})"/, arr)
            print arr[1] " " class_name
            break
        }
    }
}' ServiceDefinitions.ts > output/classnames.txt

echo "Class names extracted to output/classnames.txt" 