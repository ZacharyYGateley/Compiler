;EasyCodeName=Assembly,1

Data Section
    str0	DB	"The ",'"',"condition",'"',":\t true"
    str1	DB	"",10,""
    str2	DB	"The ",'"',"condition",'"'," was false",10,""
    str3	DB	"Check out this equation!",10,""
    str4	DB	" + "
    str5	DB	" = "
    inputHandle	DD	0
    outputHandle	DD	0
    tempGlobal	DD	64 Dup 0

Code Section
start:
    ; Prepare environment for input and output
    ; Get input handle
    Push -10D                           ; Parameter for GetStdHandle
    Call GetStdHandle
    Mov [inputHandle], Eax              ; Save input handle
    ; Get output handle
    Push -11D                           ; Parameter for GetStdHandle
    Call GetStdHandle
    Mov [outputHandle], Eax             ; Save output handle
    
    Sub Esp, 0                          ; Open scope
    ; Prepare if-then-else conditional
    ; If true, go to label0
    ; If false, go to label1
    ; Finally, go to label2
    ; Prepare operand
    Mov Ebx, 0                          ; Clear register for new usage
    Mov Eax, 1D
    Mov Ebx, 0                          ; assemble operand FALSE
    Cmp Ebx, 0                          ; Determine if condition is false
    Jz > label1                         ; If condition is false, jump
    label0:
        Sub Esp, 0                      ; Open scope
        ; Output
        ; Prepare operand
        Mov Ecx, 0                      ; Clear register for new usage
        Mov Eax, 22D
        Mov Ecx, Addr str0              ; assemble operand LITERAL
        ; Caller save registers
        Push Ebx                        ; Anonymous value added to stack
        Push Ecx                        ; Anonymous value added to stack
        Push 0                          ; Parameter for WriteConsoleA
        Push Addr tempGlobal            ; Parameter for WriteConsoleA
        Push Eax                        ; Parameter for WriteConsoleA
        Push Ecx                        ; Parameter for WriteConsoleA
        Push [outputHandle]             ; Parameter for WriteConsoleA
        Call WriteConsoleA
        ; Caller restore registers
        Pop Ecx                         ; Anonymous value removed from stack
        Pop Ebx                         ; Anonymous value removed from stack
        
        ; Output
        ; Prepare operand
        Mov Edx, 0                      ; Clear register for new usage
        Mov Eax, 1D
        Mov Edx, Addr str1              ; assemble operand LITERAL
        ; Caller save registers
        Push Ebx                        ; Anonymous value added to stack
        Push Edx                        ; Anonymous value added to stack
        Push 0                          ; Parameter for WriteConsoleA
        Push Addr tempGlobal            ; Parameter for WriteConsoleA
        Push Eax                        ; Parameter for WriteConsoleA
        Push Edx                        ; Parameter for WriteConsoleA
        Push [outputHandle]             ; Parameter for WriteConsoleA
        Call WriteConsoleA
        ; Caller restore registers
        Pop Edx                         ; Anonymous value removed from stack
        Pop Ebx                         ; Anonymous value removed from stack
        
        Add Esp, 0                      ; Close scope
        
        Jmp label2
        
    label1:
        Sub Esp, 4                      ; Open scope
        ; Output
        ; Prepare operand
        Mov Esi, 0                      ; Clear register for new usage
        Mov Eax, 26D
        Mov Esi, Addr str2              ; assemble operand LITERAL
        ; Caller save registers
        Push Ebx                        ; Anonymous value added to stack
        Push Esi                        ; Anonymous value added to stack
        Push 0                          ; Parameter for WriteConsoleA
        Push Addr tempGlobal            ; Parameter for WriteConsoleA
        Push Eax                        ; Parameter for WriteConsoleA
        Push Esi                        ; Parameter for WriteConsoleA
        Push [outputHandle]             ; Parameter for WriteConsoleA
        Call WriteConsoleA
        ; Caller restore registers
        Pop Esi                         ; Anonymous value removed from stack
        Pop Ebx                         ; Anonymous value removed from stack
        
        ; Output
        ; Prepare operand
        Mov Edi, 0                      ; Clear register for new usage
        Mov Eax, 25D
        Mov Edi, Addr str3              ; assemble operand LITERAL
        ; Caller save registers
        Push Ebx                        ; Anonymous value added to stack
        Push Edi                        ; Anonymous value added to stack
        Push 0                          ; Parameter for WriteConsoleA
        Push Addr tempGlobal            ; Parameter for WriteConsoleA
        Push Eax                        ; Parameter for WriteConsoleA
        Push Edi                        ; Parameter for WriteConsoleA
        Push [outputHandle]             ; Parameter for WriteConsoleA
        Call WriteConsoleA
        ; Caller restore registers
        Pop Edi                         ; Anonymous value removed from stack
        Pop Ebx                         ; Anonymous value removed from stack
        
        ; Store value to b
        Mov Ecx, 0                      ; Clear register for new usage
        ; Prepare operand
        Mov Edx, 0                      ; Clear register for new usage
        Mov Eax, 4D
        Mov Edx, -100D                  ; assemble operand LITERAL
        Mov [Esp + 0], Edx              ; Store value to variable
        
        ; Output
        ; Prepare operand
        Mov Esi, 0                      ; Clear register for new usage
        Mov Eax, 1D
        Mov Esi, [Esp + 0]              ; assemble operand VARIABLE
            ; Convert integer to string in Addr tempGlobal
            ; Caller save registers
            Push Ebx                    ; Anonymous value added to stack
            Push Esi                    ; Anonymous value added to stack
                ; Clear global string Addr tempGlobal
                ; Caller save registers
                Push Ebx                ; Anonymous value added to stack
                Push Esi                ; Anonymous value added to stack
                Push 256                ; Parameter for clear_global_string
                Push Addr tempGlobal    ; Parameter for clear_global_string
                Call clear_global_string
                ; Caller restore registers
                Pop Esi                 ; Anonymous value removed from stack
                Pop Ebx                 ; Anonymous value removed from stack
            Push 11D                    ; Parameter for int_to_string
            Push Addr tempGlobal        ; Parameter for int_to_string
            Push Esi                    ; Parameter for int_to_string
            Call int_to_string
            ; Caller restore registers
            Pop Esi                     ; Anonymous value removed from stack
            Pop Ebx                     ; Anonymous value removed from stack
            Mov Esi, Eax
            Not Esi                     ; Invert actual length
            Add Esi, 1D
            Add Esi, 11D                ; Add total available number of digits
            Add Esi, Addr tempGlobal    ; Positive offset from string pointer at which non-zero values start
        ; Caller save registers
        Push Esi                        ; Anonymous value added to stack
        Push Ebx                        ; Anonymous value added to stack
        Push 0                          ; Parameter for WriteConsoleA
        Push Addr tempGlobal            ; Parameter for WriteConsoleA
        Push Eax                        ; Parameter for WriteConsoleA
        Push Esi                        ; Parameter for WriteConsoleA
        Push [outputHandle]             ; Parameter for WriteConsoleA
        Call WriteConsoleA
        ; Caller restore registers
        Pop Ebx                         ; Anonymous value removed from stack
        Pop Esi                         ; Anonymous value removed from stack
        
        ; Output
        ; Prepare operand
        Mov Edi, 0                      ; Clear register for new usage
        Mov Eax, 3D
        Mov Edi, Addr str4              ; assemble operand LITERAL
        ; Caller save registers
        Push Ebx                        ; Anonymous value added to stack
        Push Edi                        ; Anonymous value added to stack
        Push 0                          ; Parameter for WriteConsoleA
        Push Addr tempGlobal            ; Parameter for WriteConsoleA
        Push Eax                        ; Parameter for WriteConsoleA
        Push Edi                        ; Parameter for WriteConsoleA
        Push [outputHandle]             ; Parameter for WriteConsoleA
        Call WriteConsoleA
        ; Caller restore registers
        Pop Edi                         ; Anonymous value removed from stack
        Pop Ebx                         ; Anonymous value removed from stack
        
        ; Output
        ; Prepare operand
        Mov Ecx, 0                      ; Clear register for new usage
        Mov Eax, 4D
        Mov Ecx, 4D                     ; assemble operand LITERAL
            ; Convert integer to string in Addr tempGlobal
            ; Caller save registers
            Push Ebx                    ; Anonymous value added to stack
            Push Ecx                    ; Anonymous value added to stack
                ; Clear global string Addr tempGlobal
                ; Caller save registers
                Push Ebx                ; Anonymous value added to stack
                Push Ecx                ; Anonymous value added to stack
                Push 256                ; Parameter for clear_global_string
                Push Addr tempGlobal    ; Parameter for clear_global_string
                Call clear_global_string
                ; Caller restore registers
                Pop Ecx                 ; Anonymous value removed from stack
                Pop Ebx                 ; Anonymous value removed from stack
            Push 11D                    ; Parameter for int_to_string
            Push Addr tempGlobal        ; Parameter for int_to_string
            Push Ecx                    ; Parameter for int_to_string
            Call int_to_string
            ; Caller restore registers
            Pop Ecx                     ; Anonymous value removed from stack
            Pop Ebx                     ; Anonymous value removed from stack
            Mov Ecx, Eax
            Not Ecx                     ; Invert actual length
            Add Ecx, 1D
            Add Ecx, 11D                ; Add total available number of digits
            Add Ecx, Addr tempGlobal    ; Positive offset from string pointer at which non-zero values start
        ; Caller save registers
        Push Ecx                        ; Anonymous value added to stack
        Push Ebx                        ; Anonymous value added to stack
        Push 0                          ; Parameter for WriteConsoleA
        Push Addr tempGlobal            ; Parameter for WriteConsoleA
        Push Eax                        ; Parameter for WriteConsoleA
        Push Ecx                        ; Parameter for WriteConsoleA
        Push [outputHandle]             ; Parameter for WriteConsoleA
        Call WriteConsoleA
        ; Caller restore registers
        Pop Ebx                         ; Anonymous value removed from stack
        Pop Ecx                         ; Anonymous value removed from stack
        
        ; Output
        ; Prepare operand
        Mov Edx, 0                      ; Clear register for new usage
        Mov Eax, 3D
        Mov Edx, Addr str5              ; assemble operand LITERAL
        ; Caller save registers
        Push Ebx                        ; Anonymous value added to stack
        Push Edx                        ; Anonymous value added to stack
        Push 0                          ; Parameter for WriteConsoleA
        Push Addr tempGlobal            ; Parameter for WriteConsoleA
        Push Eax                        ; Parameter for WriteConsoleA
        Push Edx                        ; Parameter for WriteConsoleA
        Push [outputHandle]             ; Parameter for WriteConsoleA
        Call WriteConsoleA
        ; Caller restore registers
        Pop Edx                         ; Anonymous value removed from stack
        Pop Ebx                         ; Anonymous value removed from stack
        
        ; Output
        ; Prepare operand
        Mov Esi, 0                      ; Clear register for new usage
        Mov Eax, 4D
        Mov Esi, -96D                   ; assemble operand LITERAL
            ; Convert integer to string in Addr tempGlobal
            ; Caller save registers
            Push Ebx                    ; Anonymous value added to stack
            Push Esi                    ; Anonymous value added to stack
                ; Clear global string Addr tempGlobal
                ; Caller save registers
                Push Ebx                ; Anonymous value added to stack
                Push Esi                ; Anonymous value added to stack
                Push 256                ; Parameter for clear_global_string
                Push Addr tempGlobal    ; Parameter for clear_global_string
                Call clear_global_string
                ; Caller restore registers
                Pop Esi                 ; Anonymous value removed from stack
                Pop Ebx                 ; Anonymous value removed from stack
            Push 11D                    ; Parameter for int_to_string
            Push Addr tempGlobal        ; Parameter for int_to_string
            Push Esi                    ; Parameter for int_to_string
            Call int_to_string
            ; Caller restore registers
            Pop Esi                     ; Anonymous value removed from stack
            Pop Ebx                     ; Anonymous value removed from stack
            Mov Esi, Eax
            Not Esi                     ; Invert actual length
            Add Esi, 1D
            Add Esi, 11D                ; Add total available number of digits
            Add Esi, Addr tempGlobal    ; Positive offset from string pointer at which non-zero values start
        ; Caller save registers
        Push Esi                        ; Anonymous value added to stack
        Push Ebx                        ; Anonymous value added to stack
        Push 0                          ; Parameter for WriteConsoleA
        Push Addr tempGlobal            ; Parameter for WriteConsoleA
        Push Eax                        ; Parameter for WriteConsoleA
        Push Esi                        ; Parameter for WriteConsoleA
        Push [outputHandle]             ; Parameter for WriteConsoleA
        Call WriteConsoleA
        ; Caller restore registers
        Pop Ebx                         ; Anonymous value removed from stack
        Pop Esi                         ; Anonymous value removed from stack
        
        ; Output
        ; Prepare operand
        Mov Edi, 0                      ; Clear register for new usage
        Mov Eax, 1D
        Mov Edi, Addr str1              ; assemble operand LITERAL
        ; Caller save registers
        Push Ebx                        ; Anonymous value added to stack
        Push Edi                        ; Anonymous value added to stack
        Push 0                          ; Parameter for WriteConsoleA
        Push Addr tempGlobal            ; Parameter for WriteConsoleA
        Push Eax                        ; Parameter for WriteConsoleA
        Push Edi                        ; Parameter for WriteConsoleA
        Push [outputHandle]             ; Parameter for WriteConsoleA
        Call WriteConsoleA
        ; Caller restore registers
        Pop Edi                         ; Anonymous value removed from stack
        Pop Ebx                         ; Anonymous value removed from stack
        
        Add Esp, 4                      ; Close scope
        
    
    label2:
    
    Add Esp, 0                          ; Close scope
    
    Ret                                 ; Program finish
    


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

	Mov Eax, Ecx			; actual length - 1
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