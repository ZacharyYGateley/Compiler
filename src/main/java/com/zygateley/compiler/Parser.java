package com.zygateley.compiler;

import java.util.*;


class Node implements Iterable<Node> {
	// Name is used for debugging so that the 
	// token is clearly understood at first sight
	private String _name_;
	private NonTerminal rule;
	private Terminal token;
	private Symbol symbol;
	private String value;
	private boolean negated = false;
	private ArrayList<Node> param;
	// Keep one copy of an empty parameter list
	// Point to it on empty parameter list to limit memory usage
	// Additionally, this ensures param is never null
	private static final ArrayList<Node> emptyParam = new ArrayList<Node>();
	
	public Node(NonTerminal rule) {
		this._name_ = rule+"";
		this.rule = rule;
		this.token = null;
		this.symbol = null;
		this.value = null;
		this.param = new ArrayList<Node>();
	}
	public Node(NonTerminal rule, boolean hasParameters) {
		this._name_ = rule+"";
		this.rule = rule;
		this.token = null;
		this.symbol = null;
		this.value = null;
		this.param = (hasParameters ? new ArrayList<Node>() : Node.emptyParam);
	}
	public Node(Terminal token, Symbol symbol, String value) {
		String type = "";
		if (symbol != null) {
			type = (symbol.getType() != null ? symbol.getType()+"" : symbol.getName());
		}
		this._name_ = (symbol != null ? "(" + type + ") " : "") + token;
		this.rule = null;
		this.token = token;
		this.symbol = symbol;
		this.value = value;
		// No parameters for terminals
		this.param = null;
	}
	public Node(final Node pn, boolean hasParameters) {
		this._name_ = pn._name_;
		this.rule = pn.rule;
		this.token = pn.token;
		this.symbol = pn.symbol;
		this.value = pn.value;
		this.param = (hasParameters ? pn.param : Node.emptyParam);
	}
	
	public NonTerminal getRule() {
		return this.rule;
	}
	
	public Terminal getToken() {
		return this.token;
	}
	
	public Symbol getSymbol() {
		return this.symbol;
	}
	
	public String getValue() {
		return this.value;
	}
	
	public ArrayList<Node> getParam() {
		return this.param;
	}
	
	public void addParam(final Node param) {
		// Do not add parameters to emptyParam
		if (this.param != Node.emptyParam) {
			this.param.add(param);
		}
	}
	
	public void setToken(final Terminal token) {
		if (this.token == null) {
			this.token = token;
		}
	}
	
	public boolean isNegated() {
		return this.negated;
	}
	public void setNegated(boolean negated) {
		this.negated = negated;
	}
	
	@Override
	public Iterator<Node> iterator() {
		return this.param.iterator();
	}
}


public class Parser {
	private TokenStream tokenStream;
	private boolean verbose;
	private int depth;
	
	public Parser(TokenStream ts) {
		this.tokenStream = ts;
	}
	
	public Node parse(boolean verbose) throws Exception {
		this.verbose = verbose;
		return parse();
	}
	public Node parse() throws Exception {
		// Reset parsing parameters
		depth = 0;
		
		if (verbose) {
			System.out.println("<!-- Parsing initiated -->\n");
		}
		
		// Top-level of syntaxTree should only contain first rule
		NonTerminal firstRule = NonTerminal.getNonTerminal(Token.firstNonTerminal);
		Node syntaxTree = parseRule(firstRule, tokenStream.length());
				
		if (verbose) {
			System.out.println("\n<!-- Parsing finished -->\n");
		}
		
		return syntaxTree;
	}
	
	/**
	 * Parse left-to-right using a standard CFG rule
	 * 
	 * @param rule
	 * @param endPosition exclusive
	 * @return
	 * @throws Exception
	 */
	private Node parseRule(NonTerminal rule, int endPosition) throws Exception {
		// New non-terminal node
		Node pn = new Node(rule);
		
		// Skip all EMPTY tokens
		// These are only used to hasEpsilon and inFollow
		Terminal t = null;
		while (t == null || t == Terminal.EMPTY) t = tokenStream.peekLeft().token;

    	int indexInFirst = rule.indexOfMatchFirst(t);

		// Next terminal is NOT in rule
    	if (indexInFirst < 0) {
    		boolean hasEpsilon = (rule.indexOfMatchFirst(Terminal.EMPTY) > -1);
        	boolean inFollow = rule.inFollow(t);
        	if (hasEpsilon && inFollow) {
        		// Empty string has been utilized for this rule
        		
        		// Node has no official children
        		if (verbose) {
        			for (int i = 0; i < depth; i++) System.out.print("  ");
        			System.out.println("<" + rule.toString().replaceAll("_",  "") + " />");
        		}
        		
        		return null;
        	}
        	else {
        		this.fatalError("Syntax error 1: Production rule terminated prematurely.");
        		return null;
        	}
    	}
    	
    	// Starting building this NonTerminal
		if (verbose) {
			for (int i = 0; i < depth; i++) System.out.print("  ");
			System.out.println("<" + rule.toString().replaceAll("_",  "") + ">");
		}
    	
		int[] pattern = rule.patterns[indexInFirst].PATTERN;
		
		// Get rules in order
		// If any of the rules is a NonTerminal, recur
		for (int i = 0; i < pattern.length; i++) {
			
			// Skip all EMPTY terminals in stream
			while (true) {
				StreamItem nextItem = tokenStream.peekLeft();
				if (nextItem == null || nextItem.token != Terminal.EMPTY) break;
				tokenStream.getLeft();
			}
			
			// If reached the end of this expression
			// but have not finished the NonTerminal, err out
			if (tokenStream.isEmpty()) {
				this.fatalError("Fatal error 2: Production rule terminated prematurely.");
				return null;
			}
			
			int tokenValue = pattern[i];
			
			Node next;
			// Terminal
			// These are immediately added to the syntax tree
			if (Token.isTerminal(tokenValue)) {
				StreamItem item = tokenStream.popLeft();
				if (item.token.tokenValue != tokenValue) {
					this.fatalError("Fatal error: incorrect syntax.");
					return null;
				}
				
				// Add new terminal
				addTerminal(pn, item.token, item.symbol, item.value);
			}
			// NonTerminals
			// Recur into parseRule
			else {
				NonTerminal nextToken = NonTerminal.getNonTerminal(tokenValue);
				if (!nextToken.isPrecedenceRule()) {
					depth++;
					next = parseRule(nextToken, endPosition);
					depth--;
				}
				// Precedence rules need to move into precedence branch
				else {
					// Precedence rules do not represent an increase in depth
					next = toPrecedenceStream(nextToken, endPosition, rule);
				}

				// Add resulting NonTerminal to tree
				if (next != null) {
					pn.addParam(next);
				}
			}
		}
		
		// Finished building this NonTerminal
		if (verbose) {
			for (int i = 0; i < depth; i++) System.out.print("  ");
			System.out.println("</" + rule.toString().replaceAll("_",  "") + ">");
		}
		
		// Strip empty ArrayList where applicable
		if (pn.getParam().size() == 0) {
			pn = new Node(pn, false);
		}
		
		return pn;
	}
	
	/**
	 * From precedence stream to non-precedence stream
	 * 
	 * @param ruleCFG
	 * @param endPosition exclusive
	 * @return
	 */
	private Node toCFGStream(NonTerminal ruleCFG, int startPosition, int endPosition) throws Exception {
		if (verbose) {
			System.out.println("\n...Precedence stream --> Non-precedence stream...\n");
		}
		depth++;
		tokenStream.setLeft(startPosition);
		tokenStream.setRightExclusive(endPosition);
		Node syntaxTree = parseRule(ruleCFG, endPosition);
		depth--;
		if (verbose) {
			System.out.println("\n...Non-precedence stream --> Precedence stream...\n");
		}
		return syntaxTree;
	}
	
	/**
	 * parsePrecedenceRule
	 * @param rule Precedence NonTerminal rule
	 * @param startPosition in tokenStream inclusive
	 * @param endPosition in tokenStream exclusive
	 * @return
	 */
	private Node parsePrecedenceRule(NonTerminal rule, int startPosition, int endPosition) throws Exception {
		// Patterns split at these tokens
		int[] splitAt = rule.precedencePattern.splitAt;
		int partition = -1;
		int splitTokenValue = -1;
		
		// Make sure the stream is up-to-date
		// Inclusive start
		tokenStream.setLeft(startPosition);
		// endPosition is exclusive, but so is tokenStream's rightPosition
		tokenStream.setRightExclusive(endPosition);

		// Look for a split token within the bounds of the stream 
		// If there is no match, reset the stream and send it to the next rule
		// 		Return to nonPrecedence stream will throw parse errors 
		// 		if there are no matches anywhere along the sequence of rules 
		// If there is a match,
		//		Send left side of match to rule's left rule
		//			returns parseTree
		// 		Send right side of match to rule's right rule
		//			returns parseTree
		//		Combine results into a single parse tree 
		//			by this rule's wrapper class
		Token.Direction direction = rule.precedencePattern.direction;
		boolean leftToRight = (direction == Token.Direction.LEFT_TO_RIGHT);
		StreamItem item = null;
		int itemCount = 0;
		boolean leftmostIsEmpty = false;

		// Right to left
		partition = startPosition;		// If no match
		while (tokenStream.getRightExclusive() > startPosition) {
			// Get token at next location
			StreamItem nextItem = tokenStream.popRight();
			
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
			if (item.openGroup > -1) {
				if (item.syntaxTree == null) {
					// Recur to get parse tree
					// And set as these stream items' parse trees 
					Node embeddedTree = parsePrecedenceRule(rule, item.openGroup + 1, item.closeGroup);
					StreamItem opener = tokenStream.get(item.openGroup);
					opener.syntaxTree = item.syntaxTree = embeddedTree;
					// Since we are passing a parse tree
					// from one StreamItem to another,
					// Need to apply this negation to any previous negation (XOR)
					embeddedTree.setNegated(opener.negated ^ embeddedTree.isNegated());
					tokenStream.setLeft(startPosition);
				}
				// Skip group
				tokenStream.setRightExclusive(item.openGroup);
				continue;
			}
			
			splitTokenValue = findSplit(splitAt, item.token.tokenValue);
			if (splitTokenValue > -1) {
				partition = tokenStream.getRightExclusive();
				break;
			}
		}
		// Reset stream to initial state
		tokenStream.setRightExclusive(endPosition);

		boolean haveMatch = (splitTokenValue > -1);
		
		// If we only have one EMBEDDED item,
		// return its syntax tree (set recursively during above search)
		boolean onlyEmbeddedParseTree = (
				itemCount == 1 &&
				item != null &&
				item.openGroup > -1
		);
		if (onlyEmbeddedParseTree) {
			return item.syntaxTree;
		}
		
		// Do not parse empty, placed there by negator in toPrecedenceStream 
		if (leftmostIsEmpty) {
			// Skip empty
			if (partition == startPosition) {
				partition++;
			}
			startPosition++;
			tokenStream.setLeft(startPosition);
		}
		
		
		final Terminal splitToken = Terminal.getTerminal(splitTokenValue);
		if (verbose) { 
			System.out.println(rule + ": (" + (haveMatch ? "match: " + splitToken : "no match") + ")");
			System.out.println("\tstart, partition, end (" + direction + ")");
			System.out.println(String.format("\t%d, %d, %d\n", startPosition, partition, endPosition));
		}

		// For any passing to subrules, 
		// exclude the split token
		
		/***** LEFT OPERAND *****/
		// Parse left side of split
		// Only if there is a string to pass to it
		// We always skip the splitToken, so 
		//		if searching left, to right and there is a match, check partition (- 1)
		//		otherwise, check against partition (- 0)
		int leftOffset = (leftToRight && haveMatch ? -1 : 0);
		Node leftOperand = null;
		if (startPosition < partition + leftOffset) {
			// And get parse rule
			NonTerminal leftRule = NonTerminal.getNonTerminal(rule.precedencePattern.leftRule);
			
			// Get new childNode from left-hand string
			if (leftRule.isPrecedenceRule()) {
				// Stay in precedence stream
				// startPosition inclusive
				// endPosition exclusive
				leftOperand = parsePrecedenceRule(leftRule, startPosition, partition + leftOffset);
				// This operand is in a wrapper Node
				// See Token.PrecedencePattern for more details
			}
			else {
				// __VALUE__
				// Move to non-precedence stream
				// Non-precedence stream is always left to right
				leftOperand = toCFGStream(leftRule, startPosition, partition + leftOffset);
				// This operand is a __VALUE__
			}
			
			
			// No match means 
			// we are passing an operand back upwards
			//		i.e. Had to work down operator precedence
			//			 to find a match value. 
			//			 Its result has been passed
			//			 back up to here. Keep passing it.
			if (!haveMatch) {
				// Set negated as necessary
				leftOperand.setNegated(item.negated);
				return leftOperand;
			}
			// Otherwise, we have a match.
			// This *node* is a wrapper for Token.PrecedencePattern NonTerminals
		}
		
		/***** RIGHT OPERAND ****/
		// Parse right side (exclusive of split)
		// Only if there is a stream to pass it
		// We always skip the splitToken, so... 
		//		if match, check partition (+ 1)
		// 		otherwise, check partition alone
		Node rightOperand = null;
		int rightOffset = (!leftToRight && haveMatch ? 1 : 0);
		if (partition + rightOffset < endPosition) {
			// Get parse rule
			NonTerminal rightRule = NonTerminal.getNonTerminal(rule.precedencePattern.rightRule);
			
			// Get new childNode from left-hand string
			if (rightRule.isPrecedenceRule()) {
				// Stay in precedence stream
				// startPosition inclusive
				// endPosition exclusive
				rightOperand = parsePrecedenceRule(rightRule, partition + rightOffset, endPosition);
				// This operand is in a wrapper Node
				// See Token.PrecedencePattern for more details
			}
			else {
				// __VALUE__
				// Move to non-precedence stream
				// Non-precedence stream is always left to right
				rightOperand = toCFGStream(rightRule, partition + rightOffset, endPosition);
				// This operand is a __VALUE__
			}
			
			
			// No match means 
			// we are passing an operand back upwards
			//		i.e. Had to work down operator precedence
			//			 to find a match value. 
			//			 Its result has been passed
			//			 back up to here. Keep passing it.
			if (!haveMatch) {
				// Set negated as necessary
				rightOperand.setNegated(item.negated);
				return rightOperand;
			}
			// Otherwise, we have a match.
			// This *node* is a wrapper for Token.PrecedencePattern NonTerminals
		}

		// MATCH!
		// We have found something by this rule
		
		Node wrapper = null;
		
		// Merge operands appropriately
		NonTerminal wrappingClass = NonTerminal.getNonTerminal(rule.precedencePattern.nonTerminalWrapper);
		wrapper = mergeOperands(leftOperand, rightOperand, wrappingClass, splitToken);
		
		wrapper.setNegated(item.negated);
		return wrapper;
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
	 * @param endPosition exclusive
	 * @param parentRule to determine when to stop stream for precedence rule
	 * @return
	 */
	private Node toPrecedenceStream(NonTerminal precedenceRule, int endPosition, NonTerminal parentRule) throws Exception {
		// Start parsing this precedence non-terminal
		if (verbose) {
			System.out.println("\n...Non-precedence stream --> Precedence stream...\n");
		}
		
		// Find final StreamItem for this expression 
		// 	(by parent.FOLLOW.contains)
		//  (and by balanced parentheses, curly, square brackets)
		// Concurrently, link matching open/close parens, curlies, squares
		// Concurrently, indicate all negations
		int startPos = this.tokenStream.getLeft();
		int endPos = startPos;
		ArrayDeque<StreamItem> parenStack = new ArrayDeque<>();
		ArrayDeque<StreamItem> curlyStack = new ArrayDeque<>();
		ArrayDeque<StreamItem> squareStack = new ArrayDeque<>();
		int openGroups = 0;
		boolean inFollow = false;
		boolean isBalanced = true;
		int leftPosition = startPos - 1;
		Terminal thisToken = null, lastToken = null;
		boolean thisIsSign = false, thisIsOperator = false, thisIsOpenParen = false;
		boolean lastIsSign = false, lastIsOperator = false, lastIsOpenParen = false;
		boolean lastIsBOF = true;
		while (++leftPosition < endPosition) {
			// Remember last token
			lastToken = thisToken;
			lastIsSign = thisIsSign;
			lastIsOperator = thisIsOperator;
			lastIsOpenParen = thisIsOpenParen;
			
			// Consume stream
			StreamItem item = tokenStream.popLeft();
			inFollow = parentRule.inFollow(item.token);
			isBalanced = (openGroups == 0);
			if (inFollow && isBalanced) {
				break;
			}
			
			thisToken = item.token;
			
			// Find balance
			switch (thisToken) {
			case PAREN_OPEN:
				markOpenGroup(parenStack, item, leftPosition);
				openGroups++;
				break;
			case PAREN_CLOSE:
				markCloseGroup(parenStack, item, leftPosition);
				openGroups--;
				break;
			case CURLY_OPEN:
				markOpenGroup(curlyStack, item, leftPosition);
				openGroups++;
				break;
			case CURLY_CLOSE:
				markCloseGroup(curlyStack, item, leftPosition);
				openGroups--;
				break;
			case SQUARE_OPEN:
				markOpenGroup(squareStack, item, leftPosition);
				openGroups++;
				break;
			case SQUARE_CLOSE:
				markCloseGroup(squareStack, item, leftPosition);
				openGroups--;
				break;
			default:
				break;
			}
			
			// Find negations
			//	1) any +/- operators preceded by an operator
			//	2) any open paren preceded by +/-
			thisIsSign = Token.isSign(thisToken.tokenValue);
			thisIsOperator = Token.isOperator(thisToken.tokenValue);
			thisIsOpenParen = (thisToken == Terminal.PAREN_OPEN);
			if (thisIsSign && (lastIsOperator || lastIsOpenParen || lastIsBOF)) {
				// Set operand to negated
				StreamItem nextItem = tokenStream.peekLeft();
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
		endPos = leftPosition;
		tokenStream.setLeft(startPos);
		tokenStream.setRightExclusive(endPos);
		
		// Reached end of stream (read: program)
		// If not balanced parentheses, syntax error.
		isBalanced = (openGroups == 0);
		if (!isBalanced) {
			this.fatalError("Fatal error: Incorrectly-balanced parentheses.");
			return null;
		}
		if (!inFollow) {
			this.fatalError("Syntax error: Expression does not terminate.");
			return null;
		}
		// Otherwise, there is a stream to parse
		
		// And parse using the precedence rule until the determined end position
		Node syntaxTree = this.parsePrecedenceRule(precedenceRule, startPos, endPos);
		
		// Reset stream parameters after already-parsed stream
		// Set to before FOLLOW token so that parent NonTerminal knows that it is done.
		tokenStream.setLeft(endPos);
		tokenStream.setRightExclusive(endPosition);

		// Finished parsing this precedence non-terminal
		if (verbose) {
			System.out.println("\n...Precedence stream --> Non-precedence stream...\n");
		}
		
		return syntaxTree;
	}
	
	private void markOpenGroup(ArrayDeque<StreamItem> stack, StreamItem openItem, int position) { 
		stack.push(openItem);
		openItem.openGroup = position;
	}
	
	private void markCloseGroup(ArrayDeque<StreamItem> stack, StreamItem closeItem, int position) {
		StreamItem openItem = stack.pop();
		closeItem.openGroup = openItem.openGroup;
		closeItem.closeGroup = position;
		openItem.closeGroup = position;
	}

	private int findSplit(int[] splitAt, int tokenValue) {
		for (int terminal : splitAt) {
			// Skip any terminal not in  this set
			if (tokenValue != terminal) continue;
			
			// Match!
			return terminal;
		}
		return -1;
	}
	
	private Node mergeOperands(Node leftOperand, Node rightOperand, NonTerminal wrappingClass, Terminal splitToken) {
		// Do not combine fully empty
		boolean leftIsNull = (leftOperand == null);
		boolean rightIsNull = (rightOperand == null);
		if (leftIsNull && rightIsNull) {
			return null;
		}
		
		// If both children have appropriate child nodes
		// Create a new wrapper Node (NonTerminal.PrecedencePattern.nonTerminalWrapper)
		Node wrapper = new Node(wrappingClass);
		wrapper.setToken(splitToken);
		wrapper.addParam(leftOperand);
		wrapper.addParam(rightOperand);
		
		return wrapper;
	}
	
	private Node addTerminal(Node parent, Terminal terminal, Symbol symbol, String value) {
		if (verbose) {
			String tokenName = terminal + "";
			String symbolString = "";
			if (symbol != null) {
				String name = symbol.getName();
				if (name != null) symbolString += " name=\"" + symbol.getName() + "\"";
				String valueString = symbol.getValue();
				if (valueString != null) symbolString += " value=" + valueString + "\"";
				Symbol.Type type = symbol.getType();
				if (type != null) symbolString = " type=\"" + type + "\" ";
			}
			else if (value != null && !value.isBlank()) {
				symbolString += " value=" + value + "\"";
			}
			for (int j = 0; j < depth; j++) System.out.print("  ");
			System.out.println("  <Terminal " + tokenName + symbolString + ">");
		}
		Node node = new Node(terminal, symbol, value);
		parent.addParam(node);
		return node;
	}
	
	private void fatalError(String err) throws Exception {
		throw new Exception("\n" + err + "\nAt...\n\n" + this.tokenStream);
	}
}