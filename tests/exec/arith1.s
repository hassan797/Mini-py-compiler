	.text
my_printf:
	pushq %rbp
	movq %rsp, %rbp
	andq $-16, %rsp
	call printf
	leave
	ret
my_malloc:
	pushq %rbp
	movq %rsp, %rbp
	call malloc
	leave
	ret
.globl mainblock
.type mainblock, @function
mainblock:
	pushq %rbp
	movq %rsp, %rbp

	movq $16, %rdi
	call my_malloc
	movq $2, (%rax)
	movq $1, 8(%rax)
	pushq %rax

	movq $16, %rdi
	call my_malloc
	movq $2, (%rax)
	movq $2, 8(%rax)
	pushq %rax

	popq %rdi
	popq %rsi
	movq 0(%rdi), %r8
	cmpq $0, %r8
	je type_check_end
	cmpq $1, %r8
	je type_check_end
	cmpq $2, %r8
	je type_check_end
	cmpq $3, %r8
	je type_check_end
	cmpq $4, %r8
	je type_check_end
	jmp type_mismatch
type_check_end:
	movq 0(%rsi), %r9
	cmpq $0, %r9
	je type_check_end
	cmpq $1, %r9
	je type_check_end
	cmpq $2, %r9
	je type_check_end
	cmpq $3, %r9
	je type_check_end
	cmpq $4, %r9
	je type_check_end
	jmp type_mismatch
type_check_end:
	movq 8(%rsi), %rax
	movq 8(%rdi), %rbx
	addq %rbx, %rax



	movq %rbp, %rsp
	popq %rbp
	ret

	
	.data
