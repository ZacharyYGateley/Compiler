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

import java.util.regex.*;
import java.util.stream.*;

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
	// Indication of syntax error
	public final static int
		ERROR =			id.next();
	
	// Terminals
	public final static int 
		EMPTY = 		id.next(),
		SEMICOLON = 	id.next(),
		COMMA = 		id.next(),
		EQ = 			id.next(),
		PAREN_OPEN = 	id.next(),
		PAREN_CLOSE = 	id.next(),
		CURLY_OPEN = 	id.next(),
		CURLY_CLOSE = 	id.next(),
		SQUARE_OPEN =	id.next(),
		SQUARE_CLOSE = 	id.next(),
		
		// Primitive set
		TRUE = 			id.next(),
		FALSE = 		id.next(),
		INT = 			id.next(),
		STRING = 		id.next(), 

		FUNCTION = 		id.next(),
		IF = 			id.next(),
		ELSEIF = 		id.next(),
		ELSE = 			id.next(),
		
		// Statement FIRST set
		ECHO = 			id.next(),
		INPUT = 		id.next(),
		VAR = 			id.next(),
		
		// Operator set
		ASTERISK = 		id.next(),
		SLASH = 		id.next(),
		PLUS = 			id.next(),
		MINUS = 		id.next(),
		NEQ = 			id.next(),
		LTEQ = 			id.next(),
		GTEQ = 			id.next(),
		LT = 			id.next(),
		GT = 			id.next(),
		EQEQ = 			id.next(),
		
		EOF = 			id.next();
	
	// All Terminals have id < partition
	// All NonTerminals have id >= partition
	public final static int firstNonTerminal = id.id;
	
	// Non-terminals
	public final static int
		_PROGRAM_ = 	id.next(),
		_STMTS_ = 		id.next(),
		_FUNCDEF_ = 	id.next(),
		_PARAMS0_ = 	id.next(),
		_PARAMS1_ = 	id.next(),
		_IF_ =			id.next(),
		_THEN_ = 		id.next(),
		_ELSEIF_ = 		id.next(),
		_BLOCKSTMT_ = 	id.next(),
		_BLOCK_ = 		id.next(),
		_STMT_ = 		id.next(),
		_ECHO_ = 		id.next(),
		_INPUT_ = 		id.next(),
		_VARSTMT_ =		id.next(),
		_VARDEF_ = 		id.next(),
		_EXPR_ = 		id.next(),
		//_OPEXPR_ = 		id.next(),
		_OP_ = 			id.next(),
		_VALUE_ = 		id.next(),
		_VAR_ = 		id.next(),
		_VAREXPR_ = 	id.next(),
		_FUNCCALL_ = 	id.next(),
		_ARGS0_ = 		id.next(),
		_ARGS1_ = 		id.next(),
		_LITERAL_ = 	id.next();

	public final static int firstAmbiguousNonTerminal = id.id;
	
	// NonTerminals without FIRST and FOLLOW sets
	public final static int
		__WILDCARD__ =	id.next(),
		__AMBEXPR0__ = id.next(),
		__AMBEXPR1__ = id.next(),
		__AMBEXPR2__ = id.next(),
		__AMBEXPR3__ = id.next();
	

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
	public static final int[] _STMTS_FIRST = {
			FUNCTION,
			IF,
			CURLY_OPEN,
			VAR,
			ECHO
	};
	public static final int[] _STMT_FIRST = {
			VAR,
			INPUT,
			ECHO
	};
	public final static int[] operatorSet = {
			PLUS,
			MINUS,
			ASTERISK,
			SLASH,
			NEQ,
			LTEQ,
			GTEQ,
			LT,
			GT,
			EQEQ
	};
	public final static int[] operatorSetRank0 = {
			PLUS,
			MINUS
	};
	public final static int[] operatorSetRank1 = {
			ASTERISK,
			SLASH
	};
	public final static int[] operatorSetRank2 = {
			NEQ,
			LTEQ,
			GTEQ,
			LT,
			GT,
			EQEQ
	};
	public static final int[] primitiveSet = {
			TRUE,
			FALSE,
			INT,
			STRING
	};
	public static final int[][] commonFollow1 = new int[][] { combineArrays(_STMT_FIRST, EOF) };
	public static final int[][] commonFollow2 = new int[][] { combineArrays(_STMT_FIRST, EOF, ELSEIF, ELSE) };
	public static final int[][] commonFollow3 = new int[][] { combineArrays(operatorSet, SEMICOLON, COMMA, PAREN_CLOSE) };
	
	public static boolean isNonTerminal(int tokenValue) {
		return tokenValue >= firstNonTerminal;
	}
	public static boolean isTerminal(int tokenValue) {
		return tokenValue < firstNonTerminal;
	}
}

enum Terminal implements Token {
	// Terminals
	EMPTY 		(Token.EMPTY, "", "^\\s"),
	SEMICOLON	(Token.SEMICOLON, ";"),
	COMMA		(Token.COMMA, ","),
	EQ 			(Token.EQ, "="),
	PAREN_OPEN	(Token.PAREN_OPEN, "("),
	PAREN_CLOSE (Token.PAREN_CLOSE, ")"),
	CURLY_OPEN  (Token.CURLY_OPEN, "{"),
	CURLY_CLOSE (Token.CURLY_CLOSE, "}"),
	SQUARE_OPEN (Token.SQUARE_OPEN, "["),
	SQUARE_CLOSE(Token.SQUARE_CLOSE, "]"),
	
	// PRIMITIVES
	TRUE		(Token.TRUE, "true"),
	FALSE		(Token.FALSE, "false"),
	INT 		(Token.INT, Symbol.Type.INT, "", ("^\\d*")),
	STRING      (Token.STRING, Symbol.Type.STRING, "", ("^\".*"), ("[^\\\\]{2,}(?:\\\\\\\\)*\"$")),
	
	// Other reserved words
	FUNCTION	(Token.FUNCTION, "function"),
	IF			(Token.IF, "if"),
	ELSEIF		(Token.ELSEIF, "elseif"),
	ELSE		(Token.ELSE, "else"),
	
	// Defined as <stmts> in CFG.xlsx
	ECHO 		(Token.ECHO, "echo"),
	INPUT		(Token.INPUT, "input"),
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
	EQEQ		(Token.EQEQ, "=="),
	
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

	/**
	 * Get Terminal by its tokenValue 
	 * (Token.* or Terminal.tokenValue)
	 * 
	 * @param tokenValue Terminal.tokenValue to find
	 * @return Terminal with matching tokenValue or null
	 */
	public static Terminal getNonTerminal(int tokenValue) {
		Terminal[] all = Terminal.values();
		for (Terminal nt : all) {
			if (nt.tokenValue == tokenValue) {
				return nt;
			}
		}
		return null;
	}
}

enum NonTerminal implements Token {
	_PROGRAM_	(Token._PROGRAM_,
				 firstTerminalsAndPattern(Token.combineArrays(new int[] { Token.FUNCTION, Token.IF, Token.CURLY_OPEN }, Token._STMT_FIRST), Token._STMTS_),
				 firstTerminalsAndPattern(Token.EMPTY, Token.EMPTY),
				 follow(Token.EOF)),
	
	_STMTS_		(Token._STMTS_,
				 firstTerminalsAndPattern(Token.FUNCTION, Token._FUNCDEF_, Token._STMTS_),
				 firstTerminalsAndPattern(Token.IF, Token._IF_, Token._STMTS_),
				 firstTerminalsAndPattern(Token.combineArrays(Token._STMT_FIRST, Token.CURLY_OPEN), Token._BLOCKSTMT_, Token._STMTS_),
				 firstTerminalsAndPattern(Token.EMPTY, Token.EMPTY),
				 follow(new int[] {Token.EOF, Token.CURLY_CLOSE})),
	
	_FUNCDEF_	(Token._FUNCDEF_,
				 firstTerminalsAndPattern(Token.FUNCTION, Token.FUNCTION, Token.VAR, Token.PAREN_OPEN, Token._PARAMS0_, Token.PAREN_CLOSE, Token._BLOCKSTMT_),
				 Token.commonFollow1),
	
	_PARAMS0_	(Token._PARAMS0_,
				 firstTerminalsAndPattern(Token.VAR, Token.VAR, Token._PARAMS1_),
				 firstTerminalsAndPattern(Token.EMPTY, Token.EMPTY),
				 follow(Token.PAREN_CLOSE)),
	
	_PARAMS1_	(Token._PARAMS1_,
				 firstTerminalsAndPattern(Token.COMMA, Token.COMMA, Token.VAR, Token._PARAMS1_),
				 firstTerminalsAndPattern(Token.EMPTY, Token.EMPTY),
				 follow(Token.PAREN_CLOSE)),
	
	_IF_		(Token._IF_,
			 	 firstTerminalsAndPattern(Token.IF, Token.IF, Token.PAREN_OPEN, Token._EXPR_, Token.PAREN_CLOSE, Token._THEN_),
			 	 Token.commonFollow1),
	
	_THEN_		(Token._THEN_,
				 firstTerminalsAndPattern(Token.CURLY_OPEN, Token._BLOCK_, Token._ELSEIF_),
			 	 firstTerminalsAndPattern(Token._STMT_FIRST, Token._STMT_, Token._ELSEIF_),
			 	 Token.commonFollow1),
	
	_ELSEIF_	(Token._ELSEIF_,
				 firstTerminalsAndPattern(Token.ELSEIF, Token.ELSEIF, Token.PAREN_OPEN, Token._EXPR_, Token.PAREN_CLOSE, Token._THEN_),
			 	 firstTerminalsAndPattern(Token.ELSE, Token.ELSE, Token._BLOCKSTMT_),
			 	 firstTerminalsAndPattern(Token.EMPTY, Token.EMPTY),
			 	 Token.commonFollow1),
	
	_BLOCKSTMT_	(Token._BLOCKSTMT_,
			 	 firstTerminalsAndPattern(Token.CURLY_OPEN, Token._BLOCK_),
			 	 firstTerminalsAndPattern(Token._STMT_FIRST, Token._STMT_),
			 	 Token.commonFollow2),
	
	_BLOCK_		(Token._BLOCK_,
				 firstTerminalsAndPattern(Token.CURLY_OPEN, Token.CURLY_OPEN, Token._STMTS_, Token.CURLY_CLOSE),
				 Token.commonFollow2),
	
	_STMT_		(Token._STMT_,
			 	 firstTerminalsAndPattern(Token.ECHO, Token._ECHO_, Token.SEMICOLON),
			 	 firstTerminalsAndPattern(Token.INPUT, Token._INPUT_, Token.SEMICOLON),
				 firstTerminalsAndPattern(Token.VAR, Token.VAR, Token._VARSTMT_, Token.SEMICOLON),
				 Token.commonFollow2),
	
	_ECHO_		(Token._ECHO_,
		 	 	 firstTerminalsAndPattern(Token.ECHO, Token.ECHO, Token._EXPR_),
				 follow(Token.SEMICOLON)),
	
	_INPUT_		(Token._INPUT_,
				 firstTerminalsAndPattern(Token.INPUT, Token.INPUT, Token.VAR),
				 follow(Token.SEMICOLON)),

	_VARSTMT_	(Token._VARSTMT_,
				 firstTerminalsAndPattern(Token.EQ, Token._VARDEF_),
				 firstTerminalsAndPattern(Token.PAREN_OPEN, Token._FUNCCALL_),
				 follow(Token.SEMICOLON)),
	
	_VARDEF_	(Token._VARDEF_,
				 firstTerminalsAndPattern(Token.EQ, Token.EQ, Token._EXPR_),
				 follow(Token.SEMICOLON)),
	
	_EXPR_		(Token._EXPR_,
				 //firstTerminalsAndPattern(Token.PAREN_OPEN, Token.PAREN_OPEN, Token._EXPR_, Token.PAREN_CLOSE),
				 // On anything else, 
				 // send to ambiguous branch 
				 firstTerminalsAndPattern(IntStream.rangeClosed(1, id.id).toArray(), Token.__AMBEXPR0__),
				 //firstTerminalsAndPattern(Token.combineArrays(Token.primitiveSet, Token.VAR), Token._VALUE_, Token._OPEXPR_),
	 			 follow(new int[] {Token.SEMICOLON, Token.PAREN_CLOSE})),
	
	/*
	_OPEXPR_	(Token._OPEXPR_,
				 firstTerminalsAndPattern(Token.operatorSet, Token._OP_, Token._EXPR_),
				 firstTerminalsAndPattern(Token.EMPTY, Token.EMPTY),
	 			 follow(new int[] {Token.SEMICOLON, Token.PAREN_CLOSE})),
	 */
	
	_OP_		(Token._OP_,
				 singleTerminalsPatternsAndFollow(
						 Token.operatorSet, 
						 follow(Token.combineArrays(Token.primitiveSet, Token.VAR, Token.PAREN_OPEN))
						 )
				 ),
	
	_VALUE_		(Token._VALUE_,
				 firstTerminalsAndPattern(Token.VAR, Token._VAR_),
				 firstTerminalsAndPattern(Token.primitiveSet, Token._LITERAL_),
				 Token.commonFollow3),
	
	_VAR_		(Token._VAR_,
				 firstTerminalsAndPattern(Token.VAR, Token.VAR, Token._VAREXPR_),
				 Token.commonFollow3),
	
	_VAREXPR_	(Token._VAREXPR_,
				 firstTerminalsAndPattern(Token.PAREN_OPEN, Token._FUNCCALL_),
				 firstTerminalsAndPattern(Token.EMPTY, Token.EMPTY),
				 Token.commonFollow3),
	
	_FUNCCALL_	(Token._FUNCCALL_,
				 firstTerminalsAndPattern(Token.PAREN_OPEN, Token.PAREN_OPEN, Token._ARGS0_, Token.PAREN_CLOSE),
				 Token.commonFollow3),
	
	_ARGS0_		(Token._ARGS0_,
				 firstTerminalsAndPattern(Token.combineArrays(Token.combineArrays(Token.operatorSet, Token.VAR), Token.primitiveSet), Token._VALUE_, Token._ARGS1_),
				 firstTerminalsAndPattern(Token.EMPTY, Token.EMPTY),
				 follow(Token.PAREN_CLOSE)),
	
	_ARGS1_		(Token._ARGS1_,
				 firstTerminalsAndPattern(Token.COMMA, Token.COMMA, Token._VALUE_, Token._ARGS1_),
				 firstTerminalsAndPattern(Token.EMPTY, Token.EMPTY),
				 follow(Token.PAREN_CLOSE)),
	
	_LITERAL_	(Token._LITERAL_,
				 singleTerminalsPatternsAndFollow(
						 Token.primitiveSet, 
						 Token.commonFollow3
						 )
				 ),
	
	// Patterns ambiguous by FIRST Terminal set
	// __EXPR__ enters into ambiguous patterns
	// __EXPRRANK3__ returns to non-ambiguous patterns
	
	/*
	__EXPR__	(Token.__EXPR__,
				 firstTerminalsAndPattern(Token.PAREN_OPEN, Token.PAREN_OPEN, Token.__EXPR__, Token.PAREN_CLOSE),
				 follow(Token.SEMICOLON, Token.PAREN_CLOSE)),
	*/
	__AMBEXPR0__(Token.__AMBEXPR0__, ambiguousSplitAt(Token.PAREN_OPEN), Token.__AMBEXPR1__, Token._EXPR_),
	__AMBEXPR1__(Token.__AMBEXPR1__, ambiguousSplitAt(Token.operatorSetRank0), Token.__AMBEXPR2__, Token._EXPR_),
	__AMBEXPR2__(Token.__AMBEXPR2__, ambiguousSplitAt(Token.operatorSetRank1), Token.__AMBEXPR3__, Token._EXPR_),
	__AMBEXPR3__(Token.__AMBEXPR3__, ambiguousSplitAt(Token.operatorSetRank2), Token._VALUE_, Token._EXPR_);
	/*
	__EXPRRANK3__(Token.__EXPRRANK3__,
				  firstTerminalsAndPattern(Token.combineArrays(Token.primitiveSet, Token.VAR), Token._VALUE_),
				  follow(Token.SEMICOLON, Token.PAREN_CLOSE));
				  */
	
	/**
	 * Internal class for NonTerminals
	 * 
	 * FIRST: the terminal(.tokenValue) that indicates the start of this pattern
	 * PATTERN: the respective pattern, including FIRST
	 * 
	 * @author Zachary Gateley
	 *
	 */
	public class Pattern {
		public final int[] FIRST;
		public final int[] PATTERN;
		
		public Pattern(int[] first, int... pattern) {
			this.FIRST = first;
			this.PATTERN = pattern;
		}
		public Pattern(int... pattern) {
			this.FIRST = null;
			this.PATTERN = pattern;
		}
	}
	public class AmbiguousPattern {
		public final int[] splitAt;
		public final int leftRule;
		public final int rightRule;
		
		public AmbiguousPattern(int[] splitAt, int leftRule, int rightRule) {
			this.splitAt = splitAt;
			this.leftRule = leftRule;
			this.rightRule = rightRule;
		}
	}
	
	public final int tokenValue;
	public final Pattern[] patterns;
	public final AmbiguousPattern ambiguousPattern;
	public final int[] FOLLOW;
	
	/**
	 * Constructor --> Parse by expediting from FIRST set
	 * 
	 * Contains first and follow sets
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
		this.ambiguousPattern = null;
		// Last array is FOLLOW
		this.FOLLOW = patterns[patterns.length - 1][0];
	}
	/**
	 * Constructor --> Parse by (a) getting top level stream, then by (b) search
	 * e.g.
	 * 		(1 * 2 + print( 43 ) * 3)
	 * 		
	 * 		a)--> (1 * 2 + _EXPR_ * 3)
	 * 		b)--> (_EXPRRANK1_ + _EXPRRANK0_)
	 * 		b)--> ((_EXPRRANK2_ * _EXPRRANK1_) + _EXPRRANK0_)
	 * 		b)--> ((_VALUE_ * _EXPRRANK1_) + _EXPRRANK0_)
	 * 		b)--> ((_VALUE_ * _EXPRRANK2_) + _EXPRRANK0_)
	 * 		b)--> ((_VALUE_ * _VALUE) + _EXPRRANK0_)
	 * 			...
	 * 
	 * Does not contain first and follow sets
	 * 
	 * @param tokenValue value from Token.* that corresponds to this NonTerminal
	 * @param patterns (w/o first set)
	 * 		e.g. { __EXPRRANK1__, +,  __EXPRRANK0__}
	 * 			 { __EXPRRANK1__, -,  __EXPRRANK0__}
	 */
	private NonTerminal(int tokenValue, int[] splitAt, int leftRule, int rightRule) {
		this.tokenValue = tokenValue;
		this.patterns = null;
		this.ambiguousPattern = new AmbiguousPattern(splitAt, leftRule, rightRule);
		this.FOLLOW = null;
	}

	//// CONSTRUCTOR helper methods
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
	private static int[][] firstTerminalsAndPattern(int first, int... pattern) {
		return new int[][] { { first }, pattern };
	}
	private static int[][] firstTerminalsAndPattern(int[] first, int... pattern) {
		return new int[][] { first, pattern };
	}
	/**
	 * patternsOfSingleTerminals
	 * 
	 * Stand-in for firstTerminalsAndPattern
	 * where each terminal in the argument array outputs to itself
	 * 
	 * @param terminalSet list of terminals that output to themselves only
	 * @param
	 * @returns array of firstTerminalsAndPattern sets ending with followSet
	 * 		{ 
	 * 			firstTerminalsAndPattern,
	 * 			firstTerminalsAndPattern,
	 * 			...,
	 * 			follow
	 * 		}
	 */
	private static int[][][] singleTerminalsPatternsAndFollow(int[] terminalSet, int[][] followSet) {
		int[][][] firstPatternFollow = new int[terminalSet.length + 1][][];
		int i = 0;
		for (; i < terminalSet.length; i++) {
			int terminal = terminalSet[i];
			firstPatternFollow[i] = firstTerminalsAndPattern(terminal, terminal);
		}
		firstPatternFollow[i] = followSet;
		return firstPatternFollow;
	}
	/**
	 * follow
	 * 
	 * @param follow set of Terminals that indicate the end of parsing this rule 
	 * @return respective input for NonTerminal constructor
	 */
	private static int[][] follow(int... follow) {
		return new int[][] { follow };
	}
	/**
	 * 
	 */
	private static int[] ambiguousSplitAt(int... tokens) {
		if (tokens == null) return new int[0];
		else 				return tokens;
	}
	
	
	//// PUBLIC methods
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
	
	public boolean isAmbiguous() {
		return (this.ambiguousPattern != null);
	}
}