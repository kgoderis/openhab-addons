import os
import re

# Directory to start the traversal
root_dir = 'src/main/java'

# Function to add imports to a file
def add_imports(file_path):
    with open(file_path, 'r') as file:
        content = file.read()

    # Check if the file defines a Logger
    if 'private static final Logger logger =' in content or 'private final Logger logger =' in content:
        # Check if the imports are already present
        if 'import org.slf4j.Logger;' not in content and 'import org.slf4j.LoggerFactory;' not in content:
            # Add the imports after the package declaration
            content = re.sub(r'(package .*;\n)', r'\1\nimport org.slf4j.Logger;\nimport org.slf4j.LoggerFactory;\n', content)

            # Write the updated content back to the file
            with open(file_path, 'w') as file:
                file.write(content)
            print(f"Imports added to {file_path}.")
        else:
            print(f"Imports already present in {file_path}.")
    else:
        print(f"No Logger defined in {file_path}.")

# Traverse the directory
for root, dirs, files in os.walk(root_dir):
    for file in files:
        if file.endswith('.java'):
            file_path = os.path.join(root, file)
            add_imports(file_path) 