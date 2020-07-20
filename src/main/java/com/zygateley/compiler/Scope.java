package com.zygateley.compiler;

import java.util.ArrayDeque;
import java.util.Iterator;

import com.zygateley.compiler.AssyLanguage.Register;

public class Scope implements Iterable<Variable> {
	public final Scope parent;
	// Contains all scope variables
	private final ArrayDeque<Variable> stack;
	private AssyLanguage language;
	private Variable heapPool;
	
	public Scope(Scope parent) {
		this(null, parent);
	}
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
	
	public int size() {
		return this.stack.size();
	}
	
	public int getStackOffset(Variable variable) throws Exception {
		if (variable.getStackIndex() < 0) {
			if (variable.register == null) {
				throw new Exception(String.format("Variable %s is not accessible!", variable));
			}
			else {
				this.pushVariable(variable);
			}
		}
		return this.size() - variable.getStackIndex() - 1;
	}
	
	public Variable getVariable(Symbol symbol) {
		for (Variable variable : this.stack) {
			if (variable.symbol == symbol) {
				return variable;
			}
		}
		return null;
	}
	
	public void setLanguage(AssyLanguage language) {
		this.language = language; 
	}
	
	public Variable addVariable(Symbol symbol) throws Exception {
		Variable variable = new Variable(symbol);
		variable.setStackIndex(this.stack.size());
		this.stack.push(variable);
		return variable;
	}

	/**
	 * Must adjust the stack pointer in your respective assy language
	 * @param numberOfVars
	 * @throws Exception
	 */
	public void popAnonymous(int numberOfVars) throws Exception {
		for (int i = 0; i < numberOfVars; i++) this.stack.pop();
	}
	
	public void pop(Register toRegister) throws Exception {
		this.language.io.setComment("Anonymous value removed from stack");
		this.language.assemblePop(toRegister, true);
		this.stack.pop();
	}
	
	public void pushAnonymous(Register fromRegister) throws Exception {
		if (fromRegister.variable == null) {
			this.pushAnonymous(fromRegister.toString());
		}
		else {
			this.pushVariable(fromRegister.variable);
		}
	}
	
	public void pushAnonymous(String value) throws Exception {
		this.language.io.setComment("Anonymous value added to stack");
		this.language.assemblePush(value, true);
		this.stack.push(Variable.NONE);
	}
	
	public void pushVariable(Variable variable) throws Exception { 
		if (variable.getStackIndex() < 0) {
			// Increase stack size, insert variable
			this.language.io.setComment("Linked variable added to stack");
			this.language.assemblePush(variable.register.toString(), true);
			if (variable.register == null) {
				throw new Exception(String.format("Variable %s is not currently in a register!", variable));
			}
			variable.setStackIndex(this.stack.size());
			this.stack.push(variable);
		}
	}
	
	public String getHeapPoolAddress() throws Exception {
		return String.format("[Esp + %d]", this.getStackOffset(this.heapPool) * 4);
	}
	
	/**
	 * Heap pool
	 * Address 2[this.heapPool] == Number of allocations
	 * Address 2[this.heapPool + 2] == Allocation capacity
	 * Address 4[this.heapPool + 4*n] == Allocation
	 * 
	 * @param addressRegister
	 * @throws Exception
	 */
	public void setHeapPoolAddress(Register addressRegister) throws Exception {
		this.heapPool = new Variable(addressRegister);
		this.pushVariable(this.heapPool);
	}
	
	@Override
	public Iterator<Variable> iterator() {
		// TODO Auto-generated method stub
		return this.stack.iterator();
	}
}