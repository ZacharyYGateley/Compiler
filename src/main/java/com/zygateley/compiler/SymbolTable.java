package com.zygateley.compiler;

import java.util.*;

class Symbol {
	public enum Type {
		NULL,
		COMMENT,
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
 * Performs single unescape on all escape characters
 * 
 * Courtesy 
 * https://gist.github.com/uklimaschewski
 * Found at 
 * https://gist.github.com/uklimaschewski/6741769
 * 
 */
	public static String unescapeJavaString(String st) {
	    StringBuilder sb = new StringBuilder(st.length());

	    for (int i = 0; i < st.length(); i++) {
	        char ch = st.charAt(i);
	        if (ch == '\\') {
	            char nextChar = (i == st.length() - 1) ? '\\' : st
	                    .charAt(i + 1);
	            // Octal escape?
	            if (nextChar >= '0' && nextChar <= '7') {
	                String code = "" + nextChar;
	                i++;
	                if ((i < st.length() - 1) && st.charAt(i + 1) >= '0'
	                        && st.charAt(i + 1) <= '7') {
	                    code += st.charAt(i + 1);
	                    i++;
	                    if ((i < st.length() - 1) && st.charAt(i + 1) >= '0'
	                            && st.charAt(i + 1) <= '7') {
	                        code += st.charAt(i + 1);
	                        i++;
	                    }
	                }
	                sb.append((char) Integer.parseInt(code, 8));
	                continue;
	            }
	            switch (nextChar) {
	            case '\\':
	                ch = '\\';
	                break;
	            case 'b':
	                ch = '\b';
	                break;
	            case 'f':
	                ch = '\f';
	                break;
	            case 'n':
	                ch = '\n';
	                break;
	            case 'r':
	                ch = '\r';
	                break;
	            case 't':
	                ch = '\t';
	                break;
	            case '\"':
	                ch = '\"';
	                break;
	            case '\'':
	                ch = '\'';
	                break;
	            // Hex Unicode: u????
	            case 'u':
	                if (i >= st.length() - 5) {
	                    ch = 'u';
	                    break;
	                }
	                int code = Integer.parseInt(
	                        "" + st.charAt(i + 2) + st.charAt(i + 3)
	                                + st.charAt(i + 4) + st.charAt(i + 5), 16);
	                sb.append(Character.toChars(code));
	                i += 5;
	                continue;
	            }
	            i++;
	        }
	        sb.append(ch);
	    }
	    return sb.toString();
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
	
	@Override
	public String toString() {
		return (this.getName() != null ? this.getName() : this.getValue());
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
