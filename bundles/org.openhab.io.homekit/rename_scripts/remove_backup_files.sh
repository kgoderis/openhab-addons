#!/bin/bash

# Find and remove all .java.bak files
find . -name "*.java.bak" -type f -delete

echo "All .java.bak files have been removed." 