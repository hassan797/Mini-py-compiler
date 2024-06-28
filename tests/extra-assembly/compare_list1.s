section .data
    list1:  dq 1, 2, 3        ; First list
    list1_len equ $ - list1

    list2:  dq 1, 2, 3        ; Second list (same content as the first list)
    list2_len equ $ - list2

    true_str:  db "True", 0   ; String for "True"
    false_str: db "False", 0  ; String for "False"

section .text
    global main

main:
    ; Compare the lengths of the lists
    mov rax, list1_len      ; Load the length of the first list into rax
    cmp rax, list2_len      ; Compare it with the length of the second list
    jne not_equal           ; If the lengths are not equal, the lists are not equal

    ; Compare the contents of the lists
    mov rcx, list1_len      ; Number of elements to compare
    mov rsi, list1          ; Pointer to the first list
    mov rdi, list2          ; Pointer to the second list
compare_loop:
    mov rax, [rsi]          ; Load an element from the first list
    cmp rax, [rdi]          ; Compare it with the corresponding element from the second list
    jne not_equal           ; If they are not equal, the lists are not equal
    add rsi, 8              ; Move to the next element in the first list
    add rdi, 8              ; Move to the next element in the second list
    loop compare_loop       ; Repeat for the remaining elements
    jmp equal               ; If all elements are equal, the lists are equal

not_equal:
    ; Print "False" (lists are not equal)
    mov rax, 1              ; System call number for sys_write
    mov rdi, 1              ; File descriptor 1 (STDOUT)
    mov rsi, false_str      ; Pointer to the false string
    mov rdx, 6              ; Length of the false string
    syscall                 ; Perform the system call to write to STDOUT
    jmp exit_program        ; Jump to exit the program

equal:
    ; Print "True" (lists are equal)
    mov rax, 1              ; System call number for sys_write
    mov rdi, 1              ; File descriptor 1 (STDOUT)
    mov rsi, true_str       ; Pointer to the true string
    mov rdx, 5              ; Length of the true string
    syscall                 ; Perform the system call to write to STDOUT

exit_program:
    ; Exit the program
    mov rax, 60             ; syscall number for sys_exit
    xor rdi, rdi            ; exit status 0
    syscall                 ; Perform the system call to exit the program
