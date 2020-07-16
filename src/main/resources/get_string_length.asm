get_string_length:
	Push Ebp
	Mov Ebp, Esp
	Add Ebp, 8				; Single parameter at [Esp]
	
	Xor Eax, Eax			; Stores total number of characters

	Mov Ecx, [Ebp]			; Character position in string
							; Loop until this is == \0

.loop:
	Cmp B[Ecx], 0			; Value at storage location == \0?
	Je > .next

	Add Eax, 1				; num digits += 1
	Add Ecx, 1				; next address at +4 bytes

	Jmp < .loop

.next:
	Pop Ebp					; original base pointer
	Pop Edx					; this return address
	Add Esp, 4				; consume parameter
	Push Edx

	Ret						; total # digits in Eax