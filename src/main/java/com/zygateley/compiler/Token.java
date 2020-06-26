package com.zygateley.compiler;

import java.util.regex.Pattern;

class id {
	public static int id = 0;
	public static int next() {
		return id++;
	}
}

// This created before NonTerminals
// So that NonTerminal rules can reference rules
// that have not yet been defined
public interface Token {
	// Terminals
	// Actual value does not matter, referred to as if enum
	public final static int 
		EMPTY = 		id.next(), 
		SEMICOLON = 	id.next(),
		ECHO = 			id.next(),
		VAR = 			id.next(),
		TRUE = 			id.next(),
		FALSE = 		id.next(),
		INT = 			id.next(),
		STRING = 		id.next(),
		DEF = 			id.next(),
		PAREN_OPEN = 	id.next(),
		PAREN_CLOSE = 	id.next(),
		ASTERISK = 		id.next(),
		SLASH = 		id.next(),
		PLUS = 			id.next(),
		MINUS = 		id.next(),
		NEQ = 			id.next(),
		LTEQ = 			id.next(),
		GTEQ = 			id.next(),
		LT = 			id.next(),
		GT = 			id.next(),
		EQ = 			id.next(),
		CURLY_OPEN = 	id.next(),
		CURLY_CLOSE = 	id.next(),
		EOF = 			id.next();
	
	public final static int partition = id.id;
	
	// Non-terminals
	// Actual value does not matter, referred to as if enum
	public final static int
		_PROGRAM_ = 	id.next(),
		_STMTS_ = 		id.next(),
		_BLOCK_ = 		id.next(),
		_STMT_ = 		id.next(),
		_DEF_ = 		id.next(),
		_ECHO_ = 		id.next(),
		_EXPR_ = 		id.next(),
		_OP_EXPR_ = 	id.next(),
		_OP_ = 			id.next(),
		_LITERAL_ = 	id.next();

	
	// List of all operators for ease of access in rule definitions
	public final static int[] operators = {
		Token.PLUS,
		Token.MINUS,
		Token.ASTERISK,
		Token.SLASH
	};
	public static final int[] primitives = {
			TRUE,
			FALSE,
			INT,
			STRING
	};
	/**
	 * Return integer array of operators and passed tokens
	 * @param additionalTokens tokens to include with operators
	 * @return integer array of all operators and passed tokens
	 */
	public static int[] combineArrays(int[] firstArray, int... additionalTokens) {
		int index = 0; 
		int[] array = new int[firstArray.length + additionalTokens.length];
		for (; index < firstArray.length; index++) {
			array[index] = firstArray[index];
		}
		for (int i = 0; i < additionalTokens.length; i++, index++) {
			array[index] = additionalTokens[i];
		}
		return array;
	}
	
	public static boolean isNonTerminal(int tokenValue) {
		return tokenValue >= partition;
	}
	public static boolean isTerminal(int tokenValue) {
		return tokenValue < partition;
	}
}

enum Terminal implements Token {
	// Terminals
	EMPTY 		(Token.EMPTY, "", "\\s"),
	SEMICOLON	(Token.SEMICOLON, ";"),
	ECHO 		(Token.ECHO, "echo"),
	TRUE		(Token.TRUE, Symbol.Type.BOOLEAN, "true"),
	FALSE		(Token.FALSE, Symbol.Type.BOOLEAN, "false"),
	INT 		(Token.INT, Symbol.Type.INT, "", ("\\d*")),
	STRING      (Token.STRING, Symbol.Type.STRING, "", ("\".*"), ("[^[^\\](\\\\)*\\]\"")),
	VAR			(Token.VAR, Symbol.Type.VAR, "", ("[a-z|A-Z|_][a-z|A-Z|\\d|_]*")),
	DEF 		(Token.DEF, "="),
	PAREN_OPEN	(Token.PAREN_OPEN, "("),
	PAREN_CLOSE (Token.PAREN_CLOSE, ")"),
	ASTERISK 	(Token.ASTERISK, "*"),
	SLASH 		(Token.SLASH, "/"),
	PLUS  		(Token.PLUS, "+"),
	MINUS 		(Token.MINUS, "-"),
	NEQ  		(Token.NEQ, "!="),
	LTEQ 		(Token.LTEQ, "<="),
	GTEQ  		(Token.GTEQ, ">="),
	LT 			(Token.LT, "<"),
	GT  		(Token.GT, ">"),
	EQ 			(Token.EQ, "=="),
	CURLY_OPEN  (Token.CURLY_OPEN, "{"),
	CURLY_CLOSE (Token.CURLY_CLOSE, "}"),
	EOF 		(Token.EOF, Character.toString((char) 0));
	
	public final int tokenValue;
	public final String exactString;
	public final Pattern regexStart;
	public final Pattern regexEnd;
	public final Symbol.Type symbolType;

	private Terminal(int tokenValue, String... matching) {
		this.tokenValue = tokenValue;
		this.symbolType = null;
		this.exactString = (matching.length > 0) ? matching[0] : "";
		this.regexStart = (matching.length > 1) ? Pattern.compile(matching[1]) : null;
		this.regexEnd = (matching.length > 2) ? Pattern.compile(matching[2]) : null;
	}
	private Terminal(int tokenValue, Symbol.Type symbolType, String... matching) {
		this.tokenValue = tokenValue;
		this.symbolType = symbolType;
		this.exactString = (matching.length > 0) ? matching[0] : "";
		this.regexStart = (matching.length > 1) ? Pattern.compile(matching[1]) : null;
		this.regexEnd = (matching.length > 2) ? Pattern.compile(matching[2]) : null;
	}
}

enum NonTerminal implements Token {
	_PROGRAM_	(Token._PROGRAM_,
				 firstTerminalAndPattern(new int[] { Token.CURLY_OPEN, Token.VAR, Token.ECHO }, Token._STMTS_),
				 firstTerminalAndPattern(Token.EMPTY, Token.EMPTY),
				 follow(Token.EOF)),
	
	_STMTS_		(Token._STMTS_,
				 firstTerminalAndPattern(Token.CURLY_OPEN, Token._BLOCK_, Token._STMTS_),
				 firstTerminalAndPattern(new int[] { Token.VAR, Token.ECHO }, Token._STMT_, Token._STMTS_),
				 firstTerminalAndPattern(Token.EMPTY, Token.EMPTY),
				 follow(new int[] {Token.EOF, Token.CURLY_CLOSE})),
	
	_BLOCK_		(Token._BLOCK_,
				 firstTerminalAndPattern(Token.CURLY_OPEN, Token.CURLY_OPEN, Token._STMTS_, Token.CURLY_CLOSE),
				 follow(new int[] {Token.EOF, Token.CURLY_OPEN, Token.VAR, Token.ECHO})),
	
	_STMT_		(Token._STMT_,
				 firstTerminalAndPattern(Token.VAR, Token._DEF_, Token.SEMICOLON),
				 firstTerminalAndPattern(Token.ECHO, Token._ECHO_, Token.SEMICOLON),
				 follow(new int[] {Token.EOF, Token.CURLY_OPEN, Token.VAR, Token.ECHO})),
	
	_DEF_		(Token._DEF_,
				 firstTerminalAndPattern(Token.VAR, Token.VAR, Token.DEF, Token._EXPR_),
				 follow(Token.SEMICOLON)),
	
	_ECHO_		(Token._ECHO_,
				 firstTerminalAndPattern(Token.ECHO, Token.ECHO, Token._EXPR_),
				 follow(Token.SEMICOLON)),
	
	_EXPR_		(Token._EXPR_,
				 firstTerminalAndPattern(Token.PAREN_OPEN, Token.PAREN_OPEN, Token._EXPR_, Token.PAREN_CLOSE),
				 firstTerminalAndPattern(Token.VAR, Token.VAR, Token._OP_EXPR_),
				 firstTerminalAndPattern(Token.primitives, Token._LITERAL_, Token._OP_EXPR_),
	 			 follow(new int[] {Token.SEMICOLON, Token.PAREN_CLOSE})),
	
	_OP_EXPR_	(Token._OP_EXPR_,
				 firstTerminalAndPattern(Token.operators, Token._OP_, Token._EXPR_),
				 firstTerminalAndPattern(Token.EMPTY, Token.EMPTY),
	 			 follow(new int[] {Token.SEMICOLON, Token.PAREN_CLOSE})),
	
	_OP_		(Token._OP_,
				 firstTerminalAndPattern(Token.PLUS, Token.PLUS),
				 firstTerminalAndPattern(Token.MINUS, Token.MINUS),
				 firstTerminalAndPattern(Token.ASTERISK, Token.ASTERISK),
				 firstTerminalAndPattern(Token.SLASH, Token.SLASH),
				 follow(Token.combineArrays(Token.primitives, Token.VAR, Token.PAREN_OPEN))),
	
	_LITERAL_	(Token._LITERAL_,
				 firstTerminalAndPattern(Token.TRUE, Token.TRUE),
				 firstTerminalAndPattern(Token.FALSE, Token.FALSE),
				 firstTerminalAndPattern(Token.INT, Token.INT),
				 firstTerminalAndPattern(Token.STRING, Token.STRING),
				 firstTerminalAndPattern(Token.EMPTY, Token.EMPTY),
				 follow(new int[] {Token.SEMICOLON, Token.PAREN_CLOSE}));
	
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
	/**
	 * Search this rule to see if the given terminal
	 * is part of the follow set.
	 * 
	 * @param t token to search for in FOLLOW set for this rule
	 * @return boolean true / false
	 */
	public boolean inFollow(Terminal t) {
		return inFollow(t.tokenValue);
	}
	public boolean inFollow(int t) {
		for (int item : this.FOLLOW) {
			if (t == item) {
				return true;
			}	
		}
		return false;
	}
	
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