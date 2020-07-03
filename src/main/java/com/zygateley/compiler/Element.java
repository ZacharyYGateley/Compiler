package com.zygateley.compiler;

import java.util.*;

/**
 * Basic assembly element for parse tree.
 * 
 * You can update your grammar to be any odd thing if the
 * Terminal and NonTerminal basicElement result in
 * a valid basic assembly parse tree.
 * 
 * @author Zachary Gateley
 *
 */
public enum Element {
	// End of statement
	STOP,
	
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
	VAROUT, LITERAL;
	
	// Build language
	// Immutable relationship
	public static class Relationship {
		public final Element source;
		public final Element target;
		public Relationship(Element source, Element target) {
			this.source = source;
			this.target = target;
		}
	}
	
	// In theory, relationships will be built on the fly
	// So we do not know their final length
	public static ArrayList<Relationship> bindLeft = new ArrayList<>();
	public static ArrayList<Relationship> pullChildRight = new ArrayList<>();
	static {
		// Long term: add method to build these for support for multiple languages
		
		// Indicates a function call is preceded by its name in same rule
		bindLeft.add(new Relationship(FUNCCALL, VAROUT));
	}
	public static boolean isSpecialBinding(final Element element) {
		return (getBindLeft(element) != null) || (getPullChildRight(element) != null);
	}
	private static Relationship getBinding(final Element sourceElement, ArrayList<Relationship> comparator) {
		try {
			return comparator.stream().filter((Relationship r) -> r.source == sourceElement).findAny().get();
		}
		catch (NoSuchElementException err) {
			return null;
		}
	}
	public static Relationship getBindLeft(Element sourceElement) {
		return getBinding(sourceElement, bindLeft);
	}
	public static Relationship getPullChildRight(Element sourceElement) {
		return getBinding(sourceElement, pullChildRight);
	}
}
