package com.zygateley.compiler;

import java.util.*;


class StreamItem {
	// Allow parser to change token for negation
	public Terminal token;
	public final Symbol symbol;
	public final String value;
	// If this token represents a group (paren, curly, square)
	// Pointers
	// 		from open StreamItem to close INDEX
	// 		from close StreamItem to open INDEX
	// Only when you have to, see Parser::toPrecedenceStream
	public int closeGroup = -1;
	public int openGroup = -1;
	// During parsing, Store pointer to syntaxTree here if group
	public Node syntaxTree = null;
	public boolean negated = false;
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
		return this.toString(-1);
	}
	public String toString(int position) {
		String positionString = "";
		if (position > -1) {
			positionString = " (" + position + ")\t";
		}
		String negationString = "";
		if (this.negated) {
			negationString = " (NEGATED)";
		}
		String symbolString = "";
		if (this.symbol != null) {
			symbolString = "\n\tSymbol: " + (this.symbol.getName() != null ? this.symbol.getName() : this.symbol.getValue());
		}
		String valueString = "";
		if (this.value != null) {
			valueString = "\n\tValue: " + this.value;
		}
		String groupString = "";
		if (this.openGroup > 0) {
			groupString = "\n\tGroup: [" + this.openGroup + ", " + this.closeGroup + "]";
		}
		String syntaxString = "";
		if (this.syntaxTree != null) {
			syntaxString = "\n\tHas syntax tree";
		}
		return "Token:" + positionString + this.token + negationString + 
				symbolString + valueString + groupString + syntaxString;
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
	private int rightPositionExclusive;
	private int leftPosition; 
	 
	public TokenStream() {
		this.tokens = new ArrayList<StreamItem>();
		this.rightPositionExclusive = 0;
		this.leftPosition = 0;
	}
	public TokenStream(ArrayList<StreamItem> tokens) {
		this.tokens = tokens;
	    this.rightPositionExclusive = tokens.size();
	    this.leftPosition = 0;
	}
	 
	public StreamItem peekLeft() {
	    if (this.isEmpty()) {
	        return StreamItem.EMPTY;
	    }
	    return this.tokens.get(this.leftPosition);
	}
	public StreamItem peekRight() {
		if (this.isEmpty()) {
			return StreamItem.EMPTY;
		}
		return this.tokens.get(this.rightPositionExclusive - 1);
	}
	
	public void addtoken(Terminal token, Symbol symbol, String value) {
		this.tokens.add(new StreamItem(token, symbol, value));
		this.rightPositionExclusive++;
	}
	public void addtoken(StreamItem si) {
		this.tokens.add(si);
		this.rightPositionExclusive++;
	}
	 
	public StreamItem popLeft() {
		boolean isEmpty = this.isEmpty();
	    StreamItem next = this.peekLeft();
	    if (!isEmpty) this.leftPosition++;
	    return next;
	}
	
	public StreamItem popRight() {
		boolean isEmpty = this.isEmpty();
		StreamItem prev = this.peekRight();
		if (!isEmpty) this.rightPositionExclusive--;
		return prev;
	}
	
	public void unpopLeft() {
		if (this.leftPosition > 0) {
			this.leftPosition--;
		}
	}
	
	public StreamItem get(int position) {
		return this.tokens.get(position);
	}
		
	/**
	 * For ambiguous streams,
	 * must remember where we were
	 */
	public int getLeft() {
		return this.leftPosition;
	}
	public void setLeft(int position) {
		this.leftPosition = position;
	}
	public int getRightExclusive() {
		return this.rightPositionExclusive;
	}
	public void setRightExclusive(int position) {
		this.rightPositionExclusive = position;
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
	    return this.rightPositionExclusive == this.leftPosition;
	}
	 
	public int length() {
	    return this.rightPositionExclusive - this.leftPosition;
	}
	
	@Override
	public String toString() {
		return toString(this.leftPosition, this.rightPositionExclusive);
	}
	public String toString(int startPosition, int endPosition) {
		int originalLeft = this.getLeft();
		int originalRight = this.getRightExclusive();
		this.setLeft(startPosition);
		StringBuilder sb = new StringBuilder();
		for (int i = startPosition; i < endPosition; i++) {
			StreamItem item = this.tokens.get(i);
			sb.append(item.toString(i) + "\n");
		}
		this.setLeft(originalLeft);
		this.setRightExclusive(originalRight);
		return sb.toString();
	}
}