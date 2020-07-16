move_memory:
	Push Ebp
	Mov Ebp, Esp
	Add Ebp, 8			; [Ebp + 0] == fromAddress itself
						; [Ebp + 4] == location with toAddress
						; [Ebp + 8] == number of bytes to transfer
						
	Push Ebx			; Callee saved
	
	Mov Eax, [Ebp + 4]	; Location with to address
	Mov Edx, [Ebp + 0]	; Location with from address
	
	Xor Ecx, Ecx
	
.loop:
	Cmp Ecx, [Ebp + 8]
	Jz > .next
	
	Mov Bl, B[Edx]		; Get value of "from" byte
	Mov B[Eax], Bl		; Move value to "to" byte
	
	Add Eax, 1			; Increment "to" byte address
	Add Edx, 1			; Increment "from" byte address
	
	Add Ecx, 1			; Increment iteratation variable
	
	Jmp < .loop

.next:
	Pop Ebx				; Restore callee saved registers
	
	Pop Ebp				; Actual base pointer
	Pop Edx				; This procedure's return location
	Add Esp, 12D		; Consume parameters
	
	Push Edx
	
	Ret