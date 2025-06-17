#!/usr/bin/env python3
import os
import re
from collections import defaultdict
import uuid

def is_valid_uuid(uuid_str):
    """Check if a string is a valid UUID in the HomeKit format."""
    try:
        # Check if it matches the HomeKit UUID format
        if not re.match(r'^[0-9A-F]{8}-0000-1000-8000-0026BB765291$', uuid_str, re.IGNORECASE):
            return False
        # Try to parse the first 8 characters as a hex number
        int(uuid_str[:8], 16)
        return True
    except ValueError:
        return False

def find_duplicates(java_dir, annotation_regex):
    type_id_map = defaultdict(list)
    invalid_ids = []
    
    for root, _, files in os.walk(java_dir):
        for file in files:
            if file.endswith(".java"):
                with open(os.path.join(root, file), 'r') as f:
                    content = f.read()
                    for match in re.finditer(annotation_regex, content):
                        type_id = match.group(1)
                        class_name = os.path.splitext(file)[0]
                        file_path = os.path.join(root, file)
                        
                        # Check UUID format
                        if not is_valid_uuid(type_id):
                            invalid_ids.append((type_id, class_name, file_path))
                            continue
                            
                        # Extract additional metadata
                        name_match = re.search(r'name\s*=\s*"([^"]+)"', content)
                        tag_match = re.search(r'tag\s*=\s*"([^"]+)"', content)
                        name = name_match.group(1) if name_match else "Unknown"
                        tag = tag_match.group(1) if tag_match else "Unknown"
                        
                        type_id_map[type_id].append({
                            'class_name': class_name,
                            'file_path': file_path,
                            'name': name,
                            'tag': tag
                        })
    
    return type_id_map, invalid_ids

def find_characteristic_usage(service_dir, characteristic_type):
    """Find services that use a specific characteristic type."""
    usage = []
    for root, _, files in os.walk(service_dir):
        for file in files:
            if file.endswith(".java"):
                with open(os.path.join(root, file), 'r') as f:
                    content = f.read()
                    if characteristic_type in content:
                        usage.append(os.path.splitext(file)[0])
    return usage

def print_report(type_id_map, invalid_ids, service_dir=None):
    """Print a detailed report of duplicates and invalid IDs."""
    print("\n=== HomeKit Type ID Validation Report ===\n")
    
    # Print invalid IDs
    if invalid_ids:
        print("Invalid Type IDs:")
        for type_id, class_name, file_path in invalid_ids:
            print(f"  ID: {type_id}")
            print(f"    - Class: {class_name}")
            print(f"    - File: {file_path}")
        print()
    
    # Print duplicates
    duplicates = {tid: infos for tid, infos in type_id_map.items() if len(infos) > 1}
    if duplicates:
        print("Duplicate Type IDs:")
        for tid, infos in duplicates.items():
            print(f"\n  ID: {tid}")
            for info in infos:
                print(f"    - Class: {info['class_name']}")
                print(f"      Name: {info['name']}")
                print(f"      Tag: {info['tag']}")
                print(f"      File: {info['file_path']}")
                
                # If service directory is provided, find usage
                if service_dir:
                    usage = find_characteristic_usage(service_dir, info['class_name'])
                    if usage:
                        print("      Used in services:")
                        for service in usage:
                            print(f"        - {service}")
    else:
        print("No duplicate type IDs found.")
    
    # Print summary
    print("\n=== Summary ===")
    print(f"Total type IDs: {len(type_id_map)}")
    print(f"Duplicate type IDs: {len(duplicates)}")
    print(f"Invalid type IDs: {len(invalid_ids)}")

def main():
    # CharacteristicType
    char_dir = "src/main/java/org/openhab/io/homekit/library/characteristic"
    char_regex = r'@HomekitCharacteristicType\(type\s*=\s*"([^"]+)"'
    char_duplicates, char_invalid = find_duplicates(char_dir, char_regex)
    
    # ServiceType
    svc_dir = "src/main/java/org/openhab/io/homekit/library/service"
    svc_regex = r'@HomekitServiceType\(type\s*=\s*"([^"]+)"'
    svc_duplicates, svc_invalid = find_duplicates(svc_dir, svc_regex)
    
    # Print reports
    print("\n=== Characteristic Types ===")
    print_report(char_duplicates, char_invalid, svc_dir)
    
    print("\n=== Service Types ===")
    print_report(svc_duplicates, svc_invalid)

if __name__ == "__main__":
    main() 