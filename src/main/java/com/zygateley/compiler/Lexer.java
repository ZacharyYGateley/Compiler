package com.zygateley.compiler;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.regex.*;

public class Lexer {
	private boolean verbose;
	private StringReader input;
	private TokenStream output;
	// Tokens are built into buildToken
	// When a token is complete, buildToken is reset
	private StringBuilder buildToken;
	// Run on only one character at a time
	private static final Pattern singleToken = Pattern.compile("[\\{|\\}|\\[|\\]|\\(|\\)|;]");
	private static final Pattern whiteSpace = Pattern.compile("[\\s]");
	
	class TokenMatch {
		public Terminal token;
		public boolean match = false;
	}
	private static TokenMatch[] tokenRules = new TokenMatch[Terminal.values().length];
	{
		Terminal[] tokens = Terminal.values();
		for (int i = 0; i < tokens.length; i++) {
			TokenMatch tm = new TokenMatch();
			tm.token = tokens[i];
			tokenRules[i] = tm;
		}
	}
	
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
		this.buildToken = new StringBuilder();
	}
	
	public void lex(SymbolTable st, boolean verbose) throws IOException {
		this.verbose = verbose;
		lex(st);
	}
	public void lex(SymbolTable st) throws IOException {
		// Next character
		int readIn;
		char nextIn;
		while ((readIn = input.read()) > -1) {
			nextIn = (char) readIn;
			
			// At end of current token, determine what kind of token it is
			boolean isWhiteSpace = whiteSpace.matcher("" + nextIn).matches();
			boolean isSingleCharToken = singleToken.matcher("" + nextIn).matches(); 
			if (isWhiteSpace || isSingleCharToken) {
				String thisToken = buildToken.toString();
				// ADD TOKEN
				if (!findAndAddToken(thisToken, nextIn, st)) {
					return;
				}
				// This token has been saved
				// Reset string builder to empty
				buildToken.setLength(0);
				
				
				// Single character tokens have not yet been added
				if (isSingleCharToken) {
					// ADD TOKEN
					if (!findAndAddToken(nextIn + "", nextIn, st)) {
						return;
					}
				}
				
				continue;
			}
			
			buildToken.append(nextIn);
		}
		return;
	}
		
	private boolean findAndAddToken(String token, char thisChar, SymbolTable st) {
		// All available token types
		// Save time & space by converting to string now
		// Will compare against a handful of regular expressions
		if (token.length() > 0) {
			// Current token in the works
			// Figure out what kind of token it is
			// Get first match
			for (TokenMatch tokenMatch : tokenRules) {
				Terminal tokenRule = tokenMatch.token;
				Matcher m = tokenRule.regexToken.matcher(token);
				if (m.matches()) {
					// regexEnd indicates 
					// that this token can outlast white space
					if (tokenRule.regexEnd != null) {
						buildToken.append(thisChar);
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
							return false;
						}
						finally {
							token = buildToken.toString();
						}
					}
					
					Symbol symbol = null;
					if (tokenRule.symbolType != null) {
						symbol = st.insert(token, tokenRule.symbolType);
					}
					output.addtoken(tokenRule, symbol);
					
					if (verbose) {
						StringBuilder sbtr = new StringBuilder(tokenRule + "         ");
						sbtr.setLength(10);
						System.out.println("Lexer: " + sbtr.toString() + "\t("+ token + ")");
					}
					break;
				}
			}
		}
		return true;
	}
}
