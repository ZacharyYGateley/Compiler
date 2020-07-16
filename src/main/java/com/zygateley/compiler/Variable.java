package com.zygateley.compiler;

import com.zygateley.compiler.AssyLanguage.Register;

public class Variable {
	public Register register = null;
	public final Symbol symbol;
	public TypeSystem type;
	private int stackOffset = -1;
	public final static Variable NONE = new Variable(null);
	
	public Variable() {
		this(null);
	}
	public Variable(Symbol s) {
		symbol = s;
	}
	
	public int getStackOffset() {
		return this.stackOffset;
	}
	public void setStackOffset(int stackOffset) {
		this.stackOffset = stackOffset;
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
