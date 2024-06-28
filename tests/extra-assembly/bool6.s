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
    # Perform True >= False
    mov $1, %rax            # Load True (1) into %rax
    cmp $0, %rax            # Compare with False (0)
    jge print_true          # Jump to print True if greater than or equal to

    # Print False for True >= False
    jmp print_false         # Jump to print False

print_true:
    # Print True for True >= False
    mov $1, %rax            # System call number for sys_write
    mov $1, %rdi            # File descriptor 1 (STDOUT)
    mov $true_str, %rsi     # Pointer to the true string
    mov $true_len, %rdx     # Length of the true string
    syscall                 # Perform the system call to write to STDOUT
    jmp exit_program        # Jump to exit the program

print_false:
    # Print False for True >= False
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
