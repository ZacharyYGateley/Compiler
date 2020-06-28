package com.zygateley.compiler;

import java.util.*;

class Symbol {
	public enum Type {
		NULL,
		VAR,
		BOOLEAN,
		INT,
		STRING		
	}
	
	private String name;
	private final String value;
	private final Type type;
	
	public Symbol(String name) {
		this.name = name;
		this.value = null;
		this.type = null;
	}
	public Symbol(String value, Type type) {
		this.name = null;
		this.value = value;
		this.type = type;
	}
	
	public String getName() {
		return this.name;
	}
	
	public String getValue() {
		return this.value;
	}
	
	public Type getType() {
		return this.type;
	}
	
	public boolean setName(String newName) {
		if (this.name == null) {
			this.name = newName;
			return true;
		}
		return false;
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
	public boolean equals(String name, String value, Type type) {
		boolean equivalent = true;
		if (this.type == null) {
			// Variable
			// Check name only
			equivalent &= (this.name == name || this.name != null && this.name.equals(name));
		}
		else {
			// Literal
			// Check type and value
			equivalent &= (this.value == value || this.value != null && this.value.equals(name));
			equivalent &= (this.type == type);
		}
		return equivalent;
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
	public boolean equals(Symbol s) {
		return this.equals(s.name, s.value, s.type);
	}
}

public class SymbolTable implements Iterable<Symbol> {
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
	public Symbol insert(String name) {
		Symbol newSymbol = new Symbol(name);
		if (!this.contains(newSymbol)) {
			this.symbols.add(newSymbol);
			return newSymbol;
		}
		else {
			return null;
		}
	}
	
	/**
	 * insert
	 * 
	 * Starting off simple. Just have names in the 
	 * symbol table, not scope or type
	 * 
	 * @param name String name of new variable
	 */
	public Symbol insert(String value, Symbol.Type type) {
		Symbol newSymbol = new Symbol(value, type);
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
	
	@Override
	public Iterator<Symbol> iterator() {
		return this.symbols.iterator();
	}
}
