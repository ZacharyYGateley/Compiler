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
 * When the lexer checks against valid terminals, 
 * it looks in ascending id starting from Token.firstTerminal
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
public interface GrammarRule {
	public final static int firstTerminal = id.id;
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
		COMMENT =		id.next(),
		
		// Primitive set
		TRUE = 			id.next(),
		FALSE = 		id.next(),
		INT = 			id.next(),
		STRING = 		id.next(), 

		FUNCTION = 		id.next(),
		IF = 			id.next(),
		ELSE = 			id.next(),
		// ELSEIF exists as basic type but not as a token: ("else if")
		
		// Statement FIRST set
		ECHO = 			id.next(),
		INPUT = 		id.next(),
		VARIABLE = 			id.next();
	
	// Operator set
	public final static int firstOperator = id.id;
	public final static int
		AND = 			id.next(),
		OR = 			id.next(),
		EQEQ = 			id.next(),
		NEQ = 			id.next(),
		LTEQ = 			id.next(),
		GTEQ = 			id.next(),
		LT = 			id.next(),
		GT = 			id.next(),
		PLUS = 			id.next(),
		MINUS = 		id.next(),
		ASTERISK = 		id.next(),
		SLASH = 		id.next(),
		NOT = 			id.next();
	
	public final static int lastOperator = id.id - 1;
	
	public final static int
		EOF = 			id.next();
	public final static int lastTerminal = id.id - 1;
	
	// NonTerminals
	public final static int firstCFGRule = id.id;
	public final static int startingRule = id.id; 
	public final static int
		_PROGRAM_ = 	id.next(),
		_STMTS_ = 		id.next(),
		_FUNCDEF_ = 	id.next(),
		_PARAMS0_ = 	id.next(),
		_PARAMS1_ = 	id.next(),
		_SCOPE_ = 		id.next(),
		_IF_ =		id.next(),
		_ELSE_ = 		id.next(),
		_ELSEIF_ = 		id.next(),
		_BLOCKSTMT_ = 	id.next(),
		_BLOCK_ = 		id.next(),
		_STMT_ = 		id.next(),
		_ECHO_ = 		id.next(),
		_INPUT_ = 		id.next(),
		_VARSTMT_ =		id.next(),
		_VARDEF_ = 		id.next(),
		_VALUEOREXPR_ = id.next(),
		_EXPR_ = 		id.next(),
		_VALUE_ = 		id.next(),
		_VARIABLE_ = 		id.next(),
		_VAREXPR_ = 	id.next(),
		_FUNCCALL_ = 	id.next(),
		_ARGS0_ = 		id.next(),
		_ARGS1_ = 		id.next(),
		_LITERAL_ = 	id.next();
	public final static int lastCFGRule = id.id - 1;

	public final static int firstPrecedenceRule = id.id;
	public final static int
		__WILDCARD__ = 	id.next(),
		__AMBOPEN__  = 	id.next(),
		__AMBCLOSE__ = 	id.next(),
		__PRECEDENCE1__ = id.next(),
		__PRECEDENCE2__ = id.next(),
		__PRECEDENCE3__ = id.next(),
		__PRECEDENCE4__ = id.next(),
		__PRECEDENCE5__ = id.next();
	public final static int lastPrecedenceRule = id.id - 1;
	
	public final static int firstWrappingClass = id.id;
	// Wrapping classes for return nodes of precedence rules above
	public final static int
		__UNARY__	 = id.next(),
		__BINARY__	 = id.next();
	public final static int lastWrappingClass = id.id -1;
	
	
	// Parse direction of Precedence rules
	public enum Direction {
		RIGHT_TO_LEFT,
		LEFT_TO_RIGHT
	}
	

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
	public static int[] combineArrays(int[]... arrays) {
		int [] array = arrays[0];
		for (int i = 1; i < arrays.length; i++) {
			array = combineArrays(array, arrays[i]);
		}
		return array;
	}
	public static final int[] _STMT_FIRST = {
			VARIABLE,
			INPUT,
			ECHO,
			COMMENT
	};
	public static final int[] _STMTS_FIRST = combineArrays(_STMT_FIRST, FUNCTION, IF, CURLY_OPEN, VARIABLE, ECHO);

	public final static int[] operatorSetRank1 = {
			AND,
			OR
	};
	public final static int[] operatorSetRank2 = {
			EQEQ,
			NEQ,
			LTEQ,
			GTEQ,
			LT,
			GT,
	};
	public final static int[] operatorSetRank3 = {
			PLUS,
			MINUS
	};
	public final static int[] operatorSetRank4 = {
			ASTERISK,
			SLASH
	};
	public final static int[] operatorSetRank5 = {
			NOT
	};
	public final static int[] operatorSet = 
			combineArrays(
				operatorSetRank1, operatorSetRank2, operatorSetRank3, operatorSetRank4, operatorSetRank5
				);
	public static final int[] primitiveSet = {
			TRUE,
			FALSE,
			INT,
			STRING
	};
	public static final int[][] commonFollow1 = new int[][] { combineArrays(_STMTS_FIRST, EOF, CURLY_CLOSE) };
	public static final int[][] commonFollow2 = new int[][] { combineArrays(_STMTS_FIRST, EOF, CURLY_CLOSE, 
			//ELSEIF,
			ELSE) };
	public static final int[][] commonFollow3 = new int[][] { combineArrays(operatorSet, SEMICOLON, COMMA, PAREN_CLOSE) };
	

	public boolean isTerminal();
	public static boolean isTerminal(int tokenValue) {
		return firstTerminal <= tokenValue && tokenValue <= lastTerminal;
	}
	public static boolean isNonTerminal(int tokenValue) {
		return firstCFGRule <= tokenValue && tokenValue <= lastCFGRule;
	}
	public static boolean isCFGRule(int tokenValue) {
		return firstCFGRule <= tokenValue && tokenValue <= lastCFGRule;
	}
	public static boolean isPrecedenceRule(int tokenValue) {
		return firstPrecedenceRule <= tokenValue && tokenValue <= lastPrecedenceRule;
	}
	public static boolean isWrappingClass(int tokenValue) {
		return firstWrappingClass <= tokenValue && tokenValue <= lastWrappingClass;
	}
	public static boolean isSign(int tokenValue) {
		return tokenValue == PLUS || tokenValue == MINUS;
	}
	public static boolean isOperator(int tokenValue) {
		return tokenValue >= firstOperator && tokenValue <= lastOperator;
	}
}

enum Terminal implements GrammarRule {
	// Terminals
	EMPTY 		(GrammarRule.EMPTY, Element.NULL, "", "^\\s"),
	// Allow premature termination of compilation
	EOF 		(GrammarRule.EOF, Element.NULL, "noco"),
	
	SEMICOLON	(GrammarRule.SEMICOLON, Element.NULL, ";"),
	COMMA		(GrammarRule.COMMA, Element.NULL, ","),
	EQ 			(GrammarRule.EQ, Element.NULL, "="),
	PAREN_OPEN	(GrammarRule.PAREN_OPEN, Element.NULL, "("),
	PAREN_CLOSE (GrammarRule.PAREN_CLOSE, Element.NULL, ")"),
	CURLY_OPEN  (GrammarRule.CURLY_OPEN, Element.NULL, "{"),
	CURLY_CLOSE (GrammarRule.CURLY_CLOSE, Element.NULL, "}"),
	SQUARE_OPEN (GrammarRule.SQUARE_OPEN, Element.NULL, "["),
	SQUARE_CLOSE(GrammarRule.SQUARE_CLOSE, Element.NULL, "]"),
	COMMENT		(GrammarRule.COMMENT, Element.NULL, "", ("^/(?:/[^\0\r\n\f]*(?=[^\0\r\n\f])?)?$")),
	
	// PRIMITIVES
	TRUE		(GrammarRule.TRUE, TypeSystem.BOOLEAN, Element.TRUE, "", ("^[tT](?:[rR](?:[uU](?:[eE])?)?)?$")),
	FALSE		(GrammarRule.FALSE, TypeSystem.BOOLEAN, Element.FALSE, "", ("^[fF](?:[aA](?:[lL](?:[sS](?:[eE])?)?)?)?$")),
	INT 		(GrammarRule.INT, TypeSystem.INTEGER, Element.LITERAL, "", ("^\\d*")),
	STRING      (GrammarRule.STRING, TypeSystem.STRING, Element.LITERAL, "", ("^\".*"), ("^\"(?:[^\"\\\\]|\\\\.)*\"$")), // ("^\"(?:(?:.*(?:[^\\\\]))?(?:\\\\{2})*)?\"$")
	
	// Other reserved words
	FUNCTION	(GrammarRule.FUNCTION, Element.NULL, "function"),
	IF			(GrammarRule.IF, Element.IF, "if"),
	ELSE		(GrammarRule.ELSE, Element.NULL, "else"),
	// ELSEIF exists as basic type but not as a token: ("else if")
	
	// Defined as <stmts> in CFG.xlsx
	ECHO 		(GrammarRule.ECHO, Element.NULL, "echo"),
	INPUT		(GrammarRule.INPUT, Element.NULL, "input"),
	VARIABLE	(GrammarRule.VARIABLE, Element.VARIABLE, "", ("^[a-zA-Z_][a-zA-Z\\d_]*")),
	// Any reserved words must be declared before VAR

	// Defined as <ops> in CFG.xlsx
	AND			(GrammarRule.AND, Element.AND, "&&"),
	OR			(GrammarRule.OR, Element.OR, "||"),
	EQEQ		(GrammarRule.EQEQ, Element.EQEQ, "=="),
	NEQ  		(GrammarRule.NEQ, Element.NEQ, "!="),
	LTEQ 		(GrammarRule.LTEQ, Element.LTEQ, "<="),
	GTEQ  		(GrammarRule.GTEQ, Element.GTEQ, ">="),
	LT 			(GrammarRule.LT, Element.LT, "<"),
	GT  		(GrammarRule.GT, Element.GT, ">"),
	PLUS  		(GrammarRule.PLUS, Element.ADD, "+"),
	MINUS 		(GrammarRule.MINUS, Element.SUB, "-"),
	ASTERISK 	(GrammarRule.ASTERISK, Element.MULT, "*"),
	SLASH 		(GrammarRule.SLASH, Element.INTDIV, "/"),
	NOT			(GrammarRule.NOT, Element.NOT, "!");
	
	public final int tokenValue;
	public final String exactString;
	private final Pattern regexPotential;
	private final Pattern regexFull;
	public final TypeSystem type;
	public final Element construct;

	private Terminal(int tokenValue, Element basicElement,  String... matching) {
		this(tokenValue, null, basicElement, matching);
	}
	private Terminal(int tokenValue, TypeSystem type, Element basicElement, String... matching) {
		this.tokenValue = tokenValue;
		this.type = type;
		// If there is a symbol type, exactString should be ""
		this.exactString = (matching.length > 0) ? matching[0] : "";
		this.regexPotential = (matching.length > 1) ? Pattern.compile(matching[1]) : null;
		this.regexFull = (matching.length > 2) ? Pattern.compile(matching[2]) : null;
		this.construct = basicElement;
	}
	
	public boolean isTerminal() { return true; }
	public boolean isNonTerminal() { return false; }
	
	/**
	 * fullMatch == false: looks for partial match with token
	 * fullMatch == true:  looks for full match with token
	 * @param token String token to compare against
	 * @param fullMatch whether to look for true: full match or false: partial match
	 * @return true: matches according to passed arguments
	 */
	public boolean isMatch(String token, boolean fullMatch) {
		boolean isMatch = false;
		if (this.regexPotential != null) {
			Matcher m = this.regexPotential.matcher(token);
			isMatch = (m.matches());
			
			// If you want only a partial match, 
			// only have to compare against starting regular expression ^^^ (above)
			// If you want a full match,
			// you must check against both starting and ending regular expressions
			if (fullMatch && this.regexFull != null) {
				m = this.regexFull.matcher(token);
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
		return (this.regexFull != null);
	}

	/**
	 * Get Terminal by its tokenValue 
	 * (Token.* or Terminal.tokenValue)
	 * 
	 * @param tokenValue Terminal.tokenValue to find
	 * @return Terminal with matching tokenValue or null
	 */
	public static Terminal getTerminal(int tokenValue) {
		Terminal[] all = Terminal.values();
		for (Terminal nt : all) {
			if (nt.tokenValue == tokenValue) {
				return nt;
			}
		}
		return null;
	}
}

enum NonTerminal implements GrammarRule {
	// Patterns not precedence by FIRST Terminal
	// SINGLE UNDERSCORE
	_PROGRAM_	(GrammarRule._PROGRAM_, Element.SCOPE,
				 firstTerminalsAndPattern(GrammarRule.combineArrays(new int[] { GrammarRule.FUNCTION, GrammarRule.IF, GrammarRule.CURLY_OPEN }, GrammarRule._STMT_FIRST), GrammarRule._STMTS_),
				 firstTerminalsAndPattern(GrammarRule.EMPTY, GrammarRule.EMPTY),
				 follow(GrammarRule.EOF)),
	
	_STMTS_		(GrammarRule._STMTS_, Element.REFLOW_LIMIT,
				 firstTerminalsAndPattern(GrammarRule.FUNCTION, GrammarRule._FUNCDEF_, GrammarRule._STMTS_),
				 firstTerminalsAndPattern(GrammarRule.IF, GrammarRule._IF_, GrammarRule._STMTS_),
				 firstTerminalsAndPattern(GrammarRule.combineArrays(GrammarRule._STMT_FIRST, GrammarRule.CURLY_OPEN), GrammarRule._BLOCKSTMT_, GrammarRule._STMTS_),
				 firstTerminalsAndPattern(GrammarRule.EMPTY, GrammarRule.EMPTY),
				 follow(new int[] {GrammarRule.EOF, GrammarRule.CURLY_CLOSE})),
	
	_FUNCDEF_	(GrammarRule._FUNCDEF_, Element.FUNCDEF,
				 firstTerminalsAndPattern(GrammarRule.FUNCTION, GrammarRule.FUNCTION, GrammarRule.VARIABLE, GrammarRule.PAREN_OPEN, GrammarRule._PARAMS0_, GrammarRule.PAREN_CLOSE, GrammarRule._SCOPE_),
				 GrammarRule.commonFollow1),
	
	_PARAMS0_	(GrammarRule._PARAMS0_, Element.PARAMETERS,
				 firstTerminalsAndPattern(GrammarRule.VARIABLE, GrammarRule.VARIABLE, GrammarRule._PARAMS1_),
				 firstTerminalsAndPattern(GrammarRule.EMPTY, GrammarRule.EMPTY),
				 follow(GrammarRule.PAREN_CLOSE)),
	
	_PARAMS1_	(GrammarRule._PARAMS1_, Element.PASS,
				 firstTerminalsAndPattern(GrammarRule.COMMA, GrammarRule.COMMA, GrammarRule.VARIABLE, GrammarRule._PARAMS1_),
				 firstTerminalsAndPattern(GrammarRule.EMPTY, GrammarRule.EMPTY),
				 follow(GrammarRule.PAREN_CLOSE)),
	
	_SCOPE_		(GrammarRule._SCOPE_, Element.SCOPE,
		 	 	 firstTerminalsAndPattern(GrammarRule.combineArrays(GrammarRule._STMT_FIRST, GrammarRule.CURLY_OPEN), GrammarRule._BLOCKSTMT_),
		 	 	 GrammarRule.commonFollow2),
	
	_IF_		(GrammarRule._IF_, Element.PASS,
			 	 firstTerminalsAndPattern(GrammarRule.IF, GrammarRule.IF, GrammarRule.PAREN_OPEN, GrammarRule._EXPR_, GrammarRule.PAREN_CLOSE, GrammarRule._SCOPE_, GrammarRule._ELSE_),
			 	 GrammarRule.commonFollow1),
	
	_ELSE_	    (GrammarRule._ELSE_, Element.PASS,
			 	 firstTerminalsAndPattern(GrammarRule.ELSE, GrammarRule.ELSE, GrammarRule._ELSEIF_),
			 	 firstTerminalsAndPattern(GrammarRule.EMPTY, GrammarRule.EMPTY),
			 	 GrammarRule.commonFollow1),
	
	_ELSEIF_	(GrammarRule._ELSEIF_, Element.PASS,
			 	 firstTerminalsAndPattern(GrammarRule.IF, GrammarRule.IF, GrammarRule.PAREN_OPEN, GrammarRule._EXPR_, GrammarRule.PAREN_CLOSE, GrammarRule._SCOPE_, GrammarRule._ELSE_),
			 	 firstTerminalsAndPattern(IntStream.rangeClosed(1, id.id).toArray(), GrammarRule._SCOPE_),
			 	 GrammarRule.commonFollow1),
	
	_BLOCKSTMT_	(GrammarRule._BLOCKSTMT_, Element.PASS,
			 	 firstTerminalsAndPattern(GrammarRule.CURLY_OPEN, GrammarRule._BLOCK_),
			 	 firstTerminalsAndPattern(GrammarRule._STMT_FIRST, GrammarRule._STMT_),
			 	 GrammarRule.commonFollow2),
	
	_BLOCK_		(GrammarRule._BLOCK_, Element.PASS,
				 firstTerminalsAndPattern(GrammarRule.CURLY_OPEN, GrammarRule.CURLY_OPEN, GrammarRule._STMTS_, GrammarRule.CURLY_CLOSE),
				 GrammarRule.commonFollow2),
	
	_STMT_		(GrammarRule._STMT_, Element.PASS,
			 	 firstTerminalsAndPattern(GrammarRule.ECHO, GrammarRule._ECHO_, GrammarRule.SEMICOLON),
			 	 firstTerminalsAndPattern(GrammarRule.INPUT, GrammarRule._INPUT_, GrammarRule.SEMICOLON),
			 	 firstTerminalsAndPattern(GrammarRule.COMMENT, GrammarRule.COMMENT),
				 firstTerminalsAndPattern(GrammarRule.VARIABLE, GrammarRule.VARIABLE, GrammarRule._VARSTMT_, GrammarRule.SEMICOLON),
				 GrammarRule.commonFollow2),
	
	_ECHO_		(GrammarRule._ECHO_, Element.OUTPUT,
		 	 	 firstTerminalsAndPattern(GrammarRule.ECHO, GrammarRule.ECHO, GrammarRule._EXPR_),
				 follow(GrammarRule.SEMICOLON)),
	
	_INPUT_		(GrammarRule._INPUT_, Element.INPUT,
				 firstTerminalsAndPattern(GrammarRule.INPUT, GrammarRule.INPUT, GrammarRule.VARIABLE),
				 follow(GrammarRule.SEMICOLON)),

	_VARSTMT_	(GrammarRule._VARSTMT_, Element.PASS,
				 firstTerminalsAndPattern(GrammarRule.EQ, GrammarRule._VARDEF_),
				 firstTerminalsAndPattern(GrammarRule.PAREN_OPEN, GrammarRule._FUNCCALL_),
				 follow(GrammarRule.SEMICOLON)),
	
	_VARDEF_	(GrammarRule._VARDEF_, Element.VARDEF,
				 firstTerminalsAndPattern(GrammarRule.EQ, GrammarRule.EQ, GrammarRule._EXPR_),
				 follow(GrammarRule.SEMICOLON)),
	
	_EXPR_		(GrammarRule._EXPR_, Element.PASS,
				 // Send stream to precedence branch no matter what
				 firstTerminalsAndPattern(IntStream.rangeClosed(1, id.id).toArray(), GrammarRule.__PRECEDENCE1__),
	 			 follow(new int[] {GrammarRule.SEMICOLON, GrammarRule.PAREN_CLOSE, GrammarRule.COMMA})),
	
	_VALUE_		(GrammarRule._VALUE_, Element.PASS,
				 firstTerminalsAndPattern(GrammarRule.VARIABLE, GrammarRule._VARIABLE_),
				 firstTerminalsAndPattern(GrammarRule.primitiveSet, GrammarRule._LITERAL_),
				 GrammarRule.commonFollow3),
	
	_VARIABLE_	(GrammarRule._VARIABLE_, Element.PASS,
				 firstTerminalsAndPattern(GrammarRule.VARIABLE, GrammarRule.VARIABLE, GrammarRule._VAREXPR_),
				 GrammarRule.commonFollow3),
	
	_VAREXPR_	(GrammarRule._VAREXPR_, Element.PASS,
				 firstTerminalsAndPattern(GrammarRule.PAREN_OPEN, GrammarRule._FUNCCALL_),
				 firstTerminalsAndPattern(GrammarRule.EMPTY, GrammarRule.EMPTY),
				 GrammarRule.commonFollow3),
	
	_FUNCCALL_	(GrammarRule._FUNCCALL_, Element.FUNCCALL,
				 firstTerminalsAndPattern(GrammarRule.PAREN_OPEN, GrammarRule.PAREN_OPEN, GrammarRule._ARGS0_, GrammarRule.PAREN_CLOSE),
				 GrammarRule.commonFollow3),
	
	_ARGS0_		(GrammarRule._ARGS0_, Element.ARGUMENTS,
				 firstTerminalsAndPattern(GrammarRule.combineArrays(GrammarRule.combineArrays(GrammarRule.operatorSet, GrammarRule.VARIABLE, GrammarRule.PLUS, GrammarRule.MINUS, GrammarRule.PAREN_OPEN), GrammarRule.primitiveSet), GrammarRule._EXPR_, GrammarRule._ARGS1_),
				 firstTerminalsAndPattern(GrammarRule.EMPTY, GrammarRule.EMPTY),
				 follow(GrammarRule.PAREN_CLOSE)),
	
	_ARGS1_		(GrammarRule._ARGS1_, Element.PASS,
				 firstTerminalsAndPattern(GrammarRule.COMMA, GrammarRule.COMMA, GrammarRule._EXPR_, GrammarRule._ARGS1_),
				 firstTerminalsAndPattern(GrammarRule.EMPTY, GrammarRule.EMPTY),
				 follow(GrammarRule.PAREN_CLOSE)),
	
	_LITERAL_	(GrammarRule._LITERAL_, Element.PASS,
				 singleTerminalsPatternsAndFollow(
						 GrammarRule.primitiveSet, 
						 GrammarRule.commonFollow3
						 )
				 ),
	
	// Patterns ambiguous by FIRST Terminal set
	// but not ambiguous by NonTerminal
	// Double underscore
	__PRECEDENCE1__(GrammarRule.__PRECEDENCE1__, precedenceSplitAt(GrammarRule.operatorSetRank1), 	GrammarRule.__PRECEDENCE1__, 	GrammarRule.__PRECEDENCE2__,	Direction.RIGHT_TO_LEFT,	GrammarRule.__BINARY__),
	__PRECEDENCE2__(GrammarRule.__PRECEDENCE2__, precedenceSplitAt(GrammarRule.operatorSetRank2), 	GrammarRule.__PRECEDENCE2__, 	GrammarRule.__PRECEDENCE3__,	Direction.RIGHT_TO_LEFT,	GrammarRule.__BINARY__),
	__PRECEDENCE3__(GrammarRule.__PRECEDENCE3__, precedenceSplitAt(GrammarRule.operatorSetRank3), 	GrammarRule.__PRECEDENCE3__, 	GrammarRule.__PRECEDENCE4__,	Direction.RIGHT_TO_LEFT,	GrammarRule.__BINARY__),
	__PRECEDENCE4__(GrammarRule.__PRECEDENCE4__, precedenceSplitAt(GrammarRule.operatorSetRank4), 	GrammarRule.__PRECEDENCE4__, 	GrammarRule.__PRECEDENCE5__,	Direction.RIGHT_TO_LEFT,	GrammarRule.__BINARY__),
	__PRECEDENCE5__(GrammarRule.__PRECEDENCE5__, precedenceSplitAt(GrammarRule.operatorSetRank5), 	GrammarRule.__PRECEDENCE5__,	GrammarRule._VALUE_,	Direction.RIGHT_TO_LEFT,	GrammarRule.__UNARY__),
	// Placeholder
	// All operations appear with this as parent to its two operands
	__BINARY__	(GrammarRule.__BINARY__, Element.OPERATION),
	__UNARY__	(GrammarRule.__UNARY__, Element.OPERATION);
	
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
	
	/**
	 * Precedence Pattern
	 * 
	 * Find first instance of any item in splitAt
	 * starting from the one side and working Token.Direction (i.e. LEFT or RIGHT)
	 * The match on the left side will evaluate first,
	 * returning a parse tree.
	 * Then the match on the right will evaluate.
	 * These "operands" will then be combined into a binary Node(NonTerminal wrapper)
	 * 
	 * @author Zachary Gateley
	 *
	 */
	public class PrecedencePattern {
		public final int[] splitTokens;
		public final int leftRule;
		public final int rightRule;
		public final GrammarRule.Direction direction;
		public final int nonTerminalWrapper;
		
		public PrecedencePattern(
				int[] splitAt, 
				int leftRule, 
				int rightRule, 
				GrammarRule.Direction direction,
				int nonTerminalWrapper
				) {
			this.splitTokens = splitAt;
			this.leftRule = leftRule;
			this.rightRule = rightRule;
			this.direction = direction;
			this.nonTerminalWrapper = nonTerminalWrapper;
		}
	}
	
	public final Element basicElement;
	public final int tokenValue;
	public final Pattern[] patterns;
	public final PrecedencePattern precedencePattern;
	public final int[] FOLLOW;
	
	/**
	 * Constructor --> Empty wrapper, not part of any CFG or Precedence rule
	 */
	private NonTerminal(int tokenValue, Element basicElement) {
		this.tokenValue = tokenValue;
		this.patterns = null;
		this.precedencePattern = null;
		this.FOLLOW = null;
		this.basicElement = basicElement;
	}
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
	private NonTerminal(int tokenValue, Element basicType, int[][]... patterns) {
		this.tokenValue = tokenValue;
		this.basicElement = basicType;
		this.patterns = new Pattern[patterns.length - 1];
		// Therefore, skip last item
		for (int i = 0; i < patterns.length - 1; i++) {
			Pattern p = new Pattern(patterns[i][0], patterns[i][1]);
			// Add FIRST / PATTERN sets
			this.patterns[i] = p;
		}
		this.precedencePattern = null;
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
	private NonTerminal(int tokenValue, int[] splitAt, int leftRule, int rightRule, Direction direction, int nonTerminalWrapper) {
		this.tokenValue = tokenValue;
		this.patterns = null;
		this.precedencePattern = new PrecedencePattern(splitAt, leftRule, rightRule, direction, nonTerminalWrapper);
		this.FOLLOW = null;
		this.basicElement = Element.PASS;
	}
	
	public boolean isTerminal() { return false; }
	public boolean isNonTerminal() { return true; }

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
	private static int[] precedenceSplitAt(int... tokens) {
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
	public static NonTerminal getNonTerminal(final int tokenValue) {
		NonTerminal[] all = NonTerminal.values();
		for (NonTerminal nt : all) {
			if (nt.tokenValue == tokenValue) {
				return nt;
			}
		}
		return null;
	}
	
	public boolean isCFGRule() {
		return GrammarRule.isCFGRule(this.tokenValue);
	}
	public boolean isPrecedenceRule() {
		return GrammarRule.isPrecedenceRule(this.tokenValue);
	}
}