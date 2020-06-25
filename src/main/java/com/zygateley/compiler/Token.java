package com.zygateley.compiler;

import java.util.regex.Pattern;

public interface Token {
	// Non-terminals
	public final static int 
		STMT = 100,
		DEF = 101,
		EXPR = 102;
	
	// Terminals
	public final static int 
		EMPTY = 0, 
		EOF_STMT = 1,
		ECHO = 2,
		VAR = 3,
		INT = 4,
		EQUALS = 5,
		PAREN_OPEN = 6,
		PAREN_CLOSE = 7,
		ASTERISK = 8,
		SLASH = 9,
		PLUS = 10,
		MINUS = 11;
}

enum Terminal implements Token {
	// Terminals
	EMPTY 		(Token.EMPTY, "\\s"),
	EOF_STMT	(Token.EOF_STMT, ";"),
	ECHO 		(Token.ECHO, "echo"),
	VAR			(Token.VAR, "[\\w|_][\\w|\\d|_]*"),
	INT 		(Token.INT, "\\d"),
	EQUALS 		(Token.EQUALS, "="),
	PARAN_OPEN	(Token.PAREN_OPEN, "\\("),
	PARAN_CLOSE (Token.PAREN_CLOSE, "\\)"),
	ASTERISK 	(Token.ASTERISK, "\\*"),
	SLASH 		(Token.SLASH, "/"),
	PLUS  		(Token.PLUS, "\\+"),
	MINUS 		(Token.MINUS, "\\-");

	public final int tokenValue;
	public final Pattern regex;
	
	private Terminal(int tokenValue, String regex) {
		this.tokenValue = tokenValue;
		this.regex = Pattern.compile(regex);
	}
}

enum NonTerminal implements Token {
	// Non-terminals
	//		FIRST_0: PATTERN_0
	//		FIRST_1: PATTERN_1
	//		...
	//		FOLLOW
	STMT		(Token.STMT,
				 // FIRST : PATTERN
				 new int[][] {{Token.VAR}, {Token.DEF}},
				 new int[][] {{Token.ECHO}, {Token.ECHO, Token.EXPR}},
				 new int[][] {{Token.EMPTY}, {Token.EMPTY}},
				 // FOLLOW
				 new int[][] {{Token.EOF_STMT}}),
	
	DEF			(Token.DEF,
				 // FIRST : PATTERN
				 new int[][] {{Token.VAR}, {Token.VAR, Token.EQUALS, Token.EXPR}},
				 // FOLLOW
				 new int[][] {{Token.EOF_STMT}}),
	
	EXPR		(Token.EXPR,
				 // FIRST : PATTERN
				 new int[][] {{Token.PAREN_OPEN}, {Token.EXPR, Token.PLUS, Token.EXPR}},
				 new int[][] {{Token.PAREN_OPEN}, {Token.EXPR, Token.MINUS, Token.EXPR}},
				 new int[][] {{Token.PAREN_OPEN}, {Token.EXPR, Token.PLUS, Token.EXPR}},
				 new int[][] {{Token.PAREN_OPEN}, {Token.EXPR, Token.ASTERISK, Token.EXPR}},
				 new int[][] {{Token.PAREN_OPEN}, {Token.EXPR, Token.SLASH, Token.EXPR}},
				 new int[][] {{Token.PAREN_OPEN}, {Token.PAREN_OPEN, Token.EXPR, Token.PAREN_CLOSE}},
				 new int[][] {{Token.VAR}, {Token.VAR}},
				 new int[][] {{Token.INT}, {Token.INT}},
				 // FOLLOW
	 			 new int[][] {{Token.EOF_STMT, Token.PLUS, Token.MINUS, Token.ASTERISK, Token.SLASH, Token.PAREN_CLOSE}});
	
	private class Pattern{
		public final int[] FIRST;
		public final int[] PATTERN;
		public final int[] FOLLOW;
		
		public Pattern(int[] first, int[] pattern, int[] follow) {
			this.FIRST = first;
			this.PATTERN = pattern;
			this.FOLLOW = follow;
		}
	}
	
	public final int tokenValue;
	public final Pattern[] patterns;
	
	private NonTerminal(int tokenValue, int[][]... patterns) {
		this.tokenValue = tokenValue;
		this.patterns = new Pattern[patterns.length - 1];
		// Last array is FOLLOW
		// Store ahead of time
		int[] follow = patterns[patterns.length - 1][0];
		// Therefore, skip last item
		for (int i = 0; i < patterns.length - 1; i++) {
			Pattern p = new Pattern(patterns[i][0], patterns[i][1], follow);
			// Add FIRST / PATTERN sets
			this.patterns[i] = p;
		}
	}
	
	public boolean inFirst(Terminal t) {
		return inFirst(t.tokenValue);
	}
	public boolean inFirst(int t) {
		for (Pattern p : this.patterns) {
			for (int item : p.FIRST) {
				if (t == item) {
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean inFollow(Terminal t) {
		return inFollow(t.tokenValue);
	}
	public boolean inFollow(int t) {
		for (Pattern p : this.patterns) {
			for (int item : p.FOLLOW) {
				if (t == item) {
					return true;
				}
			}
		}
		return false;
	}
}