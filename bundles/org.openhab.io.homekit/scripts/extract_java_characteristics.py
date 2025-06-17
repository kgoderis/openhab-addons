#!/usr/bin/env python3
import os
import re
import json

def extract_characteristics():
    characteristics = []
    java_dir = "src/main/java/org/openhab/io/homekit/library/characteristic"
    
    # Regular expression to match @HomekitCharacteristicType annotations
    pattern = r'@HomekitCharacteristicType\(type\s*=\s*"([^"]+)",\s*name\s*=\s*"([^"]+)"'
    
    for root, _, files in os.walk(java_dir):
        for file in files:
            if file.endswith(".java"):
                with open(os.path.join(root, file), 'r') as f:
                    content = f.read()
                    matches = re.finditer(pattern, content)
                    for match in matches:
                        type_id = match.group(1)
                        name = match.group(2)
                        characteristics.append({
                            "type": type_id,
                            "name": name,
                            "file": file
                        })
    
    # Sort by type ID
    characteristics.sort(key=lambda x: x["type"])
    
    # Write to JSON file
    with open("java_characteristics.json", "w") as f:
        json.dump(characteristics, f, indent=2)
    
    print(f"Found {len(characteristics)} characteristics in Java codebase")
    return characteristics

if __name__ == "__main__":
    extract_characteristics() 