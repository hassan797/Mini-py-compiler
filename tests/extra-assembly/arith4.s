.text
.globl main

main:
    subq    $8, %rsp            # Adjust stack for alignment

    movq    $format, %rdi       # Load format string address into %rdi
    
    movq    $19, %rax           # Load the first number (19) into %rax
    movq    $4, %rcx            # Load the second number (4) into %rcx
    xorq    %rdx, %rdx          # Clear %rdx register (no SSE registers used)
    divq    %rcx                # Divide %rax by %rcx, remainder in %rdx
    
    movq    %rdx, %rsi          # Move the remainder to %rsi for printing
    
    xorq    %rax, %rax          # Clear %rax register (no SSE registers used)
    call    printf              # Call printf function

    xorq    %rax, %rax          # Clear %rax register (return 0)
    addq    $8, %rsp            # Restore stack pointer
    ret                         # Return from main function

.data
format:
    .string "%d\n"              # Format string for printf
