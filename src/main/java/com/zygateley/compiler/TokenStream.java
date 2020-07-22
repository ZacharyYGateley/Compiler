package com.zygateley.compiler;

import java.util.*;

/**
 * Stream of StreamItems built by lexer and used by parser.
 * leftIndex is inclusive
 * rightIndex is exclusive
 * 
 * @author Zachary Gateley
 *
 */
public class TokenStream {
	
	private ArrayList<Token> tokens;
	private int leftIndex = 0;
	private int rightIndexExcl = 0;
	 
	public TokenStream() {
		this.tokens = new ArrayList<Token>();
	}
	 
	public Token peekLeft() {
	    if (this.isEmpty()) {
	        return Token.EMPTY;
	    }
	    return this.tokens.get(this.leftIndex);
	}
	public Token peekRight() {
		if (this.isEmpty()) {
			return Token.EMPTY;
		}
		return this.tokens.get(this.rightIndexExcl - 1);
	}
	
	public void write(Terminal token, Symbol symbol, String value) {
		this.tokens.add(new Token(token, symbol, value));
		this.rightIndexExcl++;
	}
	public void writeRight(Token si) {
		this.tokens.add(si);
		this.rightIndexExcl++;
	}
	 
	public Token readLeft() {
		boolean isEmpty = this.isEmpty();
	    Token next = this.peekLeft();
	    if (!isEmpty) this.leftIndex++;
	    return next;
	}
	
	public Token readRight() {
		boolean isEmpty = this.isEmpty();
		Token prev = this.peekRight();
		if (!isEmpty) this.rightIndexExcl--;
		return prev;
	}
	
	public void unreadLeft() {
		if (this.leftIndex > 0) {
			this.leftIndex--;
		}
	}
	
	public Token peekAt(int position) {
		return this.tokens.get(position);
	}
		
	/**
	 * For ambiguous streams,
	 * must remember where we were
	 */
	public int getLeftIndex() {
		return this.leftIndex;
	}
	public void setLeftIndex(int position) {
		this.leftIndex = position;
	}
	public int getRightIndexExcl() {
		return this.rightIndexExcl;
	}
	public void setRightIndexExcl(int position) {
		this.rightIndexExcl = position;
	}
	 
	public boolean isEmpty() {
	    return this.rightIndexExcl == this.leftIndex;
	}
	 
	public int length() {
	    return this.rightIndexExcl - this.leftIndex;
	}
	
	@Override
	public String toString() {
		return toString(this.leftIndex, this.rightIndexExcl);
	}
	public String toString(int startPosition, int endPosition) {
		int originalLeft = this.getLeftIndex();
		int originalRight = this.getRightIndexExcl();
		this.setLeftIndex(startPosition);
		StringBuilder sb = new StringBuilder();
		for (int i = startPosition; i < endPosition; i++) {
			Token item = this.tokens.get(i);
			sb.append(item.toString(i) + "\n");
		}
		this.setLeftIndex(originalLeft);
		this.setRightIndexExcl(originalRight);
		return sb.toString();
	}
}