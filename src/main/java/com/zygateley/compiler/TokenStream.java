package com.zygateley.compiler;

import java.util.*;


/**
 * A stream item contains a token (Terminal)
 * and may contain a symbol or a value
 * symbol: item in SymbolTable
 * value:  primitive literal or Terminal.exactString 
 * @author Zachary Gateley
 *
 */
class StreamItem {
	// Allow parser to change token for negation
	public Terminal token;
	public final Symbol symbol;
	public final String value;
	
	// Groups (paren, curly, square brackets)
	// Pointers
	// 		from open StreamItem to close INDEX
	// 		from close StreamItem to open INDEX
	// Only when you have to, see Parser::toPrecedenceStream
	public int closeGroupIndex = -1;
	public int openGroupIndex = -1;
	// During parsing, pointer to syntax subtree is stored here
	public Node syntaxSubtree = null;
	
	// During parsing, this stream item may be marked as negated
	public boolean negated = false;
	
	// Save space by storing only one empty StreamItem
	public static final StreamItem EMPTY = new StreamItem(Terminal.EMPTY, null, null);
	
	/**
	 * @param token 
	 * @param symbol entry in SymbolTable, if applicable
	 * @param value primitive literal String value or Terminal.exactString 
	 */
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
		if (this.openGroupIndex > 0) {
			groupString = "\n\tGroup: [" + this.openGroupIndex + ", " + this.closeGroupIndex + "]";
		}
		String syntaxString = "";
		if (this.syntaxSubtree != null) {
			syntaxString = "\n\tHas syntax tree";
		}
		return "Token:" + positionString + this.token + negationString + 
				symbolString + valueString + groupString + syntaxString;
	}
}

/**
 * Stream of StreamItems built by lexer and used by parser.
 * leftIndex is inclusive
 * rightIndex is exclusive
 * 
 * @author Zachary Gateley
 *
 */
public class TokenStream {
	
	private ArrayList<StreamItem> tokens;
	private int leftIndex = 0;
	private int rightIndexExcl = 0;
	private int capacity = 0;
	 
	public TokenStream() {
		this.tokens = new ArrayList<StreamItem>();
	}
	 
	public StreamItem peekLeft() {
	    if (this.isEmpty()) {
	        return StreamItem.EMPTY;
	    }
	    return this.tokens.get(this.leftIndex);
	}
	public StreamItem peekRight() {
		if (this.isEmpty()) {
			return StreamItem.EMPTY;
		}
		return this.tokens.get(this.rightIndexExcl - 1);
	}
	
	public void writeRight(Terminal token, Symbol symbol, String value) {
		this.tokens.add(new StreamItem(token, symbol, value));
		this.rightIndexExcl++;
		this.capacity++;
	}
	public void writeRight(StreamItem si) {
		this.tokens.add(si);
		this.rightIndexExcl++;
		this.capacity++;
	}
	 
	public StreamItem readLeft() {
		boolean isEmpty = this.isEmpty();
	    StreamItem next = this.peekLeft();
	    if (!isEmpty) this.leftIndex++;
	    return next;
	}
	
	public StreamItem readRight() {
		boolean isEmpty = this.isEmpty();
		StreamItem prev = this.peekRight();
		if (!isEmpty) this.rightIndexExcl--;
		return prev;
	}
	
	public void unreadLeft() {
		if (this.leftIndex > 0) {
			this.leftIndex--;
		}
	}
	
	public StreamItem peekAt(int position) {
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
			StreamItem item = this.tokens.get(i);
			sb.append(item.toString(i) + "\n");
		}
		this.setLeftIndex(originalLeft);
		this.setRightIndexExcl(originalRight);
		return sb.toString();
	}
}