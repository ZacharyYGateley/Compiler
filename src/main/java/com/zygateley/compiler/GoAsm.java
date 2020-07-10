package com.zygateley.compiler;

import java.io.FileWriter;
import java.io.PushbackReader;
import java.lang.Exception;

import com.zygateley.compiler.Assembler.Writer;

public class GoAsm extends AssyLanguage {
	public GoAsm(Writer io, SymbolTable symbolTable) {
		super(io, symbolTable, new String[] { "Ebx", "Ecx", "Edx", "Esi", "Edi" });
		// TODO Auto-generated constructor stub
	}

	@Override
	public Register assembleCalculation(Node operation) throws Exception {
		// TODO Auto-generated method stub
		return null;
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
		
		// Output includes
		// From src/main/resources/
		String basePath = FileIO.getAbsolutePath("src/main/resources/");
		String[] resources = new String[] { "int_to_string.asm" };
		for (String resource : resources) {
			PushbackReader reader = FileIO.getReader(basePath + resource);
			StringBuilder file = new StringBuilder();
			while (reader.ready()) {
				file.append((char) reader.read());
			}
			reader.close();
			io.println("\n\n\n");
			io.println(";;;;;;; INCLUDED FILE %s ;;;;;;;;", resource);
			io.println("\n\n");
			io.print(file.toString());
		}
		
		return null;
	}

	@Override
	public Register assembleHeader() throws Exception {
		io.outdent();
		io.println();
		io.println("Code Section");
		io.println("start:");
		io.indent();
		return null;
	}
	
	@Override
	public void assembleHandles() throws Exception {
		// Input handle
		this.assemblePreCall();
		this.assemblePush("-10D");
		this.assembleCall("GetStdHandle");
		io.println("Mov [%s], Eax		; Get input handle", this.inputHandle);
		io.println();
		// Argument consumed
		
		// Output handle
		this.assemblePreCall();
		this.assemblePush("-11D");
		this.assembleCall("GetStdHandle");
		io.println("Mov [%s], Eax		; Get output handle", this.outputHandle);
		io.println();
		// Argument consumed
	}
	
	/**
	 * Returns register with data or data pointer
	 * Stores length in Eax
	 */
	@Override
	public Register assembleOperand(Node operand) throws Exception { 
		// Only child
		Element operandElement = operand.getElementType();
		int byteWidth = 0;
		String operandString = null;
		Register register = registry.allocate();
		String pointer;
		switch (operandElement) {
		case BOOLEAN:
			byteWidth = 1;
			String boolVal = operand.getValue();
			if (boolVal == "false") {
				operandString = "0";
			}
			else {
				operandString = "1";
			}
			break;
		case INTEGER:
			byteWidth = 4;
			operandString = operand.getValue();
			break;
		case STRING:
			Symbol symbol = operand.getSymbol();
			byteWidth = symbol.getValue().length() - 2;
			// Move value pointer to a register
			pointer = "Addr " + this.globalSymbolMap.get(symbol);
			operandString = pointer;
			break;
		case VARIABLE:
			// Move value pointer to register
			// TODO: bytewidth and typing
			byteWidth = 1;
			Variable variable = this.getVariable(operand.getSymbol());
			if (variable == null) {
				throw new Exception("Variable used before it was declared.");
			}
			operandString = String.format("[Esp + %d]", variable.stackIndex);
			break;
		default:
			break;
		}                                                                                                                
		
		// Move length to Eax
		io.println("Mov Eax, %dD", byteWidth);
		// Move value to register
		io.println("Mov %s, %s			; assemble operand %s", register, operandString, operandElement);
		io.println();
		
		return register;
	}
	
	@Override
	public void assembleOutput(Register register) throws Exception {
		Register[] preRegisters = this.assemblePreCall();
		
		this.assemblePush("0");
		this.assemblePush("Addr " + this.temporaryGlobal);
		this.assemblePush("Eax");
		this.assemblePush(register);
		this.assemblePush("[" + this.outputHandle + "]");
		this.assembleCall("WriteConsoleA			; output value");
		io.println();
		
		this.assemblePostCall(preRegisters);
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
	public void assembleCall(String method) throws Exception {
		io.println("Call %s", method);
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
	public void assemblePop(Register toRegister) throws Exception{
		io.println("Pop %s", toRegister);
		toRegister.promote();
	}

	@Override
	public void assembleTerminal(Node leafNode) throws Exception {
		// TODO Auto-generated method stub

	}
	
	@Override
	public void assembleDataSection() throws Exception {
		io.outdent();
		io.println();
		io.println("Data Section");
		io.indent();
		super.assembleDataSection();
	}

	public String assembleGlobalString(String name, int byteWidth, String value) throws Exception {
		final String[] byteSizes = new String[] { "DB", "DW", null, "DD", null, null, null, "DQ" };
		// DB == byte			(1 byte)
		// DW == word			(2 bytes)
		// DD == double word	(4 bytes)
		// DQ == quadruple word (8 bytes)
		
		io.print(name + "\t");
		io.print(byteSizes[byteWidth - 1] + "\t");
		io.println(value);
		
		return name;
	}

	@Override
	public void assembleFunctions() throws Exception {
		// TODO Auto-generated method stub

	}
	
	@Override
	public String getPointer(String globalVariable) {
		return "[" + globalVariable + "]";
	}

}
