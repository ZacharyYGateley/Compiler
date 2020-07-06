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
	private boolean isNegated = false;
	
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
		this.isNegated = negated;

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
		return this.isNegated;
	}
	public void setNegated(boolean negated) {
		this.isNegated = negated;
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
	public Node getLastChild() {
		return this.lastChild;
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
	/**
	 * Return the position of the passed Node
	 * within this Node's children.
	 * If not found, it returns the 
	 * index of this Node's last child +1
	 * 
	 * @param childNode node to find within this Node's children
	 * @return index of this node in siblings or last position + 1 if not found
	 */
	public int indexOf(Node childNode) {
		int runningIndex = 0;
		Node childAt = this.firstChild;
		while (childAt != null) {
			if (childAt == childNode) {
				return runningIndex;
			}
			childAt = childAt.rightSibling;
			runningIndex++;
		}
		return runningIndex;
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
	/**
	 * Insert a child into this parent
	 * at the position indicated. 
	 * If a child exists at this index,
	 * push the child at the position to the right.
	 * If a child does not exist at this index,
	 * add the child to the end of the child list.
	 * 
	 * @param index
	 * @param newChild
	 */
	public void insertChild(int index, final Node newChild) {
		if (newChild == null) return;
		
		Node currentChild = getChild(index);
		if (currentChild != null) {
			// Push children right and insert
			
			// Upwards
			newChild.parent = this;
			this.childCount++;
			
			// If at beginning of children
			if (currentChild.leftSibling == null) {
				// Downwards
				this.firstChild = newChild;
				
				// Left
				// (Nothing to do)
			}
			else {
				// Downwards
				// (Nothing to do)
				
				// Left
				newChild.leftSibling = currentChild.leftSibling;
				currentChild.leftSibling.rightSibling = newChild;
			}

			// Right
			newChild.rightSibling = currentChild;
			currentChild.leftSibling = newChild;
		}
		else {
			// Append to end of list
			this.addChild(newChild);
		}
	}
	public void addRightSibling(final Node newSibling) {
		if (newSibling == null) return;
		
		// Upwards
		newSibling.parent = this.parent;
		this.parent.childCount++;
		
		// Downwards
		if (this.parent.lastChild == this) {
			this.parent.lastChild = newSibling;
		}
		
		// Leftwards
		if (this.rightSibling != null) {
			this.rightSibling.leftSibling = newSibling;
		}
		newSibling.leftSibling = this;
		
		// Rightwards
		newSibling.rightSibling = this.rightSibling;
		this.rightSibling = newSibling;
		
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
		
		// Clear siblings
		this.rightSibling = null;
		this.leftSibling = null;
		
		return this;
	}
	

	
	@Override
	public String toString() {
		return toString(true);
	}
	public String toString(boolean withChildren) {
		StringBuilder output = new StringBuilder();
		Element element = Element.NULL;
		boolean isEmpty = true;
		if (this.basicElement != null) {
			element = this.basicElement;
		}
		if (this.nonTerminal != null) {
			// NonTerminal
			output.append(nonTerminal + "");
			element = nonTerminal.basicElement;
			isEmpty = false;
		}
		if (this.terminal != null) {
			// Terminal
			if (isEmpty) {
				output.append(terminal + "");
				if (Element.NULL.equals(element) && terminal.basicElement != Element.PASS) {
					element = terminal.basicElement;
				}
			}
			else output.append(getParameterString("terminal", terminal + ""));
		}
		if (element != Element.NULL) {
			output.append(getParameterString("element", element+""));
		}
		output.append(this.getStringAllParameters());
		
		if (withChildren) {
			output.append("\n");
			output.append("Children:\n\t");
			for (Node childNode : this) {
				output.append(childNode.toString(false) + " : "); 
			}
		}
		return output.toString();
	}
	public static String getParameterString(String name, String value) {
		return " " + name + "=\"" + value.replaceAll("\"",  "&quot;") + "\"";
	}
	public String getStringAllParameters() {
		StringBuilder output = new StringBuilder();
		if (this.isNegated) {
			output.append(getParameterString("negated", "true"));
		}
		if (this.terminal != null) {
			if (symbol != null) {
				String name = symbol.getName();
				if (name != null) output.append(getParameterString("name", name));
				String value = symbol.getValue();
				if (value != null) output.append(getParameterString("value", value));
				Symbol.Type type = symbol.getType();
				if (type != null) output.append(getParameterString("type", type.toString()));
			}
			else if (this.value != null && !this.value.isBlank()) {
				output.append(getParameterString("value", this.value));
			}
		}
		else {
			
		}
		return output.toString();
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