package com.zygateley.compiler;

import java.util.ArrayDeque;
import java.util.Iterator;

import com.zygateley.compiler.AssyLanguage.Register;

public class Scope implements Iterable<Variable> {
	public final Scope parent;
	// Contains all scope variables
	private final ArrayDeque<Variable> stack;
	private AssyLanguage language;
	
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
	
	/*
	public Variable declareVariable(Register register, Symbol symbol) throws Exception {
		this.language.io.println("; Declare new variable " + symbol);
		Variable variable = new Variable(symbol);
		variable.linkRegister(register);
		this.pushVariable(register, variable);
		return variable;
	}
	*/
	
	public int size() {
		return this.stack.size();
	}
	
	public int getStackOffset(Variable variable) throws Exception {
		if (variable.getStackOffset() < 0) {
			if (variable.register == null) {
				throw new Exception(String.format("Variable %s is not accessible!", variable));
			}
			else {
				this.pushVariable(variable);
			}
		}
		return this.size() - variable.getStackOffset() - 1;
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
		variable.setStackOffset(this.stack.size());
		this.stack.push(variable);
		return variable;
	}
	
	/*
	@Deprecated
	public void pushVariable(Register register, Variable variable) throws Exception {
		this.language.assemblePush(register);
		variable.stackIndex = stack.size();
		stack.push(variable);
	}
	*/

	/**
	 * Must adjust the stack pointer in your respective assy language
	 * @param numberOfVars
	 * @throws Exception
	 */
	public void popAnonymous(int numberOfVars) throws Exception {
		for (int i = 0; i < numberOfVars; i++) this.stack.pop();
	}
	
	public void popAnonymous(Register toRegister) throws Exception {
		this.language.io.setComment("Anonymous value removed from stack");
		this.language.assemblePop(toRegister);
		this.stack.pop();
	}
	
	public void pushAnonymous(Register fromRegister) throws Exception {
		this.pushAnonymous(fromRegister.toString());
	}
	
	public void pushAnonymous(String value) throws Exception {
		this.language.io.setComment("Anonymous value added to stack");
		this.language.assemblePush(value);
		this.stack.push(Variable.NONE);
	}
	
	public void pushVariable(Variable variable) throws Exception { 
		this.language.io.setComment("Linked variable added to stack");
		this.language.assemblePush(variable.register);
		if (variable.register == null) {
			throw new Exception(String.format("Variable %s is not currently in a register!", variable));
		}
		variable.setStackOffset(this.stack.size());
		this.stack.push(variable);
	}
	@Override
	public Iterator<Variable> iterator() {
		// TODO Auto-generated method stub
		return this.stack.iterator();
	}
}