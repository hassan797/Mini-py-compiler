#!/bin/bash

# Check if the correct number of arguments is provided
if [ "$#" -ne 1 ]; then
    echo "Usage: $0 <input_file.s>"
    exit 1
fi

input_file="$1"
output_file="${input_file%.s}"

# Compile the assembly code with gcc
gcc -no-pie -o "$output_file" "$input_file"

# Check if compilation was successful
if [ $? -eq 0 ]; then
    echo "Compilation successful. Executable '$output_file' created."
    ./"$output_file"  # Execute the generated executable
else
    echo "Compilation failed."
    exit 1
fi

rm "$output_file"
