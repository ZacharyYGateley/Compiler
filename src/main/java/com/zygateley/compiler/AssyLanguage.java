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
	protected final String temporaryGlobal = "tempGlobal";
	protected final int temporaryGlobalLength = 256;
	protected int labelCount = 0;
	
	// Language-specific
	protected int maxIntegerDigits = 11;
	
	protected abstract Register assembleCalculation(Node operation) throws Exception;
	protected abstract void assembleCall(String method) throws Exception;
	protected abstract void assembleClearRegister(Register register) throws Exception;
	protected abstract void assembleCodeHeader() throws Exception;
	protected abstract Register assembleConcatenation(Node operand0, Node operand1) throws Exception;
	protected abstract void assembleConditionalJump(Node condition, Node subtreeIfTrue, Node subtreeIfFalse) throws Exception;
	protected abstract void assembleDeclaration(Variable variable, Register register) throws Exception;
	protected abstract Register assembleExpression(Node parseTree) throws Exception;
	protected abstract void assembleFinish() throws Exception;
	protected abstract Register assembleFooter() throws Exception;
	protected abstract void assembleFunctions() throws Exception;
	protected abstract String assembleGlobalString(String name, int byteWidth, String value) throws Exception;
	protected abstract void assembleHandles() throws Exception;
	protected abstract void assembleHeader() throws Exception;
	protected abstract void assembleIntegerOperation(Construct type, Node operand0, Node operand1) throws Exception;
	protected abstract String assembleIntegerToString(Register register, Node operand) throws Exception;
	protected abstract void assembleMalloc(Register byteWidth) throws Exception;
	protected abstract Register assembleOperand(Node operand) throws Exception;
	protected abstract void assembleOperation(Construct type, Node operand0, Node operand1) throws Exception;
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
			if (TypeSystem.INTEGER.equals(type)) {
				String address = this.assembleIntegerToString(operandRegister, operand);
				this.assembleOutput(address);
			}
			else {
				this.assembleOutput(operandRegister.toString());
			}
			operandRegister.free();
			
			break;
		case OPERATION:
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
			TypeSystem type0 = firstChild.getType();
			TypeSystem type1 = nextChild.getType();
			
			if (type0 != type1) {
				// Different types
				throw new Exception(String.format("Bad addition: %s and %s", type0, type1));
			}
			
			// Both types are the same
			variable = new Variable();
			switch (type0) {
			case BOOLEAN:
				// AND, OR, NOT
				this.assembleOperation(construct, firstChild, nextChild);
				break;
			case INTEGER:
				// ADD, SUB, MULT, INTDIV
				// EQEQ, NEQ, LT, LTEQ, GT, GTEQ
				this.assembleIntegerOperation(construct, firstChild, nextChild);
				break;
			case STRING:
				if (Construct.ADD.equals(construct)) {
					operandRegister = this.assembleConcatenation(firstChild, nextChild);
					variable.linkRegister(operandRegister);
					// Non-anonymous variable now stored in register
					// If future procedures need its register, it will be pushed to stack and remembered

					// New string length stored in Eax from assembleConcatenation
				}
				else if (Construct.EQEQ.equals(construct)) {
					
				}
				else if (Construct.NEQ.equals(construct)) {
					
				}
				else {
					throw new Exception(String.format("Bad string operation: %s", construct));
				}
				break;
			default:
				throw new Exception(String.format("Bad %s operation: %s", type0, construct));
			}
			
			// Link node and register
			pn.setVariable(variable);
			// Make sure node type is up to date 
			pn.setType(type0);
			variable.setType(type0);
			if (variable.symbol != null) {
				variable.symbol.setType(type0);
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
		
		// Location for handles
		assembleGlobalString(this.heapHandle, 4, "0");
		assembleGlobalString(this.inputHandle, 4, "0");
		assembleGlobalString(this.outputHandle, 4, "0");
		
		// Locate for output from API
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
			Register leastUsed = accessOrder.pollFirst();
			// Add to tail
			accessOrder.addLast(leastUsed);
			
			// If LRU has a variable, 
			// ensure it is in the stack
			if (leastUsed.variable != null) {
				this.language.assemblePush(leastUsed.variable);
			}
			
			// Reserve register
			leastUsed.allocate();
			
			// Clear register
			this.language.assembleClearRegister(leastUsed);
			
			return leastUsed;
		}
		
		/**
		 * Mark register as no longer being used
		 * 
		 * @return
		 * @throws Exception
		 */
		public void free(Register register) {
			register.free();
		}

		
		/**
		 * Get a specific register.
		 * Moves to most-recently-used position.
		 */
		public Variable get(int i) {
			Register r = registerMap.get(i);
			// Pop register and add to the tail
			promote(r);
			return r.variable;
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