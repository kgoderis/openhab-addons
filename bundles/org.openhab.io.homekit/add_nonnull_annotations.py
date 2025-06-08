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
    
    # Skip if no public class/interface/enum
    if not re.search(r'^public\s+(abstract\s+)?(class|interface|enum)', content, re.MULTILINE):
        print(f"Skipping {filepath} (no public class/interface/enum)")
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
        
        # Add annotation before class/interface/enum declaration
        if not annotation_added and re.match(r'^public\s+(abstract\s+)?(class|interface|enum)', line):
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
    """Main function to process all Java files"""
    updated_count = 0
    total_count = 0
    
    # Find all Java files
    for root, dirs, files in os.walk('src/main/java'):
        for file in files:
            if file.endswith('.java'):
                filepath = os.path.join(root, file)
                total_count += 1
                if process_java_file(filepath):
                    updated_count += 1
    
    print(f"\nProcessed {total_count} Java files, updated {updated_count} files")

if __name__ == '__main__':
    main() 