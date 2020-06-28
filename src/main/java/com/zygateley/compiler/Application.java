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
		PushbackReader sr = new PushbackReader(new StringReader(
				/*"function printIt (g, m) {echo g;\n" 
				+ "echo m;}"
				+ "input abc;\n"
				+ "rob = 3;\n"
				+ "if (abc > rob) { printIt(\"abc is bigger than rob\", \"You filthy animal\"); }"
				+ "elseif (abc == rob) { printIt(\"abc is equal to rob\", \"!\"); }"
				+ "else printIt(\"abc is less than rob\", \"And don't you forget it\");"
				*/
				//"if (false) {abc = 5; df = 5; echo abc + df * 6;}elseif ( 2 == 4) b = 2; else{echo \"My goodness\";}" // echo \"The man with the plan is yo mamma\"; {echo true;}"
				"a=(3*2)+4*5;"
				));
		
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
