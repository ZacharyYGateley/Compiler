package com.zygateley.compiler;

import java.util.*;

class Symbol {
	public enum Type {
		NULL,
		VAR,
		BOOLEAN,
		INT,
		STRING,
		FUNCTION
	}
	
	private String name;
	private final String value;
	private Type type;
	private Node parseTree;
	
	public Symbol(String name) {
		this.name = name;
		this.value = null;
		this.type = null;
		this.parseTree = null;
	}
	public Symbol(String value, Type type) {
		this.name = null;
		this.value = value;
		this.type = type;
		this.parseTree = null;
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
	
	// Need to be able to update VAR to FUNCTION
	public void setType(Type type) {
		this.type = type;
	}
	
	public void setParseTree(Node parseTree) {
		this.parseTree = parseTree;
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
		return __insert__(newSymbol);
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
		return __insert__(newSymbol);
	}
	
	public Symbol __insert__(Symbol s) {
		if (!this.contains(s)) {
			this.symbols.add(s);
			return s;
		}
		else {
			return this.find(s);
		}
	}
	
	/**
	 * find
	 * 
	 * Find and return the symbol in the symbol table.
	 * If not in symbol table, return null.
	 * 
	 * @param Symbol find duplicate of this symbol in the table
	 */
	public Symbol find(Symbol s) {
		for (Symbol symbol : this.symbols) {
			if (symbol.equals(s)) {
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
