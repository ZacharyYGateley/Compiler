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
		Terminal[] tokens = Terminal.values();
		// Current token
		StringBuilder token = new StringBuilder();
		// Next character
		int read;
		char next;
		while ((read = input.read()) > -1) {
			next = (char) read;
			
			// At end of current token, determine what kind of token it is
			if (eofToken.matcher("" + next).matches()) {
				// Save time & space by converting to string now
				// Will compare against a handful of regular expressions
				String thisToken = token.toString();
				if (thisToken.length() > 0) {
					// Current token in the works
					// Figure out what kind of token it is
					// Get first match
					for (Terminal t : tokens) {
						Matcher m = t.regex.matcher(thisToken);
						if (m.matches()) {
							Symbol s = null;
							// Create new symbol if necessary
							switch (t) {
							case VAR:
							case INT:
								s = st.insert(thisToken);
								break;
							default:									
							}
							output.addtoken(t, s);
							break;
						}
					}
				}
				
				// Is end of statement as well?
				if (Terminal.EOF_STMT.regex.matcher("" + next).matches()) {
					output.addtoken(Terminal.EOF_STMT, null);
				}
				
				// Reset string builder to empty and continue
				token.setLength(0);
				continue;
			}
			
			token.append(next);
		}
		return;
	}
}
