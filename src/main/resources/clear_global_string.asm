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