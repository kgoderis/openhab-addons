#!/bin/bash

# Script to convert @Nullable annotations to Optional patterns
# Run this script from the project root directory

echo "Converting @Nullable to Optional patterns in HomeKit solution..."

# List of files that still need conversion
FILES=(
    "src/main/java/org/openhab/io/homekit/event/model/service/HomekitServiceEvent.java"
    "src/main/java/org/openhab/io/homekit/event/model/server/HomekitAccessoryServerEvent.java"
    "src/main/java/org/openhab/io/homekit/event/core/HomekitEventSubscription.java"
    "src/main/java/org/openhab/io/homekit/api/factory/HomekitAccessoryServerFactory.java"
    "src/main/java/org/openhab/io/homekit/config/HomekitConfigurationManager.java"
    "src/main/java/org/openhab/io/homekit/config/HomekitConfigDescriptionProvider.java"
    "src/main/java/org/openhab/io/homekit/provider/HomekitChannelTypeProvider.java"
    "src/main/java/org/openhab/io/homekit/provider/HomekitChannelGroupTypeProvider.java"
    "src/main/java/org/openhab/io/homekit/provider/HomekitThingTypeProvider.java"
    "src/main/java/org/openhab/io/homekit/bridge/HomekitPassthroughBridge.java"
    "src/main/java/org/openhab/io/homekit/bridge/HomekitThingBridge.java"
    "src/main/java/org/openhab/io/homekit/server/HomekitRemoteAccessoryServer.java"
    "src/main/java/org/openhab/io/homekit/event/manager/HomekitEventManager.java"
    "src/main/java/org/openhab/io/homekit/event/util/HomekitEventOriginChecker.java"
)

# Backup function
backup_file() {
    local file="$1"
    if [[ -f "$file" ]]; then
        cp "$file" "$file.backup"
        echo "Backed up: $file"
    fi
}

# Function to add Optional import if not present
add_optional_import() {
    local file="$1"
    if ! grep -q "import java.util.Optional;" "$file"; then
        # Add import after other java.util imports
        sed -i '/import java\.util\./a import java.util.Optional;' "$file"
        echo "Added Optional import to: $file"
    fi
}

# Function to remove @Nullable imports
remove_nullable_import() {
    local file="$1"
    sed -i '/import org\.eclipse\.jdt\.annotation\.Nullable;/d' "$file"
}

# Function to convert @Nullable return types to Optional
convert_return_types() {
    local file="$1"
    
    # Convert @Nullable return types in method signatures
    sed -i 's/@Nullable\s*\([A-Z][A-Za-z0-9_<>[\]]*\)\s*\([a-zA-Z_][a-zA-Z0-9_]*\)\s*(/Optional<\1> \2(/g' "$file"
    
    # Convert specific patterns like "byte @Nullable []"
    sed -i 's/byte @Nullable \[\]/Optional<byte[]>/g' "$file"
}

# Function to convert @Nullable parameters to Optional.ofNullable calls
convert_parameters() {
    local file="$1"
    
    # Convert @Nullable JsonValue parameters
    sed -i 's/@Nullable JsonValue \([a-zA-Z_][a-zA-Z0-9_]*\)/Optional<JsonValue> \1/g' "$file"
    
    # Convert @Nullable UID parameters  
    sed -i 's/@Nullable UID \([a-zA-Z_][a-zA-Z0-9_]*\)/Optional<UID> \1/g' "$file"
    
    # Convert other @Nullable parameters
    sed -i 's/@Nullable \([A-Z][A-Za-z0-9_<>]*\) \([a-zA-Z_][a-zA-Z0-9_]*\)/Optional<\1> \2/g' "$file"
}

# Function to convert fields
convert_fields() {
    local file="$1"
    
    # Convert @Nullable fields to Optional
    sed -i 's/private @Nullable \([A-Z][A-Za-z0-9_<>[\]]*\) \([a-zA-Z_][a-zA-Z0-9_]*\);/private Optional<\1> \2 = Optional.empty();/g' "$file"
}

# Function to convert null checks to Optional patterns
convert_null_checks() {
    local file="$1"
    
    # Note: This is complex and may need manual review
    # Convert simple null checks
    sed -i 's/if (\([a-zA-Z_][a-zA-Z0-9_]*\) != null)/\1.ifPresent(value -> /g' "$file"
    
    # Convert ternary operators
    sed -i 's/\([a-zA-Z_][a-zA-Z0-9_]*\) != null ? \1 : \([a-zA-Z_][a-zA-Z0-9_().]*\)/\1.orElse(\2)/g' "$file"
}

# Main conversion function
convert_file() {
    local file="$1"
    echo "Converting: $file"
    
    if [[ ! -f "$file" ]]; then
        echo "File not found: $file"
        return 1
    fi
    
    backup_file "$file"
    add_optional_import "$file"
    remove_nullable_import "$file"
    convert_return_types "$file"
    convert_parameters "$file"
    convert_fields "$file"
    # convert_null_checks "$file" # Commented out as it needs manual review
    
    echo "Completed: $file"
}

# Convert interface methods in provider classes
convert_provider_interfaces() {
    echo "Converting provider interface methods..."
    
    # Convert Locale parameters from @Nullable to Optional
    for file in src/main/java/org/openhab/io/homekit/provider/*.java; do
        if [[ -f "$file" ]]; then
            backup_file "$file"
            sed -i 's/@Nullable Locale locale/Optional<Locale> locale/g' "$file"
            add_optional_import "$file"
            remove_nullable_import "$file"
            echo "Updated provider: $file"
        fi
    done
}

# Main execution
echo "Starting @Nullable to Optional conversion..."

# Convert individual files
for file in "${FILES[@]}"; do
    convert_file "$file"
done

# Convert provider interfaces
convert_provider_interfaces

echo ""
echo "Conversion completed!"
echo ""
echo "IMPORTANT NOTES:"
echo "1. Review all changes manually as some complex patterns may need adjustment"
echo "2. Update calling code to use Optional.ofNullable() where needed"
echo "3. Replace null parameter passing with Optional.empty()"
echo "4. Test thoroughly to ensure functionality is preserved"
echo "5. Backup files are saved with .backup extension"
echo ""
echo "Common patterns to manually review:"
echo "- Constructor calls that now need Optional.ofNullable()"
echo "- Method calls that pass null parameters"
echo "- Complex null checks that weren't converted automatically"
echo "- Return statements that return null (should return Optional.empty())" 