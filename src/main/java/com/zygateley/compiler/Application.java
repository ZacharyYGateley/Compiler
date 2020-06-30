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
	public static void main(String[] args) throws Exception {
		// TESTING
		String myString =
				"function crazyMath (a, b, c, d, e) {" 
				+ "echo a * (b * c) - d * e;\n" 
				+ "}"
				+ "input abc;\n"
				+ "rob = \"rob\";\n"
				+ "if (abc == rob) {"
				+ "  echo \"abc is bigger than rob\";"
				+ "  echo \"My goodness\\\\\";"
				+ "  echo \"The man with the plan is yo mamma\";"
				+ "}"
				+ "else {"
				+ "  echo \"abc is less than rob\";"
				+ "  echo \"Here's some crazy math for you: \";"
				+ "  crazyMath(1, 2, 3, 4, 5);"
				+ "  echo \"Here's some normal math for you: \";"
				+ "  echo -((33 + -(-22 + (11 * 25) - 11 / 3)) * 2 - -1);"
				+ "}"
				;
		PushbackReader sr = new PushbackReader(new StringReader(
				myString
				));
		System.out.println("Original code: \n");
		System.out.println(myString + "\n\n");
		
		// Objects passed to Parser
		SymbolTable st = new SymbolTable();
		TokenStream ts = new TokenStream();
		
		// Break down into tokens 
		// and populate symbol tree
		Lexer l = new Lexer(sr, ts, st);
		l.lex(true);
		
		// Build syntax tree
		Parser p = new Parser(ts);
		Node syntaxTree = p.parse(true);
		if (syntaxTree == null) {
			return;
		}
		
		// Run backend into appropriate language
		Translator tr = new Translator(syntaxTree);
		String output = tr.toPython();
		System.out.println("\n\nGenerated code: \n\n\n" + output);
		
		Assembler a = new Assembler(syntaxTree, st);
		output = a.assemble();
		System.out.println("\n\nGenerated assembly code: \n\n\n" + output);
	}

}
