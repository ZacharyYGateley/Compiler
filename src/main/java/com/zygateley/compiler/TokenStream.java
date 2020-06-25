package com.zygateley.compiler;

import java.util.*;


class TokenSymbol {
	public final Terminal token;
	public final Symbol symbol;
	public static final TokenSymbol EMPTY = new TokenSymbol(Terminal.EMPTY);
	
	public TokenSymbol(Terminal token) {
		this.token = token;
		this.symbol = null;
	}
	public TokenSymbol(Terminal token, Symbol symbol) {
		this.token = token;
		this.symbol = symbol;
	}
}

/**
 * Stream of tokens for parser.
 * 
 * @author Zachary Gateley
 *
 */
public class TokenStream {
	
	private ArrayList<TokenSymbol> tokens;
	private int len;
	private int pos = 0;
	 
	public TokenStream() {
		this.tokens = new ArrayList<TokenSymbol>();
		this.len = 0;
		this.pos = 0;
	}
	public TokenStream(ArrayList<TokenSymbol> tokens) {
		this.tokens = tokens;
	    this.len = tokens.size();
	    this.pos = 0;
	}
	 
	public TokenSymbol peek() {
	    if (this.isEmpty()) {
	        return TokenSymbol.EMPTY;
	    }
	    return this.tokens.get(this.pos);
	}
	 
	
	public void addtoken(Terminal token, Symbol symbol) {
		this.tokens.add(new TokenSymbol(token, symbol));
		this.len++;
	}
	 
	public TokenSymbol gettoken() {
	    TokenSymbol next = this.peek();
	    this.pos++;
	    return next;
	}
	
	public void ungettoken() {
		if (this.pos > 0) {
			this.pos--;
		}
	}
	 
	public boolean contains(Terminal t) {
		for (TokenSymbol tp : this.tokens) {
			if (tp.token.equals(t)) {
				return true;
			}
		}
		return false;
	}
	 
	public boolean isEmpty() {
	    return this.len == this.pos;
	}
	 
	public int length() {
	    return this.len - this.pos;
	}
}