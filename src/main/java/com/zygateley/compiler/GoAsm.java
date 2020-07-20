package com.zygateley.compiler;

import java.io.*;
import java.lang.Exception;
import java.util.*;

import com.zygateley.compiler.Assembler.Writer;

public class GoAsm extends AssyLanguage {
	private ArrayList<String> resources = new ArrayList<>();
	private int parameterCount = 0;
	
	public GoAsm(Writer io, SymbolTable symbolTable) {
		super(io, symbolTable, new String[] { "Ebx", "Ecx", "Edx", "Esi", "Edi" });
	}
	
	public void addResource(String procedure) {
		String resource = procedure + ".asm";
		if (!resources.contains(resource)) {
			resources.add(resource);
		}
	}
	

	
	@Override
	public Register assembleBooleanOperation(Construct type, Variable variable0, Variable variable1) throws Exception {
		Register r0 = this.getRegister(variable0);
		Register r1 = null;
		if (variable1 == null) {
			// Unary operator
			switch (type) {
			case NOT:
				// Keep in original register
				io.setComment("Execute Not %s", r0);
				io.println("NOT %s", r0);
				break;
			default:
				break;
			}
			return r0;
		}
		else {
			r1 = this.getRegister(variable1);
		}
		Register r2 = this.registry.allocate();
		
		switch (type) {
		case AND:
			io.setComment("Prepare AND operation");
			io.println("Mov %s, %s", r2, r0);
			io.setComment("Execute %s AND %s", r0, r1);
			io.println("And %s, %s", r2, r1);
			break;
		case OR:
			io.setComment("Prepare OR operation");
			io.println("Mov %s, %s", r2, r0);
			io.setComment("Execute %s OR %s", r0, r1);
			io.println("Or %s, %s", r2, r1);
			break;
		case EQEQ:
		case NEQ:
			String label0 = this.getNewLabel();
			String label1 = this.getNewLabel();
			io.setComment("Prepare EQEQ operation");
			io.println("Cmp %s, %s", r0, r1);
			io.println("Jnz > %s", label0);
			io.setComment("Registers are equivalent");
			if (type == Construct.EQEQ) {
				io.println("Mov %s, 1", r2);
			}
			else {
				io.println("Xor %s, %s", r2, r2);
			}
			io.println("Jmp %s", label1);
			io.outdent();
			io.println(label0 + ":");
			io.indent();
			io.setComment("Registers are not equivalent");
			if (type == Construct.EQEQ) {
				io.println("Xor %s, %s", r2, r2);
			}
			else {
				io.println("Mov %s, 1", r2);
			}
			io.outdent();
			io.println(label1 + ":");
			io.indent();
		default:
			throw new Exception(String.format("Cannot perform this operation (%s) on boolean operands.", type));
		}
		
		r0.free();
		r1.free();
		return r2;
	}
	
	@Override
	public String assembleBooleanToString(Register operandRegister) throws Exception {
		String label0 = this.getNewLabel();
		String label1 = this.getNewLabel();
		io.setComment("Prepare boolean to string");
		io.println("Cmp %s, 0", operandRegister);
		io.println("Jz > %s", label0);
		io.println("Mov [%s], Addr %s", this.temporaryGlobal, this.trueString);
		io.println("Mov Eax, 4");
		io.println("Jmp > %s", label1);
		io.outdent();
		io.println(label0 + ":");
		io.indent();
		io.println("Mov [%s], Addr %s", this.temporaryGlobal, this.falseString);
		io.println("Mov Eax, 5");
		io.outdent();
		io.println(label1 + ":");
		io.indent();
		return String.format("[%s]", this.temporaryGlobal);
	}
	
	@Override
	public void assembleCall(String procedure) throws Exception {
		io.println("Call %s", procedure);
		if (this.currentScope != null) {
			this.currentScope.popAnonymous(parameterCount);
		}
		this.parameterCount = 0;
	}
	public void assembleCall(String procedure, boolean down) throws Exception {
		io.println("Call %s %s", (down ? ">" : "<"), procedure);
	}
	
	public void assembleClearGlobal(String variable, int numBytes) throws Exception {
		io.indent();
		
		String address = "Addr " + variable;
		io.println("; Clear global string %s", address);
		
		Register[] preRegisters = this.assemblePreCall();
		
		String procedure = "clear_global_string";
		this.addResource(procedure);
		this.assembleParameter(Integer.toString(numBytes), procedure);
		this.assembleParameter(address, procedure);
		this.assembleCall(procedure);
		
		this.assemblePostCall(preRegisters);
		
		io.outdent();
	}
	
	@Override
	public void assembleClearRegister(Register register) throws Exception {
		io.setComment("Clear register for new usage");
		io.println("Xor %s, %s", register, register);
	}
	
	public void assembleCodeHeader() throws Exception {
		io.outdent();
		io.println();
		io.println("Code Section");
		io.println("start:");
		io.indent();
	}
	
	@Override
	public void assembleConditionalJump(Node condition, Node subtreeIfTrue, Node subtreeIfFalse) throws Exception {
		boolean haveElse = subtreeIfFalse != null;
		io.println("; Prepare if-then%s conditional", (haveElse ? "-else" : ""));
		String labelIf = this.getNewLabel();
		io.println("; If true, go to %s", labelIf);
		String labelElse = null;
		if (subtreeIfFalse != null) {
			labelElse = this.getNewLabel();
			io.println("; If false, go to %s", labelElse);
		}
		String labelNext = this.getNewLabel();
		io.println("; Finally, go to %s", labelNext);
		
		// Condition
		Register r0 = this.getOperandRegister(condition);
		io.setComment("Determine if condition is false");
		io.println("Cmp %s, 0", r0);
		r0.free();
		io.setComment("If condition is false, jump");
		io.println("Jz > %s", (haveElse ? labelElse : labelNext));
		
		// Code if true
		io.println(labelIf + ":");
		io.indent();
		this.assembleNode(subtreeIfTrue);
		if (haveElse) {
			io.println("Jmp %s", labelNext);
			io.println();
			io.outdent();
			
			// Code if False
			io.println(labelElse + ":");
			io.indent();
			this.assembleNode(subtreeIfFalse);
		}
		io.outdent();
		
		// Next
		io.println();
		io.println(labelNext + ":");
	}

	@Override
	public void assembleDataSection() throws Exception {
		io.outdent();
		io.println();
		io.println("Data Section");
		io.indent();
		super.assembleDataSection();
	}
	
	@Override 
	public void assembleDeclaration(Variable variable, Register register) throws Exception {
		// Move whatever value is at register into variable in stack
		String address = this.getStackAddress(variable);
		io.setComment("Store value to variable");
		io.println("Mov %s, %s", address, register);
	}

	@Override
	public void assembleFinish() throws Exception {
		io.setComment("Program finish");
		io.println("Ret");
		io.println();
	}
	
	@Override
	public Register assembleFooter() throws Exception {
		io.outdent();
		// Output functions
		for (Symbol symbol : this.symbolTable) {
			if (symbol.isFunction()) {
				io.println(symbol.getName() + ":");
				io.indent();
				// Output function
				io.outdent();
			}
		}
		
		// Output includes
		// From src/main/resources/
		String basePath = FileIO.getAbsolutePath("src/main/resources/");
		for (String resource : resources) {
			PushbackReader reader = FileIO.getReader(basePath + resource);
			StringBuilder file = new StringBuilder();
			char last = '\0';
			while (reader.ready()) {
				char c = (char) reader.read();
				if (c == '\n' && last != '\r') {
					file.append("\r\n");
				} 
				else {
					file.append(c);
				}
				last = c;
			}
			reader.close();
			io.println();
			io.println();
			io.println(";;;;;;; INCLUDED FILE %s ;;;;;;;;", resource);
			io.println();
			io.println();
			io.print(file.toString());
		}
		
		return null;
	}
	
	@Override
	public void assembleFunctions() throws Exception {
		// TODO Auto-generated method stub

	}
	
	public String assembleGlobalString(String name, int byteWidth, String value) throws Exception {
		final String[] byteSizes = new String[] { "DB", "DW", null, "DD", null, null, null, "DQ" };
		// DB == byte			(1 byte)
		// DW == word			(2 bytes)
		// DD == double word	(4 bytes)
		// DQ == quadruple word (8 bytes)
		String byteSize;
		int dup = 0;
		if (byteWidth <= 8) {
			byteSize = byteSizes[byteWidth - 1];
		}
		else {
			byteSize = "DD";
			dup = byteWidth / 4;
		}
		
		io.print(name + "\t");
		io.print(byteSize + "\t");
		if (dup > 0) {
			io.print(dup + " Dup ");
		}
		io.println(value);
		
		return name;
	}
	
	@Override
	public void assembleHandles() throws Exception {
		Register[] preRegisters = this.assemblePreCall();
		
		String procedure;
		
		// Heap handle
		io.println("; Get process heap handle");
		procedure = "GetProcessHeap";
		// No parameters
		this.assembleCall(procedure);
		io.setComment("Save heap handle");
		io.println("Mov [%s], Eax", this.heapHandle);
		
		// Input handle
		io.println("; Get input handle");
		procedure = "GetStdHandle";
		this.assembleParameter("-10D", procedure);
		this.assembleCall(procedure);
		io.setComment("Save input handle");
		io.println("Mov [%s], Eax", this.inputHandle);
		// Argument consumed
		
		// Output handle
		io.println("; Get output handle");
		this.assembleParameter("-11D", procedure);
		this.assembleCall(procedure);
		io.setComment("Save output handle");
		io.println("Mov [%s], Eax", this.outputHandle);
		// Argument consumed

		this.assemblePostCall(preRegisters);
	}
	
	@Override
	public void assembleHeader() throws Exception {
		io.println("; using the GoAsm assembly language");
	}
	
	@Override
	public void assembleInput(Variable variable) throws Exception {
		Register[] preRegisters = this.assemblePreCall();
		
		// Stores number of characters read
		this.assemblePush("0");
		// Memory location of Esp
		io.println("Mov Eax, Esp");
		
		String procedure = "ReadConsoleA";
		this.assembleParameter("0", procedure);
		this.assembleParameter("Eax", procedure);
		this.assembleParameter(String.valueOf(this.temporaryGlobalLength), procedure);
		this.assembleParameter("Addr " + this.temporaryGlobal, procedure);
		this.assembleParameter(this.getPointer(this.inputHandle), procedure);
		this.assembleCall(procedure);
		
		this.assemblePostCall(preRegisters);
		// Number of characters in [Esp]

		// Create heap allocation
		this.assembleMalloc("[Esp]", true);
		// Heap location in Eax
		String address = this.getStackAddress(variable);
		io.println("Mov %s, Eax", address);
		// Heap allocation linked to variable and in stack (placed there at beginning of SCOPE)
		
		// Move new string to allocation
		this.assembleMoveMemory("Addr " + this.temporaryGlobal, "Eax", "[Esp]");
		// Address of next byte address in Eax
		io.println("Sub Eax, 2");
		io.setComment("Remove 13,10 add 0");
		io.println("Mov B[Eax], 0");

		// Number of characters read
		io.setComment("Retrieve number of characters");
		io.println("Mov Eax, [Esp + 4]");
		io.setComment("Account for 13,10 replaced with 0");
		io.println("Sub Eax, 1");
	}
	
	@Override
	public Register assembleIntegerOperation(Construct type, Variable variable0, Variable variable1) throws Exception {
		Register r0 = this.getRegister(variable0);
		Register r1 = null;
		if (variable1 == null) {
			// Unary operator
			switch (type) {
			case NOT:
				// Keep in original register
				io.setComment("Execute NOT (inverse) %s", r0);
				io.println("Not %s", r0);
				io.println("Add %s, 1", r0);
				break;
			default:
				break;
			}
			return r0;
		}
		else {
			r1 = this.getRegister(variable1);
		}
		Register r2 = this.registry.allocate();
		
		switch (type) {
		case ADD:
			io.setComment("Prepare integer addition");
			io.println("Mov %s, %s", r2, r0);
			io.println("Add %s, %s", r2, r1);
			break;
		case SUB:
			io.setComment("Prepare integer subtraction");
			io.println("Mov %s, %s", r2, r0);
			io.println("Sub %s, %s", r2, r1);
			break;
		case MULT:
			io.setComment("Prepare integer multiplication");
			io.println("Mov Eax, %s", r0);
			io.println("IMul %s", r1);
			io.println("Mov %s, Eax", r2);
			break;
		case INTDIV:
			io.setComment("Prepare integer division");
			io.println("Mov Eax, %s", r0);			
			Register edx = this.registry.allocate("Edx");
			io.setComment("Must clear Edx for integer division");
			io.println("Xor %s, %s", edx, edx);
			io.println("IDiv %s", r1);
			io.println("Mov %s, Eax", r2);
			edx.free();
			break;
		case EQEQ: case NEQ: case LT: case LTEQ: case GT: case GTEQ:
			io.setComment("Prepare integer comparision");
			io.println("Cmp %s, %s", r0, r1);
			String jcc;
			switch (type) {
			case EQEQ:
				jcc = "Je";
				break;
			case NEQ:
				jcc = "Jne";
				break;
			case LT:
				jcc = "Jl";
				break;
			case LTEQ:
				jcc = "Jle";
				break;
			case GT: 
				jcc = "Jg";
				break;
			case GTEQ:
				jcc = "Jge";
				break;
			default:
				throw new Exception("Bad integer comparison " + type);
			}
			String label1 = this.getNewLabel();
			io.println("Mov %s, 1", r2);
			io.println("%s > %s", jcc, label1);
			io.println("Xor %s, %s", r2, r2);
			io.outdent();
			io.println(label1 + ":");
			io.indent();
		default:
			break;
		}
		
		r0.free();
		r1.free();
		return r2;
	}
	
	@Override 
	public String assembleIntegerToString(Register register) throws Exception {
		io.indent();
		
		String address = "Addr " + this.temporaryGlobal;
		io.println("; Convert integer to string in %s", address);
		
		Register[] preRegisters = this.assemblePreCall();
		
		// Empty temporary global
		this.assembleClearGlobal(this.temporaryGlobal, this.temporaryGlobalLength);
		
		String maxDig = Integer.toString(this.maxIntegerDigits) + "D";
		String procedure = "int_to_string";
		this.addResource(procedure);
		this.assembleParameter(maxDig, procedure);
		this.assembleParameter(address, procedure);
		this.assembleParameter(register.toString(), procedure);
		this.assembleCall(procedure);
		
		this.assemblePostCall(preRegisters);
		
		// Registers temporarily available
		// Calculate new starting position
		// Actual length as return value (Eax) from int_to_string
		// Register register now finished with its purpose. It will store the actual start address.
		io.println("Mov %s, Eax", register);
		io.setComment("Invert actual length");
		io.println("Not %s", register);
		io.println("Add %s, 1D", register);
		io.setComment("Add total available number of digits");
		io.println("Add %s, " + maxDig, register);
		io.setComment("Positive offset from string pointer at which non-zero values start");
		io.println("Add %s, " + address, register);
		// Length in Eax
		
		// Address stored in register
		io.outdent();
		return register.toString();
	}
	
	/**
	 * Leaves allocation address stored in Eax
	 */
	@Override
	public void assembleMalloc(Register byteWidth) throws Exception {
		this.assembleMalloc(byteWidth.toString(), true);
	}
	public void assembleMalloc(String byteWidthRegister, boolean addToPool) throws Exception {
		Register[] preRegisters = this.assemblePreCall();
		
		// Allocate memory
		if (addToPool) {
			// And Save address in scope's heap allocation pool for automatic garbage removal
			String procedure = "add_heap_allocation";
			this.addResource(procedure);
			this.assembleParameter(byteWidthRegister, procedure);
			this.assembleParameter(this.currentScope.getHeapPoolAddress(), procedure);
			this.assembleParameter(this.getPointer(this.heapHandle), procedure);
			this.assembleCall(procedure);
		}
		else {
			String procedure = "HeapAlloc";
			this.assembleParameter(byteWidthRegister, procedure);
			this.assembleParameter("0", procedure);
			this.assembleParameter(this.getPointer(this.heapHandle), procedure);
			this.assembleCall(procedure);
		}
		// Leaves pointer to allocated memory in Eax
		
		this.assemblePostCall(preRegisters);
	}
	
	/**
	 * 
	 * e.g.
	 * <pre>
	 * 			Mov Eax, 40200		; From address == 40200
	 * 			Mov Ebx, 402F0		; To address == 402F0
	 * 			Push 8D				; 8 bytes
	 * 			Push Ebx			; location with to address
	 * 			Push Eax			; location with from address
	 * </pre>
	 * 
	 * @param fromAddress location that stores the "from" memory address, not a pointer to the memory address itself
	 * @param toAddress location that stores the "to" memory address, not a pointer to the memory address itself
	 * @param bytesRegister register that contains the number of bytes to transfer
	 * @throws Exception
	 */
	public void assembleMoveMemory(String fromAddress, String toAddress, String bytesRegister) throws Exception {
		Register[] preRegisters = this.assemblePreCall();
		
		String procedure = "move_memory";
		this.addResource(procedure);
		this.assembleParameter(bytesRegister, procedure);
		this.assembleParameter(toAddress, procedure);
		this.assembleParameter(fromAddress, procedure);
		this.assembleCall(procedure);
		// Must save next byte in Eax
		
		this.assemblePostCall(preRegisters);
	}
	
	/**
	 * Returns register with data or data pointer
	 * LITERAL:
	 *	    BOOLEAN:
	 *	   		Eax: 1
	 *	   		new Register: 0 or 1
	 *	   
	 *	    INTEGER:
	 *	   		Eax: num digits
	 *	   		new Register: binary integer
	 *
	 *		STRING:
	 *			Eax: num characters
	 *			new Register: "Addr [...]", directly callable
	 *
	 * VARIABLE:
	 * 		BOOLEAN:
	 * 			Eax: 1
	 * 			new Register: 0 or 1
	 * 
	 * 		INTEGER:
	 * 			Eax: 4
	 * 			
	 */
	@Override
	public Register assembleOperand(Node operand) throws Exception {
		// Only child
		io.println("; Prepare operand");
		Construct operandElement = operand.getElementType();
		TypeSystem operandType = operand.getType();
		int byteWidth = 0;
		String operandString = null;
		String pointer;
		if (Construct.FALSE.equals(operandElement)) {
			byteWidth = 1;
			operandString = "0";
		}
		else if (Construct.TRUE.equals(operandElement)) {
			byteWidth = 1;
			operandString = "1";
		}
		else if (Construct.LITERAL.equals(operandElement)) {
			switch (operandType) {
			case INTEGER:
				byteWidth = 4;
				operandString = operand.getValue() + "D";
				if (operand.isNegated()) {
					operandString = "-" + operandString;
				}
				break;
			case STRING:
				Symbol symbol = operand.getSymbol();
				byteWidth = StringUtils.unescapeJavaString(symbol.getValue()).length() - 2;
				// Move value pointer to a register
				pointer = "Addr " + this.globalSymbolMap.get(symbol);
				operandString = pointer;
				break;
			default:
				break;
			}
		}
		else if (Construct.VARIABLE.equals(operandElement)) {
			// Move value pointer to register
			Variable variable = operand.getVariable();
			if (variable == null) {
				throw new Exception("Variable used before it was declared.");
			}
			
			TypeSystem type = variable.type;
			switch (type) {
			case INTEGER:
				byteWidth = 4;
				break;
			case STRING:
				byteWidth = 1;
				
				
				// Get string width
				Register[] preRegisters = this.assemblePreCall();
				String procedure = "get_string_length";
				this.addResource(procedure);
				String address = this.getStackAddress(variable);
				this.assembleParameter(address, procedure);
				this.assembleCall(procedure);
				this.assemblePostCall(preRegisters);;
				
				// String length in Eax
				byteWidth = -1;
				break;
			default:
				break;
			}

			// Get current stack index
			operandString = this.getStackAddress(variable);
		}
		else {
			throw new SyntaxError("Should not be here...");
		}
		
		// Move length to Eax (otherwise, assume already in Eax
		if (byteWidth > 0) {
			io.println("Mov Eax, %dD", byteWidth);
		}
		// Move value to newly allocated register
		Register register = registry.allocate();
		io.setComment("assemble operand %s", operandElement);
		io.println("Mov %s, %s", register, operandString);
		
		// Make sure it belongs to at least an anonymous variable
		Variable variable = operand.getVariable();
		if (variable != null) {
			variable.linkRegister(register);
		}
		else {
			variable = new Variable(register);
			operand.setVariable(variable);
		}
		
		return register;
	}

	/**
	 * Expects Eax to contain number of bytes to output
	 * @param dataLocation cannot be a stack pointer because this method may update the stack before access.
	 */
	@Override
	public void assembleOutput(String dataLocation) throws Exception {
		Register[] preRegisters = this.assemblePreCall();

		String procedure = "WriteConsoleA";
		this.assembleParameter("0", procedure);
		this.assembleParameter("Addr " + this.temporaryGlobal, procedure);
		this.assembleParameter("Eax", procedure);
		this.assembleParameter(dataLocation, procedure);
		this.assembleParameter("[" + this.outputHandle + "]", procedure);
		this.assembleCall(procedure);
		
		this.assemblePostCall(preRegisters);
	}
	
	public void assembleParameter(String value, String procedure) throws Exception {
		this.io.setComment("Parameter for %s", procedure);
		// Parameters should not be remembered as part of the scope, indicate scopeReady
		this.assemblePush(value);
		this.parameterCount++;
	}
	
	@Override
	public void assemblePop(Register toRegister) throws Exception {
		this.currentScope.pop(toRegister);
	}
	
	@Override
	public void assemblePop(Register toRegister, boolean scopeReady) throws Exception {
		if (!scopeReady) {
			this.assemblePop(toRegister);
		}
		io.println("Pop %s", toRegister);
		toRegister.promote();
	}
	
	/**
	 * Restore registers from stack using a Register[]
	 * returned from assemblePreCall. 
	 * Note, array is popped in REVERSE order
	 */
	@Override 
	public void assemblePostCall(Register[] registers) throws Exception {
		if (registers.length > 0) {
			io.println("; Caller restore registers " + String.join(", ", Arrays.stream(registers).map(r -> r.toString()).toArray(size -> new String[size])));
		}
		for (int i = registers.length - 1; i > -1; i--) {
			this.currentScope.pop(registers[i]);
		}
	}

	/** 
	 * Push all currently-used temporary registers
	 * to stack to save their values before a 
	 * "Call"
	 */
	@Override
	public Register[] assemblePreCall() throws Exception {
		Register[] registers = this.registry.getAll();
		if (registers.length > 0) {
			io.println("; Caller save registers " + String.join(", ", Arrays.stream(registers).map(r -> r.toString()).toArray(size -> new String[size])));
		}
		for (Register register : registers) {
			this.assemblePush(register);
		}
		return registers;
	}
	
	/**
	 * Updates currentScope as necessary,
	 * which results in assemblePush(fromRegister.toString(), true);
	 */
	@Override
	public void assemblePush(Register fromRegister) throws Exception {
		this.assemblePush(fromRegister.toString());
	}

	/**
	 * Updates currentScope as necessary,
	 * which results in assemblePush(value, true);
	 */
	@Override
	public void assemblePush(String value) throws Exception {
		if (this.currentScope != null) {
			this.currentScope.pushAnonymous(value);
		}
		else {
			this.assemblePush(value, true);
		}
	}
	
	/**
	 * Updates currentScope as necessary,
	 * which results in assemblePush(variable.register, true);
	 */
	@Override
	public void assemblePush(Variable variable) throws Exception {
		if (this.currentScope != null) {
			this.currentScope.pushVariable(variable);
		}
		else if (variable.register != null) {
			// No scope to add variable to
			this.assemblePush(variable.register.toString(), true);
		}
	}
	
	/**
	 * Prefer assemblePush(String) | assemblePush(Register) over this method.
	 * If !scopeReady, updates currentScope as necessary,
	 * then outputs an assembly push command.
	 * 
	 * 
	 * @param valueOrRegister
	 * @param scopeReady
	 * @return
	 */
	@Override
	public void assemblePush(String valueOrRegister, boolean scopeReady) throws Exception {
		if (!scopeReady) {
			this.assemblePush(valueOrRegister);
			return;
		}
		io.println("Push %s", valueOrRegister);
	}
	
	@Override
	public void assembleScope(boolean open) throws Exception {
		int totalBytes = this.currentScope.size() * 4;
		if (open) {
			// Scope variables already in currentScope.stack
			// Reflect in assembly these variables
			if (totalBytes > 0) {
				io.setComment("Open scope");
				io.println("Sub Esp, %d", totalBytes);
			}
			else {
				io.println("; Open scope");
			}
			
			// Heap allocation pool
			// Create a space to remember heap allocations
			io.setComment("Create heap allocation pool");
			io.println("Mov Eax, 1028D");
			this.assembleMalloc("Eax", false);
			// Allocation address now stored in Eax
			// Make sure first four bytes reflect # allocations and capacity
			io.setComment("Number of current allocations");
			io.println("Mov W[Eax], 0");
			io.setComment("Current allocation capacity");
			io.println("Mov W[Eax + 2], 256");
			Register heapRegister = this.registry.allocate();
			io.setComment("Address of heap allocation pool");
			io.println("Mov %s, Eax", heapRegister);
			this.currentScope.setHeapPoolAddress(heapRegister);
			heapRegister.free();
		}
		else {
			// Free all heap allocations in scope
			String procedure = "free_heap_allocations";
			this.addResource(procedure);
			io.setComment("Free all heap allocations in this scope");
			this.assembleParameter(this.currentScope.getHeapPoolAddress(), procedure);
			this.assembleParameter(String.format("[%s]", this.heapHandle), procedure);
			this.assembleCall(procedure);

			// Remove all 
			if (totalBytes > 0) {
				io.setComment("Close scope");
				io.println("Add Esp, %d", totalBytes);
			}
			else {
				io.println("; Close scope");
			}
		}
	}
	
	@Override
	public Register assembleStringCompare(Construct construct, Variable variable0, Variable variable1) throws Exception {
		Register[] preRegisters = this.assemblePreCall();
		
		String procedure = "string_compare";
		this.addResource(procedure);
		Register r0 = this.getRegister(variable0);
		Register r1 = this.getRegister(variable1);
		this.assembleParameter(r0.toString(), procedure);
		this.assembleParameter(r1.toString(), procedure);
		this.assembleCall(procedure);
		r0.free();
		r1.free();
		// Equivalence (boolean) in Eax
		
		this.assemblePostCall(preRegisters);
		
		Register register = this.registry.allocate();
		io.println("Mov %s, Eax", register);
		if (Construct.NEQ.equals(construct)) {
			io.setComment("Invert bit 0 on boolean value");
			io.println("Not %s", register);
			io.println("Shl %s, 31D", register);
			io.println("Shr %s, 31D", register);
		}
		
		return register;
	}
	
	@Override
	public Register assembleStringConcatenation(Variable variable0, Variable variable1) throws Exception {
		// Will call once for each string
		String procedure = "get_string_length";
		this.addResource(procedure);

		// Save state
		Register[] preRegisters = this.assemblePreCall();
		
		// Get length of operand0
		this.assembleParameter(this.getStackAddress(variable0), procedure);
		this.assembleCall(procedure);
		// Remember result in temporary global var
		io.println("Mov [%s], Eax", this.temporaryGlobal);
		
		// Get length of operand 1
		this.assembleParameter(this.getStackAddress(variable1), procedure);
		this.assembleCall(procedure);
		// Remember result
		io.println("Mov [%s + 4], Eax", this.temporaryGlobal);
		
		// Registers available
		// Sum lengths
		// Length of operand 1 already in Eax
		io.setComment("Sum string lengths");
		io.println("Add Eax, [Esp + 4]");
		
		// Allocate space in heap
		this.assembleMalloc("Eax", true);
		io.setComment("Remember value in temp global");
		io.println("Mov [%s + 8], Eax", this.temporaryGlobal);

		// Recall state (never overwrites Eax)
		this.assemblePostCall(preRegisters);
		
		// Move first string location to heap
		// From address
		Register r2 = this.registry.allocate();
		io.println("Mov %s, %s", r2, this.getStackAddress(variable0));
		this.assembleMoveMemory(r2.toString(), "Eax", String.format("[%s]", this.temporaryGlobal));
		// Eax now contains the location of the next byte to be placed
		
		// Move second string to heap
		io.println("Mov %s, %s", r2, this.getStackAddress(variable1));
		this.assembleMoveMemory(r2.toString(), "Eax", String.format("[%s + 4]", this.temporaryGlobal));
		// Eax now contains the location of the next byte to be placed
		r2.free();

		// Move \0 to last spot
		io.setComment("Strings must end in 0");
		io.println("Mov B[Eax], 0");
		
		// Resultant heap address in Eax
		Register register = this.registry.allocate();
		io.setComment("Move new string location to newly allocated register");
		io.println("Mov %s, [%s + 8]", register, this.temporaryGlobal);
		
		// Store string length in Eax
		io.println("Mov Eax, [%s]", this.temporaryGlobal);
		io.setComment("Store string length in Eax");
		io.println("Add Eax, [%s + 4]", this.temporaryGlobal);
		
		return register;
	}
	
	@Override
	public String compile(String assemblyFile, boolean verbose) throws IOException {
		try {
			String classPathName = FileIO.getAbsolutePath("");
			File goAsmPath = new File(classPathName + "src/main/resources/GoAsm/");
			
			// Remove disk name prefix from assemblyFile if it exists
			String assemblyPathName = assemblyFile.substring(0, assemblyFile.lastIndexOf('/') + 1);
			String assemblyFileName = assemblyFile.substring(assemblyFile.lastIndexOf('/') + 1);
			assemblyPathName = FileIO.truncateDiskName(assemblyPathName);
			
			Runtime runtime = Runtime.getRuntime();
			// Create object file
			String goAsm = "GoAsm.exe";
			String objectFileName = assemblyFileName.replace(".asm", ".obj");
			assemblyFile = assemblyPathName + assemblyFileName;
			String[] objectFileCommands = new String[] { "cmd", "/c", String.format("%s /fo %s \"%s\"", goAsm, objectFileName, assemblyFile) };
			Process p1 = runtime.exec(objectFileCommands, null, goAsmPath);
			// Must complete build of object file before it can link
			p1.waitFor();
			if (verbose) {
				System.out.print("\nGoAsm.exe output:");
				FileIO.processOutput(p1);
				System.out.println();
			}
			
			// Link kernel libraries
			String goLink = "GoLink.exe";
			String executableFile = assemblyPathName + assemblyFileName.replace(".asm", ".exe");
			String[] linkedFileCommands = new String[] { "cmd", "/c", String.format("%s /console /fo \"%s\" %s kernel32.dll", goLink, executableFile, objectFileName) };
			Process p2 = runtime.exec(linkedFileCommands, null, goAsmPath);
			// Allow completion of executable before deleting temporary .obj file
			p2.waitFor();
			if (verbose) {
				System.out.print("\nGoLink.exe output:");
				FileIO.processOutput(p2);
				System.out.println();
			}
			
			// Delete temporary .obj file
			File objectFile = new File(goAsmPath + "/" + objectFileName);
			if (objectFile.delete()) {
				System.out.println("Temporary file "+ objectFileName + " has been deleted");
			}
			
			return executableFile;
		}
		catch (Exception err) {
			System.out.println("Failed to compile");
			return null;
		}
	}
	
	@Override
	public String getPointer(String globalVariable) {
		return "[" + globalVariable + "]";
	}
	
	@Override
	public Register getRegister(Variable variable) throws Exception {
		Register register = super.getRegister(variable);
		
		if (variable.inStack()) {
			int offset = this.currentScope.getStackOffset(variable) * 4;
			io.setComment("Retrieve value from stack");
			io.println("Mov %s, [Esp + %d]", register, offset);
		}
		
		return register;
	}
	
	public String getStackAddress(Variable variable) throws Exception {
		int stackLocation = this.currentScope.getStackOffset(variable) * 4;
		return String.format("[Esp + %dD]", stackLocation);
	}

}
