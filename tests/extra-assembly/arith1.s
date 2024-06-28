.text
.globl main

main:
    subq    $16, %rsp            # Adjust stack for alignment

    movq    $format, %rdi       # Load format string address into %rdi
    
    movq    $1, %rax           # Load the first number (1) into %rax
    addq    $2, %rax           # Add the second number (2) to %rax
    
    movq    %rax, %rsi          # Move the result to %rsi for printing
    
    xorq    %rax, %rax          # Clear %rax register (no SSE registers used)
    call    printf               # Call printf function

    xorq    %rax, %rax          # Clear %rax register (return 0)
    addq    $16, %rsp            # Restore stack pointer
    ret                          # Return from main function

.data
format:
    .string "%d\n"          # Format string for printf

