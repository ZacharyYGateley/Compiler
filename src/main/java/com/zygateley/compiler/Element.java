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
	/////
	// NONTERMINAL Elements
	
	// End of statement
	REFLOW_LIMIT (true),
	
	// For optimization,
	// Skip this node in raw parse tree, continue into its children
	// Most NonTerminals in a CFG should have this element type
	PASS (true),
	
	// Control
	SCOPE, IF, 
	// Definitions
	FUNCDEF, VARDEF,
	// Execution
	FUNCCALL, OPERATION,
	// IO
	OUTPUT, INPUT,
	// Temporary holding variables for clarity
	PARAMETERS (true), ARGUMENTS (true), 
	
	
	
	/////
	// TERMINAL Elements
	
	// End of branch
	NULL (true),
	
	// Logical
	OR, AND, 
	// Arithmetic
	ADD, SUB, MULT, INTDIV,
	// Comparison
	EQEQ, NEQ, LT, LTEQ, GT, GTEQ,
	// Unary 
	NOT,
	
	// Values
	VARIABLE, LITERAL, FALSE, TRUE;
	
	public final boolean isTemporary;
	
	private Element() {
		this.isTemporary = false;
	}
	private Element(boolean isTemporary) {
		this.isTemporary = isTemporary;
	}
	
	// Build language
	// Additional bindings as required
	public static enum Reflow {
		MOVE_UPWARDS_AND_LEFT,	// move source to become target's (== parent's) left sibling
		MOVE_RIGHT_TO_CHILD,	// target.insertChild(0, source)
		MOVE_LEFT_TO_CHILD, 	// target.addChild(source)
		
		@Deprecated
		MERGE_LEFT,			 	// new combined node (target & source) w/ target children then source children
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
		
		/*
		// VAROUT FUNCCALL --> FUNCCALL
		reflowBindings.add(new ReflowRelationship(
				FUNCCALL, 
				new ReflowTransformation(Reflow.MERGE_LEFT, VARIABLE, FUNCCALL)
				));
		*/
		reflowBindings.add(new ReflowRelationship(
				VARIABLE,
				// Function Name
				new ReflowTransformation(Reflow.MOVE_RIGHT_TO_CHILD, FUNCDEF, FUNCDEF),
				new ReflowTransformation(Reflow.MOVE_RIGHT_TO_CHILD, FUNCCALL, FUNCCALL),
				// Function parameters or arguments
				new ReflowTransformation(Reflow.MOVE_UPWARDS_AND_LEFT, PARAMETERS, VARIABLE),
				new ReflowTransformation(Reflow.MOVE_UPWARDS_AND_LEFT, ARGUMENTS, VARIABLE),
				// Variable name
				new ReflowTransformation(Reflow.MOVE_RIGHT_TO_CHILD, VARDEF, VARDEF),
				// If condition
				new ReflowTransformation(Reflow.MOVE_LEFT_TO_CHILD, IF, IF)
				));
		reflowBindings.add(new ReflowRelationship(
				IF,
				// Else code
				new ReflowTransformation(Reflow.MOVE_LEFT_TO_CHILD, IF, IF)
				));
		Element[] operations = new Element[] { AND, OR, ADD, SUB, MULT, INTDIV, EQEQ, NEQ, LT, GT, LTEQ, GTEQ, NOT };
		for (Element operation : operations) {
			reflowBindings.add(new ReflowRelationship(
					operation,
					// If condition
					new ReflowTransformation(Reflow.MOVE_LEFT_TO_CHILD, IF, operation),
					// Function parameters and arguments
					new ReflowTransformation(Reflow.MOVE_UPWARDS_AND_LEFT, PARAMETERS, operation),
					new ReflowTransformation(Reflow.MOVE_UPWARDS_AND_LEFT, ARGUMENTS, operation)
					));
		}
		Element[] values = new Element[] { LITERAL, FALSE, TRUE };
		for (Element valueType : values) {
			reflowBindings.add(new ReflowRelationship(
					valueType,
					// If condition
					new ReflowTransformation(Reflow.MOVE_LEFT_TO_CHILD, IF, valueType),
					// Function parameters and arguments
					new ReflowTransformation(Reflow.MOVE_UPWARDS_AND_LEFT, PARAMETERS, valueType),
					new ReflowTransformation(Reflow.MOVE_UPWARDS_AND_LEFT, ARGUMENTS, valueType)
					));
		}
		/*
		reflowBindings.add(new ReflowRelationship(
				BOOLEAN,
				// If condition
				new ReflowTransformation(Reflow.MOVE_LEFT_TO_CHILD, IF, BOOLEAN),
				// Function parameters and arguments
				new ReflowTransformation(Reflow.MOVE_UPWARDS_AND_LEFT, PARAMETERS, BOOLEAN),
				new ReflowTransformation(Reflow.MOVE_UPWARDS_AND_LEFT, ARGUMENTS, BOOLEAN)
				));
		reflowBindings.add(new ReflowRelationship(
				INTEGER,
				// If condition
				new ReflowTransformation(Reflow.MOVE_LEFT_TO_CHILD, IF, INTEGER),
				// Function parameters and arguments
				new ReflowTransformation(Reflow.MOVE_UPWARDS_AND_LEFT, PARAMETERS, INTEGER),
				new ReflowTransformation(Reflow.MOVE_UPWARDS_AND_LEFT, ARGUMENTS, INTEGER)
				));
		reflowBindings.add(new ReflowRelationship(
				STRING,
				// If condition
				new ReflowTransformation(Reflow.MOVE_LEFT_TO_CHILD, IF, STRING),
				// Function parameters and arguments
				new ReflowTransformation(Reflow.MOVE_UPWARDS_AND_LEFT, PARAMETERS, STRING),
				new ReflowTransformation(Reflow.MOVE_UPWARDS_AND_LEFT, ARGUMENTS, STRING)
				));
		*/
		reflowBindings.add(new ReflowRelationship(
				SCOPE,
				// As code corresponding to condition==true
				new ReflowTransformation(Reflow.MOVE_LEFT_TO_CHILD, IF, IF)
				));
	}
}
