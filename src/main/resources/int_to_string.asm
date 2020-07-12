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

	Mov Eax, Ecx			; actual length - 1
	Add Eax, 1

	Ret