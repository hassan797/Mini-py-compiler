.section .data
format_row:
    .string "%d "            # Format string for row elements
    format_row_len = . - format_row

format_badmatrix:
    .string "[%d, %d, %d] "  # Format string for matrix rows
    format_badmatrix_len = . - format_badmatrix

row:
    .quad 1, 2, 3             # Initialize a row array

.globl main

main:
    subq    $8, %rsp          # Adjust stack for alignment

    # Print row [1, 2, 3]
    movq    $format_row, %rdi # Load format string address into %rdi
    movq    $row, %rsi        # Load the row array address into %rsi
    movq    $3, %rdx          # Load the length of the row
    call    printf             # Call printf function

    # Print matrix [[1, 2, 3], [1, 2, 3], [1, 2, 3]]
    movq    $format_badmatrix, %rdi # Load format string address into %rdi
    movq    $row, %rsi        # Load the row array address into %rsi
    movq    $3, %rdx          # Load the length of the row
print_matrix_loop:
    call    printf             # Call printf function
    decq    %rdx              # Decrement the counter
    jnz     print_matrix_loop # Jump back if counter is not zero

    # Modify matrix
    movq    $42, row+8        # Modify the second element of the first row

    # Print modified matrix
    movq    $format_badmatrix, %rdi # Load format string address into %rdi
    movq    $row, %rsi        # Load the row array address into %rsi
    movq    $3, %rdx          # Load the length of the row
print_modified_matrix_loop:
    call    printf             # Call printf function
    decq    %rdx              # Decrement the counter
    jnz     print_modified_matrix_loop # Jump back if counter is not zero

    addq    $8, %rsp          # Restore stack pointer
    xorq    %rax, %rax        # Clear %rax register (return 0)
    ret                        # Return from main function
