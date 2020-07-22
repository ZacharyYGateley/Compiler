package com.zygateley.compiler;

import java.io.*;

public class Application {
	private static final String version = "v0.9.0-beta";
	private static FileWriter logFile = null;
	private static boolean writeLogFile = false;
	private static boolean verbose = false;
	
	private static void log(String message) throws IOException {
		message += "\n";
		if (verbose) {
			System.out.print(message);
		}
		if (writeLogFile) {
			logFile.append(message);
		}
	}
	
	/**
	 * Show available command line options
	 */
	private static void help() {
		help(null);
	}
	
	/**
	 * Show available command line options
	 * @param cause why this help is being shown 
	 */
	private static void help(String cause) {
		final String help = "com.zygateley.compiler version " + version + " Copyright Zachary Gateley 2020\n\n"+
				"java -jar zyg_compile.jar [-alnpv] path/to/inputFile.fnc\n\n"+
				"Flags:\n" +
				"\ta\tKeep the assembly file after compilation (writes to inputFile.asm)\n"+
				"\th\tShow this help screen\n"+
				"\tl\tWrite log file to inputFile_log.txt\n"+
				"\tn\tDo not create executable\n"+
				"\tp\tTranslate the code to python (writes to inputFile.py)\n"+
				"\tv\tVerbose output\n\n";
		System.out.println("\n");
		if (cause != null && !cause.isBlank()) {
			System.out.println(cause + "\n\n");
		}
		System.out.println(help);
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
		// Flag -a ==> true
		boolean keepAssembly = false;
		// Flag -l ==> true
		Application.writeLogFile = false;
		// Flag -p ==> true
		boolean translateToPython = false;
		// Flag -v ==> true
		Application.verbose = false;
		// Flag -x ==> false
		boolean createExecutable = true;
		
		// Input validation
		String sourceFile = "";
		boolean testing = false;
		if (testing) {
			//sourceFile = "../../.test/test.fnc";
			sourceFile = "../../Examples/Example4/Example4.fnc";
			keepAssembly = true;
			writeLogFile = false;
			translateToPython = true;
			verbose = true;
		}
		else {
			if (args.length < 1) {
				help("No source file specified.");
				return;
			}
			else if (args.length > 2) {
				help("Too many arguments.");
				return;
			}
			
			// Flags
			if (args[0].charAt(0) == '-') {
				// Short circuit with help flag
				boolean showHelp = args[0].indexOf('h') > 0;
				if (showHelp) {
					help();
					return;
				}
				
				if (args.length != 2) {
					help("Not enough arguments. Source file not specified.");
					return;
				}
				
				// Flags are first argument
				keepAssembly = args[0].indexOf('a') > 0;
				writeLogFile = args[0].indexOf('l') > 0;
				createExecutable = args[0].indexOf('n') < 0;
				translateToPython = args[0].indexOf('p') > 0;
				verbose = args[0].indexOf('v') > 0;
				
				// Source file is second argument
				sourceFile = args[1];
			}
			else {
				sourceFile = args[0];
			}
		}
		
		sourceFile = FileIO.getAbsolutePath(Application.class, sourceFile);
		PushbackReader pushbackReader = FileIO.getReader(sourceFile);
		
		String baseName = sourceFile.substring(0, sourceFile.lastIndexOf('.'));
		
		
		String logFileName = "";
		if (writeLogFile) {
			logFileName = baseName + "_log.txt";
			logFile = FileIO.getWriter(logFileName, testing);
			// Will return null if file exists and user chooses not to overwrite
			if (logFile == null) writeLogFile = false;
		}
		
		String pythonFileName = "";
		FileWriter pythonFile = null;
		if (translateToPython) {
			pythonFileName = baseName + ".py";
			pythonFile = FileIO.getWriter(pythonFileName, testing);
			// Will return null if file exists and user chooses not to overwrite
			if (pythonFile == null) translateToPython = false;
		}
		
		// Must write assembly file
		// If do not want to keep, erase later
		String assemblyFileName = baseName + ".asm";
		FileWriter assemblyFile = FileIO.getWriter(assemblyFileName, testing);
		// Will return null if file exists and user chooses not to overwrite
		if (assemblyFile == null && createExecutable) {
			System.out.println("Overwriting the assembly file is required to continue.\nCompilation aborted.\n\n");
			return;
		}
		
		// Objects passed to Parser
		SymbolTable symbolTable = new SymbolTable();
		TokenStream tokenStream = new TokenStream();
		
		// Delete any previous compiled files
		String compiledFileName = null;
		if (createExecutable) {
			FileWriter compiledFile = FileIO.getWriter(baseName + ".exe");
			// Returns null if user does not want to overwrite
			if (compiledFile != null) {
				compiledFile.close();
				new File(baseName + ".exe").delete();
			}
			else createExecutable = false;
		}
		
		try {
			// Break down into tokens 
			// and populate symbol tree
			Lexer lexer = new Lexer(pushbackReader, tokenStream, symbolTable, logFile);
			lexer.lex(verbose);
			
			// Build syntax tree
			Parser parser = new Parser(tokenStream, logFile);
			Node syntaxTree = parser.parse(verbose);
			if (syntaxTree instanceof Node) {
				// Optimize parse tree
				Optimizer optimizer = new Optimizer(logFile);
				Node optimizedTree = optimizer.optimize(syntaxTree, verbose);
				
				// Type check and set types where applicable
				TypeSystem.typeAssignAndCheck(optimizedTree);
				if (verbose || writeLogFile) {
					log("\n<!-- Type checker initialized -->\n\n");
					log(optimizedTree.asXMLTree(0, false));
					log("\n<!-- Type checker finished -->\n\n");
				}
				
				// Assemble
				Assembler assembler = new Assembler(optimizedTree, symbolTable, GoAsm.class, assemblyFile);
				if (verbose || writeLogFile) log("\n<!-- Assembler initialized -->\n\n");
				assembler.assemble(verbose);
				if (verbose || writeLogFile) log("\n<!-- Assembler finished -->\n\n");
				
				if (assemblyFile != null) {
					assemblyFile.close();
				}
				
				// Compile
				if (createExecutable) {
					AssyLanguage language = assembler.getLanguage();
					if (verbose || writeLogFile) log("\n<!-- Compiler initialized -->\n\n");
					compiledFileName = language.compile(assemblyFileName, verbose);
					if (verbose || writeLogFile) log("\n<!-- Compiler finished -->\n\n");
				}
				

				// Translate into Python
				if (pythonFile != null) {
					PythonTranslator translator = new PythonTranslator(optimizedTree, pythonFile);
					translator.toPython();
				}
			}
		}
		catch (Exception err) {
			System.out.println(err.getMessage());
			System.out.println(err.getStackTrace());
			throw new Exception(err);
		}
		finally {
			System.out.println("");
			
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
				if (keepAssembly) {
					System.out.println("Assembly file written to:\n\t" + assemblyFileName);
				}
				else {
					// Erase assembly file
					new File(assemblyFileName).delete();
				}
			}
			if (compiledFileName != null) {
				System.out.println("Compiled file written to:\n\t" + compiledFileName);
			}
		}
		System.out.println("");
	}
}
