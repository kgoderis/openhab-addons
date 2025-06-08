#!/usr/bin/env python3

import os
import re
import sys

def process_java_file(filepath):
    """Process a single Java file to add @NonNullByDefault annotation"""
    
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Skip if already has @NonNullByDefault
    if '@NonNullByDefault' in content:
        print(f"Skipping {filepath} (already has @NonNullByDefault)")
        return False
    
    # Enhanced regex to handle annotations, final classes, etc.
    if not re.search(r'^public\s+(abstract\s+|final\s+)?(class|interface|enum|@interface)', content, re.MULTILINE):
        print(f"Skipping {filepath} (no public class/interface/enum/@interface)")
        return False
    
    print(f"Processing {filepath}")
    
    lines = content.split('\n')
    new_lines = []
    import_added = False
    annotation_added = False
    
    for i, line in enumerate(lines):
        # Add import after package declaration or before first import
        if not import_added:
            if line.startswith('package '):
                new_lines.append(line)
                new_lines.append('')
                new_lines.append('import org.eclipse.jdt.annotation.NonNullByDefault;')
                import_added = True
                continue
            elif line.startswith('import ') and 'NonNullByDefault' not in line:
                # Add before first import
                new_lines.append('import org.eclipse.jdt.annotation.NonNullByDefault;')
                new_lines.append(line)
                import_added = True
                continue
        
        # Add annotation before class/interface/enum/@interface declaration
        if not annotation_added and re.match(r'^public\s+(abstract\s+|final\s+)?(class|interface|enum|@interface)', line):
            new_lines.append('@NonNullByDefault')
            annotation_added = True
        
        new_lines.append(line)
    
    # If we made changes, write the file
    if import_added and annotation_added:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write('\n'.join(new_lines))
        print(f"Updated {filepath}")
        return True
    else:
        print(f"No changes made to {filepath}")
        return False

def main():
    """Process the remaining files that were skipped"""
    # Files that were skipped by the main script
    remaining_files = [
        'src/main/java/org/openhab/io/homekit/util/HomekitReadyMarkers.java',
        'src/main/java/org/openhab/io/homekit/api/characteristic/HomekitCharacteristicType.java',
        'src/main/java/org/openhab/io/homekit/api/characteristic/BindingMapping.java',
        'src/main/java/org/openhab/io/homekit/api/service/HomekitServiceType.java',
        'src/main/java/org/openhab/io/homekit/api/accessory/HomekitAccessoryType.java'
    ]
    
    updated_count = 0
    total_count = 0
    
    for filepath in remaining_files:
        if os.path.exists(filepath):
            total_count += 1
            if process_java_file(filepath):
                updated_count += 1
        else:
            print(f"File not found: {filepath}")
    
    print(f"\nProcessed {total_count} remaining files, updated {updated_count} files")

if __name__ == '__main__':
    main() 