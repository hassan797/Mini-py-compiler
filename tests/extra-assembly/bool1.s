.section .data
true_str:
    .ascii "True\n"         # String for true result
true_len = . - true_str    # Length of true string

false_str:
    .ascii "False\n"        # String for false result
false_len = . - false_str  # Length of false string

.section .text
.globl main

main:
    # Check the condition (for example, 1 < 2)
    # If the condition is true, jump to print_true
    # Otherwise, continue to print_false
    mov $1, %rax            # Set %rax to 1
    cmp $2, %rax            # Compare %rax with 2
    jl print_true           # Jump if less than (1 < 2)

print_false:
    # Print "False"
    mov $1, %rax            # System call number for sys_write
    mov $1, %rdi            # File descriptor 1 (STDOUT)
    mov $false_str, %rsi    # Pointer to the false string
    mov $false_len, %rdx    # Length of the false string
    syscall                 # Perform the system call to write to STDOUT
    jmp exit_program        # Jump to exit the program

print_true:
    # Print "True"
    mov $1, %rax            # System call number for sys_write
    mov $1, %rdi            # File descriptor 1 (STDOUT)
    mov $true_str, %rsi     # Pointer to the true string
    mov $true_len, %rdx     # Length of the true string
    syscall                 # Perform the system call to write to STDOUT

exit_program:
    # Exit the program
    mov $60, %rax           # syscall number for sys_exit
    xor %rdi, %rdi          # exit status 0
    syscall                 # Perform the system call to exit the program
