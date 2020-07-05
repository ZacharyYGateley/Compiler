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
	public static enum Reflow {
		// Merge this Node to the left, keeping this Node's Element type
		MERGE_LEFT,			// new combineNodes(target, source) w/ target children then source children
		MERGE_LEFT_TO_CHILD // target.addChild(source) 
	}
	// Immutable transformation
	private static class ReflowTransformation {
		public final Reflow type;
		public final Element prevChild;
		public final Element result;
		
		/**
		 * Create one single transformation.
		 * The source element type is not a part of this transformation.
		 * 
		 * @param type the specific Element.Reflow type corresponding to this transformation,
		 * 		which corresponds to how the source element will be transformed
		 * @param target the target element to which the ReflowRelationship will point
		 * @param result the resulting element type of the transformation
		 * 		<div style="margin-left:20px;">
		 * 		It <strong>must not be null</strong> to indicate a valid transformation.<br />
		 * 		However, the specific <i>type</i> may not *necessarily* apply. 
		 * 		For example, consider MERGE_LEFT_TO_CHILD. The Node with the 
		 * 		source Element type will be popped from the optimized tree
		 * 		and replaced as a child of the Node with the target Element type.
		 * 		Its specific result type is not used except to show that the transformation
		 * 		is valid.
		 * 		</div>
		 * 		
		 */
		public ReflowTransformation(Reflow type, Element target, Element result) {
			this.type = type;
			this.prevChild = target;
			this.result = result;
			if (result == null) {
				throw new NullPointerException("ReflowTransformation result may not be null.");
			}
		}
	}
	/**
	 * These are private, immutable reflow relationship indicators,
	 * which are built into the reflowBindings ArrayList.
	 * 
	 * Each relationship holds a source element type
	 * And all of its respective transformations
	 * 
	 * @author Zachary Gateley
	 *
	 */
	private static class ReflowRelationship {
		// Element types
		// e.g. merge [source] into [target], resulting in Element type [result]
		public final Element source;
		public final ReflowTransformation[] transformations;
		
		/**
		 * @param source the element that needs to traverse the tree in some way
		 * @param transformations all respective transformations for this source element type
		 */
		public ReflowRelationship(Element source, ReflowTransformation... transformations) {
			this.source = source;
			this.transformations = transformations;
		}
	}
	
	/**
	 * Get the specific reflow rule on this
	 * source element and target element
	 * using this specific reflow type
	 * 
	 * @param type Reflow binding type
	 * @param source the element type whose Node may need to be moved or modified into the Node of the target element
	 * @param target the element type whose Node may receive the action of the Node with the source element type
	 * @return the resulting element type of the determined, specific reflow binding rule, if it exists
	 */
	public static Element getReflowResult(final Reflow type, final Element source, final Element target) {
		try {
			Function<Element, Boolean> sourceLambda = (source != null) ? ((Element e) -> e == source) : (s -> true);
			Function<Reflow, Boolean> bindingLambda = (type != null) ? ((Reflow b) -> b == type) : (s -> true);
			Function<Element, Boolean> targetLambda = (target != null) ? ((Element e) -> e == target) : (s -> true);
			ReflowRelationship matchingSource = 
					reflowBindings.stream()
					.filter((ReflowRelationship r) -> sourceLambda.apply(r.source))
					.findAny().get();
			ReflowTransformation fullMatch =
					Arrays.stream(matchingSource.transformations)
					.filter((ReflowTransformation t) -> bindingLambda.apply(t.type))
					.filter((ReflowTransformation t) -> targetLambda.apply(t.prevChild))
					.findFirst().get();
					
			return fullMatch.result;
		}
		catch (NoSuchElementException err) {
			return null;
		}
	}
	/**
	 * Return boolean on whether this ELEMENT
	 * has special reflow bindings.
	 * 
	 * The binding might not apply to this specific NODE,
	 * so the resulting optimizer logic will have to
	 * check.
	 * 
	 * @param sourceElement
	 * @return
	 */
	public static boolean isReflow(final Element sourceElement) {
		return getReflowResult(null, sourceElement, null) != null;
	}
	
	// ADD BINDINGS
	// 		TODO: relationships will be built on the fly
	// 		So we do not know their final length
	private static ArrayList<ReflowRelationship> reflowBindings = new ArrayList<>();
	static {
		// Long term: add method to build these for support for multiple languages
		
		// VAROUT FUNCCALL --> FUNCCALL
		reflowBindings.add(new ReflowRelationship(
				FUNCCALL, 
				new ReflowTransformation(Reflow.MERGE_LEFT, VAROUT, FUNCCALL)
				));

		// if, else if , else
		reflowBindings.add(new ReflowRelationship(
				IF,
				new ReflowTransformation(Reflow.MERGE_LEFT_TO_CHILD, IF, IF)
				));
		reflowBindings.add(new ReflowRelationship(
				ELSE,
				// Merge if would result in valid if, elseif
				new ReflowTransformation(Reflow.MERGE_LEFT, IF, IF)
				// Otherwise, add as child to the left
				//new Transformation(Binding.MERGE_LEFT_TO_CHILD, ELSEIF, ELSE)
				));
		reflowBindings.add(new ReflowRelationship(
				OPERATION,
				new ReflowTransformation(Reflow.MERGE_LEFT_TO_CHILD, IF, IF)
				//new Transformation(Binding.MERGE_LEFT_TO_CHILD, ELSEIF, ELSEIF)
				));
		reflowBindings.add(new ReflowRelationship(
				VAROUT,
				new ReflowTransformation(Reflow.MERGE_LEFT_TO_CHILD, IF, IF)
				//new Transformation(Binding.MERGE_LEFT_TO_CHILD, ELSEIF, ELSEIF)
				));
		reflowBindings.add(new ReflowRelationship(
				LITERAL,
				new ReflowTransformation(Reflow.MERGE_LEFT_TO_CHILD, IF, IF)
				//new Transformation(Binding.MERGE_LEFT_TO_CHILD, ELSEIF, ELSEIF)
				));
		reflowBindings.add(new ReflowRelationship(
				SCOPE,
				new ReflowTransformation(Reflow.MERGE_LEFT_TO_CHILD, IF, IF),
				//new Transformation(Binding.MERGE_LEFT_TO_CHILD, ELSEIF, ELSEIF),
				new ReflowTransformation(Reflow.MERGE_LEFT_TO_CHILD, ELSE, ELSE)
				));
		
		reflowBindings.add(new ReflowRelationship(
				VARDEF,
				new ReflowTransformation(Reflow.MERGE_LEFT, VAROUT, VARDEF)
				));
	}
}
