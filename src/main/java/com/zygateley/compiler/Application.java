package com.zygateley.compiler;

public class Application {
	private static Rules rules;
	
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
	 */
	public static void main(String[] args) {
		// Build rules object
		rules = new Rules();
		System.out.println(rules);
	}

}
