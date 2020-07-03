package com.zygateley.compiler;

import java.util.ArrayList;
import java.util.Iterator;


/**
 * The syntaxTree is composed of Nodes.
 * Each node may be a 
 * 		NonTerminal: nonTerminal = nonTerminal
 * 					 childNodes = ArrayList<Node> of children
 * 					 terminal = null
 * 						EXCEPT for precedence rules (see mergeOperands in precedence stream)
 * 					
 * 					 
 * 		or Terminal: terminal = Terminal
 * 					 childNodes = null (no children --> leaf Node)
 * 					 May contain 
 * 						a symbol from the SymbolTable 
 * 							e.g. VAR has a symbol. PLUS does not.
 * 							This is indicated by Terminal.symbolType
 * 					 	or a value
 * 							either LITERAL value
 * 							or inherited from Terminal.exactString
 *  
 * Both NonTerminals and Terminals may be negated
 * 
 * @author Zachary Gateley
 *
 */
public class Node implements Iterable<Node> {
	// Name is used for more readable toString
	public String _name_;
	
	// Assembly element type
	private final Element basicElement;
	
	// CFG and Symbol
	// NonTerminals
	private NonTerminal nonTerminal = null;
	// Terminals
	private Terminal terminal = null;
	private Symbol symbol = null;
	private String value = null;
	private boolean negated = false;
	
	// Tree traversal
	private Node parent = null;
	private Node leftSibling = null;
	private Node rightSibling = null;
	private Node firstChild = null;
	private Node lastChild = null;
	private int childCount = 0;

	
	/////////////////////////////
	// Constructors for Parser //
	/**
	 * Non-leaf node
	 * 
	 * @param nonTerminal rule for this node
	 */
	public Node(NonTerminal nonTerminal) {
		this.basicElement = nonTerminal.basicElement;
		this.nonTerminal = nonTerminal;

		// toString override value
		this._name_ = nonTerminal + "";
	}	
	/**
	 * Non-leaf node
	 * 
	 * @param nonTerminal rule for this node
	 * @param operatorTerminal Terminal operator string
	 */
	public Node(NonTerminal nonTerminal, Terminal operatorTerminal) {
		this.basicElement = nonTerminal.basicElement;
		this.nonTerminal = nonTerminal;
		this.terminal = operatorTerminal;

		// toString override value
		this._name_ = nonTerminal + " (" + operatorTerminal + ")";
	}
	/**
	 * Leaf node
	 * 
	 * @param terminal Token terminal 
	 * @param symbol Symbol item from the SymbolTable, may be null
	 * @param value may be a LITERAL value or the exactString from terminal
	 */
	public Node(Terminal terminal, Symbol symbol, String value) {
		this.basicElement = terminal.basicElement;
		this.terminal = terminal;
		this.symbol = symbol;
		this.value = value;

		// toString override value
		String type = "";
		if (symbol != null) {
			type = (symbol.getType() != null ? symbol.getType()+"" : symbol.getName());
		}
		this._name_ = (symbol != null ? "(" + type + ") " : "") + terminal;
	}
	////////////////////////////////
	// Constructors for Optimizer //
	public Node(Element basicElement) {
		this.basicElement = basicElement;
		
		this._name_ = basicElement + "";
	}
	/**
	 * @param nonTerminal rule for this node
	 */
	public Node(Element basicElement, Node parent, NonTerminal nonTerminal, 
			Terminal terminal, Symbol symbol, String value, boolean negated) {
		this.basicElement = basicElement;
		this.parent = parent;
		
		// CFG and Grammar
		this.nonTerminal = nonTerminal;
		this.terminal = terminal;
		this.symbol = symbol;
		this.value = value;
		this.negated = negated;

		// toString override value
		this._name_ = basicElement + (negated ? " -" : " ");
		if (symbol != null) {
			this._name_ += symbol;
		}
		if (terminal != null && terminal.exactString != null && !terminal.exactString.isBlank()) {
			this._name_ += "(" + terminal.exactString + ")";
		}
		
	}


	
	// Basic element, CFG, and Symbols
	public Element getElementType() {
		return this.basicElement;
	}
	public NonTerminal getRule() {
		return this.nonTerminal;
	}
	public Terminal getToken() {
		return this.terminal;
	}
	public Symbol getSymbol() {
		return this.symbol;
	}
	public String getValue() {
		return this.value;
	}
	public boolean isNegated() {
		return this.negated;
	}
	public void setNegated(boolean negated) {
		this.negated = negated;
	}
	
	
	// Tree traversal
	public Node getParent() {
		return this.parent;
	}
	public Node getPreviousSibling() {
		return this.leftSibling;
	}
	public Node getNextSibling() {
		return this.rightSibling;
	}
	public Node getFirstChild() {
		return this.firstChild;
	}
	public int getChildCount() {
		return this.childCount;
	}
	public Node getChild(int desiredIndex) {
		int count = 0;
		Node childAt = this.firstChild;
		for (; count < desiredIndex && childAt != null; count++) {
			childAt = childAt.rightSibling;
		}
		if (count == desiredIndex) {
			return childAt;
		}
		else {
			return null;
		}
	}
	public void addChild(final Node newChild) {
		if (newChild == null) return;
		
		newChild.parent = this;
		if (this.lastChild == null) {
			// No current siblings
			this.firstChild = this.lastChild = newChild;
		}
		else {
			// Add to right of last child
			newChild.leftSibling = this.lastChild;
			this.lastChild.rightSibling = newChild; 
			this.lastChild = newChild;
		}
		
		this.childCount++;
	}
	public Node pop() {
		// Take care of parent
		Node parent = this.parent;
		if (parent != null) {
			parent.childCount--;
			if (parent.firstChild == this) {
				parent.firstChild = this.rightSibling;
			}
			if (parent.lastChild == this) {
				parent.lastChild = this.leftSibling;
			}
			this.parent = null;
		}
		
		// Take care of left sibling and parent.firstChild (if necessary)
		if (this.leftSibling != null) {
			this.leftSibling.rightSibling = this.rightSibling;
		}
		
		// Take care of right sibling and parent.lastChild (if necessary)
		if (this.rightSibling != null) {
			this.rightSibling.leftSibling = this.leftSibling;
		}
		
		return this;
	}
	

	
	@Override
	public String toString() {
		StringBuilder output = new StringBuilder();
		output.append(_name_ + "\n");
		output.append("Children:\n\t");
		for (Node childNode : this) {
			output.append(childNode.toString(false) + " : "); 
		}
		return output.toString();
	}
	
	public String toString(boolean withChildren) {
		if (!withChildren) {
			return _name_;
		}
		else {
			return toString();
		}
	}
	
	@Override
	public Iterator<Node> iterator() {
		ArrayList<Node> childList = new ArrayList<>();
		if (this.childCount > 0) {
			Node child = this.firstChild;
			while (child != null) {
				childList.add(child);
				child = child.rightSibling;
			}
		}
		return childList.iterator();
	}
}