package com.zygateley.compiler;

import java.util.*;


class ParseNode {
	private final NonTerminal rule;
	private final Terminal token;
	private final Symbol symbol;
	private final ArrayList<ParseNode> param;
	
	public ParseNode(NonTerminal rule) {
		this.rule = rule;
		this.token = null;
		this.symbol = null;
		this.param = new ArrayList<ParseNode>();
	}
	public ParseNode(Terminal token, Symbol symbol) {
		this.rule = null;
		this.token = token;
		this.symbol = symbol;
		this.param = new ArrayList<ParseNode>();
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
	
	public ArrayList<ParseNode> getParam() {
		return this.param;
	}
	
	public void addParam(final ParseNode param) {
		this.param.add(param);
	}
}


public class Parser {
	// Make accessible from same package
	StringBuilder output;
	// Current rule, recursive by stack
	ArrayDeque<NonTerminal> rule;
	TokenStream tokenStream;
	ArrayList<ParseNode> parseTree;
	
	public Parser(TokenStream ts) {
		this.output = new StringBuilder();
		this.rule = new ArrayDeque<NonTerminal>();
		this.tokenStream = ts;
		this.parseTree = new ArrayList<ParseNode>();
	}
	
	public ArrayList<ParseNode> parse() {
	    // Get token and consume stream until stream is empty
	    NEXT_TOKEN: while (tokenStream.length() > 0) {
	        // Peek next token
	    	// Does not consume stream
	        Terminal t = tokenStream.peek().token;
	         
	        // Find first applicable rule
	        for (NonTerminal rule : NonTerminal.values()) {
	        	int indexInFirst = rule.indexOfMatchFirst(t);
	        	boolean isThisRule = indexInFirst > -1;
	        	
	        	// MATCH!
	        	if (isThisRule) {
	        		// Create parse tree at this top-level rule
	        		// Starting at this location in the token stream
	        		ParseNode pn = parseRule(rule);
	        		if (pn != null) {
	        			this.parseTree.add(pn);
	        		}
	        		
	        		// When match is found and processed, keep going
	        		continue NEXT_TOKEN;
	        	}
	        }
	        
	        // No match
	        // Invalid syntax
	        System.err.println("Syntax error: Rule not found starting with " + t);
	        tokenStream.gettoken();
	    }
	     
	    return this.parseTree;
	}
	
	private ParseNode parseRule(NonTerminal rule) {
		ParseNode pn = new ParseNode(rule);
		
		Terminal t = tokenStream.peek().token;

		// Get pattern to work with
    	int indexInFirst = rule.indexOfMatchFirst(t);
    	if (indexInFirst < 0) {
    		System.err.println("Fatal error: Production Rule terminated prematurely.");
    		return null;
    	}
		int[] pattern = rule.patterns[indexInFirst].PATTERN;
		
		// Get rules in order
		// If any of the rules is a NonTerminal, recur
		for (int tokenValue : pattern) {
			ParseNode next;
			// Terminal
			// These are immediately added to the parse tree
			if (Token.isTerminal(tokenValue)) {
				TokenSymbol ts = tokenStream.gettoken();
				if (ts.token.tokenValue != tokenValue) {
					System.err.println("Fatal error: incorrect syntax.");
					return null;
				}
				next = new ParseNode(ts.token, ts.symbol);
			}
			// NonTerminals
			// Recur into parseRule
			else {
				next = parseRule(NonTerminal.getNonTerminal(tokenValue));
				if (next == null) {
					return null;
				}
			}
			
			// Add resulting token or tree as parameter to this rule
			pn.addParam(next);
		}
		
		return pn;
	}
}