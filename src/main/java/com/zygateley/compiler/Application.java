package com.zygateley.compiler;

import java.io.IOException;
import java.io.PushbackReader;
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
		PushbackReader sr = new PushbackReader(new StringReader("{abc = 5; df = 5; echo abc + df * 6;} echo \"The man with the plan is yo mamma\"; {echo true;}"));
		
		// Objects passed to Parser
		SymbolTable st = new SymbolTable();
		TokenStream ts = new TokenStream();
		
		// Break down into tokens 
		// and populate symbol tree
		Lexer l = new Lexer(sr, ts, st);
		l.lex(true);
		
		// Build parse tree
		Parser p = new Parser(ts);
		ParseNode parseTree = p.parse(true);
		if (parseTree == null) {
			return;
		}
		
		// Run backend into appropriate language
		Backend be = new Backend(parseTree);
		String output = be.python();
		System.out.println(output);
	}

}
