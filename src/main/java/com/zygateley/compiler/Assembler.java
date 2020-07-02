package com.zygateley.compiler;

import com.zygateley.compiler.AssyLanguage.*;
import java.util.*;

public class Assembler {
	public static class Writer {
		private final StringBuilder outputStream;
		private int currentIndent;
		private AssyLanguage language;
		
		public Writer() {
			outputStream = new StringBuilder();
			currentIndent = 0;
		}
		
		public void print(String s) {
			print(s);
		}
		public void print(String s, Object... formatters) {
			// Indent as necessary
			for (int i = 0; i < currentIndent; i++) outputStream.append("    ");
			
			// Format strings as necessary
			if (formatters.length > 0) {
				outputStream.append(String.format(s, (Object[]) formatters));
				
				// Promote in LRU all variables used
				for (Object e : formatters) {
					if (e instanceof AssyLanguage.Register) {
						((AssyLanguage.Register) e).promote();
					}
				}
			}
			// No formatters
			else {
				outputStream.append(s);
			}
		}
		public void println(String s) {
			println(s, "");
		}
		public void println(String s, Object... formatters) {
			print(s, formatters);
			outputStream.append("\r\n");
		}
		
		public void indent() {
			this.currentIndent++;
		}
		
		public void outdent() {
			this.currentIndent--;
		}
		
		@Override
		public String toString() {
			return outputStream.toString();
		}
	}
	
	
	private Node parseTree;
	private SymbolTable symbolTable;
	private Writer io;
	private AssyLanguage language;
	
	public Assembler(Node parseTree, SymbolTable symbolTable, Class<AssyLanguage> Language) throws Exception {
		this.parseTree = parseTree;
		this.symbolTable = symbolTable;
		this.io = new Writer();
		this.language = Language.getDeclaredConstructor().newInstance();
	}
	
	public String assemble() {
		// Create a global string pool
		language.assembleGlobal();
		
		// Indicate start of program main
		language.assembleHeader();
		
		// Crawl tree
		// Any function declarations found
		// will be stored into SymbolTable as type FUNCTION
		language.assembleNode(this.parseTree);
		
		// Indicate end of program main
		io.println("");
		io.println("jr $ra        # EOF program");
		
		// Output all functions
		// All functions are considered global
		language.assembleFooter();
		
		return this.io.toString();
	}
}