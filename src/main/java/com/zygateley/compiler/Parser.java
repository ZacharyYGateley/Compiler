package com.zygateley.compiler;

import java.util.*;


class ParseNode {
	private final NonTerminal rule;
	private final ArrayList<ParseNode> param;
	
	public ParseNode(NonTerminal rule) {
		this.rule = rule;
		this.param = new ArrayList<ParseNode>();
	}

	public NonTerminal getRule() {
		return this.rule;
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
	
	public String parse() {
	    // Get token and consume stream until stream is empty
	    NEXT_TOKEN: while (tokenStream.length() > 0) {
	        // Peek next token
	    	// Does not consume stream
	        Terminal t = tokenStream.peek().token;
	         
	        // Find first applicable rule
	        for (NonTerminal rule : NonTerminal.values()) {
	        	boolean isThisRule = rule.inFirst(t);
	        	
	        	// MATCH!
	        	if (isThisRule) {
	        		// Set this rule as the current rule in the parser
	        		this.rule.push(rule);
	        		
	        		// Create new top-level ParseNode for this statement
	        		ParseNode pn = new ParseNode(rule);
	        		// Get all parameters for this node (if not a terminal)
	        		
	        		
	        		// When finished with token, 
	        		// if this is a top-level statement, 
	        		// add this parse node to the parse tree
	        		if (this.rule.pop() != rule) {
	        			System.err.println("Fatal error: Bad rule embedding.");
	        		}
	        		if (this.rule.size() == 0) {
	        			this.parseTree.add(pn);
	        		}
	        		// When match is found and processed, keep going
	        		continue NEXT_TOKEN;
	        	}
	        }
	    }
	     
	    return output.toString();
	}
}