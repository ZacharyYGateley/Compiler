get_integer_length:
	Push Ebp
	Mov Ebp, Esp
	Add Ebp, 8				; Single parameter at [Esp]

	Push Ebx				; Callee-saved

	Mov Eax, [Ebp]

	Xor Ecx, Ecx			; Running total number of digits (plus sign)
	Mov Ebx, 10D			; Repeated divisor

	; Get sign of integer
	Cmp Eax, 0
	Jge > get_integer_loop	; positive number jumps
							; negative number continues

	Not Eax					; invert negative number
	Add Ecx, 1				; indicate (-) sign as one digit

get_integer_loop:
	Cmp Eax, 0
	Je > get_integer_next

	Xor Edx, Edx			; MUST CLEAR REMAINDER before divide
	IDiv Ebx
	Add Ecx, 1

	Jmp < get_integer_loop

get_integer_next:
	; Total # digits in Ecx
	Pop Ebx					; original Ebx
	Pop Ebp					; original base pointer
	Pop Edx					; this return address
	Add Esp, 4				; consume parameter
	Push Edx

	Mov Eax, Ecx			; total # digits

	Ret