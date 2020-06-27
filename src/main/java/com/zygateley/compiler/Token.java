/**
 * Token.java
 * 
 * At com.zygateley.compiler run time,
 * the language CFG is built up  using the rules herein.
 * 
 * Note: Token stores an integer value for each Terminal, NonTerminal
 * (Terminal implements Token) references Token values
 * (NonTerminal implements Token) references Token values
 * That is to say that each Terminal has a reference in Token and Terminal
 * and each NonTerminal has a reference in Token and NonTerminal.
 * These values are interlinked.
 * 
 * @author Zachary Gateley
 * 
 */
package com.zygateley.compiler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Quick way to create unique, auto-incrementing Token values
 * 
 * @author Zachary Gateley
 *
 */
class id {
	public static int id = 0;
	public static int next() {
		return id++;
	}
}

/**
 * Token has a reference for every Terminal and every NonTerminal.
 * This is created specifically before NonTerminals
 * so that NonTerminal rules can reference NonTerminals 
 * that have not yet been defined.
 * @author Zachary Gateley
 *
 */
public interface Token {
	// Terminals
	public final static int 
		EMPTY = 		id.next(),
		SEMICOLON = 	id.next(),
		COMMA = 		id.next(),
		VARDEF = 		id.next(),
		PAREN_OPEN = 	id.next(),
		PAREN_CLOSE = 	id.next(),
		CURLY_OPEN = 	id.next(),
		CURLY_CLOSE = 	id.next(),
		
		TRUE = 			id.next(),
		FALSE = 		id.next(),
		INT = 			id.next(),
		STRING = 		id.next(), 

		FUNCTION = 		id.next(),
		IF = 			id.next(),
		ELSEIF = 		id.next(),
		ELSE = 			id.next(),
		ECHO = 			id.next(),
		VAR = 			id.next(),
		
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
		
		EOF = 			id.next();
	
	// All Terminals have id < partition
	// All NonTerminals have id >= partition
	public final static int partition = id.id;
	
	// Non-terminals
	public final static int
		_PROGRAM_ = 	id.next(),
		_STMTS_ = 		id.next(),
		_BLOCK_ = 		id.next(),
		_IF_ =			id.next(),
		_THEN_ = 		id.next(),
		_ELSEIF_ = 		id.next(),
		_ELSE_ = 		id.next(),
		_ELSETHEN_ = 	id.next(),
		_STMT_ = 		id.next(),
		_DEF_ = 		id.next(),
		_ECHO_ = 		id.next(),
		_EXPR_ = 		id.next(),
		_OPEXPR_ = 	id.next(),
		_OP_ = 			id.next(),
		_LITERAL_ = 	id.next();

	

	/**
	 * Return integer array of operators and passed tokens
	 * Use the static integer arrays below this definition
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
	public final static int[] operatorSet = {
		Token.PLUS,
		Token.MINUS,
		Token.ASTERISK,
		Token.SLASH,
		Token.NEQ,
		Token.LTEQ,
		Token.GTEQ,
		Token.LT,
		Token.GT,
		Token.EQ
	};
	public static final int[] primitiveSet = {
			TRUE,
			FALSE,
			INT,
			STRING
	};
	public static final int[] statementFirstSet = {
			VAR,
			ECHO
	};
	public static final int[] commonFollow0 = {
			EOF,
			CURLY_OPEN,
			CURLY_CLOSE,
			IF,
	};
	public static final int[] commonFollow1 = combineArrays(commonFollow0, statementFirstSet);
	public static final int[] commonFollow2 = combineArrays(commonFollow1, ELSEIF, ELSE);
	
	public static boolean isNonTerminal(int tokenValue) {
		return tokenValue >= partition;
	}
	public static boolean isTerminal(int tokenValue) {
		return tokenValue < partition;
	}
}

enum Terminal implements Token {
	// Terminals
	EMPTY 		(Token.EMPTY, "", "^\\s"),
	SEMICOLON	(Token.SEMICOLON, ";"),
	COMMA		(Token.COMMA, ","),
	VARDEF 		(Token.VARDEF, "="),
	PAREN_OPEN	(Token.PAREN_OPEN, "("),
	PAREN_CLOSE (Token.PAREN_CLOSE, ")"),
	CURLY_OPEN  (Token.CURLY_OPEN, "{"),
	CURLY_CLOSE (Token.CURLY_CLOSE, "}"),
	
	// PRIMITIVES
	TRUE		(Token.TRUE, Symbol.Type.BOOLEAN, "true"),
	FALSE		(Token.FALSE, Symbol.Type.BOOLEAN, "false"),
	INT 		(Token.INT, Symbol.Type.INT, "", ("^\\d*")),
	STRING      (Token.STRING, Symbol.Type.STRING, "", ("^\".*"), ("[^\\\\]{2,}(?:\\\\\\\\)*\"$")),
	
	// Other reserved words
	FUNCTION	(Token.FUNCTION, "function"),
	IF			(Token.IF, "if"),
	ELSEIF		(Token.ELSEIF, "elseif"),
	ELSE		(Token.ELSE, "else"),
	
	// Defined as <stmts> in CFG.xlsx
	ECHO 		(Token.ECHO, "echo"),
	VAR			(Token.VAR, Symbol.Type.VAR, "", ("^[a-z|A-Z|_][a-z|A-Z|\\d|_]*")),
	// Any reserved words must be declared before VAR
	
	// Defined as <ops> in CFG.xlsx
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
	
	/**
	 * fullMatch == false: looks for partial match with token
	 * fullMatch == true:  looks for full match with token
	 * @param token String token to compare against
	 * @param fullMatch whether to look for true: full match or false: partial match
	 * @return true: matches according to passed arguments
	 */
	public boolean isMatch(String token, boolean fullMatch) {
		boolean isMatch = false;
		if (this.regexStart != null) {
			Matcher m = this.regexStart.matcher(token);
			isMatch = (m.matches());
			
			// If you want only a partial match, 
			// only have to compare against starting regular expression ^^^ (above)
			// If you want a full match,
			// you must check against both starting and ending regular expressions
			if (fullMatch && this.regexEnd != null) {
				m = this.regexEnd.matcher(token);
				isMatch &= m.matches();
			}
		}
		else if (this.exactString.length() >= token.length()) {
			if (fullMatch) {
				isMatch = this.exactString.contentEquals(token);
			}
			else {
				isMatch = this.exactString.substring(0, token.length()).equals(token);
			}
		}
		return isMatch;
	}
	
	public boolean requiresFullMatch() {
		return (this.regexEnd != null);
	}
}

enum NonTerminal implements Token {
	_PROGRAM_	(Token._PROGRAM_,
				 firstTerminalAndPattern(new int[] { Token.CURLY_OPEN, Token.IF, Token.VAR, Token.ECHO }, Token._STMTS_),
				 firstTerminalAndPattern(Token.EMPTY, Token.EMPTY),
				 follow(Token.EOF)),
	
	_STMTS_		(Token._STMTS_,
				 firstTerminalAndPattern(Token.CURLY_OPEN, Token._BLOCK_, Token._STMTS_),
				 firstTerminalAndPattern(Token.IF, Token._IF_, Token._STMTS_),
				 firstTerminalAndPattern(Token.statementFirstSet, Token._STMT_, Token._STMTS_),
				 firstTerminalAndPattern(Token.EMPTY, Token.EMPTY),
				 follow(new int[] {Token.EOF, Token.CURLY_CLOSE})),
	
	_BLOCK_		(Token._BLOCK_,
				 firstTerminalAndPattern(Token.CURLY_OPEN, Token.CURLY_OPEN, Token._STMTS_, Token.CURLY_CLOSE),
				 follow(Token.commonFollow2)),
	
	_IF_		(Token._IF_,
			 	 firstTerminalAndPattern(Token.IF, Token.IF, Token.PAREN_OPEN, Token._EXPR_, Token.PAREN_CLOSE, Token._THEN_),
			 	 follow(Token.commonFollow1)),
	
	_THEN_		(Token._THEN_,
				 firstTerminalAndPattern(Token.CURLY_OPEN, Token._BLOCK_, Token._ELSEIF_),
			 	 firstTerminalAndPattern(Token.statementFirstSet, Token._STMT_, Token._ELSEIF_),
			 	 follow(Token.commonFollow1)),
	
	_ELSEIF_	(Token._ELSEIF_,
				 firstTerminalAndPattern(Token.ELSEIF, Token.ELSEIF, Token.PAREN_OPEN, Token._EXPR_, Token.PAREN_CLOSE, Token._THEN_),
				 firstTerminalAndPattern(Token.ELSE, Token._ELSE_),
			 	 firstTerminalAndPattern(Token.EMPTY, Token.EMPTY),
			 	 follow(Token.commonFollow1)),
	
	_ELSE_		(Token._ELSE_,
			 	 firstTerminalAndPattern(Token.ELSE, Token.ELSE, Token._ELSETHEN_),
			 	 firstTerminalAndPattern(Token.EMPTY, Token.EMPTY),
			 	 follow(Token.commonFollow1)),
	
	_ELSETHEN_  (Token._ELSETHEN_,
			 	 firstTerminalAndPattern(Token.CURLY_OPEN, Token._BLOCK_),
			 	 firstTerminalAndPattern(Token.statementFirstSet, Token._STMT_),
			 	 follow(Token.commonFollow1)),
	
	_STMT_		(Token._STMT_,
				 firstTerminalAndPattern(Token.VAR, Token._DEF_, Token.SEMICOLON),
				 firstTerminalAndPattern(Token.ECHO, Token._ECHO_, Token.SEMICOLON),
				 follow(Token.commonFollow2)),
	
	_DEF_		(Token._DEF_,
				 firstTerminalAndPattern(Token.VAR, Token.VAR, Token.VARDEF, Token._EXPR_),
				 follow(Token.SEMICOLON)),
	
	_ECHO_		(Token._ECHO_,
				 firstTerminalAndPattern(Token.ECHO, Token.ECHO, Token._EXPR_),
				 follow(Token.SEMICOLON)),
	
	_EXPR_		(Token._EXPR_,
				 firstTerminalAndPattern(Token.PAREN_OPEN, Token.PAREN_OPEN, Token._EXPR_, Token.PAREN_CLOSE),
				 firstTerminalAndPattern(Token.VAR, Token.VAR, Token._OPEXPR_),
				 firstTerminalAndPattern(Token.primitiveSet, Token._LITERAL_, Token._OPEXPR_),
	 			 follow(new int[] {Token.SEMICOLON, Token.PAREN_CLOSE})),
	
	_OPEXPR_	(Token._OPEXPR_,
				 firstTerminalAndPattern(Token.operatorSet, Token._OP_, Token._EXPR_),
				 firstTerminalAndPattern(Token.EMPTY, Token.EMPTY),
	 			 follow(new int[] {Token.SEMICOLON, Token.PAREN_CLOSE})),
	
	_OP_		(Token._OP_,
				 firstTerminalAndPattern(Token.PLUS, Token.PLUS),
				 firstTerminalAndPattern(Token.MINUS, Token.MINUS),
				 firstTerminalAndPattern(Token.ASTERISK, Token.ASTERISK),
				 firstTerminalAndPattern(Token.SLASH, Token.SLASH),
				 firstTerminalAndPattern(Token.NEQ, Token.NEQ),
				 firstTerminalAndPattern(Token.LTEQ, Token.LTEQ),
				 firstTerminalAndPattern(Token.GTEQ, Token.GTEQ),
				 firstTerminalAndPattern(Token.LT, Token.LT),
				 firstTerminalAndPattern(Token.GT, Token.GT),
				 firstTerminalAndPattern(Token.EQ, Token.EQ),
				 follow(Token.combineArrays(Token.primitiveSet, Token.VAR, Token.PAREN_OPEN))),
	
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