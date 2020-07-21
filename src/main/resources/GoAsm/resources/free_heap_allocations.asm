free_heap_allocations:
	Push Ebp			; Realign base address to first parameter
	Mov Ebp, Esp
	Add Ebp, 8
	
	Push Ebx			; Callee saved
	
	
						; [Ebp]: Heap handle
						; [Ebp + 4]: Address of heap allocation pool
						; W[[Ebp + 4]]: Number of allocations
						; W[[Ebp + 4] + 2]: Allocation capacity
						; D[[Ebp + 4] + 4 * n]: Allocation n
						
	Mov Ebx, [Ebp + 4]	; Address of heap allocation pool
	Xor Cx, Cx
	
	Xor Edx, Edx
	Mov Dx, W[Ebx]		; Number of allocations
	Add Ebx, 4			; First allocation address
	
.loop:
	Cmp Cx, Dx
	Jz > .next
	
	Push Ecx, Edx		; Save local vars
	
	Push [Ebx]			; This allocation address
	Push 0				; Flags
	Push [Ebp]			; Heap handle
	Call HeapFree
	
	Pop Edx, Ecx		; Recall local vars
	
	Add Ebx, 4
	Add Cx, 1

	Jmp < .loop

.next:
	Push [Ebp + 4]		; Free heap pool itself
	Push 0				; Flags
	Push [Ebp]			; Heap handle
	Call HeapFree
	
	Mov Eax, 1

.finally:
	Pop Ebx				; Restore callee saved
	
	Pop Ebp				; Original base pointer
	Pop Edx				; This procedure's return address
	
	Add Esp, 8			; Consume parameters
	
	Push Edx
	
	Ret
	