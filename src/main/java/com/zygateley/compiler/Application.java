package com.zygateley.compiler;

import java.io.IOException;
import java.io.StringReader;

public class Application {
	/**
	 * main
	 * 
	 * Call this file with the un-compiled version 
	 * of your file as a parameter.
	 * 
	 * Front end
	 * 		Lexes then parses input
	 * Back end
	 * 		Symbol table and syntax tree passed to back end
	 * 		Then outputs "compiled" version with ext ".py"
	 * 		Long-term goal: MIPS assembly, possibly
	 * 
	 * @param args un-compiled file
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TESTING
		StringReader sr = new StringReader("abc = 33; echo 44;");
		
		// Objects passed to Parser
		SymbolTable st = new SymbolTable();
		TokenStream ts = new TokenStream();
		
		// First lex the string
		Lexer l = new Lexer(sr, ts);
		l.lex(st);
		
		// Build rules object
		//Parser p = new Parser(ts, rules);
		//String output = p.parse();
		System.out.println(l);
	}

}
