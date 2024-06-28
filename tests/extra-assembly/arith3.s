.text
.globl main

main:
    subq    $8, %rsp            # Adjust stack for alignment
    movq    $format, %rdi       # Load format string address into %rdi
    movq    $8, %rsi            # Load the result (17//2) into %rsi
    xorq    %rax, %rax          # Clear %rax register (no SSE registers used)
    call    printf               # Call printf function
    xorq    %rax, %rax          # Clear %rax register (return 0)
    addq    $8, %rsp            # Restore stack pointer
    ret                          # Return from main function

.data
format:
    .string "%d\n"          # Format string for printf
