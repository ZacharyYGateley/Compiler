package com.zygateley.compiler;

import java.util.*;



public class MIPS {
	public static class Writer {
		private final StringBuilder outputStream;
		private int currentIndent;
		
		public Writer() {
			outputStream = new StringBuilder();
			currentIndent = 0;
		}
		
		public void println(String s) {
			println(s, "");
		}
		public void println(String s, Object... formatters) {
			// Indent as necessary
			for (int i = 0; i < currentIndent; i++) outputStream.append("    ");
			
			// Format strings as necessary
			if (formatters.length > 0) {
				outputStream.append(String.format(s, (Object[]) formatters));
				
				// Promote in LRU all variables used
				for (Object e : formatters) {
					if (e instanceof Register) {
						((Register) e).promote();
					}
				}
			}
			// No formatters
			else {
				outputStream.append(s);
			}
			outputStream.append("\r\n");
		}
		
		public void indent() {
			this.currentIndent++;
		}
		
		public void outdent() {
			this.currentIndent--;
		}
		
		@Override
		public String toString() {
			return outputStream.toString();
		}
	}
	
	private static class Variable {
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
	
	private static class Register {
		public Variable variable;
		private final String registerName;
		private final RegisterLRU LRU;
		
		public Register(String register, RegisterLRU LRU) {
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
	
	private static class Stack {
		public ArrayDeque<Variable> stack;
		public int length;
		
		public Stack() {
			stack = new ArrayDeque<Variable>();
			length = 0;
		}
		
		public void push(Variable v) {
			v.indexStack = length;
			length++;
			stack.push(v);
		}
		
		/*
		public Variable pop() {
			length--;
			Variable v = stack.pop();
			v.indexStack = -1;
			return v;
		}
		*/
	}
	
	private static class RegisterLRU {
		private ArrayDeque<Register> accessOrder;
		private HashMap<Integer, Register> registerMap;
		private Stack stack;
		private Writer outputStream;
		
		public RegisterLRU(String prefix, int capacity, Stack stack, Writer outputStream) {
			accessOrder = new ArrayDeque<Register>(capacity);
			registerMap = new HashMap<Integer, Register>(capacity);
			// Build empty registers
			for (int i = 0; i < capacity; i++) {
				Register r = new Register("$" + prefix + i, this);
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
		public Register allocateLRU() {
			// Pop least-recently-used
			Register LRU = accessOrder.pollFirst();
			// Add to tail
			accessOrder.addLast(LRU);
			
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
				accessOrder.addLast(r);
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
				outputStream.println("subu $sp, $sp, 4\n");
				outputStream.println("sw %s, 0($sp)\n", r);
				
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
	
	public static class Scope {
		private RegisterLRU tLRU;
		private RegisterLRU sLRU;
		
		private final Stack stack;
		
		private Writer outputStream;

		/**
		 * LinkedHashMaps initialized to LRU by ACCESS order
		 * @param stackFrame
		 */
		public Scope(Writer outputStream) {
			// Stack starting at stack pointer = stack
			stack = new Stack();
			
			this.outputStream = outputStream; 
			
			// Temporary, anonymous registers
			tLRU = new RegisterLRU("t", 8, stack, outputStream);
			
			// "Named" registers
			sLRU = new RegisterLRU("s", 8, stack, outputStream);
		}
		
		/**
		 * Allocate a new anonymous register
		 * @return Register object
		 */
		private Register allocate(RegisterLRU LRU, Variable v) {
			// Get least-recently-register
			// Moves variable to stack as necessary
			Register r = LRU.allocateLRU();
			v.linkRegister(r);
			
			return r;
		}
		private Register allocate_t() {
			Variable newVariable = new Variable(); 
			return allocate(tLRU, newVariable);
		}
		private Register allocate_s(Symbol s) {
			Variable newVariable = new Variable(s);
			return allocate(sLRU, newVariable);
		}
	}
	
	/**
	 * Testing purposes
	 * @param args
	 */
	public static void main(String[] args) {
		Writer sb = new Writer();

		sb.println("# Beginning of program");
		sb.println("\n.globl main");
		sb.println("main:");
		
		Scope s = new Scope(sb);
		Register t0 = s.allocate_t();
		Register t1 = s.allocate_t();
		Register t2 = s.allocate_t();
		Register t3 = s.allocate_t();
		Register t4 = s.allocate_t();
		Register t5 = s.allocate_t();
		Register t6 = s.allocate_t();
		Register t7 = s.allocate_t();
		Register t8 = s.allocate_t();
		Register t9 = s.allocate_t();
		
		sb.println("li %s, 12", t3);
		sb.println("li %s, 34", t8);
		sb.println("add %s, %s, %s", t9, t3, t8);
		// Print integer call code
		sb.println("li $v0, 1");
		// Print sum of 12 and 34
		sb.println("or $a0, %s, $0", t9);
		sb.println("syscall");
		sb.println("\njr $ra		# End of program");
		
		System.out.println(sb);
		System.out.println("\n\nTemp registers LRU: " + s.tLRU.accessOrder);
		System.out.println("Named registers LRU: " + s.sLRU.accessOrder);
	}
}
