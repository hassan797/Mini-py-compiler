.section .data              # Define a section for initialized data
msg:                        # Define a label 'msg' to mark the start of the message
    .ascii "Hello, world!\n"  # Define the ASCII string "Hello, world!\n"

.section .text              # Define a section for executable code
.globl _start              # Declare '_start' as a global symbol, indicating the program's entry point

_start:                     # Entry point of the program
    # Write the message to stdout   
    mov $1, %rax            # Move the syscall number for sys_write (1) into register %rax
    mov $1, %rdi            # Move the file descriptor 1 (stdout) into register %rdi
    lea msg(%rip), %rsi     # Load the effective address of the message (using RIP-relative addressing) into register %rsi
    mov $13, %rdx           # Move the length of the message (13 characters) into register %rdx
    syscall                 # Perform the system call to write the message to stdout

    # Exit the program             
    mov $60, %rax           # Move the syscall number for sys_exit (60) into register %rax
    xor %rdi, %rdi          # XOR register %rdi with itself to set it to 0 (exit status)
    syscall                 # Perform the system call to exit the program
