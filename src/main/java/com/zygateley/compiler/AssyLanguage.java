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
	protected abstract void assembleMalloc(Register byteWidth) throws Exception;
	protected abstract String assembleIntegerToString(Register register, Node operand) throws Exception;
	protected abstract Register assembleOperand(Node operand) throws Exception;
	protected abstract void assembleOutput(String dataLocation) throws Exception;
	protected abstract Register[] assemblePreCall() throws Exception;
	protected abstract void assemblePostCall(Register[] registers) throws Exception;
	protected abstract void assemblePush(Register fromRegister) throws Exception;
	protected abstract void assemblePush(String value) throws Exception;
	protected abstract void assemblePop(Register toRegister) throws Exception;
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
	
	public void assembleNode(Node pn) throws Exception {
		Element construct = pn.getElementType();
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
			r0 = this.registry.allocate();
			if (variable == null) {
				throw new Exception("Variable not properly linked during parsing.");
				// Variable not found, declare new
				// Allocates stack space
				//v0 = this.currentScope.declareVariable(r0, symbol);
			}
			else {
				// Variable found, link to this register
				variable.linkRegister(r0);
			}
			
			operand = pn.getLastChild();
			operandRegister = this.getOperandRegister(operand);
			this.assembleDeclaration(variable, operandRegister);
			operandRegister.free();
			
			// Make sure type of variable is up-to-date
			symbol.setType(operand.getType());
			
			r0.free();
			
			break;
		case OUTPUT:
			io.println("; Output");
			operand = pn.getLastChild();
			operandRegister = this.getOperandRegister(operand);
			variable = operand.getVariable();
			TypeSystem type;
			if (variable != null) {
				type = variable.getType();
				symbol = variable.getSymbol();
			}
			else {
				type = operand.getType();
				symbol = operand.getSymbol();
			}
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
		case ADD:
			// Make sure value/result of first child is saved in stack 
			firstChild = pn.getFirstChild();
			r0 = this.getOperandRegister(firstChild);
			Variable v0 = firstChild.getVariable();
			if (v0 == null) {
				v0 = new Variable();
				v0.linkRegister(r0);
				firstChild.setVariable(v0);
			}
			// Make sure operand variable is in stack
			if (v0.getStackOffset() < 0) {
				this.currentScope.pushVariable(v0);
			}
			r0.free();
			
			// Make sure value/result of second child is saved in stack 
			nextChild = firstChild.getNextSibling();
			r0 = this.getOperandRegister(nextChild);
			Variable v1 = nextChild.getVariable();
			if (v1 == null) {
				v1 = new Variable();
				v1.linkRegister(r0);
				nextChild.setVariable(v1);
			}
			// Make sure operand variable is in stack
			if (v1.getStackOffset() < 0) {
				this.currentScope.pushVariable(v1);
			}
			r0.free();
			
			// Check for bad type operation
			TypeSystem type0 = firstChild.getType();
			if (type0 == null && firstChild.getSymbol() != null) {
				type0 = firstChild.getSymbol().getType();
			}
			TypeSystem type1 = nextChild.getType();
			if (type1 == null && nextChild.getSymbol() != null) {
				type1 = nextChild.getType();
			}
			
			if (type0 != type1) {
				// Different types
				throw new Exception(String.format("Bad addition: %s and %s", type0, type1));
			}
			
			// Both types are the same
			variable = new Variable();
			switch (type0) {
			case INTEGER:
				break;
			case STRING:
				operandRegister = this.assembleConcatenation(firstChild, nextChild);
				variable.linkRegister(operandRegister);
				// Non-anonymous variable now stored in register
				// If future procedures need its register, it will be pushed to stack and remembered

				// New string length stored in Eax from assembleConcatenation
				break;
			default:
				throw new Exception(String.format("Bad addition: %s", type0));
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
			// move that variable to the stack
			/* 
			 * Variable is already in the stack
			 * 
			if (leastUsed.variable != null) {
				this.language.currentScope.pushVariable(leastUsed, leastUsed.variable);
			}
			*/
			
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

class StringUtils {
	public static String unescapeAssemblyString(String input) {
		if (!(input instanceof String)) {
			return "";
		}
		return input
				.replaceAll("\\\\\"", "\",'\"',\"")
				.replaceAll("\0", "\",0,\"")
				.replaceAll("\\\\n", "\",10,\"")
				.replaceAll("\\\\f", "\",12,\"")
				.replaceAll("\\\\r", "\",13,\"")
				.replaceAll("\"\",", "");
	}
	
/**
 * Performs single unescape on all escape characters
 * 
 * Courtesy 
 * https://gist.github.com/uklimaschewski
 * Found at 
 * https://gist.github.com/uklimaschewski/6741769
 * 
 */
	public static String unescapeJavaString(String st) {
	    StringBuilder sb = new StringBuilder(st.length());

	    for (int i = 0; i < st.length(); i++) {
	        char ch = st.charAt(i);
	        if (ch == '\\') {
	            char nextChar = (i == st.length() - 1) ? '\\' : st
	                    .charAt(i + 1);
	            // Octal escape?
	            if (nextChar >= '0' && nextChar <= '7') {
	                String code = "" + nextChar;
	                i++;
	                if ((i < st.length() - 1) && st.charAt(i + 1) >= '0'
	                        && st.charAt(i + 1) <= '7') {
	                    code += st.charAt(i + 1);
	                    i++;
	                    if ((i < st.length() - 1) && st.charAt(i + 1) >= '0'
	                            && st.charAt(i + 1) <= '7') {
	                        code += st.charAt(i + 1);
	                        i++;
	                    }
	                }
	                sb.append((char) Integer.parseInt(code, 8));
	                continue;
	            }
	            switch (nextChar) {
	            case '\\':
	                ch = '\\';
	                break;
	            case 'b':
	                ch = '\b';
	                break;
	            case 'f':
	                ch = '\f';
	                break;
	            case 'n':
	                ch = '\n';
	                break;
	            case 'r':
	                ch = '\r';
	                break;
	            case 't':
	                ch = '\t';
	                break;
	            case '\"':
	                ch = '\"';
	                break;
	            case '\'':
	                ch = '\'';
	                break;
	            // Hex Unicode: u????
	            case 'u':
	                if (i >= st.length() - 5) {
	                    ch = 'u';
	                    break;
	                }
	                int code = Integer.parseInt(
	                        "" + st.charAt(i + 2) + st.charAt(i + 3)
	                                + st.charAt(i + 4) + st.charAt(i + 5), 16);
	                sb.append(Character.toChars(code));
	                i += 5;
	                continue;
	            }
	            i++;
	        }
	        sb.append(ch);
	    }
	    return sb.toString();
	}
}