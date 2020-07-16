package com.zygateley.compiler;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;


/**
 * Instances of this class can
 * parse a TokenStream and return the 
 * root of the resulting syntax tree.
 * 
 * There are two streams the parser may follow:
 * 		CFG (Context Free Grammar)
 * 			Processes the stream linearly
 * 			Must be unambiguous
 * 		Precedence
 * 			Processes an expression (as the token stream) recursively
 * 			Used to properly apply operator precedence on expressions
 * 			toPrecedenceStream() accounts for
 * 				balanced parentheses
 * 				unary operators
 * 			parsePrecedenceRule() accounts for all other rules
 * 				See NonTerminal.PrecedencePattern
 * 				for more information on these rules
 * 
 * @author Zachary Gateley
 *
 */
public class Parser {
	private TokenStream tokenStream;
	private FileWriter logFileWriter;
	private ArrayList<Scope> scopeStack;
	private Scope currentScope = null;
	private Scope globalScope = null;
	
	// Verbose output shows an XML representation
	// of the parse tree, indenting appropriately by depth 
	private boolean verbose = false;
	// Show switches between streams
	private boolean doublyVerbose = false;
	private int depth = 0;
	
	/**
	 * The only variable needed to instantiate a
	 * parser is a TokenStream
	 * @param tokenStream
	 */
	public Parser(TokenStream tokenStream) {
		this(tokenStream, null);
	}
	public Parser(TokenStream tokenStream, FileWriter logFileWriter) {
		this.tokenStream = tokenStream;
		this.logFileWriter = logFileWriter;
		this.scopeStack = new ArrayList<Scope>();
	}

	/**
	 * Parse the TokenStream in verbose mode.
	 * Outputs XML structure of parseTree
	 * and shows switching between CFG stream and Precedence stream.  
	 * 
	 * @param verbose
	 * @return Node root of resulting syntax tree
	 * @throws SyntaxError
	 * @throws IOException 
	 */
	public Node parse(boolean verbose) throws Exception {
		return parse(verbose, false);
	}
	/**
	 * Parse the TokenStream in verbose mode.
	 * Outputs XML structure of parseTree
	 * and shows switching between CFG stream and Precedence stream.  
	 * 
	 * @param verbose
	 * @param doublyVerbose
	 * @return Node root of resulting syntax tree
	 * @throws SyntaxError
	 * @throws IOException 
	 */
	public Node parse(boolean verbose, boolean doublyVerbose) throws Exception {
		this.verbose = verbose;
		this.doublyVerbose = doublyVerbose;
		return parse();
	}
	/**
	 * Parse the TokenStream.
	 * 
	 * @return Node root of resulting syntax tree
	 * @throws Exception 
	 */
	public Node parse() throws Exception {
		// Reset parsing parameters
		depth = 0;
		
		this.log("<!-- Parsing initiated -->\n");

		this.log("// --> To CFG stream");
		this.log("//");
		
		// Root node of syntax tree
		// The starting rule MUST be a CFG rule
		// Precedence rules depend on parent rules and their respective follow sets
		NonTerminal startingRule = NonTerminal.getNonTerminal(GrammarRule.startingRule);
		Node syntaxTree = parseCFGRule(startingRule, tokenStream.length());
		
		Token nextItem = tokenStream.peekLeft();
		if (nextItem == null || nextItem.token != Terminal.EOF) {
			this.fatalError("Syntax error: program closed before code finished parsing.");
			return null;
		}
				
		this.log("//");
		this.log("// <-- From CFG stream");
		
		this.log("\n<!-- Parsing finished -->\n\n");
		
		return syntaxTree;
	}

	/**
	 * 
	 * From Precedence stream to non-precedence stream.
	 * Must set token stream positioning appropriately
	 * before calling parseCFGRule
	 * 
	 * @param ruleCFG
	 * @param endPosition exclusive
	 * @return
	 * @throws Exception 
	 */
	private Node toCFGStream(NonTerminal ruleCFG, int startPosition, int endPosition) throws Exception {
		if (doublyVerbose) {
			this.log("// --> To CFG stream from Precedence stream");
			this.log("//");
		}
		tokenStream.setLeftIndex(startPosition);
		tokenStream.setRightIndexExcl(endPosition);
		Node syntaxSubtree = parseCFGRule(ruleCFG, endPosition);
		if (doublyVerbose) {
			this.log("//");
			this.log("// <-- To Precedence stream from CFG stream");
		}
		return syntaxSubtree;
	}
	
	/**
	 * Parse left-to-right using a standard CFG rule.
	 * 
	 * TokenStream.leftPosition and TokenStream.rightPosition
	 * assumed to be appropriately set.
	 * 
	 * @param rule NonTerminal representing this CFG rule
	 * @param endPosition exclusive in TokenStream
	 * @return Node root of resulting parse subtree 
	 * @throws Exception 
	 */
	private Node parseCFGRule(NonTerminal rule, int endPosition) throws Exception {
		// Begin new subtree
		Node syntaxSubtree;
		Construct construct = rule.basicElement;
		if (Construct.SCOPE.equals(construct)) {
			syntaxSubtree = new Node(rule, null, this.currentScope);
			this.currentScope = syntaxSubtree.getScope();
			this.scopeStack.add(this.currentScope);
			if (this.globalScope == null) {
				this.globalScope = this.currentScope;
			}
			if (this.currentScope == null) {
				System.out.println("");
			}
		}
		else {
			syntaxSubtree = new Node(rule);
		}
		
		// Skip all EMPTY tokens
		// These are only used to hasEpsilon and inFollow
		Terminal t = tokenStream.peekLeft().token;
		while (!tokenStream.isEmpty() && (t == null || t == Terminal.EMPTY)) tokenStream.readLeft();

    	int indexInFirst = rule.indexOfMatchFirst(t);

		// Next terminal is NOT in rule
    	if (indexInFirst < 0) {
    		boolean hasEpsilon = (rule.indexOfMatchFirst(Terminal.EMPTY) > -1);
        	boolean inFollow = rule.inFollow(t);
        	if (hasEpsilon && inFollow) {
        		// Empty string has been utilized for this rule
        		this.log("<" + rule.toString().replaceAll("_",  "") + " />");
        	}
        	else {
        		this.fatalError("Syntax error: Production rule terminated prematurely.");
        	}
        	return null;
    	}
    	
    	// Starting building this NonTerminal
		int[] pattern = rule.patterns[indexInFirst].PATTERN;
		
		// Show XML structure 
		if (verbose) {
			log(syntaxSubtree);
		}
		
		// Get rules in order
		// If any of the rules is a NonTerminal, recur
		int patternIndex = -1;
		while (!tokenStream.isEmpty()) {
			// Skip all EMPTY terminals in stream
			while (!tokenStream.isEmpty()) {
				Token nextItem = tokenStream.peekLeft();
				if (nextItem == null || nextItem.token != Terminal.EMPTY) break;
				tokenStream.readLeft();
			}
			
			// If reached the end of this expression
			// but have not finished the NonTerminal, err out
			if (tokenStream.isEmpty()) {
				this.fatalError("Syntax error: Missing value or expression.");
				return null;
			}
			
			// Is pattern finished?
			boolean patternFinished = patternIndex == pattern.length - 1;
			if (patternFinished) {
				Token nextItem = tokenStream.peekLeft();
				boolean inFollow = rule.inFollow(nextItem.token);
				if (!inFollow) {
					// Syntax error
					fatalError("Syntax error: Non-empty production rule terminated prematurely.");
					return null;
				}
				// Pattern finished successfully
				break;
			}
			
			// Otherwise, keep building from rule
			patternIndex++;
			int patternTokenValue = pattern[patternIndex];
			GrammarRule patternToken;
			if (GrammarRule.isTerminal(patternTokenValue)) {
				patternToken = Terminal.getTerminal(patternTokenValue);
			}
			else {
				patternToken = NonTerminal.getNonTerminal(patternTokenValue);
			}
			
			// Too much in the expression?
			if (patternIndex == pattern.length) {
				// If too much in the expression
				// We are missing a follow character
				this.fatalError("Syntax error: Missing termination or separation character.");
				return null;
			}
			
			// Terminal
			// These are immediately added to the syntax tree
			if (patternToken.isTerminal()) {
				Token item = tokenStream.readLeft();
				
				// Verify pattern match
				if (item.token != patternToken) {
					this.fatalError("Fatal error: incorrect syntax.");
					return null;
				}
				
				// Add new terminal
				Node terminalNode = addTerminal(syntaxSubtree, item.token, item.symbol, item.value);
			}
			// NonTerminals
			// Recur into parseRule
			else {
				depth++;
				NonTerminal nextRule = (NonTerminal) patternToken;
				Node next;
				if (!nextRule.isPrecedenceRule()) {
					next = parseCFGRule(nextRule, endPosition);
				}
				// Precedence rules need to move into precedence branch
				else {
					// Precedence rules do not represent an increase in depth
					next = toPrecedenceStream(nextRule, rule, tokenStream.getLeftIndex(), endPosition);
				}
				depth--;

				// Add resulting NonTerminal to tree
				if (next != null) {
					syntaxSubtree.addChild(next);
				}
			}
		}
		
		if (Construct.SCOPE.equals(construct)) {
			int lastIndex = this.scopeStack.size() - 1;
			this.scopeStack.remove(lastIndex);
			this.currentScope = (lastIndex > 0 ? this.scopeStack.get(lastIndex - 1) : null);
		}
		
		// Finished building this NonTerminal
		this.log("</" + rule + ">");
		
		return syntaxSubtree;
	}
	
	/**
	 * toPrecedenceStream
	 * 
	 * In order to keep operator precedence,
	 * some rules do not have a FIRST set (see __EXPR__)
	 * and are precedence by first Terminal.
	 * However, they are not precedence by NonTerminal.
	 * To parse these, first get the entire tokenStream belonging to this rule
	 * (gettoken() until in FOLLOW and open/close paren/curly/square bracket are balanced)
	 * With this new stream, look for matches (w/ ambiguity)
	 * 
	 * @param precedenceRule, next rule (precedence)
	 * @param maxEndPosition exclusive
	 * @param parentRule to determine when to stop stream for precedence rule
	 * @return
	 * @throws Exception 
	 */
	private Node toPrecedenceStream(NonTerminal precedenceRule, NonTerminal parentRule, int startPosition, int maxEndPosition) throws Exception {
		// Start parsing this precedence non-terminal
		if (doublyVerbose) {
			this.log("// --> To CFG stream from Precedence stream");
			this.log("//");
		}
		
		// Make sure stream positioning is correct
		tokenStream.setLeftIndex(startPosition);
		tokenStream.setRightIndexExcl(maxEndPosition);
		
		// Find final StreamItem for this expression 
		// 	(by parent.FOLLOW.contains)
		//  (and by balanced parentheses, curly, square brackets)
		// Concurrently, link matching open/close parens, curlies, squares
		// Concurrently, indicate all negations
		ArrayDeque<Token> 
			parenStack = new ArrayDeque<>(), 
			curlyStack = new ArrayDeque<>(), 
			squareStack = new ArrayDeque<>();
		int openGroupCount = 0;
		// Current left position
		int leftPosition = startPosition - 1;
		Terminal thisToken = null, lastToken = null;
		boolean inFollow = false,
				isBalanced = true;
		boolean thisIsSign = false, 
				thisIsOperator = false, 
				thisIsOpenParen = false,
				lastIsSign = false, 
				lastIsOperator = false, 
				lastIsOpenParen = false,
				lastIsBOF = true;
		while (++leftPosition < maxEndPosition) {
			// Remember last token
			lastToken = thisToken;
			lastIsSign = thisIsSign;
			lastIsOperator = thisIsOperator;
			lastIsOpenParen = thisIsOpenParen;
			
			// Consume stream
			Token item = tokenStream.readLeft();
			inFollow = parentRule.inFollow(item.token);
			isBalanced = (openGroupCount == 0);
			if (inFollow && isBalanced) {
				break;
			}
			
			thisToken = item.token;
			
			// Find balance
			switch (thisToken) {
			case PAREN_OPEN:
				markOpenGroup(parenStack, item, leftPosition);
				openGroupCount++;
				break;
			case PAREN_CLOSE:
				markCloseGroup(parenStack, item, leftPosition);
				openGroupCount--;
				break;
			case CURLY_OPEN:
				markOpenGroup(curlyStack, item, leftPosition);
				openGroupCount++;
				break;
			case CURLY_CLOSE:
				markCloseGroup(curlyStack, item, leftPosition);
				openGroupCount--;
				break;
			case SQUARE_OPEN:
				markOpenGroup(squareStack, item, leftPosition);
				openGroupCount++;
				break;
			case SQUARE_CLOSE:
				markCloseGroup(squareStack, item, leftPosition);
				openGroupCount--;
				break;
			default:
				break;
			}
			
			// Find negations
			//	1) any +/- operators preceded by an operator
			//	2) any open paren preceded by +/-
			thisIsSign = GrammarRule.isSign(thisToken.tokenValue);
			thisIsOperator = GrammarRule.isOperator(thisToken.tokenValue);
			thisIsOpenParen = (thisToken == Terminal.PAREN_OPEN);
			if (thisIsSign && (lastIsOperator || lastIsOpenParen || lastIsBOF)) {
				// Set operand to negated
				Token nextItem = tokenStream.peekLeft();
				nextItem.negated = (thisToken == Terminal.MINUS);
				// Clear this token from stream
				item.token = Terminal.EMPTY;
			}
			else if (thisIsOpenParen && lastIsSign) {
				item.negated = (lastToken == Terminal.MINUS);
				lastToken = Terminal.EMPTY;
			}
			lastIsBOF = false;
		}

		// Store result and reset stream pointers
		tokenStream.setLeftIndex(startPosition);
		int endPosition = leftPosition;
		tokenStream.setRightIndexExcl(endPosition);
		
		// Reached end of stream (read: program)
		// If not balanced parentheses, syntax error.
		isBalanced = (openGroupCount == 0);
		if (!isBalanced) {
			this.fatalError("Syntax error: Incorrectly-balanced parentheses.");
			return null;
		}
		if (!inFollow) {
			this.fatalError("Syntax error: Expression does not terminate.");
			return null;
		}
		// Otherwise, there is a stream to parse
		
		// And parse using the precedence rule until the determined end position
		Node syntaxTree = this.parsePrecedenceRule(precedenceRule, startPosition, endPosition);
		
		// Reset stream parameters after already having read the stream
		tokenStream.setLeftIndex(endPosition);
		tokenStream.setRightIndexExcl(maxEndPosition);

		// Finished parsing this precedence non-terminal
		if (doublyVerbose) {
			this.log("//");
			this.log("// <-- To Precedence stream from CFG stream");
		}
		
		return syntaxTree;
	}
	
	/**
	 * Push a stream item to the appropriate stack
	 * 
	 * @param stack stack to push from
	 * @param openItem stream item to push
	 * @param position position in TokenStream
	 */
	private void markOpenGroup(ArrayDeque<Token> stack, Token openItem, int position) { 
		stack.push(openItem);
		openItem.openGroupIndex = position;
	}
	
	/**
	 * Pop a stream item from the appropriate open-item stack (not returned)
	 * Sets the open and close positions for 
	 * both the StreamItem from the stack (open item)
	 * and the StreamItem passed as argument (close item)
	 * 
	 * @param stack appropriate stack with open items
	 * @param closeItem matching close item for the last item placed in the stack
	 * @param closePosition position of this close item in the TokenStream
	 * @return no return value, all processing complete
	 */
	private void markCloseGroup(ArrayDeque<Token> stack, Token closeItem, int closePosition) {
		Token openItem = stack.pop();
		closeItem.openGroupIndex = openItem.openGroupIndex;
		closeItem.closeGroupIndex = closePosition;
		openItem.closeGroupIndex = closePosition;
	}
	
	/**
	 * parsePrecedenceRule
	 * 
	 * Read through the stream from startPosition to endPosition
	 * 
	 * Look for a SPLIT token within the bounds of the stream 
	 * 		(See NonTerminal.PrecedencePattern for more details)
	 * If there is no match, 
	 * 		Reset the stream and send it to the next rule
	 * If there is a match,
	 *  	Send left side of match to rule's left rule
	 *  		returns parseTree
	 *  	Send right side of match to rule's right rule
	 *  		returns parseTree
	 *  	Combine results into a single parse tree
	 *  		by this rule's wrapper class
	 *  
	 *  Next rule:
	 *  	If the next rule is a Precedence rule, 
	 *  		parsePrecedenceRule()
	 *  	Otherwise,
	 *  		toCFGStream()
	 * 
	 * @param rule Precedence (NonTerminal) rule
	 * @param startPosition in tokenStream inclusive
	 * @param endPosition in tokenStream exclusive
	 * @return parse subtree given for this span [startPosition, endPosition)
	 * @throws Exception 
	 */
	private Node parsePrecedenceRule(NonTerminal rule, int startPosition, int endPosition) throws Exception {
		// Patterns split at these tokens
		int[] splitTokens = rule.precedencePattern.splitTokens;
		// If a split token is found, this is its Terminal.tokenValue
		int splitTokenValue = -1;
		
		
		if (startPosition == endPosition) {
			return new Node(Terminal.EMPTY);
		}
		
		// Make sure the stream is up-to-date
		// Inclusive start
		tokenStream.setLeftIndex(startPosition);
		// endPosition is exclusive, but so is tokenStream's rightPosition
		tokenStream.setRightIndexExcl(endPosition);

		// Look for a SPLIT token within the bounds of the stream

		// Look for partition starting at start position
		int partition = startPosition; 
		Token item = null;
		int itemCount = 0;
		boolean leftmostIsEmpty = false;
		while (tokenStream.getRightIndexExcl() > startPosition) {
			// Get token at next location
			Token nextItem = tokenStream.readRight();
			
			// Skip empty
			if (nextItem.token == Terminal.EMPTY) {
				leftmostIsEmpty = true;
				continue;
			}
			leftmostIsEmpty = false;
			
			item = nextItem;
			itemCount++;
			
			// If this is an embedded group,
			// Recur then skip group
			if (item.openGroupIndex > -1) {
				if (item.syntaxSubtree == null) {
					// Recur to get parse tree
					// And set as these stream items' parse trees 
					Node embeddedTree = parsePrecedenceRule(rule, item.openGroupIndex + 1, item.closeGroupIndex);
					Token opener = tokenStream.peekAt(item.openGroupIndex);
					opener.syntaxSubtree = item.syntaxSubtree = embeddedTree;
					// Since we are passing a parse tree
					// from one StreamItem to another,
					// Need to apply this negation to any previous negation (XOR)
					embeddedTree.setNegated(opener.negated ^ embeddedTree.isNegated());
					tokenStream.setLeftIndex(startPosition);
				}
				// Skip group
				tokenStream.setRightIndexExcl(item.openGroupIndex);
				continue;
			}
			
			boolean isSplitToken = splitContains(splitTokens, item.token.tokenValue);
			splitTokenValue = (isSplitToken ? item.token.tokenValue : -1);
			if (splitTokenValue > -1) {
				partition = tokenStream.getRightIndexExcl();
				break;
			}
		}
		// Reset stream to initial state
		tokenStream.setRightIndexExcl(endPosition);

		boolean haveMatch = (splitTokenValue > -1);
		
		// If we only have one EMBEDDED item,
		// return its syntax tree (set recursively during above search)
		boolean onlyEmbeddedParseTree = (
				itemCount == 1 &&
				item != null &&
				item.openGroupIndex > -1
		);
		if (onlyEmbeddedParseTree) {
			return item.syntaxSubtree;
		}
		
		// Do not parse empty, placed there by negator in toPrecedenceStream 
		if (leftmostIsEmpty) {
			// Skip empty
			if (partition == startPosition) {
				partition++;
			}
			startPosition++;
			tokenStream.setLeftIndex(startPosition);
		}
		
		
		final Terminal splitToken = Terminal.getTerminal(splitTokenValue);
		if (doublyVerbose) {
			this.log("//");
			this.log("// Try " + rule + ": (" + (haveMatch ? "match: " + splitToken : "no match") + ")");
			this.log(String.format("//     start=%d, partition=%d, end=%d", startPosition, partition, endPosition));
		}

		// For any passing to sub-rules, 
		// exclude the split token
		
		/***** LEFT OPERAND *****/
		// Parse left side of split if left side is non-empty
		// We always exclude the splitToken, and 
		// We always exclude the endPosition, so
		//		Send [startPosition, partition)
		//		(if no match and searching left-to-right, partition==endPosition)
		Node leftOperand = null;
		if (startPosition < partition) {
			// Get subsequent parse rule
			NonTerminal leftRule = NonTerminal.getNonTerminal(rule.precedencePattern.leftRule);
			leftOperand = precedenceParseNextRule(leftRule, startPosition, partition);
			
			// No match means 
			// leftOperand represents the entire span [startPosition, endPosition),
			// so leftOperand==parseSubtree
			if (!haveMatch) {
				// Set subtree negation as necessary
				if (leftOperand != null) {
					leftOperand.setNegated(leftOperand.isNegated() ^ item.negated);
				}
				// Do not reuse negated
				item.negated = false;
				return leftOperand;
			}
		}
		
		/***** RIGHT OPERAND ****/
		// Parse right side of split if right side is non-empty (exclusive of split character)
		// We always exclude the splitToken, so
		// If there is a match exclude the left side,
		//		Send [partition + 1, endPosition)
		// If there is no match, include the left side
		//		send [partition, endPosition)
		Node rightOperand = null;
		int rightOffset = (haveMatch ? 1 : 0);
		if (partition + rightOffset < endPosition) {
			// Get parse rule
			NonTerminal rightRule = NonTerminal.getNonTerminal(rule.precedencePattern.rightRule);
			rightOperand = precedenceParseNextRule(rightRule, partition + rightOffset, endPosition);
			
			// No match means 
			// rightOperand represents the entire span [startPosition, endPosition),
			// so rightOperand==parseSubtree
			if (!haveMatch) {
				// Set subtree negation as necessary
				if (rightOperand != null) {
					rightOperand.setNegated(rightOperand.isNegated() ^ item.negated);
				}
				// Do not reuse negated
				item.negated = false;
				return rightOperand;
			}
		}

		
		// MATCH!
		// Therefore, leftOperand and rightOperand 
		// are both subtrees
		
		// Merge operands using the rule's wrapping class
		NonTerminal wrappingClass = NonTerminal.getNonTerminal(rule.precedencePattern.nonTerminalWrapper);
		Node wrapper = mergeOperands(wrappingClass, leftOperand, rightOperand, splitToken);
		
		// Set subtree negation as necessary
		wrapper.setNegated(item.negated);
		
		return wrapper;
	}
	
	/**
	 * Expedites the next rule as necessary
	 * Returns a parse subtree for this span [startPosition, endPosition)
	 * 
	 * @param nextRule NonTerminal rule to match
	 * @param startPosition inclusive
	 * @param endPosition exclusive
	 * @return Node parse subtree
	 * @throws Exception 
	 */
	private Node precedenceParseNextRule(NonTerminal nextRule, int startPosition, int endPosition) throws Exception {
		Node parseSubtree;
		if (nextRule.isPrecedenceRule()) {
			// Stay in precedence stream
			parseSubtree = parsePrecedenceRule(nextRule, startPosition, endPosition);
		}
		else {
			// No match in Precedence patterns,
			// Send to CFG instead.
			// In valid syntax, this happens for _VALUE_
			
			if (doublyVerbose) {
				this.log("//");
				this.log("// Capture by CFG " + nextRule);
				this.log("//");
			}
			
			// Extend the stream span to include the FOLLOW character (CFG requires this)
			while (!tokenStream.isEmpty()) {
				// Extend by one
				tokenStream.setRightIndexExcl(++endPosition);
				// If this newly-included character is empty, extend again
				Token nextItem = tokenStream.peekRight();
				if (nextItem != null && nextItem.token != Terminal.EMPTY) break;
			}
			
			parseSubtree = toCFGStream(nextRule, startPosition, endPosition);
		}
		return parseSubtree;
	}

	/**
	 * Check if this token is in the split tokens array from the rule's pattern
	 * If it is, return the token's index in the array
	 * 
	 * @param splitTokens
	 * @param tokenValue
	 * @return
	 */
	private boolean splitContains(int[] splitTokens, int tokenValue) {
		for (int terminal : splitTokens) {
			// Skip any terminal not in  this set
			if (tokenValue != terminal) continue;
			
			// Match!
			return true;
		}
		return false;
	}
	
	/**
	 * Merge the operands (subtrees) into its respective wrapping class.
	 * Return the resulting subtree
	 * 
	 * @param leftOperand Node parse subtree
	 * @param rightOperand Node parse subtree
	 * @param wrappingClass
	 * @param splitToken token to label wrapping class with
	 * @return Node the resulting subtree
	 * @throws IOException 
	 */
	private Node mergeOperands(NonTerminal wrappingClass, Node leftOperand, Node rightOperand, Terminal splitToken) throws IOException {
		// Do not combine fully empty
		boolean leftIsNull = (leftOperand == null);
		boolean rightIsNull = (rightOperand == null);
		if (leftIsNull && rightIsNull) {
			return new Node(splitToken);
		}
		
		Node wrapper = new Node(splitToken.construct, wrappingClass);
		wrapper.addChild(leftOperand);
		wrapper.addChild(rightOperand);
		
		return wrapper;
	}
	
	/**
	 * Create a leaf for the parse tree,
	 * add that leaf to the parent node,
	 * and return the leaf.
	 * 
	 * @param parentNode
	 * @param terminal respective to this leaf
	 * @param symbol from SymbolTable or null
	 * @param value LITERAL value or matching terminal string
	 * @return
	 * @throws IOException 
	 */
	private Node addTerminal(Node parentNode, Terminal terminal, Symbol symbol, String value) throws Exception {
		Node node;
		
		// Make sure the current symbol is scoped correctly
		// Static scoping
		if (symbol != null && Construct.VARIABLE.equals(terminal.construct)) {
			Variable variable = null;
			for (Scope scope : this.scopeStack) {
				variable = scope.getVariable(symbol);
				if (variable != null) {
					break;
				}
			}
			if (variable == null) {
				variable = this.currentScope.addVariable(symbol);
			}
			
			node = new Node(terminal, variable);
		}
		else {
			// Symbols need to be stored for literals
			node = new Node(terminal, symbol, value);
		}
		
		parentNode.addChild(node);
		
		log(node);
		
		return node;
	}
	
	private void log(Node node) throws IOException {
		if (!this.verbose || this.logFileWriter == null) return;
		
		boolean isTerminal = node.getToken() != null;
		
		StringBuilder output = new StringBuilder();
		output.append("<");
		output.append(node.toString(false));
		if (isTerminal) {
			// Terminal
			output.append(" /");
			depth++;
		}
		output.append(">");
		
		this.log(output.toString());
		
		if (isTerminal) depth--;
	}
	
	private void log(String message) throws IOException {
		if (!this.verbose || this.logFileWriter == null) return;
		
		StringBuilder output = new StringBuilder();
		for (int i = 0; i < depth; i++) output.append("  ");
		output.append(message);
		output.append("\n");
		
		if (verbose) {
			System.out.print(output);
		}
		if (logFileWriter != null) {
			logFileWriter.append(output);
		}
	}
	
	private void fatalError(String err) throws SyntaxError, IOException {
		String stream = "End of stream";
		if (!this.tokenStream.isEmpty()) {
			int leftPos = this.tokenStream.getLeftIndex();
			int rightPos = this.tokenStream.getRightIndexExcl();
			boolean isLongStream = (rightPos - leftPos > 5); 
			if (isLongStream) {
				 stream = this.tokenStream.toString(leftPos, leftPos + 5) + "...";
			}
			else {
				stream = this.tokenStream.toString();
			}
		}
		String error = "\n" + err + "\nAt...\n" + stream + "\n";
		this.log(error);
		throw new SyntaxError(error);
	}
}