package com.zygateley.compiler;

import java.util.ArrayDeque;
import java.util.HashMap;


public abstract class AssyLanguage {
	private String inputHandle = null;
	private String outputHandle = null;
	protected final Assembler.Writer io;
	protected final Scope scope;
	protected final Stack stack;
	protected final SymbolTable symbolTable;
	protected final HashMap<Symbol, String> globalVariables;
	protected int globalVariableCount = 0;
	
	public AssyLanguage(Assembler.Writer io, Scope scope, Stack stack, SymbolTable symbolTable, HashMap<Symbol, String> globalVariables) {
		this.io = io;
		this.scope = scope;
		this.stack = stack;
		this.symbolTable = symbolTable;
		this.globalVariables = globalVariables;
	}

	public static class Variable {
		public Register register;
		public final Symbol symbol;
		public int indexStack;
		
		public Variable() {
			register = null;
			symbol = null;
			indexStack = -1;
		}
		public Variable(Symbol s) {
			register = null;
			symbol = s;
			indexStack = -1;
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
	
	public static class Register {
		public Variable variable;
		private final String registerName;
		private final Registry LRU;
		
		public Register(String register, Registry LRU) {
			variable = null;
			this.registerName = register;
			this.LRU = LRU;
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
	
	public static class Registry {
		private ArrayDeque<Register> accessOrder;
		private HashMap<Integer, Register> registerMap;
		private Stack stack;
		private Assembler.Writer outputStream;
		
		private final String stackPush = 
				"subu $sp, $sp, 4\n" +
				"sw %s, 0($sp)\n";
		
		public Registry(String[] registerNames, Stack stack, Assembler.Writer outputStream) {
			int capacity = registerNames.length;
			accessOrder = new ArrayDeque<Register>(capacity);
			registerMap = new HashMap<Integer, Register>(capacity);
			// Build empty registers
			for (int i = 0; i < capacity; i++) {
				Register r = new Register(registerNames[i], this);
				accessOrder.addLast(r);
				registerMap.put(i, r);
			}
			
			this.stack = stack;
			
			this.outputStream = outputStream;
		}
		
		/**
		 * Access least-recently-used register
		 * Moves to most-recently-used position.
		 * @return register to allocate to
		 */
		public Register allocate() {
			// Pop least-recently-used
			Register LRU = accessOrder.pollLast();
			// Add to tail
			accessOrder.addFirst(LRU);
			
			// If LRU has a variable, 
			// move that variable to the stack
			moveVariableToStack(LRU);
			
			return LRU;
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
		
		public void promote(Register r) {
			if (accessOrder.remove(r)) {
				accessOrder.addFirst(r);
			}
		}
		
		public void moveVariableToStack(Register r) {
			// No need to move to stack if...
			// There is no variable in this register
			Variable v = r.variable;
			if (v == null) return;
			
			// Only need to physically move variable to stack
			// if it is not already there
			if (v.indexStack < 0) {
				// Move variable from register to stack
				outputStream.println(this.stackPush, r);
				
				// Indicate new stack element in compiler
				// Links location to variable
				stack.push(v);
			} else {
				// Otherwise, update value in stack
				int offset = (stack.length - v.indexStack - 1) * 4;
				outputStream.println("sw %s, %d($sp)\n", r, offset);
			}
			
			// Compiler: unlink register and variable
			v.unlinkRegister();
		}
	}


	public static class Stack {
		public ArrayDeque<AssyLanguage.Variable> stack;
		public int length;
		
		public Stack() {
			stack = new ArrayDeque<AssyLanguage.Variable>();
			length = 0;
		}
		
		public void push(AssyLanguage.Variable v) {
			v.indexStack = length;
			length++;
			stack.push(v);
		}
	}
	
	public static class Scope {
		private Registry registry;
		
		private final Stack stack;
		
		private Assembler.Writer outputStream;

		/**
		 * LinkedHashMaps initialized to LRU by ACCESS order
		 * @param stackFrame
		 */
		public Scope(Assembler.Writer outputStream, String[] tempRegisters) {
			// Stack starting at stack pointer = stack
			stack = new Stack();
			
			this.outputStream = outputStream; 
			
			// Register sets
			registry = new Registry(tempRegisters, stack, outputStream);
		}
		
		/**
		 * Allocate a new anonymous register
		 * @return Register object
		 */
		public Register allocate() {
			// Get least-recently-register
			// Moves variable to stack as necessary
			Register r = registry.allocate();
			Variable v = new Variable();
			v.linkRegister(r);
			
			return r;
		}
	}
	
	public abstract Variable allocateMemory();
	public Register allocateRegister() {
		return scope.allocate();
	}
	public abstract Variable assembleCalculation();
	public abstract Variable assembleExpression(Node parseTree);
	public abstract Variable assembleFooter();
	public abstract Variable assembleHeader();
	public void assembleNode(Node pn) {
		boolean isNonTerminal = (pn.getToken() == null);
		if (isNonTerminal) {
			assembleNonTerminal(pn);
		}
		else {
			assembleTerminal(pn);
		}
	}
	public void assembleNonTerminal(Node pn) {
		NonTerminal rule = pn.getRule();
		switch (rule) {
		case _FUNCDEF_:
			// Save all functions into SymbolTable
			// To be processed and output at the end of file
			Symbol symbol = pn.childNodes().get(1).getSymbol();
			symbol.setType(Symbol.Type.FUNCTION);
			symbol.setParseTree(pn);
			
			// Do not parse this tree now
			return;
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
	
	public abstract void assembleTerminal(Node node);
	
	/**
	 * Find all string literals and create 
	 * global string pool with these values,
	 * naming them along the way. 
	 *  
	 */
	public void assembleGlobal() {
		io.println(".data         # String pool");
		
		// Indent the next block
		io.indent();
		for (Symbol symbol : this.symbolTable) {
			if (symbol.getType() == Symbol.Type.STRING) {
				// String found
				// Name it by autoincrement, 
				String name = String.format("str%d", globalVariableCount++);
				symbol.setName(name);
				// Then add it to the string pool
				assembleGlobalString(name, symbol);
			}
		}
		// Finished this block
		io.outdent();
	}
	public abstract void assembleGlobalString(String name, Symbol symbol);
	
	/**
	 * outputFunctions
	 * 
	 * All functions are stored in the SymbolTable as type FUNCTION.
	 * Output all functions at once at the end of the file
	 * 
	 */
	public abstract void assembleFunctions();
}