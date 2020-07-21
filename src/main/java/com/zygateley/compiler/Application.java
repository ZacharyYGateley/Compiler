package com.zygateley.compiler;

import java.io.*;

public class Application {
	private static void log(String message, FileWriter logFile) throws IOException {
		message += "\n";
		System.out.print(message);
		if (logFile != null) {
			logFile.append(message);
		}
	}
	
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
		String sourceFile;
		//sourceFile = FileIO.getAbsolutePath(".test/test.fnc");
		sourceFile = FileIO.getAbsolutePath("Examples/Example0/Example0.fnc");
		PushbackReader pushbackReader = FileIO.getReader(sourceFile);
		
		String baseName = sourceFile.substring(0, sourceFile.lastIndexOf('.'));
		
		String logFileName = baseName + "_log.txt";
		FileWriter logFile = FileIO.getWriter(logFileName, true);
		// Will return null if file exists and user chooses not to override
		
		String pythonFileName = baseName + ".py";
		FileWriter pythonFile = FileIO.getWriter(pythonFileName, true);
		// Will return null if file exists and user chooses not to override
		
		String assemblyFileName = baseName + ".asm";
		FileWriter assemblyFile = FileIO.getWriter(assemblyFileName, true);
		// Will return null if file exists and user chooses not to override
		
		// Objects passed to Parser
		SymbolTable symbolTable = new SymbolTable();
		TokenStream tokenStream = new TokenStream();
		
		// Make sure to close appropriate streams
		String compiledFileName = null;
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
				
				// Type check and set types where applicable
				TypeSystem.typeAssignAndCheck(optimizedTree);
				log("\n<!-- Type checker initialized -->\n\n", logFile);
				log(optimizedTree.asXMLTree(0, false), logFile);
				log("\n<!-- Type checker finished -->\n\n", logFile);
				
				// Translate into Python
				if (pythonFile != null) {
					PythonTranslator translator = new PythonTranslator(optimizedTree, pythonFile);
					translator.toPython();
				}

				// Assemble
				Assembler assembler = new Assembler(optimizedTree, symbolTable, GoAsm.class, assemblyFile);
				log("\n<!-- Assembler initialized -->\n\n", logFile);
				assembler.assemble(true);
				log("\n<!-- Assembler finished -->\n\n", logFile);
				if (assemblyFile != null) {
					assemblyFile.close();
				}
				
				// Compile
				AssyLanguage language = assembler.getLanguage();
				log("\n<!-- Compiler initialized -->\n\n", logFile);
				compiledFileName = language.compile(assemblyFileName, true);
				log("\n<!-- Compiler finished -->\n\n", logFile);
			}
		}
		catch (Exception err) {
			throw new Exception(err);
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
			if (pythonFile != null) {
				pythonFile.close();
				System.out.println("Python translation written to:\n\t" + pythonFileName);
			}
			if (assemblyFile != null) {
				System.out.println("Assembly file written to:\n\t" + assemblyFileName);
			}
			if (compiledFileName != null) {
				System.out.println("Compiled file written to:\n\t" + compiledFileName);
			}
		}
	}
}
