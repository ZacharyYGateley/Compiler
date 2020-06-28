package com.zygateley.compiler;

import java.util.*;


class StreamItem {
	public final Terminal token;
	public final Symbol symbol;
	public final String value;
	public static final StreamItem EMPTY = new StreamItem(Terminal.EMPTY);
	
	public StreamItem(Terminal token) {
		this.token = token;
		this.symbol = null;
		this.value = null;
	}
	public StreamItem(Terminal token, Symbol symbol, String value) {
		this.token = token;
		this.symbol = symbol;
		this.value = value;
	}
	
	@Override
	public String toString() {
		return "Token: " + this.token + 
				(this.symbol != null ? "\n\tSymbol: " + this.symbol : "") + 
				(this.value != null ? "\n\tValue: " + this.value : ""); 
	}
}

/**
 * Stream of tokens for parser.
 * 
 * @author Zachary Gateley
 *
 */
public class TokenStream {
	
	private ArrayList<StreamItem> tokens;
	private int len;
	private int pos = 0;
	 
	public TokenStream() {
		this.tokens = new ArrayList<StreamItem>();
		this.len = 0;
		this.pos = 0;
	}
	public TokenStream(ArrayList<StreamItem> tokens) {
		this.tokens = tokens;
	    this.len = tokens.size();
	    this.pos = 0;
	}
	 
	public StreamItem peek() {
	    if (this.isEmpty()) {
	        return StreamItem.EMPTY;
	    }
	    return this.tokens.get(this.pos);
	}
	
	public void addtoken(Terminal token, Symbol symbol, String value) {
		this.tokens.add(new StreamItem(token, symbol, value));
		this.len++;
	}
	public void addtoken(StreamItem si) {
		this.tokens.add(si);
		this.len++;
	}
	 
	public StreamItem gettoken() {
	    StreamItem next = this.peek();
	    this.pos++;
	    return next;
	}
	
	public void ungettoken() {
		if (this.pos > 0) {
			this.pos--;
		}
	}
	
	/**
	 * For ambiguous streams,
	 * must remember where we were
	 */
	public int getPosition() {
		return this.pos;
	}
	public void setPosition(int position) {
		this.pos = position;
	}
	 
	public boolean contains(Terminal t) {
		for (StreamItem tp : this.tokens) {
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
	
	@Override
	public String toString() {
		return toString(this.len);
	}
	public String toString(int limit) {
		int originalPosition = this.getPosition();
		StringBuilder sb = new StringBuilder();
		while (!this.isEmpty() && this.getPosition() < limit) {
			sb.append(this.gettoken() + "\n");
		}
		this.setPosition(originalPosition);
		return sb.toString();
	}
}