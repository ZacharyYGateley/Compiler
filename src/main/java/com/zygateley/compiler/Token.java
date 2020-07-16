package com.zygateley.compiler;

/**
 * A stream item contains a token (Terminal)
 * and may contain a symbol or a value
 * symbol: item in SymbolTable
 * value:  primitive literal or Terminal.exactString 
 * @author Zachary Gateley
 *
 */
public class Token {
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
	public static final Token EMPTY = new Token(Terminal.EMPTY, null, null);
	
	/**
	 * @param token 
	 * @param symbol entry in SymbolTable, if applicable
	 * @param value primitive literal String value or Terminal.exactString 
	 */
	public Token(Terminal token, Symbol symbol, String value) {
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
			symbolString = "\n\tSymbol: " + this.symbol;
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