package com.zygateley.compiler;

import java.io.*;

class LexicalException extends Exception {
	static final long serialVersionUID = 58008;
	public LexicalException(String message) {
		super(message);
	}
}

public class Lexer {
	private boolean verbose;
	// In
	private PushbackReader stringReader;
	// Out
	private TokenStream tokenStream;
	// Build in the meantime
	private SymbolTable symbolTable;
	// Tokens are character-by-character
	private StringBuilder tokenBuilder;
	// Write to log file if not null
	private FileWriter logFileWriter;
	
	/**
	 * Mutable coupling of a given terminal
	 * and whether it matches the current
	 * token string.
	 * 
	 * @author Zachary Gateley
	 *
	 */
	private class TerminalMatch {
		public Terminal terminal;
		public boolean isPossibleMatch = true;
		public boolean isFullMatch = true;
	}
	private TerminalMatch[] thisRoundMatches = new TerminalMatch[Terminal.values().length];
	private TerminalMatch[] lastRoundMatches = new TerminalMatch[thisRoundMatches.length];
	{
		Terminal[] tokens = Terminal.values();
		for (int i = 0; i < tokens.length; i++) {
			// This time token matches
			TerminalMatch tm = new TerminalMatch();
			tm.terminal = tokens[i];
			tm.isPossibleMatch = true;
			tm.isFullMatch = true;
			thisRoundMatches[i] = tm;
			
			// Last time token matches
			tm = new TerminalMatch();
			tm.terminal = tokens[i];
			tm.isPossibleMatch = false;
			tm.isFullMatch = false;
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
		this(input, output, symbolTable, null);
	}
	public Lexer(PushbackReader input, TokenStream output, SymbolTable symbolTable, FileWriter logFileWriter) {
		this.stringReader = input;
		this.tokenStream = output;
		this.tokenBuilder = new StringBuilder();
		this.symbolTable = symbolTable;
		this.logFileWriter = logFileWriter;
	}
	
	public void lex(boolean verbose) throws LexicalException, IOException {
		this.verbose = verbose;
		lex();
	}
	public void lex() throws LexicalException, IOException {
		this.log("<!-- Lexer started -->\n");
		
		// Next character
		resetMatches();
		boolean isEOFProgram = false;
		do { // while !isEof
			int readIn = stringReader.read();
			String nextIn;
			if (readIn < 0) {
				// End of input character
				isEOFProgram = true;
				nextIn = Character.toString((char) 0);
			}
			else {
				// Valid character
				nextIn = Character.toString((char) readIn);
			}
			// Add new character to token itself, not token stream
			tokenBuilder.append(nextIn);
			
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
					stringReader.unread(readIn);
				}
				
				// Get the current token
				currentToken = currentToken.substring(0, currentToken.length() - 1);
				
				// Find the which terminal type this is
				// It will be the first full match from the last match set (ordered by com.zygateley.compiler.Token.id)
				Terminal thisRule = getTerminalRuleFromLast(currentToken);
				
				// Ignore EMPTY terminals
				if (thisRule == Terminal.EMPTY) {
					// Empty terminals only take up extra space
				}
				// If no full match, invalid syntax
				else if (thisRule == null) {
					// This can happen when Terminal.regexStart matches 
					// but Terminal.regexFull does not
					String message = "Lexical error at " + currentToken;
					log(message);
					throw new LexicalException(message);
				}
				// Otherwise, we have a valid rule
				else {
					createAddToken(currentToken, thisRule);
				}

				// Token add complete!
				// Reset token builder and terminal match count
				resetMatches();
			}
		} while (!isEOFProgram);
		
		// Finished, add EOF
		createAddToken(Character.toString((char) 0), Terminal.EOF);
		
		log("\n<!-- Lexer finished -->\n\n");
		
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
		// Reset tokenBuilder to zero-length
		tokenBuilder.setLength(0);
		
		// Reset all potential matches to TRUE  (tokenMatches)
		// 		 all last potential matches to FALSE (lastTokenMatches)
		//			ensures we do not incorrectly assume match on first char
		for (int i = 0; i < thisRoundMatches.length; i++) {
			// Reset this round matches
			TerminalMatch thisTm = thisRoundMatches[i];
			thisTm.isPossibleMatch 	= true;
			thisTm.isFullMatch 		= false;

			// Reset last round matches
			TerminalMatch lastTm = lastRoundMatches[i];
			lastTm.isPossibleMatch 	= false;
			lastTm.isFullMatch 		= false;
		}
	}
	
	/**
	 * Find how many Terminals this (partial) token
	 * might fit.
	 * @param token
	 * @return
	 */
	private int getMatchCount(String partialToken) {
		int matchCount = 0;
		for (int i = 0; i < thisRoundMatches.length; i++) {
			TerminalMatch tm = thisRoundMatches[i];
			// Consider only those that still match
			// First character: all matches set to true
			if (tm.isPossibleMatch) {
				Terminal tokenRule = tm.terminal;

				// Check for partial match
				boolean isPartialMatch = tokenRule.isMatch(partialToken, false);
				
				// Some tokens require "full matches" 
				// and might only full-match ONCE e.g. string
				// In these cases, partial match is not enough
				// e.g. String will return partial match until EOF
				if (isPartialMatch && tokenRule.requiresFullMatch()) {
					boolean isFullMatch = tokenRule.isMatch(partialToken, true);
					if (isFullMatch) {
						tm.isFullMatch = true;
					}
					else {
						TerminalMatch lastTm = lastRoundMatches[i];
						if (lastTm.isFullMatch) {
							// Had full match
							// No longer have full match!
							// we have reached the end of this token
							isPartialMatch = false;
						}
					}
				}
				
				if (isPartialMatch) {
					// Keep match as true
					matchCount++;
				}
				else {
					tm.isPossibleMatch = false;
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
			TerminalMatch thisMatch = thisRoundMatches[i];
			TerminalMatch lastMatch = lastRoundMatches[i];
			lastMatch.isPossibleMatch = thisMatch.isPossibleMatch;
			lastMatch.isFullMatch	  = thisMatch.isFullMatch;
		}
	}
	
	/**
	 * Get the first match from the last set of matches (lastRoundMatches)
	 * Calling this implies that there is no match in the current match set (thisRoundMatches)
	 * @return Terminal first matching rule (priority by order)
	 */
	private Terminal getTerminalRuleFromLast(String fullToken) {
		for (TerminalMatch tm : lastRoundMatches) {
			if (tm.isPossibleMatch) {
				boolean isFullMatch = tm.terminal.isMatch(fullToken, true);
				if (isFullMatch) return tm.terminal;
			}
		}
		return null;
	}
	
	private void createAddToken(String newToken, Terminal thisRule) throws IOException {
		Symbol symbol = null;
		String value = null;
		if (thisRule == Terminal.VARIABLE) {
			// Variable name
			symbol = symbolTable.insert(newToken);
		}
		else if (thisRule.type == TypeSystem.STRING) { 
			symbol = symbolTable.insert(newToken, thisRule.type);
		}
		else if (thisRule.type != null) {
			// All other literals
			value = newToken;
		}
		else {
			// All non-literals inherit their value from the rule
			value = thisRule.exactString;
		}
		if (thisRule != Terminal.COMMENT) {
			// Do not consider comments
			// They can short circuit single-command if/elseif/else
			tokenStream.write(thisRule, symbol, value);
		}
		
		if (verbose) {
			StringBuilder sbtr = new StringBuilder(thisRule + "         ");
			sbtr.setLength(10);
			log("Token: " + sbtr.toString() + "\t( "+ newToken + " )");
		}
	}
	
	private void log(String message) throws IOException {
		if (this.verbose) {
			System.out.println(message);
		}
		if (this.logFileWriter != null) { 
			this.logFileWriter.append(message + "\r\n");
		}
	}
}
