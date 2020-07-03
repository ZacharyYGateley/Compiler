package com.zygateley.compiler;

import java.util.ArrayList;
import java.util.Iterator;

import com.zygateley.compiler.Basic.Element;

/**
 * Backend-readable parse tree node values
 */
public class Basic {
	public static enum Element {
		// End of branch
		NULL,
		
		// Skip this, continue into its children
		PASS,
		
		// Control
		SCOPE, IF, THEN, ELSE, 
		// Definitions
		FUNCDEF, PARAM, VARDEF,
		// IO
		OUTPUT, INPUT,
		// Execution
		CALCULATION, FUNCCALL,
		// Logical
		OR, AND, 
		// Arithmetic
		ADD, SUB, MULT, INTDIV,
		// Comparison
		EQEQ, NEQ, LT, LTEQ, GT, GTEQ, 
		// Unary 
		NOT,
		
		// Terminals
		VAROUT, LITERAL
	}
	
	public static class Node implements Iterable<Node> {
		// Name is used for more readable toString
		public String _name_;
		
		// Assembly element type
		public final Element basicElement;
		private Node parent = null;
		private Node rightSibling = null;
		private Node firstChild = null;
		private Node lastChild = null;
		private int childCount = 0;
		private boolean negated = false;
		
		// CONSTRUCTORS
		/**
		 * Non-leaf node
		 * 
		 * @param nonTerminal rule for this node
		 */
		public Node(Element basicElement, Node parent, boolean negated) {
			this.basicElement = basicElement;
			this.parent = parent;
			this.negated = negated;

			// toString override value
			this._name_ = basicElement + "";
			
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
				this.lastChild.rightSibling = newChild; 
				this.lastChild = newChild;
			}
			
			this.childCount++;
		}
		
		// Negation
		public boolean isNegated() {
			return this.negated;
		}
		public void setNegated(boolean negated) {
			this.negated = negated;
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
}