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
		
		String sourceFile = FileIO.getAbsolutePath("Examples/Example0.fnc");
		PushbackReader pushbackReader = FileIO.getReader(sourceFile);
		
		String baseName = sourceFile.substring(0, sourceFile.lastIndexOf('.'));
		
		String logFileName = baseName + "_log.txt";
		FileWriter logFile = FileIO.getWriter(logFileName, true);
		// Will return null if file exists and user chooses not to override
		
		String pythonFileName = baseName + ".py";
		FileWriter pythonFile = FileIO.getWriter(pythonFileName, true);
		// Will return null if file exists and user chooses not to override
		
		// Objects passed to Parser
		SymbolTable symbolTable = new SymbolTable();
		TokenStream tokenStream = new TokenStream();
		
		// Make sure to close appropriate streams
		try {
			// Break down into tokens 
			// and populate symbol tree
			Lexer lexer = new Lexer(pushbackReader, tokenStream, symbolTable, logFile);
			lexer.lex(true);
			
			// Build syntax tree
			Parser parser = new Parser(tokenStream, logFile);
			Node syntaxTree = parser.parse(true);
			if (syntaxTree instanceof Node) {
				// Optimize parse tree
				Optimizer optimizer = new Optimizer(logFile);
				Node optimizedTree = optimizer.optimize(syntaxTree, true);
				
				// Translate into Python
				if (pythonFile != null) {
					PythonTranslator translator = new PythonTranslator(optimizedTree, pythonFile);
					translator.toPython();
				}
			}
		
			if (pythonFile != null) {
				pythonFile.close();
				System.out.println("Python translation written to:\n\t" + pythonFileName);
			}
		}
		finally {
			// Always close pushbackReader
			// and logFile
			// pythonFile should be empty if there is an exception
			pushbackReader.close();
			if (logFile != null) {
				logFile.close();
				System.out.println("Compilation log written to:\n\t" + logFileName);
			}
		}
		
		//Assembler a = new Assembler(syntaxTree, st);
		//output = a.assemble();
		//System.out.println("\n\nGenerated assembly code: \n\n\n" + output);
	}

}
