package com.zygateley.compiler;

import java.util.ArrayDeque;
import java.util.Iterator;

import com.zygateley.compiler.AssyLanguage.Register;

class Variable {
	public Register register = null;
	public final Symbol symbol;
	public TypeSystem type;
	public int stackIndex = -1;
	public static Variable NONE = new Variable(null);
	
	public Variable(Symbol s) {
		symbol = s;
	}
	
	public Symbol getSymbol() {
		return this.symbol;
	}
	
	public TypeSystem getType() {
		return this.type;
	}
	
	public void setType(TypeSystem type) {
		this.type = type;
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

public class Scope implements Iterable {
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
		variable.stackIndex = this.stack.size();
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
	
	public void pushAnonymous(Register fromRegister) throws Exception {
		this.pushAnonymous(fromRegister.toString());
	}
	
	public void pushAnonymous(String value) throws Exception {
		this.language.io.setComment("Anonymous value added to stack");
		this.language.assemblePush(value);
	}
	
	public void popAnonymous(Register toRegister) throws Exception {
		this.language.io.setComment("Anonymous value removed from stack");
		this.language.assemblePop(toRegister);
	}
	@Override
	public Iterator<Variable> iterator() {
		// TODO Auto-generated method stub
		return this.stack.iterator();
	}
}