package com.zygateley.compiler;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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
	SCOPE, IF, ELSEIF, ELSE, 
	// Definitions
	FUNCDEF, PARAM, VARDEF,
	// IO
	OUTPUT, INPUT,
	// Execution
	OPERATION, FUNCCALL,
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
	// Additional bindings as required
	public static enum Binding {
		// Merge this Node to the left, keeping this Node's Element type
		MERGE_LEFT,			// new combineNodes(target, source) w/ target children then source children
		MERGE_LEFT_TO_CHILD // target.addChild(source) 
	}
	// Immutable transformation
	public static class Transformation {
		public final Binding type;
		public final Element prevPrevChild;
		public final Element prevChild;
		public final Element result;
		public Transformation(Binding type, Element target, Element result) {
			this(type, null, target, result);
		}
		public Transformation(Binding type, Element prevPrevChild, Element prevChild, Element result) {
			this.type = type;
			this.prevPrevChild = prevPrevChild;
			this.prevChild = prevChild;
			this.result = result;
		}
	}
	// Immutable relationship
	private static class Relationship {
		// Element types
		// e.g. merge [source] into [target], resulting in Element type [result]
		public final Element source;
		public final Transformation[] transformations;
		public Relationship(Element source, Transformation... transformations) {
			this.source = source;
			this.transformations = transformations;
		}
	}
	
	private static Element getBindingResultType(final Binding binding, final Element source, final Element target) {
		try {
			Function<Element, Boolean> sourceLambda = (source != null) ? ((Element e) -> e == source) : (s -> true);
			Function<Binding, Boolean> bindingLambda = (binding != null) ? ((Binding b) -> b == binding) : (s -> true);
			Function<Element, Boolean> targetLambda = (target != null) ? ((Element e) -> e == target) : (s -> true);
			Relationship matchingSource = 
					bindings.stream()
					.filter((Relationship r) -> sourceLambda.apply(r.source))
					.findAny().get();
			Transformation fullMatch =
					Arrays.stream(matchingSource.transformations)
					.filter((Transformation t) -> bindingLambda.apply(t.type))
					.filter((Transformation t) -> targetLambda.apply(t.prevChild))
					.findFirst().get();
					
			return fullMatch.result;
		}
		catch (NoSuchElementException err) {
			return null;
		}
	}
	public static boolean isSpecialBinding(final Element sourceElement) {
		return getBindingResultType(null, sourceElement, null) != null;
	}
	public static Element getMergeLeft(Element source, Element target) {
		return getBindingResultType(Binding.MERGE_LEFT, source, target);
	}
	public static boolean isMergeLeft(Element source, Element target) {
		return getMergeLeft(source, target) != null;
	}
	public static Element getMergeLeftToChild(Element source, Element target) {
		return getBindingResultType(Binding.MERGE_LEFT_TO_CHILD, source, target);
	}
	public static boolean isMergeLeftToChild(Element source, Element target) {
		return getMergeLeftToChild(source, target) != null;
	}
	// In theory, relationships will be built on the fly
	// So we do not know their final length
	public static ArrayList<Relationship> bindings = new ArrayList<>();
	static {
		// Long term: add method to build these for support for multiple languages
		
		// VAROUT FUNCCALL --> FUNCCALL
		bindings.add(new Relationship(
				FUNCCALL, 
				new Transformation(Binding.MERGE_LEFT, VAROUT, FUNCCALL)
				));

		// if, else if , else
		bindings.add(new Relationship(
				IF,
				new Transformation(Binding.MERGE_LEFT_TO_CHILD, IF, IF)
				));
		bindings.add(new Relationship(
				ELSE,
				// Merge if would result in valid if, elseif
				new Transformation(Binding.MERGE_LEFT, IF, IF)
				// Otherwise, add as child to the left
				//new Transformation(Binding.MERGE_LEFT_TO_CHILD, ELSEIF, ELSE)
				));
		bindings.add(new Relationship(
				OPERATION,
				new Transformation(Binding.MERGE_LEFT_TO_CHILD, IF, IF)
				//new Transformation(Binding.MERGE_LEFT_TO_CHILD, ELSEIF, ELSEIF)
				));
		bindings.add(new Relationship(
				VAROUT,
				new Transformation(Binding.MERGE_LEFT_TO_CHILD, IF, IF)
				//new Transformation(Binding.MERGE_LEFT_TO_CHILD, ELSEIF, ELSEIF)
				));
		bindings.add(new Relationship(
				LITERAL,
				new Transformation(Binding.MERGE_LEFT_TO_CHILD, IF, IF)
				//new Transformation(Binding.MERGE_LEFT_TO_CHILD, ELSEIF, ELSEIF)
				));
		bindings.add(new Relationship(
				SCOPE,
				new Transformation(Binding.MERGE_LEFT_TO_CHILD, IF, IF),
				//new Transformation(Binding.MERGE_LEFT_TO_CHILD, ELSEIF, ELSEIF),
				new Transformation(Binding.MERGE_LEFT_TO_CHILD, ELSE, ELSE)
				));
		
		bindings.add(new Relationship(
				VARDEF,
				new Transformation(Binding.MERGE_LEFT, VAROUT, VARDEF)
				));
	}
}
