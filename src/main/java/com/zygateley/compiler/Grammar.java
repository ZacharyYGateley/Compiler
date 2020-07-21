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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.regex.*;
import java.util.stream.*;


public class Grammar {
	// Build language
		// Additional bindings as required
		public static enum Reflow {
			MOVE_UPWARDS_AND_LEFT,	// move source to become target's (== parent's) left sibling
			MOVE_RIGHT_TO_CHILD,	// target.insertChild(0, source)
			MOVE_LEFT_TO_CHILD, 	// target.addChild(source)
			
			@Deprecated
			MERGE_LEFT,			 	// new combined node (target & source) w/ target children then source children
		}
		// Immutable transformation
		private static class ReflowTransformation {
			public final Reflow type;
			public final Construct prevChild;
			@Deprecated
			public final Construct result;
			
			/**
			 * Create one single transformation.
			 * The source element type is not a part of this transformation.
			 * 
			 * @param type the specific Element.Reflow type corresponding to this transformation,
			 * 		which corresponds to how the source element will be transformed
			 * @param target the target element to which the ReflowRelationship will point
			 * @param result the resulting element type of the transformation
			 * 		<div style="margin-left:20px;">
			 * 		It <strong>must not be null</strong> to indicate a valid transformation.<br />
			 * 		However, the specific <i>type</i> may not *necessarily* apply. 
			 * 		For example, consider MERGE_LEFT_TO_CHILD. The Node with the 
			 * 		source Element type will be popped from the optimized tree
			 * 		and replaced as a child of the Node with the target Element type.
			 * 		Its specific result type is not used except to show that the transformation
			 * 		is valid.
			 * 		</div>
			 * 		
			 */
			public ReflowTransformation(Reflow type, Construct target, Construct result) {
				this.type = type;
				this.prevChild = target;
				this.result = result;
				if (result == null) {
					throw new NullPointerException("ReflowTransformation result may not be null.");
				}
			}
		}
		/**
		 * These are private, immutable reflow relationship indicators,
		 * which are built into the reflowBindings ArrayList.
		 * 
		 * Each relationship holds a source element type
		 * And all of its respective transformations
		 * 
		 * @author Zachary Gateley
		 *
		 */
		private static class ReflowRelationship {
			// Element types
			// e.g. merge [source] into [target], resulting in Element type [result]
			public final Construct source;
			public final ReflowTransformation[] transformations;
			
			/**
			 * @param source the element that needs to traverse the tree in some way
			 * @param transformations all respective transformations for this source element type
			 */
			public ReflowRelationship(Construct source, ReflowTransformation... transformations) {
				this.source = source;
				this.transformations = transformations;
			}
		}
		
		/**
		 * Get the specific reflow rule on this
		 * source element and target element
		 * using this specific reflow type
		 * 
		 * @param type Reflow binding type
		 * @param source the element type whose Node may need to be moved or modified into the Node of the target element
		 * @param target the element type whose Node may receive the action of the Node with the source element type
		 * @return the resulting element type of the determined, specific reflow binding rule, if it exists
		 */
		public static Construct getReflowResult(final Reflow type, final Construct source, final Construct target) {
			try {
				Function<Construct, Boolean> sourceLambda = (source != null) ? ((Construct e) -> e == source) : (s -> true);
				Function<Reflow, Boolean> bindingLambda = (type != null) ? ((Reflow b) -> b == type) : (s -> true);
				Function<Construct, Boolean> targetLambda = (target != null) ? ((Construct e) -> e == target) : (s -> true);
				ReflowRelationship matchingSource = 
						reflowBindings.stream()
						.filter((ReflowRelationship r) -> sourceLambda.apply(r.source))
						.findAny().get();
				ReflowTransformation fullMatch =
						Arrays.stream(matchingSource.transformations)
						.filter((ReflowTransformation t) -> bindingLambda.apply(t.type))
						.filter((ReflowTransformation t) -> targetLambda.apply(t.prevChild))
						.findFirst().get();
						
				return fullMatch.result;
			}
			catch (NoSuchElementException err) {
				return null;
			}
		}
		/**
		 * Return boolean on whether this ELEMENT
		 * has special reflow bindings.
		 * 
		 * The binding might not apply to this specific NODE,
		 * so the resulting optimizer logic will have to
		 * check.
		 * 
		 * @param sourceElement
		 * @return
		 */
		public static boolean isReflow(final Construct sourceElement) {
			return getReflowResult(null, sourceElement, null) != null;
		}
		
		// ADD BINDINGS
		// 		TODO: relationships will be built on the fly
		// 		So we do not know their final length
		private static ArrayList<ReflowRelationship> reflowBindings = new ArrayList<>();
		static {
			// Long term: add method to build these for support for multiple languages
			
			reflowBindings.add(new ReflowRelationship(
					Construct.FUNCCALL, 
					new ReflowTransformation(Reflow.MOVE_LEFT_TO_CHILD, Construct.LOOP, Construct.LOOP)
					));
			reflowBindings.add(new ReflowRelationship(
					Construct.VARIABLE,
					// Function Name
					new ReflowTransformation(Reflow.MOVE_RIGHT_TO_CHILD, Construct.FUNCDEF, Construct.FUNCDEF),
					new ReflowTransformation(Reflow.MOVE_RIGHT_TO_CHILD, Construct.FUNCCALL, Construct.FUNCCALL),
					// Function parameters or arguments
					new ReflowTransformation(Reflow.MOVE_UPWARDS_AND_LEFT, Construct.PARAMETERS, Construct.VARIABLE),
					new ReflowTransformation(Reflow.MOVE_UPWARDS_AND_LEFT, Construct.ARGUMENTS, Construct.VARIABLE),
					// Variable name
					new ReflowTransformation(Reflow.MOVE_RIGHT_TO_CHILD, Construct.VARSET, Construct.VARSET),
					// While condition
					new ReflowTransformation(Reflow.MOVE_LEFT_TO_CHILD, Construct.LOOP, Construct.LOOP),
					// If condition
					new ReflowTransformation(Reflow.MOVE_LEFT_TO_CHILD, Construct.IF, Construct.IF),
					// Variable definition
					new ReflowTransformation(Reflow.MOVE_LEFT_TO_CHILD, Construct.VARDECL, Construct.VARDECL)
					));
			reflowBindings.add(new ReflowRelationship(
					Construct.IF,
					// Else code
					new ReflowTransformation(Reflow.MOVE_LEFT_TO_CHILD, Construct.IF, Construct.IF)
					));
			Construct[] operations = new Construct[] { Construct.AND, Construct.OR, Construct.ADD, Construct.SUB, Construct.MULT, Construct.INTDIV, Construct.EQEQ, Construct.NEQ, Construct.LT, Construct.GT, Construct.LTEQ, Construct.GTEQ, Construct.NOT };
			for (Construct operation : operations) {
				reflowBindings.add(new ReflowRelationship(
						operation,
						// While condition
						new ReflowTransformation(Reflow.MOVE_LEFT_TO_CHILD, Construct.LOOP, operation),
						// If condition
						new ReflowTransformation(Reflow.MOVE_LEFT_TO_CHILD, Construct.IF, operation),
						// Variable definition
						new ReflowTransformation(Reflow.MOVE_LEFT_TO_CHILD, Construct.VARDECL, operation),
						// Function parameters and arguments
						new ReflowTransformation(Reflow.MOVE_UPWARDS_AND_LEFT, Construct.PARAMETERS, operation),
						new ReflowTransformation(Reflow.MOVE_UPWARDS_AND_LEFT, Construct.ARGUMENTS, operation)
						));
			}
			Construct[] values = new Construct[] { Construct.LITERAL, Construct.FALSE, Construct.TRUE };
			for (Construct valueType : values) {
				reflowBindings.add(new ReflowRelationship(
						valueType,
						// While condition
						new ReflowTransformation(Reflow.MOVE_LEFT_TO_CHILD, Construct.LOOP, valueType),
						// If condition
						new ReflowTransformation(Reflow.MOVE_LEFT_TO_CHILD, Construct.IF, valueType),
						// Variable definition
						new ReflowTransformation(Reflow.MOVE_LEFT_TO_CHILD, Construct.VARDECL, valueType),
						// Function parameters and arguments
						new ReflowTransformation(Reflow.MOVE_UPWARDS_AND_LEFT, Construct.PARAMETERS, valueType),
						new ReflowTransformation(Reflow.MOVE_UPWARDS_AND_LEFT, Construct.ARGUMENTS, valueType)
						));
			}
			reflowBindings.add(new ReflowRelationship(
					Construct.SCOPE,
					// While body
					new ReflowTransformation(Reflow.MOVE_LEFT_TO_CHILD, Construct.LOOP, Construct.LOOP),
					// Conditional body
					new ReflowTransformation(Reflow.MOVE_LEFT_TO_CHILD, Construct.IF, Construct.IF)
					));
		}
}


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
interface GrammarRule {
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
		INTEGER = 			id.next(),
		STRING = 		id.next(), 

		FUNCTION = 		id.next(),
		WHILE =			id.next(),
		FOR = 			id.next(),
		TO = 			id.next(),
		STEP = 			id.next(),
		IF = 			id.next(),
		ELSE = 			id.next(),
		// ELSEIF exists as basic type but not as a token: (i.e. "else if", not "elseif")
		VAR = 			id.next(),
		
		// Statement FIRST set
		ECHO = 			id.next(),
		INPUT = 		id.next(),
		VARIABLE = 		id.next();
	
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
		_WHILE_ = 		id.next(),
		_FOR_ = 		id.next(),
		_FORBOUND_ = 	id.next(),
		_FORSTEP_ =		id.next(),
		_IF_ =			id.next(),
		_ELSE_ = 		id.next(),
		_ELSEIF_ = 		id.next(),
		_BLOCKSTMT_ = 	id.next(),
		_BLOCK_ = 		id.next(),
		_STMT_ = 		id.next(),
		_ECHO_ = 		id.next(),
		_INPUT_ = 		id.next(),
		_VARDECLSTMT_ = 	id.next(),
		_VARSETSTMT_ =	id.next(),
		_VARSET_ = 		id.next(),
		_VALUEOREXPR_ = id.next(),
		_EXPR_ = 		id.next(),
		_VALUE_ = 		id.next(),
		_VARIABLE_ = 	id.next(),
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
			VAR,
			INPUT,
			ECHO,
			COMMENT
	};
	public static final int[] _STMTS_FIRST = combineArrays(_STMT_FIRST, FUNCTION, WHILE, FOR, IF, CURLY_OPEN, VARIABLE, ECHO);

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
			INTEGER,
			STRING
	};
	public static final int[][] commonFollow1 = new int[][] { combineArrays(_STMTS_FIRST, EOF, CURLY_CLOSE) };
	public static final int[][] commonFollow2 = new int[][] { combineArrays(_STMTS_FIRST, EOF, CURLY_CLOSE, 
			//ELSEIF,
			ELSE) };
	public static final int[][] commonFollow3 = new int[][] { combineArrays(
			operatorSet, SEMICOLON, TO, COMMA, PAREN_CLOSE
			) };
	

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
	EMPTY 		(GrammarRule.EMPTY, Construct.NULL, "", "^\\s"),
	// Allow premature termination of compilation
	EOF 		(GrammarRule.EOF, Construct.NULL, "noco"),

	// Defined as <ops> in CFG.xlsx
	AND			(GrammarRule.AND, Construct.AND, "&&"),
	OR			(GrammarRule.OR, Construct.OR, "||"),
	EQEQ		(GrammarRule.EQEQ, Construct.EQEQ, "=="),
	NEQ  		(GrammarRule.NEQ, Construct.NEQ, "!="),
	LTEQ 		(GrammarRule.LTEQ, Construct.LTEQ, "<="),
	GTEQ  		(GrammarRule.GTEQ, Construct.GTEQ, ">="),
	LT 			(GrammarRule.LT, Construct.LT, "<"),
	GT  		(GrammarRule.GT, Construct.GT, ">"),
	PLUS  		(GrammarRule.PLUS, Construct.ADD, "+"),
	MINUS 		(GrammarRule.MINUS, Construct.SUB, "-"),
	ASTERISK 	(GrammarRule.ASTERISK, Construct.MULT, "*"),
	// Want to define slash before comment
	SLASH 		(GrammarRule.SLASH, Construct.INTDIV, "/"),
	NOT			(GrammarRule.NOT, Construct.NOT, "!"),
	
	SEMICOLON	(GrammarRule.SEMICOLON, Construct.NULL, ";"),
	COMMA		(GrammarRule.COMMA, Construct.NULL, ","),
	EQ 			(GrammarRule.EQ, Construct.NULL, "="),
	PAREN_OPEN	(GrammarRule.PAREN_OPEN, Construct.NULL, "("),
	PAREN_CLOSE (GrammarRule.PAREN_CLOSE, Construct.NULL, ")"),
	CURLY_OPEN  (GrammarRule.CURLY_OPEN, Construct.NULL, "{"),
	CURLY_CLOSE (GrammarRule.CURLY_CLOSE, Construct.NULL, "}"),
	SQUARE_OPEN (GrammarRule.SQUARE_OPEN, Construct.NULL, "["),
	SQUARE_CLOSE(GrammarRule.SQUARE_CLOSE, Construct.NULL, "]"),
	COMMENT		(GrammarRule.COMMENT, Construct.NULL, "", ("^/(?:/[^\0\r\n\f]*(?=[^\0\r\n\f])?)?$")),
	
	// PRIMITIVES
	TRUE		(GrammarRule.TRUE, TypeSystem.BOOLEAN, Construct.TRUE, "", ("^[tT](?:[rR](?:[uU](?:[eE])?)?)?$")),
	FALSE		(GrammarRule.FALSE, TypeSystem.BOOLEAN, Construct.FALSE, "", ("^[fF](?:[aA](?:[lL](?:[sS](?:[eE])?)?)?)?$")),
	INTEGER		(GrammarRule.INTEGER, TypeSystem.INTEGER, Construct.LITERAL, "", ("^\\d*")),
	STRING      (GrammarRule.STRING, TypeSystem.STRING, Construct.LITERAL, "", ("^\".*"), ("^\"(?:[^\"\\\\]|\\\\.)*\"$")), // ("^\"(?:(?:.*(?:[^\\\\]))?(?:\\\\{2})*)?\"$")
	
	// Other reserved words
	FUNCTION	(GrammarRule.FUNCTION, Construct.NULL, "function"),
	WHILE		(GrammarRule.WHILE, Construct.LOOP, "while"),
	FOR			(GrammarRule.FOR, Construct.LOOP, "for"),
	TO			(GrammarRule.TO, Construct.NULL, "to"),
	STEP		(GrammarRule.STEP, Construct.NULL, "step"),
	IF			(GrammarRule.IF, Construct.IF, "if"),
	ELSE		(GrammarRule.ELSE, Construct.NULL, "else"),
	// ELSEIF exists as basic type but not as a token: ("else if")
	VAR		 	(GrammarRule.VAR, Construct.VARDECL, "var"),
	
	// Defined as <stmts> in CFG.xlsx
	ECHO 		(GrammarRule.ECHO, Construct.NULL, "echo"),
	INPUT		(GrammarRule.INPUT, Construct.NULL, "input"),
	VARIABLE	(GrammarRule.VARIABLE, Construct.VARIABLE, "", ("^[a-zA-Z_][a-zA-Z\\d_]*"));
	// Any reserved words must be declared before VAR
	
	public final int tokenValue;
	public final String exactString;
	private final Pattern regexPotential;
	private final Pattern regexFull;
	public final TypeSystem type;
	public final Construct construct;

	private Terminal(int tokenValue, Construct basicElement,  String... matching) {
		this(tokenValue, null, basicElement, matching);
	}
	private Terminal(int tokenValue, TypeSystem type, Construct basicElement, String... matching) {
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
	_PROGRAM_	(GrammarRule._PROGRAM_, Construct.SCOPE,
				 firstTerminalsAndPattern(GrammarRule.combineArrays(new int[] { GrammarRule.FUNCTION, GrammarRule.WHILE, GrammarRule.FOR, GrammarRule.IF, GrammarRule.CURLY_OPEN }, GrammarRule._STMT_FIRST), GrammarRule._STMTS_),
				 firstTerminalsAndPattern(GrammarRule.EMPTY, GrammarRule.EMPTY),
				 follow(GrammarRule.EOF)),
	
	_STMTS_		(GrammarRule._STMTS_, Construct.REFLOW_LIMIT,
				 firstTerminalsAndPattern(GrammarRule.FUNCTION, GrammarRule._FUNCDEF_, GrammarRule._STMTS_),
				 firstTerminalsAndPattern(GrammarRule.WHILE, GrammarRule._WHILE_, GrammarRule._STMTS_),
				 firstTerminalsAndPattern(GrammarRule.FOR, GrammarRule._FOR_, GrammarRule._STMTS_),
				 firstTerminalsAndPattern(GrammarRule.IF, GrammarRule._IF_, GrammarRule._STMTS_),
				 firstTerminalsAndPattern(GrammarRule.combineArrays(GrammarRule._STMT_FIRST, GrammarRule.CURLY_OPEN), GrammarRule._BLOCKSTMT_, GrammarRule._STMTS_),
				 firstTerminalsAndPattern(GrammarRule.EMPTY, GrammarRule.EMPTY),
				 follow(new int[] {GrammarRule.EOF, GrammarRule.CURLY_CLOSE})),
	
	_FUNCDEF_	(GrammarRule._FUNCDEF_, Construct.FUNCDEF,
				 firstTerminalsAndPattern(GrammarRule.FUNCTION, GrammarRule.FUNCTION, GrammarRule.VARIABLE, GrammarRule.PAREN_OPEN, GrammarRule._PARAMS0_, GrammarRule.PAREN_CLOSE, GrammarRule._SCOPE_),
				 GrammarRule.commonFollow1),
	
	_PARAMS0_	(GrammarRule._PARAMS0_, Construct.PARAMETERS,
				 firstTerminalsAndPattern(GrammarRule.VARIABLE, GrammarRule.VARIABLE, GrammarRule._PARAMS1_),
				 firstTerminalsAndPattern(GrammarRule.EMPTY, GrammarRule.EMPTY),
				 follow(GrammarRule.PAREN_CLOSE)),
	
	_PARAMS1_	(GrammarRule._PARAMS1_, Construct.PASS,
				 firstTerminalsAndPattern(GrammarRule.COMMA, GrammarRule.COMMA, GrammarRule.VARIABLE, GrammarRule._PARAMS1_),
				 firstTerminalsAndPattern(GrammarRule.EMPTY, GrammarRule.EMPTY),
				 follow(GrammarRule.PAREN_CLOSE)),
	
	_SCOPE_		(GrammarRule._SCOPE_, Construct.SCOPE,
		 	 	 firstTerminalsAndPattern(GrammarRule.combineArrays(GrammarRule._STMT_FIRST, GrammarRule.CURLY_OPEN), GrammarRule._BLOCKSTMT_),
		 	 	 GrammarRule.commonFollow2),
	
	_WHILE_		(GrammarRule._WHILE_, Construct.PASS,
				 firstTerminalsAndPattern(GrammarRule.WHILE, GrammarRule.WHILE, GrammarRule._EXPR_, GrammarRule._SCOPE_),
				 GrammarRule.commonFollow1),
	
	_FOR_		(GrammarRule._FOR_, Construct.PASS,
			 	 firstTerminalsAndPattern(GrammarRule.FOR, 
			 			 GrammarRule.FOR, GrammarRule.PAREN_OPEN,
			 			 GrammarRule.VARIABLE, GrammarRule.EQ,
			 			 GrammarRule._FORBOUND_, GrammarRule.TO, GrammarRule._FORBOUND_,
			 			 GrammarRule._FORSTEP_, GrammarRule.PAREN_CLOSE,
			 			 GrammarRule._SCOPE_),
			 	 GrammarRule.commonFollow1),
	
	_FORBOUND_	(GrammarRule._FORBOUND_, Construct.PASS,
				 firstTerminalsAndPattern(GrammarRule.INTEGER, GrammarRule.INTEGER),
				 firstTerminalsAndPattern(GrammarRule.VARIABLE, GrammarRule._VARIABLE_),
				 follow(GrammarRule.combineArrays(GrammarRule._STMT_FIRST, GrammarRule.TO, GrammarRule.STEP, GrammarRule.PAREN_CLOSE))),
	
	_FORSTEP_ 	(GrammarRule._FORSTEP_, Construct.PASS,
				 firstTerminalsAndPattern(GrammarRule.STEP, GrammarRule.STEP, GrammarRule._EXPR_),
				 firstTerminalsAndPattern(GrammarRule.EMPTY, GrammarRule.EMPTY),
			 	 follow(GrammarRule.PAREN_CLOSE)),
	
	_IF_		(GrammarRule._IF_, Construct.PASS,
			 	 firstTerminalsAndPattern(GrammarRule.IF, GrammarRule.IF, GrammarRule.PAREN_OPEN, GrammarRule._EXPR_, GrammarRule.PAREN_CLOSE, GrammarRule._SCOPE_, GrammarRule._ELSE_),
			 	 GrammarRule.commonFollow1),
	
	_ELSE_	    (GrammarRule._ELSE_, Construct.PASS,
			 	 firstTerminalsAndPattern(GrammarRule.ELSE, GrammarRule.ELSE, GrammarRule._ELSEIF_),
			 	 firstTerminalsAndPattern(GrammarRule.EMPTY, GrammarRule.EMPTY),
			 	 GrammarRule.commonFollow1),
	
	_ELSEIF_	(GrammarRule._ELSEIF_, Construct.PASS,
			 	 firstTerminalsAndPattern(GrammarRule.IF, GrammarRule.IF, GrammarRule.PAREN_OPEN, GrammarRule._EXPR_, GrammarRule.PAREN_CLOSE, GrammarRule._SCOPE_, GrammarRule._ELSE_),
			 	 firstTerminalsAndPattern(IntStream.rangeClosed(1, id.id).toArray(), GrammarRule._SCOPE_),
			 	 GrammarRule.commonFollow1),
	
	_BLOCKSTMT_	(GrammarRule._BLOCKSTMT_, Construct.PASS,
			 	 firstTerminalsAndPattern(GrammarRule.CURLY_OPEN, GrammarRule._BLOCK_),
			 	 firstTerminalsAndPattern(GrammarRule._STMT_FIRST, GrammarRule._STMT_),
			 	 GrammarRule.commonFollow2),
	
	_BLOCK_		(GrammarRule._BLOCK_, Construct.PASS,
				 firstTerminalsAndPattern(GrammarRule.CURLY_OPEN, GrammarRule.CURLY_OPEN, GrammarRule._STMTS_, GrammarRule.CURLY_CLOSE),
				 GrammarRule.commonFollow2),
	
	_STMT_		(GrammarRule._STMT_, Construct.PASS,
			 	 firstTerminalsAndPattern(GrammarRule.ECHO, GrammarRule._ECHO_, GrammarRule.SEMICOLON),
			 	 firstTerminalsAndPattern(GrammarRule.INPUT, GrammarRule._INPUT_, GrammarRule.SEMICOLON),
			 	 firstTerminalsAndPattern(GrammarRule.COMMENT, GrammarRule.COMMENT),
			 	 firstTerminalsAndPattern(GrammarRule.VAR, GrammarRule.VAR, GrammarRule.VARIABLE, GrammarRule._VARDECLSTMT_, GrammarRule.SEMICOLON),
				 firstTerminalsAndPattern(GrammarRule.VARIABLE, GrammarRule.VARIABLE, GrammarRule._VARSETSTMT_, GrammarRule.SEMICOLON),
				 GrammarRule.commonFollow2),
	
	_ECHO_		(GrammarRule._ECHO_, Construct.OUTPUT,
		 	 	 firstTerminalsAndPattern(GrammarRule.ECHO, GrammarRule.ECHO, GrammarRule._EXPR_),
				 follow(GrammarRule.SEMICOLON)),
	
	_INPUT_		(GrammarRule._INPUT_, Construct.INPUT,
				 firstTerminalsAndPattern(GrammarRule.INPUT, GrammarRule.INPUT, GrammarRule.VARIABLE),
				 follow(GrammarRule.SEMICOLON)),

	_VARDECLSTMT_(GrammarRule._VARDECLSTMT_, Construct.PASS,
				 firstTerminalsAndPattern(GrammarRule.EQ, GrammarRule.EQ, GrammarRule._EXPR_),
				 firstTerminalsAndPattern(GrammarRule.EMPTY, GrammarRule.EMPTY),
				 follow(GrammarRule.SEMICOLON)),
	
	_VARSETSTMT_(GrammarRule._VARSETSTMT_, Construct.PASS,
				 firstTerminalsAndPattern(GrammarRule.EQ, GrammarRule._VARSET_),
				 firstTerminalsAndPattern(GrammarRule.PAREN_OPEN, GrammarRule._FUNCCALL_),
				 follow(GrammarRule.SEMICOLON)),
	
	_VARSET_	(GrammarRule._VARSET_, Construct.VARSET,
				 firstTerminalsAndPattern(GrammarRule.EQ, GrammarRule.EQ, GrammarRule._EXPR_),
				 follow(GrammarRule.SEMICOLON)),
	
	_EXPR_		(GrammarRule._EXPR_, Construct.PASS,
				 // Send stream to precedence branch no matter what
				 firstTerminalsAndPattern(IntStream.rangeClosed(1, id.id).toArray(), GrammarRule.__PRECEDENCE1__),
	 			 follow(new int[] {GrammarRule.SEMICOLON, GrammarRule.PAREN_CLOSE, GrammarRule.COMMA})),
	
	_VALUE_		(GrammarRule._VALUE_, Construct.PASS,
				 firstTerminalsAndPattern(GrammarRule.VARIABLE, GrammarRule._VARIABLE_),
				 firstTerminalsAndPattern(GrammarRule.primitiveSet, GrammarRule._LITERAL_),
				 GrammarRule.commonFollow3),
	
	_VARIABLE_	(GrammarRule._VARIABLE_, Construct.PASS,
				 firstTerminalsAndPattern(GrammarRule.VARIABLE, GrammarRule.VARIABLE, GrammarRule._VAREXPR_),
				 GrammarRule.commonFollow3),
	
	_VAREXPR_	(GrammarRule._VAREXPR_, Construct.PASS,
				 firstTerminalsAndPattern(GrammarRule.PAREN_OPEN, GrammarRule._FUNCCALL_),
				 firstTerminalsAndPattern(GrammarRule.EMPTY, GrammarRule.EMPTY),
				 GrammarRule.commonFollow3),
	
	_FUNCCALL_	(GrammarRule._FUNCCALL_, Construct.FUNCCALL,
				 firstTerminalsAndPattern(GrammarRule.PAREN_OPEN, GrammarRule.PAREN_OPEN, GrammarRule._ARGS0_, GrammarRule.PAREN_CLOSE),
				 GrammarRule.commonFollow3),
	
	_ARGS0_		(GrammarRule._ARGS0_, Construct.ARGUMENTS,
				 firstTerminalsAndPattern(GrammarRule.combineArrays(GrammarRule.combineArrays(GrammarRule.operatorSet, GrammarRule.VARIABLE, GrammarRule.PLUS, GrammarRule.MINUS, GrammarRule.PAREN_OPEN), GrammarRule.primitiveSet), GrammarRule._EXPR_, GrammarRule._ARGS1_),
				 firstTerminalsAndPattern(GrammarRule.EMPTY, GrammarRule.EMPTY),
				 follow(GrammarRule.PAREN_CLOSE)),
	
	_ARGS1_		(GrammarRule._ARGS1_, Construct.PASS,
				 firstTerminalsAndPattern(GrammarRule.COMMA, GrammarRule.COMMA, GrammarRule._EXPR_, GrammarRule._ARGS1_),
				 firstTerminalsAndPattern(GrammarRule.EMPTY, GrammarRule.EMPTY),
				 follow(GrammarRule.PAREN_CLOSE)),
	
	_LITERAL_	(GrammarRule._LITERAL_, Construct.PASS,
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
	__BINARY__	(GrammarRule.__BINARY__, Construct.OPERATION),
	__UNARY__	(GrammarRule.__UNARY__, Construct.OPERATION);
	
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
	
	public final Construct basicElement;
	public final int tokenValue;
	public final Pattern[] patterns;
	public final PrecedencePattern precedencePattern;
	public final int[] FOLLOW;
	
	/**
	 * Constructor --> Empty wrapper, not part of any CFG or Precedence rule
	 */
	private NonTerminal(int tokenValue, Construct basicElement) {
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
	private NonTerminal(int tokenValue, Construct basicType, int[][]... patterns) {
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
		this.basicElement = Construct.PASS;
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