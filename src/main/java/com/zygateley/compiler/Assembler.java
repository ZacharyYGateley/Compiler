package com.zygateley.compiler;

import java.io.FileWriter;
import java.lang.Exception;

public class Assembler {
	private Node parseTree;
	private Writer io;
	private AssyLanguage language;
	private boolean verbose;
	
	public Assembler(Node parseTree, SymbolTable symbolTable, Class<? extends AssyLanguage> Language) throws Exception {
		this(parseTree, symbolTable, Language, null);
	}
	public Assembler(Node parseTree, SymbolTable symbolTable, Class<? extends AssyLanguage> Language, FileWriter fileWriter) throws Exception {
		this.parseTree = parseTree;
		this.io = new Writer(fileWriter);
		// Initialize new instance of the assembly language
		this.language = Language.getDeclaredConstructor(Assembler.Writer.class, SymbolTable.class).newInstance(this.io, symbolTable);
	}
	
	public String assemble() throws Exception {
		return this.assemble(false);
	}
	public String assemble(boolean verbose) throws Exception {
		this.io.setVerbose(verbose);
		
		// Any headers before the data section
		language.assembleHeader();
		
		// Create a global string pool
		language.assembleDataSection();
		
		// Indicate start of program main
		language.assembleCodeSection(this.parseTree);
		
		// Indicate end of program main
		language.assembleFinish();
		
		// Output all functions
		// All functions are considered global from the viewpoint of assembly
		language.assembleFooter();
		
		return this.io.toString();
	}
	
	public AssyLanguage getLanguage() {
		return this.language;
	}

	public static class Writer {
		private final StringBuilder stringBuilder;
		private final FileWriter fileWriter;
		private int currentIndent;
		private final String indentString = "    ";
		private boolean newLine = true;
		private String comment = "";
		private int commentsAt = 40;
		private boolean verbose = false;
		
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
				String indent = new String(new char[currentIndent]).replace("\0",  this.indentString);
				stringBuilder.append(indent);
				if (fileWriter instanceof FileWriter) {
					fileWriter.append(indent);
				}
				if (verbose) {
					System.out.print(indent);
				}
				newLine = false;
			}
			
			stringBuilder.append(s);
			if (fileWriter instanceof FileWriter) {
				fileWriter.append(s);
			}
			if (verbose) {
				System.out.print(s);
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
			// Add formatters
			if (formatters != null) {
				s = String.format(s,  formatters);
			}

			// Pad to certain width so that comments align
			// But do not truncate
			if (!this.comment.isBlank() && s.length() < 40) {
				int width = this.commentsAt - (currentIndent * this.indentString.length());
				s = String.format("%-" + width + "s", s);
			}
			
			// Print (possibly padded) value
			print(s);
			
			// Add comment
			if (!this.comment.isBlank()) { 
				print("; " + comment);
				this.comment = "";
			}
			
			// End line
			print("\r\n");
			newLine = true;
		}
		
		public void indent() {
			this.currentIndent++;
		}
		
		public void outdent() {
			this.currentIndent = Math.max(this.currentIndent - 1, 0);
		}
		
		public void setComment(String comment) {
			this.comment = comment;
		}
		public void setComment(String comment, Object... formatters) {
			this.comment = String.format(comment, formatters);
		}
		
		public void setVerbose(boolean verbose) {
			this.verbose = verbose;
		}
		
		@Override
		public String toString() {
			return stringBuilder.toString();
		}
	}
}