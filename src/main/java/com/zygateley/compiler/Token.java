package com.zygateley.compiler;

import java.util.regex.Pattern;

public interface Token {
	// Non-terminals
	public final static int
		_BLOCK_ = 100,
		_STMT_ = 110,
		_DEF_ = 111,
		_ECHO_ = 112,
		_EXPR_ = 113,
		_ANY_ = 114;
	
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
		MINUS = 11,
		LITERAL = 12,
		TRUE = 13,
		FALSE = 14,
		CURLY_OPEN = 15,
		CURLY_CLOSE = 16;
	
	public static boolean isNonTerminal(int tokenValue) {
		return tokenValue >= 100;
	}
	public static boolean isTerminal(int tokenValue) {
		return tokenValue < 100;
	}
}

enum Terminal implements Token {
	// Terminals
	EMPTY 		(Token.EMPTY, ("\\s")),
	EOF_STMT	(Token.EOF_STMT, (";")),
	ECHO 		(Token.ECHO, ("echo")),
	VAR			(Token.VAR, ("[a-z|A-Z|_][a-z|A-Z|\\d|_]*"), Symbol.Type.VAR),
	INT 		(Token.INT, ("\\d"), Symbol.Type.INT),
	EQUALS 		(Token.EQUALS, ("=")),
	PAREN_OPEN	(Token.PAREN_OPEN, ("\\(")),
	PAREN_CLOSE (Token.PAREN_CLOSE, ("\\)")),
	ASTERISK 	(Token.ASTERISK, ("\\*")),
	SLASH 		(Token.SLASH, ("/")),
	PLUS  		(Token.PLUS, ("\\+")),
	MINUS 		(Token.MINUS, ("\\-")),
	LITERAL     (Token.LITERAL, ("\".*"), ("\""), Symbol.Type.STRING),
	TRUE		(Token.TRUE, ("true"), Symbol.Type.BOOLEAN),
	FALSE		(Token.FALSE, ("false"), Symbol.Type.BOOLEAN),
	CURLY_OPEN  (Token.CURLY_CLOSE, ("\\{")),
	CURLY_CLOSE (Token.CURLY_CLOSE, ("\\}"));
	
	public final int tokenValue;
	public final Pattern regexToken;
	public final Pattern regexEnd;
	public final Symbol.Type symbolType;

	private Terminal(int tokenValue, String regex) {
		this.tokenValue = tokenValue;
		this.regexToken = Pattern.compile(regex);
		this.regexEnd = null;
		this.symbolType = null;
	}
	private Terminal(int tokenValue, String regex, Symbol.Type symbolType) {
		this.tokenValue = tokenValue;
		this.regexToken = Pattern.compile(regex);
		this.regexEnd = null;
		this.symbolType = symbolType;
	}
	private Terminal(int tokenValue, String regexStart, String regexEnd, Symbol.Type symbolType) {
		this.tokenValue = tokenValue;
		this.regexToken = Pattern.compile(regexStart);
		this.regexEnd = Pattern.compile(regexEnd);
		this.symbolType = symbolType;
	}
}

enum NonTerminal implements Token {
	_BLOCK_		(Token._BLOCK_,
				 firstTerminalAndPattern(Token.CURLY_OPEN, Token.CURLY_OPEN, Token._STMT_, Token.CURLY_CLOSE),
				 follow()),
	_STMT_		(Token._STMT_,
				 firstTerminalAndPattern(Token.VAR, Token._DEF_, Token.EOF_STMT),
				 firstTerminalAndPattern(Token.ECHO, Token._ECHO_, Token.EOF_STMT),
				 firstTerminalAndPattern(Token.EMPTY, Token.EMPTY, Token.EOF_STMT),
				 follow()),
	
	_DEF_		(Token._DEF_,
				 firstTerminalAndPattern(Token.VAR, Token.VAR, Token.EQUALS, Token._EXPR_),
				 follow(Token.EOF_STMT)),
	
	_ECHO_		(Token._ECHO_,
				 firstTerminalAndPattern(Token.ECHO, Token.ECHO, Token._EXPR_),
				 follow(Token.EOF_STMT)),
	
	_EXPR_		(Token._EXPR_,
			     firstTerminalAndPattern(Token.PLUS, Token.PLUS, Token._EXPR_, Token._EXPR_),
				 firstTerminalAndPattern(Token.MINUS, Token.MINUS, Token._EXPR_, Token._EXPR_),
				 firstTerminalAndPattern(Token.ASTERISK, Token.ASTERISK, Token._EXPR_, Token._EXPR_),
				 firstTerminalAndPattern(Token.SLASH, Token.SLASH, Token._EXPR_, Token._EXPR_),
				 firstTerminalAndPattern(Token.PAREN_OPEN, Token.PAREN_OPEN, Token._EXPR_, Token.PAREN_CLOSE),
				 firstTerminalAndPattern(Token.VAR, Token.VAR),
				 firstTerminalAndPattern(Token.INT, Token.INT),
				 firstTerminalAndPattern(Token.LITERAL, Token.LITERAL),
	 			 follow(
	 					 Token.EOF_STMT, 
	 					 Token.PLUS, Token.MINUS, 
	 					 Token.ASTERISK, Token.SLASH, 
	 					 Token.PAREN_OPEN, Token.PAREN_CLOSE, 
	 					 Token.VAR, Token.INT
	 					 ));
	
	/**
	 * Internal class for NonTerminals
	 * 
	 * FIRST: the terminal(.tokenValue) that indicates the start of this pattern
	 * PATTERN: the respective pattern, including FIRST
	 * 
	 * @author Zachary Gateley
	 *
	 */
	public class Pattern{
		public final int[] FIRST;
		public final int[] PATTERN;
		
		public Pattern(int[] first, int... pattern) {
			this.FIRST = first;
			this.PATTERN = pattern;
		}
		
		/*
		 * It appears this is unused.
		 * 
		public int indexInFirst(Terminal t) {
			return indexInFirst(t.tokenValue);
		}
		public int indexInFirst(int t) {
			for (int i = 0; i < this.FIRST.length; i++) {
				if (this.FIRST[i] == t) {
					return i;
				}
			}
			return -1;
		}
		*/
	}
	
	public final int tokenValue;
	public final Pattern[] patterns;
	public final int[] FOLLOW;
	
	/**
	 * Constructor 
	 * 
	 * @param tokenValue value from Token.* that corresponds to this NonTerminal 
	 * @param patterns
	 * 		// Sequence of  
	 * 		firstTerminalAndPattern()...
	 * 		// Followed by one single
	 * 		follow()
	 */
	private NonTerminal(int tokenValue, int[][]... patterns) {
		this.tokenValue = tokenValue;
		this.patterns = new Pattern[patterns.length - 1];
		// Therefore, skip last item
		for (int i = 0; i < patterns.length - 1; i++) {
			Pattern p = new Pattern(patterns[i][0], patterns[i][1]);
			// Add FIRST / PATTERN sets
			this.patterns[i] = p;
		}
		// Last array is FOLLOW
		this.FOLLOW = patterns[patterns.length - 1][0];
	}
	
	/**
	 * Search this rule to see if the given terminal
	 * indicates the start of one of this rule's patterns.
	 * If it exists, return the index within the patterns array list.
	 * Otherwise, return -1.
	 * 
	 * @param t token to search for in FIRST sets for this rule
	 * @return index in patterns or -1 for not found
	 */
	public int indexOfMatchFirst(Terminal t) {
		return indexOfMatchFirst(t.tokenValue);
	}
	public int indexOfMatchFirst(int t) {
		for (int i_p = 0; i_p < this.patterns.length; i_p++) {
			for (int item : this.patterns[i_p].FIRST) {
				if (t == item) {
					return i_p;
				}
			}
			
		}
		return -1;
	}
	
	/*
	 * It appears this is not used
	public boolean inFollow(Terminal t) {
		return inFollow(t.tokenValue);
	}
	public boolean inFollow(int t) {
		for (int i = 0; i < this.FOLLOW.length; i++) {
			if (t == this.FOLLOW[i]) {
				return true;
			}
		}
		return false;
	}
	*/
	
	/**
	 * Get NonTerminal by its tokenValue 
	 * (Token.* or NonTerminal.tokenValue)
	 * 
	 * @param tokenValue NonTerminal.tokenValue to find
	 * @return NonTerminal with matching tokenValue or null
	 */
	public static NonTerminal getNonTerminal(int tokenValue) {
		NonTerminal[] all = NonTerminal.values();
		for (NonTerminal nt : all) {
			if (nt.tokenValue == tokenValue) {
				return nt;
			}
		}
		return null;
	}
	
	/**
	 * firstTerminalAndPattern
	 * 
	 * The first argument is the Terminal.tokenValue that indicates the start of this rule
	 * The following arguments indicate the sequence of Terminals and NonTerminals in the pattern
	 * 		(Represented as *.tokenValue each)
	 * 
	 * @param first Terminal tokenValue
	 * @param pattern Sequence of {Terminal, NonTerminal} as *.tokenValue
	 * @return respective input for NonTerminal constructor
	 */
	public static int[][] firstTerminalAndPattern(int first, int... pattern) {
		return new int[][] { { first }, pattern };
	}
	public static int[][] firstTerminalAndPattern(int[] first, int... pattern) {
		return new int[][] { first, pattern };
	}
	/**
	 * follow
	 * 
	 * @param follow set of Terminals that indicate the end of parsing this rule 
	 * @return respective input for NonTerminal constructor
	 */
	public static int[][] follow(int... follow) {
		return new int[][] { follow };
	}
}