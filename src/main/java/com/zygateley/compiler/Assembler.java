package com.zygateley.compiler;

import java.io.FileWriter;
import java.lang.Exception;
import java.util.*;

public class Assembler {
	private Node parseTree;
	private SymbolTable symbolTable;
	private Writer io;
	private AssyLanguage language;
	
	public Assembler(Node parseTree, SymbolTable symbolTable, Class<? extends AssyLanguage> Language) throws Exception {
		this(parseTree, symbolTable, Language, null);
	}
	public Assembler(Node parseTree, SymbolTable symbolTable, Class<? extends AssyLanguage> Language, FileWriter fileWriter) throws Exception {
		this.parseTree = parseTree;
		this.symbolTable = symbolTable;
		this.io = new Writer(fileWriter);
		// Initialize new instance of the assembly language
		this.language = Language.getDeclaredConstructor(Assembler.Writer.class, SymbolTable.class).newInstance(this.io, symbolTable);
	}
	
	public String assemble() throws Exception {
		// Create a global string pool
		language.assembleDataSection();
		
		// Indicate start of program main
		language.assembleHeader();
		
		// Crawl tree
		// Any function declarations found
		// will be stored into SymbolTable as type FUNCTION
		language.assembleNode(this.parseTree);
		
		// Indicate end of program main
		language.assembleFinish();
		
		// Output all functions
		// All functions are considered global
		language.assembleFooter();
		
		return this.io.toString();
	}

	public static class Writer {
		private final StringBuilder stringBuilder;
		private final FileWriter fileWriter;
		private int currentIndent;
		private boolean newLine = true;
		
		public Writer() {
			this(null);
		}
		public Writer(FileWriter fileWriter) {
			stringBuilder = new StringBuilder();
			this.fileWriter = fileWriter;
			currentIndent = 0;
		}
		
		public void print(String s) throws Exception {
			// Indent as necessary
			if (newLine) {
				final String indent = "\t";
				for (int i = 0; i < currentIndent; i++) {
					stringBuilder.append(indent);
					if (fileWriter instanceof FileWriter) {
						fileWriter.append(indent);
					}
				}
				newLine = false;
			}
			
			stringBuilder.append(s);
			if (fileWriter instanceof FileWriter) {
				fileWriter.append(s);
			}
		}
		public void print(String s, Object... formatters) throws Exception {
			// Format strings as necessary
			if (formatters.length > 0) {
				String outputString = String.format(s, (Object[]) formatters);
				print (outputString);
				
				// Promote in LRU all variables used
				for (Object e : formatters) {
					if (e instanceof AssyLanguage.Register) {
						((AssyLanguage.Register) e).promote();
					}
				}
			}
			// No formatters
			else {
				print(s);
			}
		}
		public void println() throws Exception {
			println("");
		}
		public void println(String s) throws Exception {
			println(s, "");
		}
		public void println(String s, Object... formatters) throws Exception {
			print(s, formatters);
			print("\r\n");
			newLine = true;
		}
		
		public void indent() {
			this.currentIndent++;
		}
		
		public void outdent() {
			this.currentIndent = Math.max(this.currentIndent - 1, 0);
		}
		
		@Override
		public String toString() {
			return stringBuilder.toString();
		}
	}
}