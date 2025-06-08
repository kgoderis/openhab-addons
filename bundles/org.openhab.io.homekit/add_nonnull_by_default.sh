#!/bin/bash

# Script to add @NonNullByDefault annotation to all Java files

find src/main/java -name "*.java" -type f | while read -r file; do
    # Skip files that already have @NonNullByDefault
    if grep -q "@NonNullByDefault" "$file"; then
        echo "Skipping $file (already has @NonNullByDefault)"
        continue
    fi
    
    # Skip files that don't have class/interface/enum declarations
    if ! grep -q "^\s*public\s\+\(class\|interface\|enum\)" "$file"; then
        echo "Skipping $file (no public class/interface/enum found)"
        continue
    fi
    
    echo "Processing $file"
    
    # Create a temporary file
    temp_file=$(mktemp)
    
    # Flag to track if we've added the import and annotation
    import_added=false
    annotation_added=false
    
    while IFS= read -r line; do
        # Add import after the last existing import or after package declaration
        if ! $import_added && [[ $line =~ ^import\ .* ]] && [[ ! $line =~ NonNullByDefault ]]; then
            echo "$line" >> "$temp_file"
            # Check if this might be the last import line by peeking ahead
            continue
        elif ! $import_added && [[ $line =~ ^package\ .* ]]; then
            echo "$line" >> "$temp_file"
            echo "" >> "$temp_file"
            echo "import org.eclipse.jdt.annotation.NonNullByDefault;" >> "$temp_file"
            import_added=true
            continue
        elif ! $import_added && [[ $line =~ ^$ ]] && grep -q "^import" "$file"; then
            # Empty line after imports - add our import here
            echo "import org.eclipse.jdt.annotation.NonNullByDefault;" >> "$temp_file"
            echo "$line" >> "$temp_file"
            import_added=true
            continue
        fi
        
        # Add annotation before class/interface/enum declaration
        if ! $annotation_added && [[ $line =~ ^public\ +(class|interface|enum) ]]; then
            echo "@NonNullByDefault" >> "$temp_file"
            annotation_added=true
        fi
        
        echo "$line" >> "$temp_file"
        
    done < "$file"
    
    # Only replace the file if we made changes
    if $import_added && $annotation_added; then
        mv "$temp_file" "$file"
        echo "Updated $file"
    else
        rm "$temp_file"
        echo "No changes needed for $file"
    fi
    
done 