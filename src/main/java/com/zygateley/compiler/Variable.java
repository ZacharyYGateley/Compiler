package com.zygateley.compiler;

import com.zygateley.compiler.AssyLanguage.Register;

public class Variable {
	public Register register = null;
	public final Symbol symbol;
	public TypeSystem type;
	private int stackIndex = -1;
	public final static Variable NONE = new Variable();
	
	public Variable() {
		symbol = null;
	}
	public Variable(Register r) {
		register = r;
		symbol = null;
	}
	/**
	 * Symbols (one-to-one with Variables) are created once, before all variables are created.
	 * Thus, Variable symbols are final.
	 * @param s
	 */
	public Variable(Symbol s) {
		symbol = s;
	}
	
	/**
	 * Distance from first item in stack.<br />
	 * <strong>Not the stack pointer offset</strong>. Use Scope.getStackOffset for that.
	 * @return
	 */
	public int getStackIndex() {
		return this.stackIndex;
	}
	
	public boolean inStack() {
		return this.stackIndex > -1;
	}
	
	public void setStackIndex(int stackIndex) {
		this.stackIndex = stackIndex;
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
