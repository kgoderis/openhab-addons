#!/bin/bash

# Output file
OUTPUT_FILE="ts_uuids.txt"

# Extract class names and UUIDs using awk (POSIX-compatible)
awk '
  /export class/ { class = $3 }
  /public static readonly UUID/ {
    uuid = $NF
    gsub(/"/, "", uuid)
    gsub(/;/, "", uuid)
    print class, uuid
  }
' ServiceDefinitions.ts > "$OUTPUT_FILE"

echo "Extracted UUIDs have been saved to $OUTPUT_FILE" 