package com.zygateley.compiler;

import java.util.*;

enum Token {
	EMPTY 		(""),
	VAR			("r\\d"),
	ECHO 		("echo"),
	EOF			("\\$"),
	INT 		("\\d"),
	EQUALS 		("="),
	PARAN_OPEN	("\\("),
	PARAN_CLOSE ("\\)"),
	ASTERISK 	("*"),
	SLASH 		("/"),
	PLUS  		("\\+"),
	MINUS 		("-");

	public final String regex;
	
	private Token(String regex) {
		this.regex = regex;
	}
}

class TokenSymbol {
	public final Token token;
	public final Symbol symbol;
	public static final TokenSymbol EMPTY = new TokenSymbol(Token.EMPTY);
	
	public TokenSymbol(Token token) {
		this.token = token;
		this.symbol = null;
	}
	public TokenSymbol(Token token, Symbol symbol) {
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
	 
	
	public void addtoken(Token token, Symbol symbol) {
		this.tokens.add(new TokenSymbol(token, symbol));
		this.len++;
	}
	 
	public TokenSymbol gettoken() {
	    TokenSymbol next = this.peek();
	    if (next.token == Token.EMPTY) return next;
	    this.pos++;
	    return next;
	}
	
	public void ungettoken() {
		if (this.pos > -1) {
			this.pos--;
		}
	}
	 
	public boolean contains(Token t) {
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