package com.zygateley.compiler;

import java.io.IOException;
import java.io.StringReader;
import java.util.regex.*;

public class Lexer {
	private StringReader input;
	private TokenStream output;
	// Run on only one character at a time
	private static final Pattern eofToken = Pattern.compile("[\\s|;]");
	
	/**
	 * Lexer
	 * 
	 * Requires open input stream to process its tokens
	 * Tokens are then stored in a Tokens object
	 * 
	 * @param stream input stream to process for tokens
	 * @param tokens Tokens output stream
	 */
	public Lexer(StringReader input, TokenStream output) {
		this.input = input;
		this.output = output;
		System.out.println(this.input);
		System.out.println(this.output);
	}
	
	public void lex(SymbolTable st) throws IOException {
		// All available token types
		Terminal[] tokenRules = Terminal.values();
		// Current token
		StringBuilder buildToken = new StringBuilder();
		// Next character
		int readIn;
		char nextIn;
		while ((readIn = input.read()) > -1) {
			nextIn = (char) readIn;
			
			// At end of current token, determine what kind of token it is
			if (eofToken.matcher("" + nextIn).matches()) {
				// Save time & space by converting to string now
				// Will compare against a handful of regular expressions
				String thisToken = buildToken.toString();
				if (thisToken.length() > 0) {
					// Current token in the works
					// Figure out what kind of token it is
					// Get first match
					for (Terminal tokenRule : tokenRules) {
						Matcher m = tokenRule.regexToken.matcher(thisToken);
						if (m.matches()) {
							// regexEnd indicates 
							// that this token can outlast white space
							if (tokenRule.regexEnd != null) {
								try {
									int _readIn;
									char _nextIn;
									do {
										_readIn = input.read();
										_nextIn = (char) _readIn;
										buildToken.append(_nextIn);
									} while (!tokenRule.regexEnd.matcher("" + _nextIn).matches());
								}
								catch (Exception err) {
									System.err.println("Fatal error: Incorrect syntax at token " + tokenRule);
								}
								finally {
									thisToken = buildToken.toString();
								}
							}
							
							Symbol symbol = null;
							if (tokenRule.symbolType != null) {
								symbol = st.insert(thisToken, tokenRule.symbolType);
							}
							output.addtoken(tokenRule, symbol);
							break;
						}
					}
				}
				
				// Is end of statement as well?
				if (Terminal.EOF_STMT.regexToken.matcher("" + nextIn).matches()) {
					output.addtoken(Terminal.EOF_STMT, null);
				}
				
				// Reset string builder to empty and continue
				buildToken.setLength(0);
				continue;
			}
			
			buildToken.append(nextIn);
		}
		return;
	}
}
