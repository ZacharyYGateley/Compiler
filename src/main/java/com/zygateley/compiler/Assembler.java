package com.zygateley.compiler;

import com.zygateley.compiler.MIPS.*;
import java.util.*;

public class Assembler {
	private ParseNode parseTree;
	private SymbolTable symbolTable;
	
	private HashMap<Symbol, String> stringPool; 
	private int stringPoolCount;
	private Writer io;
	private Scope global;
	
	public Assembler(ParseNode parseTree, SymbolTable symbolTable) {
		this.parseTree = parseTree;
		this.symbolTable = symbolTable;
		
		this.stringPool = new HashMap<Symbol, String>();
		this.stringPoolCount = 0;
		this.io = new Writer();
		this.global = new Scope(this.io);
	}
	
	public String assemble() {
		// Create a global string pool
		createStringPool();
		
		// Indicate start of program main
		io.println("");
		io.println(".text");
		io.println(".globl main   # Begin program");
		io.println("main:");
		
		// Main program indented
		io.indent();
		assembleNode(this.parseTree);
		
		// Indicate end of program main
		io.println("");
		io.println("jr $ra        # EOF program");
		
		return this.io.toString();
	}
	
	private void assembleNode(ParseNode pn) {
		
	}
	
	/**
	 * createStringPool
	 * 
	 * Find all string literals and create 
	 * global string pool with these values,
	 * naming them along the way. 
	 *  
	 */
	private void createStringPool() {
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
}