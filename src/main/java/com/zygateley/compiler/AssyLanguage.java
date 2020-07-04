package com.zygateley.compiler;

import java.util.ArrayDeque;
import java.util.HashMap;


public abstract class AssyLanguage {
	protected final Assembler.Writer io;
	protected final SymbolTable symbolTable;
	protected final Scope globalScope;
	protected final HashMap<Symbol, String> globalVariables;
	protected int globalVariableCount = 0;
	
	// Language-specific
	protected Variable inputHandle = null;
	protected Variable outputHandle = null;
	protected String[] tempRegisters;
	public abstract Variable assembleCalculation(Node operation);
	public abstract Variable assembleExpression(Node parseTree);
	public abstract Variable assembleFooter();
	public abstract Variable assembleHeader();
	public abstract Variable assembleInputHandle();
	public abstract Variable assembleOutputHandle();
	public abstract Variable assemblePush(Variable variable);
	public abstract void assembleTerminal(Node leafNode);
	public abstract void assembleGlobalString(String name, Symbol symbol);
	public abstract void assembleFunctions();
	
	
	public AssyLanguage(Assembler.Writer io, Scope scope, Stack stack, SymbolTable symbolTable, HashMap<Symbol, String> globalVariables) {
		this.io = io;
		this.globalScope = new Scope(this);
		this.symbolTable = symbolTable;
		this.globalVariables = new HashMap<Symbol, String>();
	}

	public Variable allocateMemory(int numBytes) {
		return globalScope.allocateMemory();
	}
	public Variable allocateRegister() {
		return globalScope.allocateRegister();
	}
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
			Symbol symbol = pn.getFirstChild().getNextSibling().getSymbol();
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
	/**
	 * Find all string literals and create 
	 * global string pool with these values,
	 * naming them along the way. 
	 *  
	 */
	public void assembleGlobal() {
		// Indent the next block
		io.indent();
		for (Symbol symbol : this.symbolTable) {
			if (symbol.getType() == Symbol.Type.STRING) {
				// String found
				// Name it by auto-increment
				String name = String.format("str%d", globalVariableCount++);
				symbol.setName(name);
				// Then add it to the string pool
				assembleGlobalString(name, symbol);
			}
		}
		// Finished this block
		io.outdent();
	}

	public Variable getInputHandle() {
		return (inputHandle != null ? inputHandle : assembleInputHandle());
	}
	public Variable getOutputHandle() {
		return (outputHandle != null ? outputHandle : assembleOutputHandle());
	}

	////////////////////////////////////////////////
	// Class declarations
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
		private final ArrayDeque<Register> accessOrder;
		private final HashMap<Integer, Register> registerMap;
		private final Scope scope;
		

		
		public Registry(Scope scope, String[] registerNames) {
			int capacity = registerNames.length;
			accessOrder = new ArrayDeque<Register>(capacity);
			registerMap = new HashMap<Integer, Register>(capacity);
			// Build empty registers
			for (int i = 0; i < capacity; i++) {
				Register r = new Register(registerNames[i], this);
				accessOrder.addLast(r);
				registerMap.put(i, r);
			}
			
			this.scope = scope;
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
			if (LRU.variable != null) {
				scope.push(LRU.variable);
			}
			// Register is now available
			
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
	}
	
	public static class Stack {
		public ArrayDeque<Variable> stack;
		public int length;
		
		public Stack() {
			stack = new ArrayDeque<Variable>();
			length = 0;
		}
		
		public Variable push(Variable v) {
			v.indexStack = length;
			length++;
			stack.push(v);
			return v;
		}
		public Variable push() {
			Variable v = new Variable();
			return this.push(v);
		}
	}

	public static class Scope {
		private final Registry registry;
		private final Stack stack;
		private final AssyLanguage language;
		
		/**
		 * LinkedHashMaps initialized to LRU by ACCESS order
		 * @param stackFrame
		 */
		public Scope(AssyLanguage language) {
			// Stack starting at stack pointer = stack
			this.stack = new Stack();
			this.language = language;
			
			// Available temporary register locations
			// Organized as least-recently-used
			registry = new Registry(this, language.tempRegisters);
		}
		
		/**
		 * Allocate a new anonymous register
		 * @return new anonymous Variable object
		 */
		public Variable allocateRegister() {
			// Get least-recently-register
			// Moves variable to stack as necessary
			Register r = registry.allocate();
			Variable v = new Variable();
			v.linkRegister(r);
			
			return v;
		}
		
		/**
		 * Allocate new anonymous memory location
		 * @return new anonymous Variable object 
		 */
		public Variable allocateMemory() {
			Variable v = new Variable();
			return push(v);
		}
		public Variable push(Variable variable) {
			stack.push(variable);
			return language.assemblePush(variable);
		}
	}
}