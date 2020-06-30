package com.zygateley.compiler;

import com.zygateley.compiler.MIPS.*;
import java.util.*;

public class Assembler {
	private Node parseTree;
	private SymbolTable symbolTable;
	
	private HashMap<Symbol, String> stringPool; 
	private int stringPoolCount;
	private Writer io;
	private Scope global;
	
	public Assembler(Node parseTree, SymbolTable symbolTable) {
		this.parseTree = parseTree;
		this.symbolTable = symbolTable;
		
		this.stringPool = new HashMap<Symbol, String>();
		this.stringPoolCount = 0;
		this.io = new Writer();
		this.global = new Scope(this.io);
	}
	
	public String assemble() {
		// Create a global string pool
		createAndOutputStringPool();
		
		// Indicate start of program main
		io.println("");
		io.println(".text");
		io.println(".globl main   # Begin program");
		io.println("main:");
		io.indent();
		
		// Crawl tree
		// Any function declarations found
		// will be stored into SymbolTable as type FUNCTION
		assembleNode(this.parseTree);
		
		// Indicate end of program main
		io.println("");
		io.println("jr $ra        # EOF program");
		
		// Output all functions
		// All functions are considered global
		outputFunctions();
		
		return this.io.toString();
	}
	
	private void assembleNode(Node pn) {
		boolean isNonTerminal = (pn.getRule() != null);
		if (isNonTerminal) {
			assembleNonTerminal(pn);
		}
		else {
			assembleTerminal(pn);
		}
	}
	
	private void assembleNonTerminal(Node pn) {
		NonTerminal rule = pn.getRule();
		switch (rule) {
		case _FUNCDEF_:
			// Save all functions into SymbolTable
			// To be processed and output at the end of file
			Symbol symbol = pn.childNodes().get(1).getSymbol();
			symbol.setType(Symbol.Type.FUNCTION);
			symbol.setParseTree(pn);
			
			// Do not parse this tree now
			return;
		default:
			break;
		}
		
		// Iterate
		for (Node child : pn) {
			if (child != null) {
				assembleNode(child);
			}
		}
	}
	
	private void assembleTerminal(Node pn) {
		
	}
	
	/**
	 * createStringPool
	 * 
	 * Find all string literals and create 
	 * global string pool with these values,
	 * naming them along the way. 
	 *  
	 */
	private void createAndOutputStringPool() {
		io.println(".data         # String pool");
		
		// Indent the next block
		io.indent();
		for (Symbol symbol : this.symbolTable) {
			if (symbol.getType() == Symbol.Type.STRING) {
				// String found
				// Name it by autoincrement, 
				// then add it to the string pool
				String name = String.format("str%d", stringPoolCount++);
				symbol.setName(name);
				io.println("%s:\t.asciiz %s", name, symbol.getValue());
			}
		}
		// Finished this block
		io.outdent();
	}
	
	/**
	 * outputFunctions
	 * 
	 * All functions are stored in the SymbolTable as type FUNCTION.
	 * Output all functions at once at the end of the file
	 * 
	 */
	private void outputFunctions() {
		
	}
}