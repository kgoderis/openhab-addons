#!/usr/bin/env python3
import json
from collections import defaultdict

def compare_characteristics():
    # Load characteristics from both sources
    with open("java_characteristics.json", "r") as f:
        java_chars = json.load(f)
    
    with open("nodejs_characteristics.json", "r") as f:
        nodejs_chars = json.load(f)
    
    # Create sets of type IDs
    java_types = {char["type"] for char in java_chars}
    nodejs_types = {char["type"] for char in nodejs_chars}
    
    # Find differences
    only_in_java = java_types - nodejs_types
    only_in_nodejs = nodejs_types - java_types
    in_both = java_types & nodejs_types
    
    # Create detailed comparison
    comparison = {
        "only_in_java": [
            {
                "type": char["type"],
                "name": char["name"],
                "file": char["file"]
            }
            for char in java_chars
            if char["type"] in only_in_java
        ],
        "only_in_nodejs": [
            {
                "type": char["type"],
                "name": char["name"]
            }
            for char in nodejs_chars
            if char["type"] in only_in_nodejs
        ],
        "in_both": [
            {
                "type": char["type"],
                "java_name": next(c["name"] for c in java_chars if c["type"] == char["type"]),
                "nodejs_name": next(c["name"] for c in nodejs_chars if c["type"] == char["type"])
            }
            for char in java_chars
            if char["type"] in in_both
        ]
    }
    
    # Write comparison to JSON file
    with open("characteristic_comparison.json", "w") as f:
        json.dump(comparison, f, indent=2)
    
    # Print summary
    print("\nComparison Summary:")
    print(f"Total characteristics in Java: {len(java_chars)}")
    print(f"Total characteristics in HAP-NodeJS: {len(nodejs_chars)}")
    print(f"Characteristics only in Java: {len(only_in_java)}")
    print(f"Characteristics only in HAP-NodeJS: {len(only_in_nodejs)}")
    print(f"Characteristics in both: {len(in_both)}")
    
    return comparison

if __name__ == "__main__":
    compare_characteristics() 