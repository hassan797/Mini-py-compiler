.section .data
true_str:
    .ascii "True\n"         # String for True
true_len = . - true_str    # Length of True string

false_str:
    .ascii "False\n"        # String for False
false_len = . - false_str  # Length of False string

.section .text
.globl main

main:
    # Print True
    mov $1, %rax            # System call number for sys_write
    mov $1, %rdi            # File descriptor 1 (STDOUT)
    mov $true_str, %rsi     # Pointer to the true string
    mov $true_len, %rdx     # Length of the true string
    syscall                 # Perform the system call to write to STDOUT

    # Print False
    mov $1, %rax            # System call number for sys_write
    mov $1, %rdi            # File descriptor 1 (STDOUT)
    mov $false_str, %rsi    # Pointer to the false string
    mov $false_len, %rdx    # Length of the false string
    syscall                 # Perform the system call to write to STDOUT

    # Perform and Boolean operation
    mov $1, %rax            # Load True (1) into %rax
    and $0, %rax            # Perform 'and' operation with False (0)
    
    # Check the result and print accordingly
    test %rax, %rax         # Test if the result is zero
    jz print_false_and      # If zero, jump to print_false_and
    jmp print_true_and      # Otherwise, jump to print_true_and

print_true_and:
    # Print True (result of True and False)
    mov $1, %rax            # System call number for sys_write
    mov $1, %rdi            # File descriptor 1 (STDOUT)
    mov $true_str, %rsi     # Pointer to the true string
    mov $true_len, %rdx     # Length of the true string
    syscall                 # Perform the system call to write to STDOUT
    jmp perform_or          # Jump to perform OR operation

print_false_and:
    # Print False (result of True and False)
    mov $1, %rax            # System call number for sys_write
    mov $1, %rdi            # File descriptor 1 (STDOUT)
    mov $false_str, %rsi    # Pointer to the false string
    mov $false_len, %rdx    # Length of the false string
    syscall                 # Perform the system call to write to STDOUT

perform_or:
    # Perform or Boolean operation
    mov $1, %rax            # Load True (1) into %rax
    or $0, %rax             # Perform 'or' operation with False (0)
    
    # Check the result and print accordingly
    test %rax, %rax         # Test if the result is zero
    jz print_false_or       # If zero, jump to print_false_or
    jmp print_true_or       # Otherwise, jump to print_true_or

print_true_or:
    # Print True (result of True or False)
    mov $1, %rax            # System call number for sys_write
    mov $1, %rdi            # File descriptor 1 (STDOUT)
    mov $true_str, %rsi     # Pointer to the true string
    mov $true_len, %rdx     # Length of the true string
    syscall                 # Perform the system call to write to STDOUT
    jmp exit_program        # Jump to exit the program

print_false_or:
    # Print False (result of True or False)
    mov $1, %rax            # System call number for sys_write
    mov $1, %rdi            # File descriptor 1 (STDOUT)
    mov $false_str, %rsi    # Pointer to the false string
    mov $false_len, %rdx    # Length of the false string
    syscall                 # Perform the system call to write to STDOUT

exit_program:
    # Exit the program
    mov $60, %rax           # syscall number for sys_exit
    xor %rdi, %rdi          # exit status 0
    syscall                 # Perform the system call to exit the program
