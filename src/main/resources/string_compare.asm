string_compare:
	; parameters:
	;		string 1 address (string ends in \0)
	;		strign 2 address (string ends in \0)
	Push Ebp
	Mov Ebp, Esp
	Add Ebp, 8
	
	Mov Ecx, [Ebp]			; string 1 address
	Mov Edx, [Ebp + 4]		; string 2 address
	
.loop:
	Mov Al, B[Ecx]			; actual value of string 1 byte
	Cmp Al, B[Edx]			; actual value of string 2 byte
	Jne > .not_equivalent
	
	Cmp Eax, 0
	Je > .equivalent
	
	Add Ecx, 1				; increment bytes
	Add Edx, 1
	
	Jmp < .loop

.not_equivalent:
	Xor Eax, Eax
	Jmp > .finally
	
.equivalent:
	Mov Eax, 1
	
.finally:
	Pop Ebp					; original base pointer
	Pop Ecx					; this procedure's return address
	
	Add Esp, 8				; consume parameters
	Push Ecx
	
	Ret