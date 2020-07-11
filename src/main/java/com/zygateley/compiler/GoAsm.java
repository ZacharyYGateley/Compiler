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
	
	public void addResource(String resource) {
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
	public void assembleCall(String method) throws Exception {
		io.println("Call %s", method);
	}
	
	public void assembleClearGlobal(String variable, int numBytes) throws Exception {
		// Make sure the resource function is included
		this.addResource("clear_global_string.asm");
		
		
		Register[] preRegisters = this.assemblePreCall();
		
		this.assemblePush(Integer.toString(numBytes));
		this.assemblePush("Addr " + variable);
		this.assembleCall("clear_global_string");
		
		this.assemblePostCall(preRegisters);
	}
	
	@Override
	public void assembleCodeSection() throws Exception {
		io.outdent();
		io.println();
		io.println("Code Section");
		io.println("start:");
		io.indent();
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
		int offset = variable.stackIndex;
		io.println("Mov [Esp + %d], %s", offset, register);
	}

	@Override
	public Register assembleExpression(Node parseTree) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void assembleFinish() throws Exception {
		io.println("Ret 				; Program finish");
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
			while (reader.ready()) {
				file.append((char) reader.read());
			}
			reader.close();
			io.println("\r\n\r\n");
			io.println(";;;;;;; INCLUDED FILE %s ;;;;;;;;", resource);
			io.println("\r\n\r\n");
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
		
		// Input handle
		this.assemblePush("-10D");
		this.assembleCall("GetStdHandle");
		io.println("Mov [%s], Eax		; Get input handle", this.inputHandle);
		io.println();
		// Argument consumed
		
		// Output handle
		this.assemblePush("-11D");
		this.assembleCall("GetStdHandle");
		io.println("Mov [%s], Eax		; Get output handle", this.outputHandle);
		io.println();
		// Argument consumed

		this.assemblePostCall(preRegisters);
	}
	
	@Override
	public void assembleHeader() throws Exception {
		io.println(";EasyCodeName=Assembly,1");
	}
	
	@Override 
	public String assembleIntegerToString(Register register, Node operand) throws Exception {
		this.addResource("int_to_string.asm");
		
		Register[] preRegisters = this.assemblePreCall();
		
		// Empty temporary global
		this.assembleClearGlobal(this.temporaryGlobal, this.temporaryGlobalLength);
		
		String address = "Addr " + this.temporaryGlobal;
		String maxDig = Integer.toString(this.maxIntegerDigits) + "D";
		this.assemblePush(maxDig);
		this.assemblePush(address);
		this.assemblePush(register);
		this.assembleCall("int_to_string");
		
		// Registers temporarily available
		// Calculate new starting position
		int integer = Integer.parseInt(operand.getValue());
		int length = 1 + (int) Math.floor(Math.log10(Math.abs(integer)));
		if (operand.isNegated()) {
			length += 1;
		}
		io.println("Mov Eax, " + address);
		io.println("Add Eax, " + maxDig);
		// New address starting position
		io.println("Sub Eax, " + length + "D   				; starting address of integer string");
		
		// New address starting position stored in register
		// that originally held the value
		this.assemblePostCall(preRegisters);
		io.println("Mov %s, Eax 			; stored in allocated register", register);
		// Length in Eax
		io.println("Mov Eax, " + length + "D				; length of string");
		
		io.println();
		
		// Address stored in register
		return register.toString();
	}
	
	/**
	 * Returns register with data or data pointer
	 * Stores length in Eax
	 */
	@Override
	public Register assembleOperand(Node operand) throws Exception { 
		// Only child
		Element operandElement = operand.getElementType();
		TypeSystem operandType = operand.getType();
		int byteWidth = 0;
		String operandString = null;
		Register register = registry.allocate();
		String pointer;
		if (Element.FALSE.equals(operandElement)) {
			byteWidth = 1;
			operandString = "0";
		}
		else if (Element.TRUE.equals(operandElement)) {
			byteWidth = 1;
			operandString = "1";
		}
		else if (Element.LITERAL.equals(operandElement)) {
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
		else if (Element.VARIABLE.equals(operandElement)) {
			// Move value pointer to register
			// TODO: bytewidth and typing
			byteWidth = 1;
			Variable variable = this.getVariable(operand.getSymbol());
			if (variable == null) {
				throw new Exception("Variable used before it was declared.");
			}
			operandString = String.format("[Esp + %d]", variable.stackIndex);
		}
		else {
			throw new SyntaxError("Should not be here...");
		}
		
		// Move length to Eax
		io.println("Mov Eax, %dD", byteWidth);
		// Move value to register
		io.println("Mov %s, %s				; assemble operand %s", register, operandString, operandType);
		io.println();
		
		return register;
	}

	@Override
	public void assembleOutput(String dataLocation) throws Exception {
		Register[] preRegisters = this.assemblePreCall();
		
		this.assemblePush("0");
		this.assemblePush("Addr " + this.temporaryGlobal);
		this.assemblePush("Eax");
		this.assemblePush(dataLocation);
		this.assemblePush("[" + this.outputHandle + "]");
		this.assembleCall("WriteConsoleA			; output value");
		io.println();
		
		this.assemblePostCall(preRegisters);
	}
	
	@Override
	public void assemblePop(Register toRegister) throws Exception{
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
		for (int i = registers.length - 1; i > -1; i--) {
			Register register = registers[i];
			this.assemblePop(register);
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
		for (Register register : registers) {
			this.assemblePush(register);
		}
		return registers;
	}
	
	@Override
	public void assemblePush(Register fromRegister) throws Exception {
		// TODO Auto-generated method stub
		io.println("Push %s", fromRegister);
	}

	@Override
	public void assemblePush(String value) throws Exception {
		io.println("Push %s", value);
	}

	@Override
	public void assembleTerminal(Node leafNode) throws Exception {
		// TODO Auto-generated method stub

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

}
