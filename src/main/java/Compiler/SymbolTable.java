package Compiler;

import java.util.ArrayList;

class Symbol {
	protected String name;
	
	public Symbol(String name) {
		this.name = name;
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
	public boolean equals(String name) {
		return this.name == name;
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
		return this.name == s.name;
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
	public void insert(String name) {
		Symbol newSymbol = new Symbol(name);
		if (!this.contains(newSymbol)) {
			this.symbols.add(newSymbol);
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
	private Symbol find(String name) {
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
