package com.zygateley.compiler;

import java.util.*;


class Node implements Iterable<Node> {
	// Name is used for debugging so that the 
	// token is clearly understood at first sight
	private final String _name_;
	private final NonTerminal rule;
	private final Terminal token;
	private final Symbol symbol;
	private final String value;
	private final ArrayList<Node> param;
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
	public Node(Terminal token, Symbol symbol, String value) {
		this._name_ = (symbol != null ? "(" + symbol + ") " : "") + token;
		this.rule = null;
		this.token = token;
		this.symbol = symbol;
		this.value = value;
		this.param = new ArrayList<Node>();
	}
	public Node(final Node pn, boolean emptyParam) {
		this._name_ = pn._name_;
		this.rule = pn.rule;
		this.token = pn.token;
		this.symbol = pn.symbol;
		this.value = pn.value;
		this.param = (emptyParam ? Node.emptyParam : pn.param);
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
		this.param.add(param);
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
	public boolean ambiguousStreamOpen;
	
	public Parser(TokenStream ts) {
		this.tokenStream = ts;
		this.ambiguousStreamOpen = false;
	}
	
	public Node parse(boolean verbose) {
		this.verbose = verbose;
		return parse();
	}
	public Node parse() {
		// Reset program depth (for verbose output)
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
	
	private Node parseRule(NonTerminal rule, int limit) {
		// New non-terminal node
		Node pn = new Node(rule);
		
		Terminal t = tokenStream.peek().token;

		// Get pattern to work with empty strings
    	int indexInFirst = rule.indexOfMatchFirst(t);
    	if (indexInFirst < 0) {
    		Node childNode = getEmptyNodeIfValid(rule, t);
    		if (childNode != null) {
    			pn.addParam(childNode);
    			return pn;
    		}
    		else {
    			return null;
    		}
    	}
    	
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
			if (tokenStream.getPosition() == limit) {
				System.err.println("Fatal error 2: Production rule terminated prematurely.");
			}
			
			int tokenValue = pattern[i];
			
			Node next;
			// Terminal
			// These are immediately added to the syntax tree
			if (Token.isTerminal(tokenValue)) {
				StreamItem item = tokenStream.gettoken();
				if (item.token.tokenValue != tokenValue) {
					System.err.println("Fatal error: incorrect syntax.");
					return null;
				}
				
				// Add new terminal
				
				// VERBOSE
				if (verbose) {
					String tokenName = item.token + "";
					String symbolString = "";
					if (item.symbol != null) {
						String name = item.symbol.getName();
						if (name != null) symbolString += " name=\"" + item.symbol.getName() + "\"";
						String value = item.symbol.getValue();
						if (value != null) symbolString += " value=" + item.symbol.getName() + "\"";
						Symbol.Type type = item.symbol.getType();
						if (type != null) symbolString = " type=\"" + type + "\" ";
					}
					else if (item.value != null && !item.value.isBlank()) {
						symbolString += " value=" + item.value + "\"";
					}
					for (int j = 0; j < depth; j++) System.out.print("  ");
					System.out.println("  <Terminal " + tokenName + symbolString + ">");
				}
				// VERBOSE
				
				next = new Node(item.token, item.symbol, item.value);
			}
			// NonTerminals
			// Recur into parseRule
			else {
				depth++;
				NonTerminal nextToken = NonTerminal.getNonTerminal(tokenValue);
				if (!nextToken.isAmbiguous()) {
					next = parseRule(nextToken, limit);
				}
				// Ambiguous rules need to move into ambiguous branch
				else {
					next = toAmbiguousStream(nextToken, limit, rule);
				}
				if (next == null) {
					return null;
				}
				depth--;
			}
			
			// Add resulting token or tree as parameter to this rule
			pn.addParam(next);
		}
		
		if (verbose) {
			for (int i = 0; i < depth; i++) System.out.print("  ");
			System.out.println("</" + rule.toString().replaceAll("_",  "") + ">");
		}
		
		// Strip empty ArrayList where applicable
		if (pn.getParam().size() == 0) {
			pn = new Node(pn, true);
		}
		
		return pn;
	}
	
	/**
	 * getEmptyNodeIfValid
	 * 
	 * Return empty string if 
	 * 		epsilon is in the rule's first
	 * 		and the token is in the rule's follow
	 *  
	 * @param rule production rule in question
	 * @param token token in question
	 * @return Node with Terminal.EMPTY or null if invalid
	 */
	private Node getEmptyNodeIfValid(NonTerminal rule, Terminal token) {
		boolean hasEpsilon = (rule.indexOfMatchFirst(Terminal.EMPTY) > -1);
    	boolean inFollow = rule.inFollow(token);
    	if (hasEpsilon && inFollow) {
    		if (verbose) {
    			for (int i = 0; i < depth; i++) System.out.print("  ");
    			System.out.println("<" + rule.toString().replaceAll("_",  "") + " content=\"EMPTY\" />");
    		}
    		
    		// Empty string has been utilized for this rule
    		// Return rule with empty node parameter
    		Node preStrippedNode = new Node(Terminal.EMPTY, null, null);
    		// Copy Node
    		// while stripping empty ArrayList from Node
    		Node strippedNode = new Node(preStrippedNode, true);
    		
    		return strippedNode;
    	}
    	else {
    		System.err.println("Fatal error 1: Production rule terminated prematurely.");
    		return null;
    	}
	}
	
	/**
	 * From ambiguous stream to non-ambiguous stream
	 * 
	 * @param nonAmbiguousRule
	 * @param endPosition exclusive
	 * @return
	 */
	private Node toNonAmbiguousStream(NonTerminal nonAmbiguousRule, int endPosition) {
		Node syntaxTree = parseRule(nonAmbiguousRule, endPosition);
		return syntaxTree;
	}
	
	/**
	 * parseAmbiguousRule
	 * @param rule Ambiguous NonTerminal rule
	 * @param endPosition exclusive
	 * @return
	 */
	private Node parseAmbiguousRule(NonTerminal rule, int endPosition) {
		// Patterns split at these tokens
		Node parseNode = new Node(rule);
		int[] splitAt = rule.ambiguousPattern.splitAt;
		int startPosition = tokenStream.getPosition();
		int partition = endPosition;
		// Loop through
		while (tokenStream.getPosition() < endPosition) {
			StreamItem item = tokenStream.gettoken();
			for (int terminal : splitAt) {
				// Skip any terminal not in  this set
				if (item.token.tokenValue != terminal) continue;
				
				// Match!
				partition = tokenStream.getPosition();
				break;
			}
		}
		// Either there was a match or not
		// If so, partition < endPosition
		//		Send left of partition to leftRule,
		//		Send right of partition to rightRule
		// Otherwise
		//		Send whole stream to leftule
		tokenStream.setPosition(startPosition);
		if (verbose) {
			//System.out.println("NonTerminal: " + rule);
			//System.out.println(tokenStream.toString(partition));
		}
		
		// Parse left side of split
		NonTerminal leftRule = NonTerminal.getNonTerminal(rule.ambiguousPattern.leftRule);
		Node leftChild;
		if (partition > startPosition + 1) {
			// Only use left-hand rule if there is a string to pass it
			if (leftRule.isAmbiguous()) {
				// Stay in ambiguous stream
				// endPosition = partition exclusive
				leftChild = parseAmbiguousRule(leftRule, partition);
			}
			else {
				// Move to non-ambiguous stream
				// endPosition = partition exclusive
				leftChild = toNonAmbiguousStream(leftRule, partition);
			}
			// Add to syntax tree
			parseNode.addParam(leftChild);
		}
		
		// Parse right side (inclusive of split)
		NonTerminal rightRule = NonTerminal.getNonTerminal(rule.ambiguousPattern.rightRule);
		Node rightChild;
		if (partition < endPosition) {
			// A split was found (i.e. RHS is not null)
			
			if (verbose) {
				//System.out.println("Parse RHS of " + rule);
				//System.out.println(tokenStream.toString(endPosition));
			}
			
			// Add the split token
			Node operator = new Node(tokenStream.gettoken().token, null, null);
			operator = new Node(operator, true);
			parseNode.addParam(operator);
			if (verbose) {
    			for (int i = 0; i < depth; i++) System.out.print("  ");
    			System.out.println("<Terminal " + operator.getToken() + "/>");
			}
			
			
			// Parse right-hand side of split
			if (rightRule.isAmbiguous()) {
				rightChild = parseAmbiguousRule(rightRule, endPosition);
			}
			else {
				rightChild = toNonAmbiguousStream(rightRule, endPosition);
			}
		}
		else {
			// Create an empty node
			rightChild = new Node(Terminal.EMPTY, null, null);
			// Strip the node of unused allocated space
			rightChild = new Node(rightChild, true);
		}
		// Add to syntax tree
		parseNode.addParam(rightChild);
		
		return parseNode;
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
	private Node toAmbiguousStream(NonTerminal ambiguousRule, int endPosition, NonTerminal parentRule) {
		// Because of the nature of the ambiguous stream
		// Only one ambiguous stream may be open at once
		if (this.ambiguousStreamOpen) {
			Node syntaxTree = new Node(Terminal.EMPTY, null, null);
			syntaxTree = new Node(syntaxTree, true);
			return syntaxTree;
		}
		this.ambiguousStreamOpen = true;
		
		// Find end position for this ambiguous NonTerminal
		int startPos = tokenStream.getPosition();
		int endPos = startPos;
		
		// Loop until a FOLLOW character has been found
		// and open paren == closed paren
		int parenCount = 0;
		boolean isBalanced = true;
		while (tokenStream.getPosition() < endPosition) {
			// Consume stream
			StreamItem nextItem = tokenStream.gettoken();
			boolean inFollow = parentRule.inFollow(nextItem.token);
			isBalanced = (parenCount == 0);
			if (inFollow && isBalanced) {
				break;
			}
			
			// Find balance
			switch (nextItem.token) {
			case PAREN_OPEN:
				parenCount++;
				break;
			case PAREN_CLOSE:
				parenCount--;
				break;
			default:
				break;
			}
		}
		
		// Reached end of stream (read: program)
		// If not balanced parentheses, syntax error.
		if (!isBalanced) {
			System.err.println("Fatal error 3: Imbalanced parentheses.");
			return null;
		}
		// Otherwise, there is a stream to parse

		// Reset stream pointer to start
		endPos = tokenStream.getPosition();
		tokenStream.setPosition(startPos);
		// And parse using the ambiguous rule until the determined end position
		Node syntaxTree = this.parseAmbiguousRule(ambiguousRule, endPos);
		
		// Reset stream parameters after already-parsed stream
		// Set to before FOLLOW token so that parent NonTerminal knows that it is done.
		tokenStream.setPosition(endPos - 1);
		startPos = endPos;
		
		// Close ambiguous stream
		this.ambiguousStreamOpen = false;
		
		return syntaxTree;
	}
}