package com.zygateley.compiler;

import java.util.*;


class ParseNode implements Iterable<ParseNode> {
	// Name is used for debugging so that the 
	// token is clearly understood at first sight
	private final String _name_;
	private final NonTerminal rule;
	private final Terminal token;
	private final Symbol symbol;
	private final ArrayList<ParseNode> param;
	// Keep one copy of an empty parameter list
	// Point to it on empty parameter list to limit memory usage
	// Additionally, this ensures param is never null
	private static final ArrayList<ParseNode> emptyParam = new ArrayList<ParseNode>();
	
	public ParseNode(NonTerminal rule) {
		this._name_ = rule+"";
		this.rule = rule;
		this.token = null;
		this.symbol = null;
		this.param = new ArrayList<ParseNode>();
	}
	public ParseNode(Terminal token, Symbol symbol) {
		this._name_ = (symbol != null ? "(" + symbol + ") " : "") + token;
		this.rule = null;
		this.token = token;
		this.symbol = symbol;
		this.param = new ArrayList<ParseNode>();
	}
	public ParseNode(final ParseNode pn, boolean emptyParam) {
		this._name_ = pn._name_;
		this.rule = pn.rule;
		this.token = pn.token;
		this.symbol = pn.symbol;
		this.param = (emptyParam ? ParseNode.emptyParam : pn.param);
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
	
	@Override
	public Iterator<ParseNode> iterator() {
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
	
	public ParseNode parse(boolean verbose) {
		this.verbose = verbose;
		return parse();
	}
	public ParseNode parse() {
		// Reset program depth (for verbose output)
		depth = 0;
		
		if (verbose) {
			System.out.println("<!-- Parser started -->\n");
		}
		
		// Top-level of parseTree should only contain first rule
		NonTerminal firstRule = NonTerminal.values()[0];
		ParseNode parseTree = parseRule(firstRule); 
				
		if (verbose) {
			System.out.println("\n<!-- Parser finished -->\n");
		}
		
		return parseTree;
	}
	
	private ParseNode parseRule(NonTerminal rule) {
		// New non-terminal node
		ParseNode pn = new ParseNode(rule);
		
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
        		pn.addParam(new ParseNode(Terminal.EMPTY, null));
        		// Strip empty ArrayList from ParseNode
        		return new ParseNode(pn, true);
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
					/*
					StringBuilder tokenName = new StringBuilder(ts.token + "          ");
					tokenName.setLength(10);
					*/
					String tokenName = ts.token + "";
					String symbolString = "";
					if (ts.symbol != null) {
						String name = ts.symbol.getName();
						if (name != null) symbolString += "name=\"" + ts.symbol.getName() + "\"";
						String value = ts.symbol.getValue();
						if (value != null) symbolString += "value=" + ts.symbol.getName() + "\"";
						Symbol.Type type = ts.symbol.getType();
						if (type != null) symbolString = " type=\"" + type + "\" ";
					}
					for (int j = 0; j < depth; j++) System.out.print("  ");
					System.out.println("  <Terminal " + tokenName + symbolString + ">");
				}
				next = new ParseNode(ts.token, ts.symbol);
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
			pn = new ParseNode(pn, true);
		}
		
		return pn;
	}
}