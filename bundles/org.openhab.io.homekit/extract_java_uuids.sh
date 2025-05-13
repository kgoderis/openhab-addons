#!/bin/bash

# Create output directory if it doesn't exist
mkdir -p output

# Extract UUIDs from Java files
echo "Extracting UUIDs from Java files..."
grep -r "@HomekitServiceType" --include="*.java" . | \
    grep -o "type = \"[0-9A-F-]\{36\}\"" | \
    sed 's/type = "//g' | \
    sed 's/"//g' | \
    sort -u > output/java_uuids.txt

echo "Found $(wc -l < output/java_uuids.txt) unique UUIDs in Java files"
echo "Results saved to output/java_uuids.txt" 