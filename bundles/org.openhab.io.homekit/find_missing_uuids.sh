#!/bin/bash

# Create temporary files
JAVA_UUIDS=$(mktemp)
TS_UUIDS=$(mktemp)
JAVA_UUIDS_NAMES=$(mktemp)
TS_UUIDS_NAMES=$(mktemp)

# Extract UUIDs and names from Java files
find . -name "*.java" -type f -exec grep -h "@HomekitServiceType" {} \; | \
    sed -n 's/.*type = "\([0-9A-F-]\{36\}\)".*name = "\([^\"]*\)".*/\1 \2/p' | \
    sort -u > "$JAVA_UUIDS_NAMES"
cut -d' ' -f1 "$JAVA_UUIDS_NAMES" | sort -u > "$JAVA_UUIDS"

# Download ServiceDefinitions.ts
curl -s https://raw.githubusercontent.com/homebridge/HAP-NodeJS/refs/heads/latest/src/lib/definitions/ServiceDefinitions.ts > ServiceDefinitions.ts

# Extract UUIDs and class names using a robust regex match
awk '
/export class/ { class = $3 }
/UUID:/ {
    if (match($0, /UUID:[^A-Za-z0-9]*"([0-9A-F-]{36})"/, arr) && class != "") {
        print arr[1], class
    }
}
' ServiceDefinitions.ts | sort -u > "$TS_UUIDS_NAMES"

cut -d' ' -f1 "$TS_UUIDS_NAMES" | sort -u > "$TS_UUIDS"

echo "DEBUG: Content of TS_UUIDS_NAMES:"
cat "$TS_UUIDS_NAMES"

echo -e "\nDEBUG: Content of JAVA_UUIDS_NAMES:"
cat "$JAVA_UUIDS_NAMES"

echo -e "\nServices in Java files but not in ServiceDefinitions.ts:"
comm -23 "$JAVA_UUIDS" "$TS_UUIDS" | while read uuid; do
    grep "^$uuid " "$JAVA_UUIDS_NAMES"
done

echo -e "\nServices in ServiceDefinitions.ts but not in Java files:"
comm -13 "$JAVA_UUIDS" "$TS_UUIDS" | while read uuid; do
    grep "^$uuid " "$TS_UUIDS_NAMES"
done

# New section: Look up UUIDs for Services in ServiceDefinitions.ts but not in Java files
echo -e "\nUUIDs for Services in ServiceDefinitions.ts but not in Java files:"
while read -r line; do
    uuid=$(echo "$line" | cut -d' ' -f1)
    if ! grep -q "^$uuid " "$JAVA_UUIDS_NAMES"; then
        echo "$line"
    fi
done < "$TS_UUIDS_NAMES"

# Clean up
rm "$JAVA_UUIDS" "$TS_UUIDS" "$JAVA_UUIDS_NAMES" "$TS_UUIDS_NAMES" ServiceDefinitions.ts 