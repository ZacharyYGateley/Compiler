package com.zygateley.compiler;

import java.io.IOException;
import java.io.PushbackReader;
import java.util.regex.*;

public class Lexer {
	private boolean verbose;
	private PushbackReader stringReaderIn;
	private TokenStream tokenStreamOut;
	private SymbolTable symbolTable;
	// Tokens are built into buildToken
	// When a token is complete, buildToken is reset
	private StringBuilder tokenBuilder;
	
	class TerminalAndMatch {
		public Terminal terminal;
		public boolean isMatch = true;
	}
	private static TerminalAndMatch[] thisRoundMatches = new TerminalAndMatch[Terminal.values().length];
	private static TerminalAndMatch[] lastRoundMatches = new TerminalAndMatch[thisRoundMatches.length];
	{
		Terminal[] tokens = Terminal.values();
		for (int i = 0; i < tokens.length; i++) {
			// This time token matches
			TerminalAndMatch tm = new TerminalAndMatch();
			tm.terminal = tokens[i];
			tm.isMatch = true;
			thisRoundMatches[i] = tm;
			
			// Last time token matches
			tm = new TerminalAndMatch();
			tm.terminal = tokens[i];
			tm.isMatch = false;
			lastRoundMatches[i] = tm;
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
	public Lexer(PushbackReader input, TokenStream output, SymbolTable symbolTable) {
		this.stringReaderIn = input;
		this.tokenStreamOut = output;
		this.tokenBuilder = new StringBuilder();
		this.symbolTable = symbolTable;
	}
	
	public void lex(boolean verbose) throws IOException {
		this.verbose = verbose;
		lex();
	}
	public void lex() throws IOException {
		// Next character
		boolean isEOF = false;
		do {
			int readIn = stringReaderIn.read();
			String nextIn;
			if (readIn < 0) {
				// End of input character
				isEOF = true;
				nextIn = Character.toString((char) 0);
			}
			else {
				// Valid character
				nextIn = Character.toString((char) readIn);
			}
			// Add new character to token itself, not token stream
			tokenBuilder.append(nextIn);
			
			boolean isFirstChar = (tokenBuilder.length() == 1);
			if (isFirstChar) {
				// This is the first character
				// Reset all this matches to TRUE  (tokenMatches)
				//			ensures we loop all tokens
				// 		 all last matches to FALSE (lastTokenMatches)
				//			ensures we do not incorrectly assume match 
				//			when there are no matches on first character
				resetMatches();
			}
			
			// Find the number of matches on the current string
			// update tokenMatches with match boolean
			String currentToken = tokenBuilder.toString();
			int matchCount = getMatchCount(currentToken);

			
			// Keep expanding the token until there are no more matches
			// If there are matches, copy current matches to last matches
			// so that we know what the best match was 
			// before the next token gave us no matches
			if (matchCount > 0) {
				// Copy current matches to last matches
				copyThisToLast();
				// Repeat until no matches
			}
			// If no more matches, check the last match set
			// and grab the first match (in order) from the matching set
			else {
				// This last character does not belong to this token
				// Return it to the stream (if it is a valid entry)
				if (readIn > -1) {
					stringReaderIn.unread(readIn);
				}
				tokenBuilder.setLength(0);
				
				// Find the which terminal type this is
				// It will be the first match in the ordered set TerminalAndMatch (from Terminal.values())
				Terminal thisRule = getTerminalRuleFromLast();
				
				// Ignore EMPTY terminals
				// They would only take up unnecessary space at this point
				if (thisRule == Terminal.EMPTY) {
					continue;
				}
				
				
				// If there is no match at all,
				// there is an invalid character
				if (thisRule == null) {
					System.err.println("Lexical error at " + currentToken);
					return;
				}
				// Otherwise, we have our rule
				// Create new token
				else {
					createAddToken(currentToken, thisRule);
				}
			}
		} while (!isEOF);
		
		// Finished, add EOF
		createAddToken(Character.toString((char) 0), Terminal.EOF);
		
		return;
	}
	
	/**
	 * Reset all this matches to TRUE  (tokenMatches)
	 * 			ensures we loop all tokens
	 * 		 all last matches to FALSE (lastTokenMatches)
	 *			ensures we do not incorrectly assume match 
	 *			when there are no matches on first character
	 * 
	 */
	private void resetMatches() {
		// Reset all this matches to TRUE  (tokenMatches)
		//			ensures we loop all tokens
		// 		 all last matches to FALSE (lastTokenMatches)
		//			ensures we do not incorrectly assume match 
		//			when there are no matches on first character
		for (int i = 0; i < thisRoundMatches.length; i++) {
			thisRoundMatches[i].isMatch 	= true;
			lastRoundMatches[i].isMatch 	= false;
		}
	}
	
	private int getMatchCount(String token) {
		int matchCount = 0;
		for (TerminalAndMatch tm : thisRoundMatches) {
			// Consider only those that still match
			// First character: all matches set to true
			if (tm.isMatch) {					
				Terminal tokenRule = tm.terminal;
				Matcher m = tokenRule.regexStart.matcher(token);
				if (m.matches()) {
					// Keep match as true
					matchCount++;
				}
				else {
					tm.isMatch = false;
				}
			}
		}
		return matchCount;
	}
	
	/**
	 * Copy thisRoundMatches to lastRoundMatches
	 * i.e. we are keeping a one-iteration memory of matches
	 * Store memory. 
	 */
	private void copyThisToLast() {
		for (int i = 0; i < thisRoundMatches.length; i++) {
			TerminalAndMatch thisMatch = thisRoundMatches[i];
			TerminalAndMatch lastMatch = lastRoundMatches[i];
			lastMatch.isMatch = thisMatch.isMatch;
		}
	}
	
	/**
	 * Get the first match from the last set of matches (lastRoundMatches)
	 * Calling this implies that there is no match in the current match set (thisRoundMatches)
	 * @return Terminal first matching rule (priority by order)
	 */
	private Terminal getTerminalRuleFromLast() {
		Terminal thisRule = null;
		for (TerminalAndMatch tm : lastRoundMatches) {
			if (tm.isMatch) {
				thisRule = tm.terminal;
			}
		}
		return thisRule;
	}
	
	private void createAddToken(String newToken, Terminal thisRule) {
		Symbol symbol = null;
		if (thisRule.symbolType != null) {
			symbol = symbolTable.insert(newToken, thisRule.symbolType);
		}
		tokenStreamOut.addtoken(thisRule, symbol);
		
		if (verbose) {
			StringBuilder sbtr = new StringBuilder(thisRule + "         ");
			sbtr.setLength(10);
			System.out.println("Lexer: " + sbtr.toString() + "\t("+ newToken + ")");
		}
	}
		
	/*
	private boolean findAndAddToken(String token, char thisChar, SymbolTable st) {
		// All available token types
		// Save time & space by converting to string now
		// Will compare against a handful of regular expressions
		if (token.length() > 0) {
			// Current token in the works
			// Figure out what kind of token it is
			// Get first match
			for (TerminalAndMatch tokenMatch : thisRoundMatches) {
				Terminal tokenRule = tokenMatch.terminal;
				Matcher m = tokenRule.regexToken.matcher(token);
				if (m.matches()) {
					// regexEnd indicates 
					// that this token can outlast white space
					if (tokenRule.regexEnd != null) {
						tokenBuilder.append(thisChar);
						try {
							int _readIn;
							char _nextIn;
							do {
								_readIn = stringReaderIn.read();
								_nextIn = (char) _readIn;
								tokenBuilder.append(_nextIn);
							} while (!tokenRule.regexEnd.matcher("" + _nextIn).matches());
						}
						catch (Exception err) {
							System.err.println("Fatal error: Incorrect syntax at token " + tokenRule);
							return false;
						}
						finally {
							token = tokenBuilder.toString();
						}
					}
					
					Symbol symbol = null;
					if (tokenRule.symbolType != null) {
						symbol = st.insert(token, tokenRule.symbolType);
					}
					tokenStreamOut.addtoken(tokenRule, symbol);
					
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
	*/
}
