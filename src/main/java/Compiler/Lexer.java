package Compiler;

import Compiler.SymbolTable;
import java.io.InputStream;

public class Lexer {
	private InputStream input;
	private Tokens output;
	
	/**
	 * Lexer
	 * 
	 * Requires open input stream to process its tokens
	 * Tokens are then stored in a Tokens object
	 * 
	 * @param stream input stream to process for tokens
	 * @param tokens Tokens output deque
	 */
	public Lexer(InputStream input, Tokens output) {
		this.input = input;
		this.output = output;
	}
}
