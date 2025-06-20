#!/usr/bin/env python3
"""
Script to fix TLV8 characteristic getDefault() methods that throw UnsupportedOperationException.
Replaces the exception with return Map.of() to prevent service creation failures.
"""

import os
import glob

def fix_tlv8_defaults():
    """Fix all TLV8 characteristic getDefault() methods."""
    
    # Find all Java files in the characteristic directory
    characteristic_dir = "src/main/java/org/openhab/io/homekit/library/characteristic"
    java_files = glob.glob(f"{characteristic_dir}/*.java")
    
    fixed_files = []
    
    for file_path in java_files:
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                lines = f.readlines()
            
            # Check if this file has the problematic pattern
            has_issue = False
            for line in lines:
                if "throw new UnsupportedOperationException(\"Default value must be implemented" in line:
                    has_issue = True
                    break
            
            if has_issue:
                # Replace the problematic line
                new_lines = []
                for line in lines:
                    if "throw new UnsupportedOperationException(\"Default value must be implemented" in line:
                        # Replace with return statement
                        new_line = line.replace(
                            'throw new UnsupportedOperationException("Default value must be implemented for the specific device.");',
                            'return Map.of(); // Return empty map for TLV8 characteristics'
                        )
                        new_lines.append(new_line)
                    else:
                        new_lines.append(line)
                
                # Write back the file
                with open(file_path, 'w', encoding='utf-8') as f:
                    f.writelines(new_lines)
                fixed_files.append(file_path)
                print(f"Fixed: {file_path}")
            else:
                print(f"Skipped (no issue): {file_path}")
                
        except Exception as e:
            print(f"Error processing {file_path}: {e}")
    
    print(f"\nTotal files fixed: {len(fixed_files)}")
    return fixed_files

if __name__ == "__main__":
    print("Fixing TLV8 characteristic getDefault() methods...")
    fixed = fix_tlv8_defaults()
    print("Done!") 