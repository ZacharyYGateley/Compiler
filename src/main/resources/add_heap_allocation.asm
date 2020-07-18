add_heap_allocation:
	Push Ebp			; Realign base address to first parameter
	Mov Ebp, Esp
	Add Ebp, 8
	
	Push Ebx			; Callee saved
	
						; [Ebp]: Heap handle
						; [Ebp + 4]: Address of heap allocation pool
						; W[[Ebp + 4]]: Number of allocations
						; W[[Ebp + 4] + 2]: Allocation capacity
						; D[[Ebp + 4] + 4 * n]: Allocation n
						; [Ebp + 8]: Number of bytes requested
						
	Mov Ebx, [Ebp + 4]	; Address of heap allocation pool
	Add W[Ebx], 1		; D[Ebx]: High: Number of allocations, Low: Allocation capacity
	Mov Edx, D[Ebx]		; Pull to registers
	
	Cmp Dl, Dh
	Jl > .fail			; Not enough capacity for addition
	
.capable
	Push [Ebp + 8]		; Number of bytes required
	Push 0				; Flags
	Push [Ebp]			; Heap handle
	Call HeapAlloc
	; Newly allocated address in Eax
	Cmp Eax, 0
	Je > .fail
	
	; Add allocated address to heap allocation pool
	; Ebx should have persisted through HeapAlloc
	Xor Edx, Edx
	Mov Dx, W[Ebx]		; Number of allocations, @ first two bytes of heap allocation pool
	IMul Edx, 4D		; Address width, 4 bytes
	Add Edx, [Ebp + 4]	; Address of heap allocation pool
	
	Mov [Edx], Eax		; Newly allocated address to heap allocation pool

	Jmp > .finally

.fail:
	Mov Eax, 0			; Not enough space in heap allocation pool

.finally:
	Pop Ebx				; Restore callee saved
	
	Pop Ebp				; Original base pointer
	Pop Edx				; This procedure's return address
	
	Add Esp, 12D		; Consume parameters
	
	Push Edx			; Restore this procedure's return address
	
	Ret
	