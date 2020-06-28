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
	
	public Parser(TokenStream ts) {
		this.tokenStream = ts;
	}
	
	public Node parse(boolean verbose) {
		this.verbose = verbose;
		return parse();
	}
	public Node parse() {
		// Reset program depth (for verbose output)
		depth = 0;
		
		if (verbose) {
			System.out.println("<!-- Parser started -->\n");
		}
		
		// Top-level of parseTree should only contain first rule
		NonTerminal firstRule = NonTerminal.values()[0];
		Node parseTree = parseRule(firstRule); 
				
		if (verbose) {
			System.out.println("\n<!-- Parser finished -->\n");
		}
		
		return parseTree;
	}
	
	private Node parseRule(NonTerminal rule) {
		// New non-terminal node
		Node pn = new Node(rule);
		
		Terminal t = tokenStream.peek().token;

		// Get pattern to work with
    	int indexInFirst = rule.indexOfMatchFirst(t);
    	if (indexInFirst < 0) {
    		boolean hasEpsilon = (rule.indexOfMatchFirst(NonTerminal.EMPTY) > -1);
        	boolean inFollow = rule.inFollow(t);
        	if (hasEpsilon && inFollow) {
        		if (verbose) {
        			for (int i = 0; i < depth; i++) System.out.print("  ");
        			System.out.println("<" + rule.toString().replaceAll("_",  "") + " content=\"EMPTY\" />");
        		}
        		
        		// Empty string has been utilized for this rule
        		// Return rule with empty node parameter
        		pn.addParam(new Node(Terminal.EMPTY, null, null));
        		// Strip empty ArrayList from ParseNode
        		return new Node(pn, true);
        	}
        	else {
        		System.err.println("Fatal error: Production Rule terminated prematurely.");
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
			int tokenValue = pattern[i];
			
			Node next;
			// Terminal
			// These are immediately added to the parse tree
			if (Token.isTerminal(tokenValue)) {
				StreamItem item = tokenStream.gettoken();
				if (item.token.tokenValue != tokenValue) {
					System.err.println("Fatal error: incorrect syntax.");
					return null;
				}
				
				// VERBOSE
				if (verbose) {
					/*
					StringBuilder tokenName = new StringBuilder(ts.token + "          ");
					tokenName.setLength(10);
					*/
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
				next = parseRule(NonTerminal.getNonTerminal(tokenValue));
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
}