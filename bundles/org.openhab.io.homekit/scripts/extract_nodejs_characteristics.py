#!/usr/bin/env python3
import requests
import re
import json

def extract_nodejs_characteristics():
    # URL of the HAP-NodeJS CharacteristicDefinitions.ts file
    url = "https://raw.githubusercontent.com/homebridge/HAP-NodeJS/60502a0afac9ab8d7e9c804e034dfdf0ce06ed57/src/lib/definitions/CharacteristicDefinitions.ts"
    response = requests.get(url)
    
    if response.status_code != 200:
        print(f"Failed to fetch HAP-NodeJS characteristics: {response.status_code}")
        return []
    
    content = response.text
    
    # Regex to match: export class <Name> extends Characteristic { public static readonly UUID: string = "<UUID>"
    pattern = r'export class (\w+) extends Characteristic \{[^}]*?public static readonly UUID: string = "([^"]+)";'
    
    characteristics = []
    for match in re.finditer(pattern, content, re.DOTALL):
        name = match.group(1)
        uuid = match.group(2)
        characteristics.append({
            "type": uuid,
            "name": name
        })
    
    # Sort by type ID
    characteristics.sort(key=lambda x: x["type"])
    
    # Write to JSON file
    with open("nodejs_characteristics.json", "w") as f:
        json.dump(characteristics, f, indent=2)
    
    print(f"Found {len(characteristics)} characteristics in HAP-NodeJS")
    return characteristics

if __name__ == "__main__":
    extract_nodejs_characteristics() 