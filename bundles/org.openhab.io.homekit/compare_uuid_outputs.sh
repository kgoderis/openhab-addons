#!/bin/bash

JAVA_UUIDS=output/java_uuids.txt
TS_UUIDS=output/ts_uuids.txt
CLASSNAMES=classnames.txt

if [ ! -f "$JAVA_UUIDS" ] || [ ! -f "$TS_UUIDS" ]; then
  echo "Both $JAVA_UUIDS and $TS_UUIDS must exist. Run the extraction scripts first."
  exit 1
fi

if [ ! -f "$CLASSNAMES" ]; then
  echo "Class names file $CLASSNAMES not found. Run extract_classnames.sh first."
  exit 1
fi

lookup_name() {
  uuid="$1"
  # Skip header lines and match UUID in the table
  grep -A 1 "$uuid" "$CLASSNAMES" | grep -v "|--" | grep -v "UUID" | head -n 1 | sed 's/|//g' | awk '{print $1}'
}

echo "Classes in Java but not in TypeScript:"
echo "-----------------------------------"
comm -23 <(sort "$JAVA_UUIDS") <(sort "$TS_UUIDS") | while read -r uuid; do
  name=$(lookup_name "$uuid")
  echo "$name (UUID: $uuid)"
done
echo

echo "Classes in TypeScript but not in Java:"
echo "-----------------------------------"
comm -13 <(sort "$JAVA_UUIDS") <(sort "$TS_UUIDS") | while read -r uuid; do
  name=$(lookup_name "$uuid")
  echo "$name (UUID: $uuid)"
done 