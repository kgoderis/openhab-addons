#!/usr/bin/env python3
"""
Script to remove redundant method overrides from HomeKit service classes.

This script removes the isExtensible(), isPrimary(), and isHidden() method overrides
from all HomeKit service classes that have been updated to use the builder pattern
in their constructors (.withExtensible(false), .withPrimary(false), .withHidden(false)).
"""

import os
import re
import glob

def remove_redundant_methods(file_path):
    """Remove redundant method overrides from a Java file."""
    print(f"Processing: {file_path}")
    
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original_content = content
    
    # Pattern to match the isExtensible() method with its JavaDoc
    isExtensible_pattern = r'(\s+)/\*\*\s*\n(\s+\*\s*[^\n]*\n)*\s+\*/\s*\n\s+@Override\s*\n\s+public boolean isExtensible\(\) \{\s*\n\s+return false;\s*\n\s+\}\s*\n'
    content = re.sub(isExtensible_pattern, '', content)
    
    # Pattern to match the isPrimary() method override
    isPrimary_pattern = r'(\s+)@Override\s*\n\s+public boolean isPrimary\(\) \{\s*\n\s+return false;\s*\n\s+\}\s*\n'
    content = re.sub(isPrimary_pattern, '', content)
    
    # Pattern to match the isHidden() method override
    isHidden_pattern = r'(\s+)@Override\s*\n\s+public boolean isHidden\(\) \{\s*\n\s+return false;\s*\n\s+\}\s*\n'
    content = re.sub(isHidden_pattern, '', content)
    
    # If the file was modified, write it back
    if content != original_content:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"  ✓ Removed redundant method overrides")
        return True
    else:
        print(f"  - No redundant methods found")
        return False

def main():
    """Main function to process all HomeKit service files."""
    # Get all HomeKit service files
    service_files = glob.glob('src/main/java/org/openhab/io/homekit/library/service/Homekit*Service.java')
    
    print(f"Found {len(service_files)} HomeKit service files")
    print("=" * 50)
    
    modified_count = 0
    
    for file_path in service_files:
        if remove_redundant_methods(file_path):
            modified_count += 1
    
    print("=" * 50)
    print(f"Modified {modified_count} files")
    print("Done!")

if __name__ == "__main__":
    main() 