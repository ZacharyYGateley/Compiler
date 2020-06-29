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
	
	@Override
	public Iterator<Node> iterator() {
		return this.param.iterator();
	}
}


public class Parser {
	private TokenStream tokenStream;
	private boolean verbose;
	private int depth;
	private boolean ambiguousStreamOpen;
	
	public Parser(TokenStream ts) {
		this.tokenStream = ts;
		this.ambiguousStreamOpen = false;
	}
	
	public Node parse(boolean verbose) throws Exception {
		this.verbose = verbose;
		return parse();
	}
	public Node parse() throws Exception {
		// Reset parsing parameters
		depth = 0;
		
		if (verbose) {
			System.out.println("<!-- New parser started -->\n");
		}
		
		// Top-level of syntaxTree should only contain first rule
		NonTerminal firstRule = NonTerminal.getNonTerminal(Token.firstNonTerminal);
		Node syntaxTree = parseRule(firstRule, tokenStream.length());
				
		if (verbose) {
			System.out.println("\n<!-- New parser finished -->\n");
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
		
		Terminal t = tokenStream.peekLeft().token;

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
			// If reached the end of this expression
			// but have not finished the NonTerminal, err out
			if (tokenStream.getLeft() > endPosition) {
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
				if (!nextToken.isAmbiguous()) {
					depth++;
					next = parseRule(nextToken, endPosition);
					depth--;
				}
				// Ambiguous rules need to move into ambiguous branch
				else {
					// Ambiguous rules do not represent an increase in depth
					next = toAmbiguousStream(nextToken, endPosition, rule);
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
	 * From ambiguous stream to non-ambiguous stream
	 * 
	 * @param nonAmbiguousRule
	 * @param endPosition exclusive
	 * @return
	 */
	private Node toNonAmbiguousStream(NonTerminal nonAmbiguousRule, int startPosition, int endPosition) throws Exception {
		if (verbose) {
			System.out.println("\n...Ambiguous stream --> Non-ambiguous stream...\n");
		}
		depth++;
		tokenStream.setLeft(startPosition);
		tokenStream.setRightExclusive(endPosition);
		Node syntaxTree = parseRule(nonAmbiguousRule, endPosition);
		depth--;
		if (verbose) {
			System.out.println("\n...Non-ambiguous stream --> Ambiguous stream...\n");
		}
		return syntaxTree;
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
	
	/**
	 * parseAmbiguousRule
	 * @param rule Ambiguous NonTerminal rule
	 * @param startPosition in tokenStream inclusive
	 * @param endPosition in tokenStream exclusive
	 * @return
	 */
	private Node parseAmbiguousRule(NonTerminal rule, int startPosition, int endPosition) throws Exception {
		// Patterns split at these tokens
		int[] splitAt = rule.ambiguousPattern.splitAt;
		int partition = -1;
		int splitTokenValue = -1;
		
		// Make sure the stream is up-to-date
		// Inclusive start
		tokenStream.setLeft(startPosition);
		// endPosition is exclusive, but so is tokenStream's rightPosition
		tokenStream.setRightExclusive(endPosition);

		// Look for a split token within the bounds of the stream 
		// If there is no match, reset the stream and send it to the next rule
		// 		Return to nonAmbiguous stream will throw parse errors 
		// 		if there are no matches anywhere along the sequence of rules 
		// If there is a match,
		//		Send left side of match to rule's left rule
		//			returns parseTree
		// 		Send right side of match to rule's right rule
		//			returns parseTree
		//		Combine results into a single parse tree 
		//			by this rule's wrapper class
		Token.Direction direction = rule.ambiguousPattern.direction;
		boolean leftToRight = (direction == Token.Direction.LEFT_TO_RIGHT);
		StreamItem item = null;
		int itemCount = 0;
/*
		if (leftToRight) {
			// Left to right
			partition = endPosition;
			while (tokenStream.getLeft() < endPosition) {
				StreamItem item = tokenStream.popLeft();
				splitTokenValue = findSplit(splitAt, item.token.tokenValue);
				if (splitTokenValue > -1) {
					partition = tokenStream.getLeft() - 1;
					break;
				}
			}
			// Reset stream to initial state
			tokenStream.setLeft(startPosition);
		}
		else {
*/
			// Right to left
			partition = startPosition;		// If no match
			while (tokenStream.getRightExclusive() > startPosition) {
				// Get token at next location
				item = tokenStream.popRight();
				itemCount++;
				
				// If this is an embedded group,
				// Recur then skip group
				if (item.openGroup > -1) {
					if (item.syntaxTree == null) {
						// Recur to get parse tree
						// And set as these stream items' parse trees 
						Node embeddedTree = parseAmbiguousRule(rule, item.openGroup + 1, item.closeGroup);
						StreamItem opener = tokenStream.get(item.openGroup);
						opener.syntaxTree = item.syntaxTree = embeddedTree;
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
				else if (item.openGroup > -1) {
					
				}
			}
			// Reset stream to initial state
			tokenStream.setRightExclusive(endPosition);
/*
		}
*/

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
		
		
		final Terminal splitToken = Terminal.getTerminal(splitTokenValue);
		System.out.println(rule + ": ");
		System.out.println("\t" + (haveMatch ? "HAVE MATCH: " + splitToken : "NO MATCH"));
		//System.out.println("\tstart, partition, end (" + direction + ")");
		System.out.println(String.format("\t%d, %d, %d\n", startPosition, partition, endPosition));

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
			NonTerminal leftRule = NonTerminal.getNonTerminal(rule.ambiguousPattern.leftRule);
			System.out.println("--> left (" + leftRule + ")");
			
			// Get new childNode from left-hand string
			if (leftRule.isAmbiguous()) {
				// Stay in ambiguous stream
				// startPosition inclusive
				// endPosition exclusive
				leftOperand = parseAmbiguousRule(leftRule, startPosition, partition + leftOffset);
				// This operand is in a wrapper Node
				// See Token.AmbiguousPattern for more details
			}
			else {
				// __VALUE__
				// Move to non-ambiguous stream
				// Non-ambiguous stream is always left to right
				leftOperand = toNonAmbiguousStream(leftRule, startPosition, partition + leftOffset);
				// This operand is a __VALUE__
			}
			
			// No match means 
			// we are passing an operand back upwards
			//		i.e. Had to work down operator precedence
			//			 to find a match value. 
			//			 Its result has been passed
			//			 back up to here. Keep passing it.
			if (!haveMatch) {
				return leftOperand;
			}
			// Otherwise, we have a match.
			// This *node* is a wrapper for Token.AmbiguousPattern NonTerminals
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
			NonTerminal rightRule = NonTerminal.getNonTerminal(rule.ambiguousPattern.rightRule);
			System.out.println("--> right (" + rightRule + ")");
			
			// Get new childNode from left-hand string
			if (rightRule.isAmbiguous()) {
				// Stay in ambiguous stream
				// startPosition inclusive
				// endPosition exclusive
				rightOperand = parseAmbiguousRule(rightRule, partition + rightOffset, endPosition);
				// This operand is in a wrapper Node
				// See Token.AmbiguousPattern for more details
			}
			else {
				// __VALUE__
				// Move to non-ambiguous stream
				// Non-ambiguous stream is always left to right
				rightOperand = toNonAmbiguousStream(rightRule, partition + rightOffset, endPosition);
				// This operand is a __VALUE__
			}
			
			// No match means 
			// we are passing an operand back upwards
			//		i.e. Had to work down operator precedence
			//			 to find a match value. 
			//			 Its result has been passed
			//			 back up to here. Keep passing it.
			if (!haveMatch) {
				return rightOperand;
			}
			// Otherwise, we have a match.
			// This *node* is a wrapper for Token.AmbiguousPattern NonTerminals
		}
		
		Node wrapper = null;
		
		// MATCH!
		// We have found something by this rule
		// Merge operands appropriately
		NonTerminal wrappingClass = NonTerminal.getNonTerminal(rule.ambiguousPattern.nonTerminalWrapper);
		wrapper = mergeOperands(leftOperand, rightOperand, wrappingClass, splitToken);

		
		System.out.println(wrapper);
		
		return wrapper;
	}
	
	/**
	 * toAmbiguousStream
	 * 
	 * In order to keep operator precedence,
	 * some rules do not have a FIRST set (see __EXPR__)
	 * and are ambiguous by first Terminal.
	 * However, they are not ambiguous by NonTerminal.
	 * To parse these, first get the entire tokenStream belonging to this rule
	 * (gettoken() until in FOLLOW and open/close paren/curly/square bracket are balanced)
	 * With this new stream, look for matches (w/ ambiguity)
	 * 
	 * @param ambiguousRule, next rule (ambiguous)
	 * @param endPosition exclusive
	 * @param parentRule to determine when to stop stream for ambiguous rule
	 * @return
	 */
	private Node toAmbiguousStream(NonTerminal ambiguousRule, int endPosition, NonTerminal parentRule) throws Exception {
		// Because of the nature of the ambiguous stream
		// Only one ambiguous stream may be open at once
		if (this.ambiguousStreamOpen) {
			Node syntaxTree = new Node(Terminal.EMPTY, null, null);
			return syntaxTree;
		}
		this.ambiguousStreamOpen = true;
		
		// Start parsing this ambiguous non-terminal
		if (verbose) {
			System.out.println("\n...Non-ambiguous stream --> Ambiguous stream...\n");
		}
		
		// Find final StreamItem for this expression 
		// 	(by parent.FOLLOW.contains)
		//  (and by balanced parentheses, curly, square brackets)
		// Concurrently, link matching open/close parens, curlies, squares
		int startPos = this.tokenStream.getLeft();
		int endPos = startPos;
		ArrayDeque<StreamItem> parenStack = new ArrayDeque<>();
		ArrayDeque<StreamItem> curlyStack = new ArrayDeque<>();
		ArrayDeque<StreamItem> squareStack = new ArrayDeque<>();
		int openGroups = 0;
		boolean inFollow = false;
		boolean isBalanced = true;
		int leftPosition = startPos - 1;
		while (++leftPosition < endPosition) {
			// Consume stream
			StreamItem nextItem = tokenStream.popLeft();
			inFollow = parentRule.inFollow(nextItem.token);
			isBalanced = (openGroups == 0);
			if (inFollow && isBalanced) {
				break;
			}
			
			// Find balance
			switch (nextItem.token) {
			case PAREN_OPEN:
				markOpenGroup(parenStack, nextItem, leftPosition);
				openGroups++;
				break;
			case PAREN_CLOSE:
				markCloseGroup(parenStack, nextItem, leftPosition);
				openGroups--;
				break;
			case CURLY_OPEN:
				markOpenGroup(curlyStack, nextItem, leftPosition);
				openGroups++;
				break;
			case CURLY_CLOSE:
				markCloseGroup(curlyStack, nextItem, leftPosition);
				openGroups--;
				break;
			case SQUARE_OPEN:
				markOpenGroup(squareStack, nextItem, leftPosition);
				openGroups++;
				break;
			case SQUARE_CLOSE:
				markCloseGroup(squareStack, nextItem, leftPosition);
				openGroups--;
				break;
			default:
				break;
			}
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
		
		// And parse using the ambiguous rule until the determined end position
		Node syntaxTree = this.parseAmbiguousRule(ambiguousRule, startPos, endPos);
		syntaxTree = cleanAmbiguousTree(syntaxTree);
		
		// Reset stream parameters after already-parsed stream
		// Set to before FOLLOW token so that parent NonTerminal knows that it is done.
		tokenStream.setLeft(endPos);
		tokenStream.setRightExclusive(endPosition);

		// Finished parsing this ambiguous non-terminal
		if (verbose) {
			System.out.println("\n...Ambigous stream --> Non-ambiguous stream...\n");
		}
		
		// Close ambiguous stream
		this.ambiguousStreamOpen = false;
		
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
	
	private Node cleanAmbiguousTree(Node topNode) {
		Node syntaxTree = topNode;
		
		if (topNode != null) {
			ArrayList<Node> param = topNode.getParam();
			if (param != null) {
				if (param.get(0) == null) {
					syntaxTree = param.get(1);
				}
				else if (param.get(1) == null) {
					syntaxTree = param.get(0);
				}
			}
		}
		
		return syntaxTree;
	}
	
	private Node mergeOperands(Node leftOperand, Node rightOperand, NonTerminal wrappingClass, Terminal splitToken) {
		Node wrapper = null;
		
		// If either of the children are missing 
		// one single child, combine add this additional
		// operand as the missing operand
		
		// Left
		boolean leftIsNull = (leftOperand == null);
		boolean leftIsWrappingClass = (!leftIsNull && Token.isWrappingClass(leftOperand.getRule().tokenValue));
		ArrayList<Node> leftParam = (!leftIsNull ? leftOperand.getParam() : null);

		// Right
		boolean rightIsNull = (rightOperand == null);
		boolean rightIsWrappingClass = (!rightIsNull && Token.isWrappingClass(rightOperand.getRule().tokenValue));
		ArrayList<Node> rightParam = (!rightIsNull ? rightOperand.getParam() : null);
		
		// Possible situations:
		//		Left Wrapping?		Right Wrapping?			Action
		//			yes					yes		 			Wrap
		//			no					yes					Wrap
		//			yes					no					Wrap
		//			no					no					Wrap
		//			null				no					Wrap
		//			no					null				Wrap
		//			yes					null				Pass 1 back up the stream
		//			null				yes					Pass 2 back up the call stack
		//			null				null				null
		
		boolean doWrap = false;
		doWrap = true;
		/*
		// Both are null
		if (rightIsNull && leftIsNull) {
			return null;
		}
		// Both are non-null
		else if (!rightIsNull && !leftIsNull) {
			// Merge the trees
			// If one is a wrapping class (binary) with an empty child
/////
// 
			if (rightIsWrappingClass) {
				// Right has an empty binary child
				// merge left into that spot
				wrapper = rightOperand;
				if (rightParam.get(0) == null) {
					rightParam.set(0, leftOperand);
				}
				else {
					rightParam.set(1, leftOperand);
				}
			}
			else if (leftIsWrappingClass && (leftParam.get(0) == null || leftParam.get(1) == null)) {
				// Left has an empty binary child
				// merge right into that spot
				wrapper = leftOperand;
				if (leftParam.get(0) == null) {
					leftParam.set(0,  rightOperand);
				}
				else {
					leftParam.set(1,  rightOperand);
				}
			}
			// Do normal wrapper
			// Otherwise
			else {
				doWrap = true;
			}
		}
		// One is null, one is not
		// If no wrapping class appears, wrap
		else if (!rightIsWrappingClass || !leftIsWrappingClass) {
			doWrap = true;
		}
		*/
		// If both children have appropriate child nodes
		// Create a new wrapper Node (NonTerminal.AmbiguousPattern.nonTerminalWrapper)
		if (doWrap) {
			wrapper = new Node(wrappingClass);
			wrapper.setToken(splitToken);
			wrapper.addParam(leftOperand);
			wrapper.addParam(rightOperand);
		}
		
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