#!/bin/bash

JAVA_UUIDS=output/java_uuids.txt
TS_UUIDS=output/ts_uuids.txt
CLASSNAMES=classnames.txt

if [ ! -f "$JAVA_UUIDS" ] || [ ! -f "$TS_UUIDS" ] || [ ! -f "$CLASSNAMES" ]; then
  echo "All of $JAVA_UUIDS, $TS_UUIDS, and $CLASSNAMES must exist. Run the extraction scripts first."
  exit 1
fi

# Download ServiceDefinitions.ts if it doesn't exist
if [ ! -f ServiceDefinitions.ts ]; then
    echo "Downloading ServiceDefinitions.ts..."
    curl -o ServiceDefinitions.ts https://raw.githubusercontent.com/homebridge/HAP-NodeJS/refs/heads/latest/src/lib/definitions/ServiceDefinitions.ts
fi

# Create a temporary file with UUID to name mapping from classnames.txt
# Skip the header lines and process the table content
awk '
BEGIN { FS = "|" }
NR > 2 {  # Skip header and separator lines
    gsub(/^[ \t]+|[ \t]+$/, "", $2)  # Trim whitespace from class name
    gsub(/^[ \t]+|[ \t]+$/, "", $3)  # Trim whitespace from UUID
    if ($2 != "" && $3 != "") {
        print $3 " " $2
    }
}
' "$CLASSNAMES" > output/service_names.txt

echo "UUIDs in Java but not in TypeScript:"
echo "-----------------------------------"
while read -r uuid; do
    name=$(grep "^$uuid " output/service_names.txt | awk '{print $2}')
    if [ -n "$name" ]; then
        echo "$uuid - $name"
    else
        echo "$uuid - (Unknown)"
    fi
done < <(comm -23 <(sort "$JAVA_UUIDS") <(sort "$TS_UUIDS"))
echo

echo "UUIDs in TypeScript but not in Java:"
echo "-----------------------------------"
while read -r uuid; do
    name=$(grep "^$uuid " output/service_names.txt | awk '{print $2}')
    if [ -n "$name" ]; then
        echo "$uuid - $name"
    else
        echo "$uuid - (Unknown)"
    fi
done < <(comm -13 <(sort "$JAVA_UUIDS") <(sort "$TS_UUIDS")) 