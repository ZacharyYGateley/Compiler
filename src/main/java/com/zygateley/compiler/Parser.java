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
	private TokenStream tokenStream;
	private boolean verbose;
	
	public Parser(TokenStream ts) {
		this.tokenStream = ts;
	}
	
	public ParseNode parse(boolean verbose) {
		this.verbose = verbose;
		return parse();
	}
	public ParseNode parse() {
		// Top-level of parseTree should only contain first rule
		NonTerminal firstRule = NonTerminal.values()[0];
		return parseRule(firstRule);
	}
	
	private ParseNode parseRule(NonTerminal rule) {
		if (verbose) {
			StringBuilder nameBuilder = new StringBuilder(rule + "          ");
			nameBuilder.setLength(10); 
			System.out.print("\nParsing " + nameBuilder.toString());
		}
		// New non-terminal node
		ParseNode pn = new ParseNode(rule);
		
		Terminal t = tokenStream.peek().token;

		// Get pattern to work with
    	int indexInFirst = rule.indexOfMatchFirst(t);
    	if (indexInFirst < 0) {
    		boolean hasEpsilon = (rule.indexOfMatchFirst(NonTerminal.EMPTY) > -1);
        	boolean inFollow = rule.inFollow(t);
        	if (hasEpsilon && inFollow) {
        		// Empty string has been utilized for this rule
        		// Return rule with empty node parameter
        		pn.addParam(new ParseNode(Terminal.EMPTY, null));
        		return pn;
        	}
        	else {
        		System.err.println("Fatal error: Production Rule terminated prematurely.");
        		return null;
        	}
    	}
		int[] pattern = rule.patterns[indexInFirst].PATTERN;
		
		// Get rules in order
		// If any of the rules is a NonTerminal, recur
		for (int i = 0; i < pattern.length; i++) {
			int tokenValue = pattern[i];
			
			ParseNode next;
			// Terminal
			// These are immediately added to the parse tree
			if (Token.isTerminal(tokenValue)) {
				TokenSymbol ts = tokenStream.gettoken();
				if (ts.token.tokenValue != tokenValue) {
					System.err.println("Fatal error: incorrect syntax.");
					return null;
				}
				if (verbose) {
					StringBuilder tokenName = new StringBuilder(ts.token + "          ");
					tokenName.setLength(10);
					String symbolName = (ts.symbol != null) ? ts.symbol.getName() : "";
					System.out.print("\t" + tokenName + "\t" + symbolName);
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