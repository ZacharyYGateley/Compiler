package com.zygateley.compiler;

import java.io.*;

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
		/*
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
				*/
		//PushbackReader sr = new PushbackReader(new StringReader("aj();"));
		
		String sourceFile = FileIO.getAbsolutePath(".test/test.fnc");
		PushbackReader pushbackReader = FileIO.getReader(sourceFile);
		String baseName = sourceFile.substring(0, sourceFile.lastIndexOf('.'));
		String pythonFile = baseName + ".py";
		FileWriter targetFile = FileIO.getWriter(pythonFile);
		// File already exists?
		if (targetFile == null) {
			return;
		}
		
		// Objects passed to Parser
		SymbolTable symbolTable = new SymbolTable();
		TokenStream tokenStream = new TokenStream();
		
		// Break down into tokens 
		// and populate symbol tree
		Lexer lexer = new Lexer(pushbackReader, tokenStream, symbolTable);
		lexer.lex(true);
		
		// Build syntax tree
		Parser parser = new Parser(tokenStream);
		Node syntaxTree = parser.parse(true);
		if (syntaxTree instanceof Node) {
			// Run backend into appropriate language
			Translator tr = new Translator(syntaxTree, targetFile);
			String output = tr.toPython();
			System.out.println("\n\nGenerated code: \n\n\n" + output);
		}
		
		pushbackReader.close();
		targetFile.close();
		
		System.out.println("Python translation written to " + pythonFile);
		
		//Assembler a = new Assembler(syntaxTree, st);
		//output = a.assemble();
		//System.out.println("\n\nGenerated assembly code: \n\n\n" + output);
	}

}
