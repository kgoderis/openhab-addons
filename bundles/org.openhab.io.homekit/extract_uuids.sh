#!/bin/bash

# Extract UUIDs from Java files
grep -o 'type = "[0-9A-Fa-f\-]\+"' src/main/java/org/openhab/io/homekit/library/characteristic/*.java | sed 's/.*type = "\([0-9A-Fa-f\-]\+\)"/\1/' | sort | uniq > implemented_uuids.txt

# Clean up the file to ensure only raw UUIDs are present and sort them
grep -Eo '[0-9A-Fa-f\-]{36}' implemented_uuids.txt | sort > implemented_uuids_clean.txt && mv implemented_uuids_clean.txt implemented_uuids.txt

echo "UUIDs have been extracted and saved to implemented_uuids.txt" 

# Download the CharacteristicDefinitions.ts file
curl -s https://raw.githubusercontent.com/homebridge/HAP-NodeJS/4eb81ee52934b5f312ef470440a0947542b6fd5b/src/lib/definitions/CharacteristicDefinitions.ts > CharacteristicDefinitions.ts

# Extract UUIDs from the file and strip prefixes
grep -o 'UUID: string = \"[0-9A-Fa-f\-]\+\"' CharacteristicDefinitions.ts | sed 's/UUID: string = \"\([0-9A-Fa-f\-]\+\)\"/\1/' | sort | uniq > official_uuids.txt

# Clean up the file to ensure only raw UUIDs are present
grep -Eo '[0-9A-Fa-f\-]{36}' official_uuids.txt > official_uuids_clean.txt && mv official_uuids_clean.txt official_uuids.txt

echo "UUIDs have been extracted and saved to official_uuids.txt" 

# Compare the two UUID lists and find missing UUIDs
comm -23 official_uuids.txt implemented_uuids.txt > missing_uuids.txt

echo "Missing UUIDs have been saved to missing_uuids.txt" 

# Ensure CharacteristicDefinitions.ts is present
if [ ! -f CharacteristicDefinitions.ts ]; then
  echo "CharacteristicDefinitions.ts not found! Downloading..."
  curl -s https://raw.githubusercontent.com/homebridge/HAP-NodeJS/4eb81ee52934b5f312ef470440a0947542b6fd5b/src/lib/definitions/CharacteristicDefinitions.ts -o CharacteristicDefinitions.ts
fi

# Extract UUID and Name pairs from CharacteristicDefinitions.ts
awk 'BEGIN{uuid="";name=""} /export class /{name=$3} /UUID: string = /{if(name!=""){if(match($0, /"[0-9A-Fa-f\-]{36}"/)){uuid=substr($0, RSTART+1, RLENGTH-2); print uuid " " name; name=""}}}' CharacteristicDefinitions.ts | sort > uuid_name_map.txt

# Map missing UUIDs to names
while read uuid; do
  grep "^$uuid " uuid_name_map.txt || echo "$uuid UNKNOWN";
done < missing_uuids.txt > missing_uuids_with_names.txt

echo "Output written to missing_uuids_with_names.txt" 