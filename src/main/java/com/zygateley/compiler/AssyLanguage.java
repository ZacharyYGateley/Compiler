package com.zygateley.compiler;

import java.lang.Exception;
import java.util.ArrayDeque;
import java.util.HashMap;


/**
 * Everything starts with Scope, an abstract object
 * that translates high-level language to a
 * registry and stack.
 * 
 * Registry mirrors the current assembly registry
 * 		Contains static registers that are passed around in its LRU queue
 * Stack mirrors the current assembly stack
 * 
 * Variable may be currently bound to some register
 * It has a symbol table entry (high-level)
 * and a stack index (low-level)
 * 
 * As scopes nest themselves
 * 
 * @author Zachary Gateley
 *
 */
public abstract class AssyLanguage {
	protected final Assembler.Writer io;
	protected final SymbolTable symbolTable;
	protected final Registry registry;
	protected Scope globalScope;
	protected Scope currentScope;
	protected HashMap<Symbol, String> globalSymbolMap = new HashMap<>();
	protected int globalVariableCount = 0;
	protected final String heapHandle = "heapHandle";
	protected final String inputHandle = "inputHandle";
	protected final String outputHandle = "outputHandle";
	protected final String trueString = "trueString";
	protected final String falseString = "falseString";
	protected final String temporaryGlobal = "tempGlobal";
	protected final int temporaryGlobalLength = 256;
	protected int labelCount = 0;
	
	// Language-specific
	protected int maxIntegerDigits = 11;

	protected abstract Register assembleBooleanOperation(Construct type, Variable variable0, Variable variable1) throws Exception;
	protected abstract String assembleBooleanToString(Register register) throws Exception;
	protected abstract void assembleCall(String method) throws Exception;
	protected abstract void assembleClearRegister(Register register) throws Exception;
	protected abstract void assembleCodeHeader() throws Exception;
	protected abstract void assembleConditionalJump(Node condition, Node subtreeIfTrue, Node subtreeIfFalse) throws Exception;
	protected abstract void assembleDeclaration(Variable variable, Register register) throws Exception;
	protected abstract void assembleFinish() throws Exception;
	protected abstract Register assembleFooter() throws Exception;
	protected abstract void assembleFunctions() throws Exception;
	protected abstract String assembleGlobalString(String name, int byteWidth, String value) throws Exception;
	protected abstract void assembleHandles() throws Exception;
	protected abstract void assembleHeader() throws Exception;
	protected abstract Register assembleIntegerOperation(Construct type, Variable variable0, Variable variable1) throws Exception;
	protected abstract String assembleIntegerToString(Register register) throws Exception;
	protected abstract void assembleMalloc(Register byteWidth) throws Exception;
	protected abstract Register assembleOperand(Node operand) throws Exception;
	protected abstract void assembleOutput(String dataLocation) throws Exception;
	protected abstract Register[] assemblePreCall() throws Exception;
	protected abstract void assemblePop(Register toRegister) throws Exception;
	protected abstract void assemblePop(Register toRegister, boolean scopeReady) throws Exception;
	protected abstract void assemblePostCall(Register[] registers) throws Exception;
	protected abstract void assemblePush(Register fromRegister) throws Exception;
	protected abstract void assemblePush(String value) throws Exception;
	protected abstract void assemblePush(Variable variable) throws Exception;
	protected abstract void assemblePush(String valueOrRegister, boolean scopeReady) throws Exception;
	protected abstract void assembleScope(boolean open) throws Exception;
	protected abstract Register assembleStringCompare(Construct construct, Variable variable0, Variable variable1) throws Exception;
	protected abstract Register assembleStringConcatenation(Variable variable0, Variable variable1) throws Exception;
	protected abstract String compile(String fileName, boolean verbose) throws Exception;
	protected abstract String getPointer(String globalVariable);
	
	
	public AssyLanguage(Assembler.Writer io, SymbolTable symbolTable, String[] tempRegisters) {
		this.io = io;
		this.symbolTable = symbolTable;
		this.registry = new Registry(this, tempRegisters);
	}
	
	public void assembleChildren(Node pn) throws Exception {
		for (Node child : pn) {
			if (child != null) {
				assembleNode(child);
			}
		}
	}
	
	public void assembleCodeSection(Node parseTree) throws Exception {
		this.assembleCodeHeader();
		
		if (!Construct.SCOPE.equals(parseTree.getElementType())) {
			throw new Exception("Invalid language organization. Scope should be root of syntax tree.");
		}
		
		// Assemble handles
		// Reason why it is here and not at the beginning of OUTPUT:
		//		if first output appears in a conditional, 
		//		the handles are not properly prepared
		// Reasonable to add only if input/output exists,
		// 		but at the moment, that would take crawling the tree,
		//		which is unnecessary overhead
		io.println("; Prepare environment for input and output");
		this.assembleHandles();
		io.println();
		
		// Crawl tree
		// Any function declarations found
		// will be stored into SymbolTable as type FUNCTION
		this.assembleNode(parseTree);
	}
	
	@SuppressWarnings("unused")
	public void assembleNode(Node pn) throws Exception {
		Construct construct = pn.getElementType();
		Variable variable;
		Symbol symbol;
		Node operand, firstChild, nextChild;
		Register operandRegister, r0, r1;
		TypeSystem type0, type1;
		switch (construct) {
		case SCOPE:
			this.currentScope = pn.getScope();
			this.currentScope.setLanguage(this);
			if (this.globalScope == null) {
				this.globalScope = this.currentScope;
			}
			
			// Open new scope (set variables into stack)
			this.assembleScope(true);
			
			// Assemble contents of scope
			this.assembleChildren(pn);
			
			// Close scope
			this.assembleScope(false);
			
			this.currentScope = this.currentScope.parent;
			break;
		case IF:
			Node condition = pn.getFirstChild();
			Node subtreeIfTrue = condition.getNextSibling();
			Node subtreeIfFalse = subtreeIfTrue.getNextSibling();
			this.assembleConditionalJump(condition, subtreeIfTrue, subtreeIfFalse);
			break;
		case FUNCDEF:
			// Save all functions into SymbolTable
			// To be processed and output at the end of file
			symbol = pn.getFirstChild().getSymbol();
			// Parameters are next
			// Finally is function body
			io.println("; function " + symbol);
			return;
		case VARDEF:
			firstChild = pn.getFirstChild();
			variable = firstChild.getVariable();
			symbol = variable.getSymbol();
			io.println("; Store value to " + symbol);
			
			// Allocate a new temporary register
			if (variable == null) {
				throw new Exception("Variable not properly linked during parsing.");
				// Variable not found, declare new
				// Allocates stack space
				//v0 = this.currentScope.declareVariable(r0, symbol);
			}
			
			operand = pn.getLastChild();
			// Let pn and operand share the same variable
			operand.setVariable(variable);
			operandRegister = this.getOperandRegister(operand);
			this.assembleDeclaration(variable, operandRegister);
			operandRegister.free();
			
			// Make sure type of variable is up-to-date
			symbol.setType(operand.getType());
			
			break;
		case OUTPUT:
			io.println("; Output");
			operand = pn.getLastChild();
			operandRegister = this.getOperandRegister(operand);
			variable = operand.getVariable();
			TypeSystem type = operand.getType();
			symbol = operand.getSymbol();
			String address;
			switch (type) {
			case BOOLEAN:
				address = this.assembleBooleanToString(operandRegister);
				break;
			case INTEGER:
				address = this.assembleIntegerToString(operandRegister);
				break;
			case STRING:
				address = operandRegister.toString();
				break;
			default:
				throw new Exception("Bad output " + type);
			}
			this.assembleOutput(address);
			operandRegister.free();
			
			break;
		case NOT:
			// Make sure value/result of first child is saved in stack 
			firstChild = pn.getFirstChild();
			// Operand assembles. Operand variable contains data location.
			this.getOperandRegister(firstChild).free();
			
			type0 = firstChild.getType();
			
			switch (type0) {
			case BOOLEAN:
				operandRegister = this.assembleBooleanOperation(
						construct, firstChild.getVariable(), null
						);
				break;
			case INTEGER:
				// ADD, SUB, MULT, INTDIV
				// EQEQ, NEQ, LT, LTEQ, GT, GTEQ
				operandRegister = this.assembleIntegerOperation(
						construct, firstChild.getVariable(), null
						);
				break;
			default:
				throw new Exception("Invalid operand (" + type0 + ") for operation NOT");
			}

			// Link node and register
			variable = pn.getVariable();
			if (variable == null) {
				variable = new Variable();
				pn.setVariable(variable);
			}
			variable.linkRegister(operandRegister);
			
			// Make sure node type is up to date
			pn.setType(type0);
			
			break;
		case OR: case AND:
		case ADD: case SUB: case MULT: case INTDIV:
		case EQEQ: case NEQ: case LT: case LTEQ: case GT: case GTEQ:
			// Make sure value/result of first child is saved in stack 
			firstChild = pn.getFirstChild();
			// Operand assembles. Operand variable contains data location.
			this.getOperandRegister(firstChild).free();
			
			// Make sure value/result of second child is saved in stack 
			nextChild = firstChild.getNextSibling();
			// Operand assembles. Operand variable contains data location.
			this.getOperandRegister(nextChild).free();
			
			// Check for bad type operation
			type0 = firstChild.getType();
			type1 = nextChild.getType();
			
			if (type0 != type1) {
				// Different types
				throw new Exception(String.format("Bad addition: %s and %s", type0, type1));
			}
			
			// Both types are the same
			// And both operands have a variable
			operandRegister = null;
			Variable variable0 = firstChild.getVariable();
			Variable variable1 = nextChild.getVariable();
			switch (type0) {
			case BOOLEAN:
				// AND, OR
				operandRegister = this.assembleBooleanOperation(
						construct, variable0, variable1
						);
				break;
			case INTEGER:
				// ADD, SUB, MULT, INTDIV
				// EQEQ, NEQ, LT, LTEQ, GT, GTEQ
				operandRegister = this.assembleIntegerOperation(
						construct, variable0, variable1
						);
				break;
			case STRING:
				if (Construct.ADD.equals(construct)) {
					operandRegister = this.assembleStringConcatenation(variable0, variable1);
					// New string length stored in Eax from assembleConcatenation
				}
				else {
					boolean isEQEQ = Construct.EQEQ.equals(construct);
					boolean isNEQ = Construct.NEQ.equals(construct);
					if (isEQEQ || isNEQ) {
						operandRegister = this.assembleStringCompare(construct, variable0, variable1);
					}
					else {
						throw new Exception(String.format("Bad string operation: %s", construct));
					}
				}
				break;
			default:
				throw new Exception(String.format("Bad %s operation: %s", type0, construct));
			}
			
			if (operandRegister == null) {
				throw new Exception("Failed operation on " + type0 + " and " + type1);
			}

			// Link node and register
			variable = pn.getVariable();
			if (variable == null) {
				variable = new Variable();
				pn.setVariable(variable);
			}
			variable.linkRegister(operandRegister);
			
			// Make sure node type is up to date 
			TypeSystem resultantType;
			switch (construct) {
			case AND: case OR:
			case EQEQ: case NEQ: case LT: case LTEQ: case GT: case GTEQ:
				resultantType = TypeSystem.BOOLEAN;
				break;
			default:
				resultantType = type0;
				break;
			}
			pn.setType(resultantType);
			variable.setType(resultantType);
			if (variable.symbol != null) {
				variable.symbol.setType(resultantType);
			}
			
			break;
		default:
			io.println("; Instruction skipped (" + construct + ")");
			this.assembleChildren(pn);
			break;
		}
		io.println();
		
	}
	
	/**
	 * Find all string literals and create 
	 * global string pool with these values,
	 * naming them along the way. 
	 *  
	 */
	public void assembleDataSection() throws Exception {
		io.println("; String pool");
		for (Symbol symbol : this.symbolTable) {
			int byteWidth = 0;
			String prefix = "";
			TypeSystem type = symbol.getType();
			String value = symbol.getValue();
			if (type == null || value == null) {
				continue;
			}
			
			switch (type) {
			case BOOLEAN:
				byteWidth = 1;
				prefix = "bool";
				break;
			case STRING:
				byteWidth = 1;
				prefix = "str";
				value = "\"" + StringUtils.unescapeAssemblyString(value.substring(1, value.length() - 1) + "\0") + "\"";
				value = value.replace("\"\",", "").replace(",\"\"", "");
				break;
			case INTEGER:
				byteWidth = 4;
				prefix = "int";
				break;
			default:
				break;
			}
			
			// Then add it to the string pool
			if (byteWidth > 0 && !prefix.isBlank()) {
				// New global variable required
				// Name it by auto-increment
				// Any pointers to this symbol REMAIN THE SAME
				String name = String.format("%s%d", prefix, globalVariableCount++);
				symbol.setName(name);
				this.globalSymbolMap.put(symbol, name);
				
				assembleGlobalString(name, byteWidth, value);
			}
		}
		
		// String pool true/false
		assembleGlobalString(this.trueString, 1, "\"TRUE\",0");
		assembleGlobalString(this.falseString, 1, "\"FALSE\",0");
		
		
		io.println();
		io.println("; Other global variables");
		
		// Location for handles
		assembleGlobalString(this.heapHandle, 4, "0");
		assembleGlobalString(this.inputHandle, 4, "0");
		assembleGlobalString(this.outputHandle, 4, "0");
		
		// Location for output from API
		assembleGlobalString(this.temporaryGlobal, this.temporaryGlobalLength, "0");
	}
	
	protected Register getOperandRegister(Node operand) throws Exception {
		Register operandRegister;
		if (operand.getChildCount() == 0) {
			// Literal or variable
			operandRegister = this.assembleOperand(operand);
		}
		else {
			// Expression / operation
			this.assembleNode(operand);
			operandRegister = operand.getVariable().register;
		}
		
		// Make sure operand has a variable and is in the stack
		Variable variable = operand.getVariable();
		if (variable == null) {
			variable = new Variable(operandRegister);
			operand.setVariable(variable);
		}
		else {
			variable.linkRegister(operandRegister);
		}
		// Make sure operand variable is in stack
		this.assemblePush(variable);
		
		return operandRegister;
	}
	
	/**
	 * Return unique label string
	 */
	protected String getNewLabel() {
		return "label" + this.labelCount++;
	}
	
	/**
	 * For the given node, get a/its register. 
	 * If the node has a variable, link the variable and register.
	 * @param variable named or anonymous whose register to retrieve
	 * @return current or new register for current or no variable
	 */
	protected Register getRegister(Variable variable) throws Exception {
		Register register;
		if (variable == null || variable.register == null) {
			register = registry.allocate();
			if (variable != null) {
				variable.linkRegister(register);
			}
		}
		else {
			register = variable.register;
		}
		
		return register;
	}
	
	/**
	 * Find symbol in scope, then global scope
	 * @param symbol
	 * @return
	 */
	protected Variable getVariable(Symbol symbol) {
		Variable variable = this.currentScope.getVariable(symbol);
		if (variable == null) {
			variable = this.globalScope.getVariable(symbol);
		}
		return variable;
	}

	////////////////////////////////////////////////
	// Class declarations
	protected static class Registry {
		private final AssyLanguage language;
		private final ArrayDeque<Register> accessOrder;
		private final HashMap<Integer, Register> registerMap;
		

		
		public Registry(AssyLanguage language, String[] registerNames) {
			this.language = language;
			
			int capacity = 0;
			if (registerNames != null) {
				capacity = registerNames.length;
			}
			accessOrder = new ArrayDeque<Register>(capacity);
			registerMap = new HashMap<Integer, Register>(capacity);
			// Build empty registers
			for (int i = 0; i < capacity; i++) {
				Register r = new Register(registerNames[i], this);
				accessOrder.addLast(r);
				registerMap.put(i, r);
			}
		}
		
		/**
		 * Access least-recently-used register
		 * Moves to most-recently-used position.
		 * @return register to allocate to
		 */
		public Register allocate() throws Exception {
			// Pop least-recently-used
			Register leastUsed = accessOrder.peekFirst();
			return this.allocate(leastUsed);
		}
		public Register allocate(Register register) throws Exception {
			// Get indicated register
			accessOrder.remove(register);
			// Add to tail
			accessOrder.addLast(register);
			
			// If LRU has a variable, 
			// ensure it is in the stack
			if (register.variable != null) {
				this.language.assemblePush(register.variable);
			}
			
			// Reserve register
			register.allocate();
			
			// Clear register
			// This is a really bad idea
			// It bulks up the assembly code excessively
			// Very few times do we even need an empty register
			//this.language.assembleClearRegister(register);
			
			return register;
		}

		
		/**
		 * Allocate a specific register.
		 */
		public Register allocate(String regName) throws Exception {
			for (Register register : this.accessOrder) {
				if (register.registerName == regName) {
					return this.allocate(register);
				}
			}
			return null;
		}
		
		/**
		 * Get all registers as register array
		 */
		public Register[] getAll() {
			return accessOrder.stream().filter(r -> !r.isAvailable).toArray(Register[]::new);
		}
		
		/**
		 * Indicate register r was last used.
		 * 
		 * @param r
		 */
		public void promote(Register r) {
			if (accessOrder.remove(r)) {
				accessOrder.addLast(r);
			}
		}
	}
	
	protected static class Register {
		public Variable variable;
		private final String registerName;
		private final Registry LRU;
		private boolean isAvailable = true;
		
		public Register(String register, Registry LRU) {
			variable = null;
			this.registerName = register;
			this.LRU = LRU;
		}
		
		public void allocate() {
			this.isAvailable = false;
		}
		
		public void free() {
			this.isAvailable = true;
			if (this.variable != null) {
				// Clears this.variable
				this.variable.unlinkRegister();
			}
		}
		
		/**
		 * Move register to most-recently-used
		 */
		public void promote() {
			LRU.promote(this);
		}
		
		@Override
		public String toString() {
			return this.registerName;
		}
	}
}