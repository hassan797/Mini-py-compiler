#include <stdio.h>
#include <stdlib.h>
#include <string.h>

int main() {
    // Allocate memory for the integer, the length, and the string
    char *ptr = malloc(sizeof(int) + sizeof(int) + strlen("Test") + 1); // +1 for null terminator
    if (ptr == NULL) {
        fprintf(stderr, "Memory allocation failed\n");
        return 1;
    }

    // Assign integer value
    *(int *)ptr = 3;
    ptr += sizeof(int); // Move pointer past the integer

    // Assign length of the string
    *(int *)ptr = strlen("Test");
    ptr += sizeof(int); // Move pointer past the length

    // Copy string into memory
    strcpy(ptr, "Test");

    // Access values
    int num = *(int *)(ptr - sizeof(int) - sizeof(int)); // Get the integer value
    int len = *(int *)(ptr - sizeof(int)); // Get the length
    char *str = ptr; // Get the string

    printf("%s\n", str);

    return 0;
}
