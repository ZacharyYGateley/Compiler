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
	protected final Scope globalScope;
	protected Scope currentScope;
	protected HashMap<Symbol, String> globalSymbolMap = new HashMap<>();
	protected int globalVariableCount = 0;
	protected final String inputHandle = "inputHandle";
	protected final String outputHandle = "outputHandle";
	protected final String temporaryGlobal = "tempGlobal";
	protected final int temporaryGlobalLength = 256;
	
	// Language-specific
	protected boolean handlesDeclared = false;
	protected int maxIntegerDigits = 11;
	
	protected abstract Register assembleCalculation(Node operation) throws Exception;
	protected abstract void assembleCall(String method) throws Exception;
	//protected abstract void assembleDeclaration(Variable variable, String value) throws Exception;
	//protected abstract void assembleDeclaration(Variable variable, Symbol symbol) throws Exception;
	protected abstract void assembleDeclaration(Variable variable, Register register) throws Exception;
	protected abstract Register assembleExpression(Node parseTree) throws Exception;
	protected abstract void assembleFinish() throws Exception;
	protected abstract Register assembleFooter() throws Exception;
	protected abstract Register assembleHeader() throws Exception;
	protected abstract void assembleHandles() throws Exception;
	protected abstract String assembleIntegerToString(Register register, Node operand) throws Exception;
	protected abstract Register assembleOperand(Node operand) throws Exception;
	protected abstract void assembleOutput(String dataLocation) throws Exception;
	protected abstract Register[] assemblePreCall() throws Exception;
	protected abstract void assemblePostCall(Register[] registers) throws Exception;
	protected abstract void assemblePush(Register fromRegister) throws Exception;
	protected abstract void assemblePush(String value) throws Exception;
	protected abstract void assemblePop(Register toRegister) throws Exception;
	protected abstract void assembleTerminal(Node leafNode) throws Exception;
	protected abstract void assembleFunctions() throws Exception;
	protected abstract String assembleGlobalString(String name, int byteWidth, String value) throws Exception;
	protected abstract String getPointer(String globalVariable);
	
	
	public AssyLanguage(Assembler.Writer io, SymbolTable symbolTable, String[] tempRegisters) {
		this.io = io;
		this.symbolTable = symbolTable;
		this.registry = new Registry(this, tempRegisters);
		this.globalScope = new Scope(this, null);
	}
	
	public void assembleNode(Node pn) throws Exception {
		Element construct = pn.getElementType();
		Symbol symbol;
		Node operand;
		Element operandElement;
		Register operandRegister;
		switch (construct) {
		case FUNCDEF:
			// Save all functions into SymbolTable
			// To be processed and output at the end of file
			symbol = pn.getFirstChild().getSymbol();
			// Parameters are next
			// Finally is function body
			io.println(";function " + symbol);
			return;
		case VARDEF:
			symbol = pn.getFirstChild().getSymbol();
			Register r0 = this.registry.allocate();
			// Variable linked to register and to symbol
			Variable v0 = this.currentScope.declareVariable(r0, symbol);
			
			operand = pn.getLastChild();
			operandRegister = this.assembleOperand(operand);
			this.assembleDeclaration(v0, operandRegister);
			operandRegister.free();
			
			break;
		case OUTPUT:
			if (!handlesDeclared) {
				this.assembleHandles();
				handlesDeclared = true;
			}
			
			operand = pn.getLastChild();
			operandRegister = this.assembleOperand(operand);
			if (TypeSystem.INTEGER.equals(operand.getType())) {
				String address = this.assembleIntegerToString(operandRegister, operand);
				this.assembleOutput(address);
			}
			else {
				this.assembleOutput(operandRegister.toString());
			}
			operandRegister.free();
			
			break;
		default:
			break;
		}
		
		// Iterate
		for (Node child : pn) {
			if (child != null) {
				assembleNode(child);
			}
		}
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
			switch (type) {
			case BOOLEAN:
				byteWidth = 1;
				prefix = "bool";
				break;
			case STRING:
				byteWidth = 1;
				prefix = "str";
				value = "\"" + StringUtils.unescapeAssemblyString(value.substring(1, value.length() - 1)) + "\"";
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
		assembleGlobalString(this.inputHandle, 4, "0");
		assembleGlobalString(this.outputHandle, 4, "0");
		
		// Locate for output from API
		assembleGlobalString(this.temporaryGlobal, this.temporaryGlobalLength, "0");
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
			if (leastUsed.variable != null) {
				this.language.currentScope.pushVariable(leastUsed, leastUsed.variable);
			}
			
			leastUsed.allocate();
			// Register is now reserved
			
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

	private static class Scope {
		private final Scope parent;
		// Contains all scope variables
		private final ArrayDeque<Variable> stack;
		private final AssyLanguage language;
		
		/**
		 * LinkedHashMaps initialized to LRU by ACCESS order
		 * @param stackFrame
		 */
		public Scope(AssyLanguage language, Scope parent) {
			this.parent = parent;
			// Stack starting at stack pointer = stack
			this.stack = new ArrayDeque<>();
			this.language = language;
		}
		
		public Variable declareVariable(Register register, Symbol symbol) throws Exception {
			Variable variable = new Variable(symbol);
			this.pushVariable(register, variable);
			return variable;
		}
		
		public Variable getVariable(Symbol symbol) {
			for (Variable variable : this.stack) {
				if (variable.symbol == symbol) {
					return variable;
				}
			}
			return null;
		}
		
		public void pushVariable(Register register, Variable variable) throws Exception {
			this.language.assemblePush(register);
			stack.push(variable);
		}
	}
	
	protected static class Variable {
		public Register register;
		public final Symbol symbol;
		public int stackIndex;
		
		public Variable() {
			register = null;
			symbol = null;
			stackIndex = -1;
		}
		public Variable(Symbol s) {
			register = null;
			symbol = s;
			stackIndex = -1;
		}
		
		public void linkRegister(Register r) {
			r.variable = this;
			register = r;
		}
		public void unlinkRegister() {
			if (register != null) {
				register.variable = null;
			}
			register = null;
		}
	}
}

class StringUtils {
	public static String unescapeAssemblyString(String input) {
		if (!(input instanceof String)) {
			return "";
		}
		return input
				.replaceAll("\\\"",  "\",'\"',\"")
				.replaceAll("\\\\n", "\",10,\"")
				.replaceAll("\\\\f",  "\",12,\"")
				.replaceAll("\\\\r",  "\",13,\"")
				.replaceAll("\"\",", "")
				.replaceAll(",\"\"", "");
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