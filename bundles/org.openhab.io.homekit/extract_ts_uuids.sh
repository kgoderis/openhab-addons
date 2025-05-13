#!/bin/bash

# Create output directory if it doesn't exist
mkdir -p output

# Download ServiceDefinitions.ts if it doesn't exist
if [ ! -f ServiceDefinitions.ts ]; then
    echo "Downloading ServiceDefinitions.ts..."
    curl -o ServiceDefinitions.ts https://raw.githubusercontent.com/homebridge/HAP-NodeJS/refs/heads/latest/src/lib/definitions/ServiceDefinitions.ts
fi

# Extract UUIDs from ServiceDefinitions.ts
echo "Extracting UUIDs from ServiceDefinitions.ts..."
grep -o "UUID: string = \"[0-9A-F-]\{36\}\"" ServiceDefinitions.ts | \
    sed "s/UUID: string = \"//g" | \
    sed "s/\"//g" | \
    sort -u > output/ts_uuids.txt

echo "Found $(wc -l < output/ts_uuids.txt) unique UUIDs in ServiceDefinitions.ts"
echo "Results saved to output/ts_uuids.txt" 