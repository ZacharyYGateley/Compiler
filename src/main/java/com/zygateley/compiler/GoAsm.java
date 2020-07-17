package com.zygateley.compiler;

import java.io.*;
import java.lang.Exception;
import java.util.*;

import com.zygateley.compiler.Assembler.Writer;

public class GoAsm extends AssyLanguage {
	private ArrayList<String> resources = new ArrayList<>();
	
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
	public Register assembleCalculation(Node operation) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void assembleCall(String procedure) throws Exception {
		io.println("Call %s", procedure);
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
		io.println("Mov %s, 0", register);
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
		Register r0 = this.assembleOperand(condition);
		io.setComment("Determine if condition is false");
		io.println("Cmp %s, 0", r0);
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
	public Register assembleConcatenation(Node operand0, Node operand1) throws Exception {
		// Will call once for each string
		String procedure = "get_string_length";
		this.addResource(procedure);

		// Save state
		Register[] preRegisters = this.assemblePreCall();
		
		// Get length of operand0
		Variable v0 = operand0.getVariable();
		this.assembleParameter(this.getStackAddress(v0), procedure);
		this.assembleCall(procedure);
		// Remember result in temporary global var
		io.println("Mov [%s], Eax", this.temporaryGlobal);
		
		// Get length of operand 1
		Variable v1 = operand1.getVariable();
		this.assembleParameter(this.getStackAddress(v1), procedure);
		this.assembleCall(procedure);
		// Remember result
		io.println("Mov [%s + 4], Eax", this.temporaryGlobal);
		
		// Registers available
		// Sum lengths
		// Length of operand 1 already in Eax
		io.setComment("Sum string lengths");
		io.println("Add Eax, [Esp + 4]");
		
		// Allocate space in heap
		this.assembleMalloc("Eax");
		io.setComment("Remember value in temp global");
		io.println("Mov [%s + 8], Eax", this.temporaryGlobal);

		// Recall state (never overwrites Eax)
		this.assemblePostCall(preRegisters);
		
		// Move first string location to heap
		// From address
		Register r2 = this.registry.allocate();
		io.println("Mov %s, %s", r2, this.getStackAddress(v0));
		this.assembleMoveMemory(r2.toString(), "Eax", String.format("[%s]", this.temporaryGlobal));
		// Eax now contains the location of the next byte to be placed
		
		// Move second string to heap
		io.println("Mov %s, %s", r2, this.getStackAddress(v1));
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
	public Register assembleExpression(Node parseTree) throws Exception {
		// TODO Auto-generated method stub
		return null;
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
		io.println("; This code generated by com.zygateley.compiler");
	}
	
	@Override 
	public String assembleIntegerToString(Register register, Node operand) throws Exception {
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
	 * Allocation address is stored in Eax
	 */
	@Override
	public void assembleMalloc(Register byteWidth) throws Exception {
		this.assembleMalloc(byteWidth.toString());
	}
	public void assembleMalloc(String byteWidthRegister) throws Exception {
		Register[] preRegisters = this.assemblePreCall();
		
		String procedure = "HeapAlloc";
		this.assembleParameter(byteWidthRegister, procedure);
		this.assembleParameter("0", procedure);
		this.assembleParameter(this.getPointer(this.heapHandle), procedure);
		this.assembleCall(procedure);
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
		Register register = registry.allocate();
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
		// Move value to register
		io.setComment("assemble operand %s", operandElement);
		io.println("Mov %s, %s", register, operandString);
		
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
		this.assemblePush(value);
	}
	
	@Override
	public void assemblePop(Register toRegister) throws Exception {
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
			io.println("; Caller restore registers");
		}
		for (int i = registers.length - 1; i > -1; i--) {
			Register register = registers[i];
			this.currentScope.popAnonymous(register);
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
			io.println("; Caller save registers");
		}
		for (Register register : registers) {
			this.currentScope.pushAnonymous(register);
		}
		return registers;
	}
	
	@Override
	public void assemblePush(Register fromRegister) throws Exception {
		io.println("Push %s", fromRegister);
	}

	@Override
	public void assemblePush(String value) throws Exception {
		io.println("Push %s", value);
	}
	
	@Override
	public void assembleScope(boolean open) throws Exception {
		int totalBytes = this.currentScope.size() * 4;
		if (open) {
			if (totalBytes > 0) {
				io.setComment("Open scope");
				io.println("Sub Esp, %d", totalBytes);
			}
			else {
				io.println("; Open scope");
			}
		}
		else {
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
	
	public String getStackAddress(Variable variable) throws Exception {
		int stackLocation = this.currentScope.getStackOffset(variable) * 4;
		return String.format("[Esp + %dD]", stackLocation);
	}
	
	@Override
	public String getPointer(String globalVariable) {
		return "[" + globalVariable + "]";
	}

}
