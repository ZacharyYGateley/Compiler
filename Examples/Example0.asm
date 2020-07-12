;EasyCodeName=Assembly,1

Data Section
	str0	DB	" + "
	str1	DB	" = "
	str2	DB	"",10,""
	str3	DB	"Hello, World"
	inputHandle	DD	0
	outputHandle	DD	0
	tempGlobal	DD	64 Dup 0

Code Section
start:
	; Instruction skipped (SCOPE)
	
	; Instruction skipped (SCOPE)
	
	; Instruction skipped (IF)
	
	; Instruction skipped (TRUE)
	
	; Instruction skipped (SCOPE)
	
	Mov Ebx, 0				; Clear register for new usage
	; Declare new variable b
	Push Ebx
	Mov Ecx, 0				; Clear register for new usage
	Mov Eax, 4D
	Mov Ecx, -100D				; assemble operand LITERAL
	Mov [Esp + 0], Ecx			; Store value to variable
	
	; Instruction skipped (VARIABLE)
	
	; Instruction skipped (LITERAL)
	
	
	; Get input handle
	Push -10D
	Call GetStdHandle
	Mov [inputHandle], Eax		; Save input handle
	
	; Get output handle
	Push -11D
	Call GetStdHandle
	Mov [outputHandle], Eax		; Save output handle
	
	
	Mov Edx, 0				; Clear register for new usage
	Mov Eax, 1D
	Mov Edx, [Esp + 0]				; assemble operand VARIABLE
	; Caller save registers
	Push Edx
	
	; Caller save registers
	Push Edx
	
	Push 256
	Push Addr tempGlobal
	Call clear_global_string
	; Caller restore registers
	Pop Edx
	
	Push 11D
	Push Addr tempGlobal
	Push Edx
	Call int_to_string
	; Caller restore registers
	Pop Edx
	
	Mov Edx, Eax
	; invert actual length
	Not Edx
	Add Edx, 1D
	; add total available number of digits
	Add Edx, 11D  			; positive offset from string pointer at which non-zero values start
	Add Edx, Addr tempGlobal
	; Caller save registers
	Push Edx
	
	Push 0
	Push Addr tempGlobal
	Push Eax
	Push Edx
	Push [outputHandle]
	Call WriteConsoleA			; output value
	; Caller restore registers
	Pop Edx
	
	
	; Instruction skipped (VARIABLE)
	
	Mov Esi, 0				; Clear register for new usage
	Mov Eax, 3D
	Mov Esi, Addr str0				; assemble operand LITERAL
	; Caller save registers
	Push Esi
	
	Push 0
	Push Addr tempGlobal
	Push Eax
	Push Esi
	Push [outputHandle]
	Call WriteConsoleA			; output value
	; Caller restore registers
	Pop Esi
	
	
	; Instruction skipped (LITERAL)
	
	Mov Edi, 0				; Clear register for new usage
	Mov Eax, 4D
	Mov Edi, 4D				; assemble operand LITERAL
	; Caller save registers
	Push Edi
	
	; Caller save registers
	Push Edi
	
	Push 256
	Push Addr tempGlobal
	Call clear_global_string
	; Caller restore registers
	Pop Edi
	
	Push 11D
	Push Addr tempGlobal
	Push Edi
	Call int_to_string
	; Caller restore registers
	Pop Edi
	
	Mov Edi, Eax
	; invert actual length
	Not Edi
	Add Edi, 1D
	; add total available number of digits
	Add Edi, 11D  			; positive offset from string pointer at which non-zero values start
	Add Edi, Addr tempGlobal
	; Caller save registers
	Push Edi
	
	Push 0
	Push Addr tempGlobal
	Push Eax
	Push Edi
	Push [outputHandle]
	Call WriteConsoleA			; output value
	; Caller restore registers
	Pop Edi
	
	
	; Instruction skipped (LITERAL)
	
	Mov Ebx, 0				; Clear register for new usage
	Mov Eax, 3D
	Mov Ebx, Addr str1				; assemble operand LITERAL
	; Caller save registers
	Push Ebx
	
	Push 0
	Push Addr tempGlobal
	Push Eax
	Push Ebx
	Push [outputHandle]
	Call WriteConsoleA			; output value
	; Caller restore registers
	Pop Ebx
	
	
	; Instruction skipped (LITERAL)
	
	Mov Ecx, 0				; Clear register for new usage
	Mov Eax, 4D
	Mov Ecx, -96D				; assemble operand LITERAL
	; Caller save registers
	Push Ecx
	
	; Caller save registers
	Push Ecx
	
	Push 256
	Push Addr tempGlobal
	Call clear_global_string
	; Caller restore registers
	Pop Ecx
	
	Push 11D
	Push Addr tempGlobal
	Push Ecx
	Call int_to_string
	; Caller restore registers
	Pop Ecx
	
	Mov Ecx, Eax
	; invert actual length
	Not Ecx
	Add Ecx, 1D
	; add total available number of digits
	Add Ecx, 11D  			; positive offset from string pointer at which non-zero values start
	Add Ecx, Addr tempGlobal
	; Caller save registers
	Push Ecx
	
	Push 0
	Push Addr tempGlobal
	Push Eax
	Push Ecx
	Push [outputHandle]
	Call WriteConsoleA			; output value
	; Caller restore registers
	Pop Ecx
	
	
	; Instruction skipped (LITERAL)
	
	Mov Edx, 0				; Clear register for new usage
	Mov Eax, 1D
	Mov Edx, Addr str2				; assemble operand LITERAL
	; Caller save registers
	Push Edx
	
	Push 0
	Push Addr tempGlobal
	Push Eax
	Push Edx
	Push [outputHandle]
	Call WriteConsoleA			; output value
	; Caller restore registers
	Pop Edx
	
	
	; Instruction skipped (LITERAL)
	
	Mov Esi, 0				; Clear register for new usage
	Mov Eax, 12D
	Mov Esi, Addr str3				; assemble operand LITERAL
	; Caller save registers
	Push Esi
	
	Push 0
	Push Addr tempGlobal
	Push Eax
	Push Esi
	Push [outputHandle]
	Call WriteConsoleA			; output value
	; Caller restore registers
	Pop Esi
	
	
	; Instruction skipped (LITERAL)
	
	Mov Edi, 0				; Clear register for new usage
	Mov Eax, 1D
	Mov Edi, Addr str2				; assemble operand LITERAL
	; Caller save registers
	Push Edi
	
	Push 0
	Push Addr tempGlobal
	Push Eax
	Push Edi
	Push [outputHandle]
	Call WriteConsoleA			; output value
	; Caller restore registers
	Pop Edi
	
	
	; Instruction skipped (LITERAL)
	
	Ret 				; Program finish
	



;;;;;;; INCLUDED FILE int_to_string.asm ;;;;;;;;



int_to_string:
	; loop through bits of number from least sig to most sig
	; convert digit to character (+ 0x30)
	; place at end of result location

	Push Ebp
	Mov Ebp, Esp
	Add Ebp, 8				; skip return address
							; [ebp] 	first arg 	== number
							; [ebp+4] 	second arg 	== address of data location to store string (e.g. 11 bytes)
							; [ebp+8] 	third arg 	== # of digits to convert at data location sign (e.g. 11)


	Mov Eax, [Ebp]			; number to convert
	Cmp Eax, 0
	Jge > int_to_string_pos

	; store sign for later	; [esp + 4]
int_to_string_neg:
	Push 1   				; negative
	Not Eax
	Add Eax, 1				; invert eax
	Mov [Ebp], Eax			; save as POSTIVE number
							; already have negative sign character
	Jmp int_to_string_signfin

int_to_string_pos:
	Push 0 					; positive
	

int_to_string_signfin:
	Mov Eax, [Ebp + 8]		; # total characters to print, exclusive
	Sub Eax, 1				; inclusive
	Add Eax, [Ebp + 4]		; inclusive last character position
	Push Eax				; [esp] = ptr to highest unused character position

	Mov Eax, [Ebp]			; number to convert

	Xor Ecx, Ecx
	Sub Ecx, 1				; i = -1

int_to_string_loop:
	Add Ecx, 1
	Cmp Ecx, [Ebp + 8]		; for i = 0 to 10
	Jz > int_to_string_addsign

	Mov Ebx, 10D			; divisor
	Xor Edx, Edx			; need to clear remainder before divide
	Mov Esi, [Esp + 4]		; 1==neg, 0==pos
	Cmp Esi, 0
	Je > int_to_string_skipsign
	Cdq						; sign extend into edx
int_to_string_skipsign:
	IDiv Ebx				; result number to eax
							; remainder digit in edx
	Add Edx, 0X30			; convert to ascii digit

	Mov Ebx, [Esp]			; address of last character position
	Mov B[Ebx], Dl			; store digit (low byte) in last character position

	Cmp Eax, 0				; are the remaining digits all 0?
	Jz > int_to_string_addsign			; then have done all of the characters we can

	Sub D[Esp], 1			; decrement last character position by 1 byte

	Jmp < int_to_string_loop	; iterate

int_to_string_addsign:
							; make room for sign
	Sub D[Esp], 1			; character left of last output
	Mov Ebx, [Esp]			; ptr to ptr. Dereference once

	Mov Eax, [Esp + 4D]		; 0==pos, 1==neg
	Cmp Eax, 0
	Je > int_to_string_final

	Mov B[Ebx], 0X2D		; add sign at beg of string
	Add Ecx, 1				; indicate length of string has increased by 1

int_to_string_final:
	Add Esp, 8				; last character position, sign character
	Pop Ebp					; restore base pointer

	Pop Ebx					; Return address
	Add Esp, 12				; Consume parameters
	Push Ebx				; Put return address back into the stack

	Mov Eax, Ecx			; actual length
	Add Eax, 1

	Ret


;;;;;;; INCLUDED FILE clear_global_string.asm ;;;;;;;;



clear_global_string:
	Push Ebp
	Mov Ebp, Esp
	Add Ebp, 8			; Skip old Ebp and Return address
	
						; Param 1 [Ebp + 0 ]: Data location
						; Param 2 [Ebp + 4 ]: Number of bytes to set to 0
	
	Mov Eax, [Ebp]
	Mov Ecx, 0
	Mov Edx, [Ebp + 4]
	
clear_global_string_loop:
	Cmp Ecx, Edx
	Jz > clear_global_string_next
	
	Mov B[Eax], 0
	Add Eax, 4
	Add Ecx, 1
	
	Jmp < clear_global_string_loop

clear_global_string_next:
	Pop Ebp				; Original base pointer
	Pop Ecx				; Return address
	Add Esp, 8			; Consume parameters
	Push Ecx			; Return address belongs in first position in stack
	
	Ret