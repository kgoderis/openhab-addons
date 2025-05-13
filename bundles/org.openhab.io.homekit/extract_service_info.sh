#!/bin/bash

# Download ServiceDefinitions.ts if it doesn't exist
if [ ! -f ServiceDefinitions.ts ]; then
    echo "Downloading ServiceDefinitions.ts..."
    curl -o ServiceDefinitions.ts https://raw.githubusercontent.com/homebridge/HAP-NodeJS/refs/heads/latest/src/lib/definitions/ServiceDefinitions.ts
fi

# Create output directory
mkdir -p output

# Extract service information
echo "Extracting service information..."
grep -A 10 "UUID: string = \"" ServiceDefinitions.ts | \
    grep -E "UUID: string = \"|export class" | \
    sed 's/export class //g' | \
    sed 's/UUID: string = "/UUID: /g' | \
    sed 's/";//g' > output/service_info.txt

echo "Service information extracted to output/service_info.txt" 