package com.zygateley.compiler;

import java.util.ArrayList;

class Symbol {
	public enum Type {
		VAR,
		BOOLEAN,
		INT,
		STRING		
	}
	
	protected final String name;
	protected final Type type;
	
	public Symbol(String name, Type type) {
		this.name = name;
		this.type = type;
	}
	
	public String getName() {
		return this.name;
	}
	
	/**
	 * equals
	 * 
	 * Compare symbol against passed symbol parameters.
	 * Return true if there is a match.
	 * 
	 * @param name
	 * @return boolean equivalent
	 */
	public boolean equals(String name, Type type) {
		return this.name == name && this.type == type;
	}
	/**
	 * equals
	 * 
	 * Compare this symbol against another.
	 * Return true if they are equal.
	 * 
	 * @param s comparator symbol
	 * @return boolean equivalent
	 */
	public boolean equals(Symbol s, Type type) {
		return this.name == s.name && this.type == type;
	}
}

public class SymbolTable {
	private ArrayList<Symbol> symbols;
	
	public SymbolTable() {
		this.symbols = new ArrayList<Symbol>();
	}
	
	/**
	 * insert
	 * 
	 * Starting off simple. Just have names in the 
	 * symbol table, not scope or type
	 * 
	 * @param name String name of new variable
	 */
	public Symbol insert(String name, Symbol.Type type) {
		Symbol newSymbol = new Symbol(name, type);
		if (!this.contains(newSymbol)) {
			this.symbols.add(newSymbol);
			return newSymbol;
		}
		else {
			return null;
		}
	}
	
	/**
	 * find
	 * 
	 * Find and return the symbol in the symbol table.
	 * If not in symbol table, return null.
	 * 
	 * @param name String name of variable to find
	 */
	public Symbol find(String name) {
		for (Symbol symbol : this.symbols) {
			if (symbol.getName().equals(name)) {
				return symbol;
			}
		}
		return null;
	}
	
	/**
	 * contains
	 * 
	 * Determine whether symbol exists in the symbol
	 * table.
	 * 
	 * @param comparator symbol to check existence
	 */
	private boolean contains(Symbol comparator) {
		for (Symbol symbol : this.symbols) {
			if (symbol.equals(comparator)) {
				return true;
			}
		}
		return false;
	}
}
