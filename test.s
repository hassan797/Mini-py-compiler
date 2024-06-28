my_printf:
    pushq %rbp
    movq %rsp, %rbp
    andq $-16, %rsp
    call printf
    movq %rbp, %rsp
    popq %rbp
    ret
my_malloc:
    pushq %rbp
    movq %rsp, %rbp
    andq $-16, %rsp
    call malloc
    movq %rbp, %rsp
    popq %rbp
    ret
.globl main
.type main, @function

main:
    movq $3, %rdi 
	movq $4 , %rsi
	cmpq %rdi , %rdi
	
intFormatString: .string "%ld\n"
