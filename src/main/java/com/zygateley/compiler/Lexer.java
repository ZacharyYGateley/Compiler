package com.zygateley.compiler;

import java.io.InputStream;

public class Lexer {
	private InputStream input;
	private TokenStream output;
	
	/**
	 * Lexer
	 * 
	 * Requires open input stream to process its tokens
	 * Tokens are then stored in a Tokens object
	 * 
	 * @param stream input stream to process for tokens
	 * @param tokens Tokens output deque
	 */
	public Lexer(InputStream input, TokenStream output) {
		this.input = input;
		this.output = output;
		System.out.println(this.input);
		System.out.println(this.output);
	}
}
