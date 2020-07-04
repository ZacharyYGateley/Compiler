package com.zygateley.compiler;

import java.util.*;
import com.zygateley.compiler.Element.Transformation;

/**
 * Crawl syntaxTree<Node> looking for BasicToken.Elements
 * 	For each element, 
 * 		loop all children
 * 			if child.basicElement == Element.PASS
 * 				recur 
 * 			Otherwise, if child.basicElement != Element.NULL
 * 				Create a BasicNode for the Node
 * 				Add the BasicNode to the parent BasicNode
 * 				Recur into the BasicNode's children
 * 
 * Using these rules, build a new, concise syntax tree syntaxTree<BasicNode>
 * @author Zachary Gateley
 *
 */
public class Optimizer {
	public static Node optimize(Node syntaxTree) throws Exception {
		Node optimizedTree = new Node(Element.SCOPE);
		crawlChildren(syntaxTree, optimizedTree);
		removeStops(optimizedTree);
		return optimizedTree;
	}
	
	/**
	 * We are working with two trees at once.
	 * We crawl parseTree and build optimizedTree
	 * Recursion will always go to next level in parseTree
	 * But may not go to next level in optimizedTree (Element.PASS ignores parse node)
	 * Adds temporary STOP nodes to prevent improper Element.bindings
	 * 
	 * @param parseParentNode
	 * @param optimizedParentNode
	 */
	private static void crawlChildren(Node parseParentNode, Node optimizedParentNode) throws Exception {
		NonTerminal nonTerminal;
		Terminal terminal;
		for (Node childNodeAsBasic : parseParentNode) {
			Node parseChildNode = (Node) childNodeAsBasic;
			// Get basic element type
			Element basicElement;
			nonTerminal = parseChildNode.getRule();
			if (nonTerminal != null) {
				basicElement = nonTerminal.basicElement;
			}
			else {
				terminal = parseChildNode.getToken();
				if (terminal == null) {
					continue;
				}
				basicElement = terminal.basicElement;
			}
			
			// Skip NULL (non-process leaf branch) 
			if (!basicElement.equals(Element.NULL)) {
				Node optimizedRecursionNode = null;
				if (!basicElement.equals(Element.PASS)) {
					// This parse tree node is a basic element
					// Create new optimized node, duplicating contents of parse tree node
					Node optimizedChildNode = new Node(basicElement, optimizedParentNode, 
							parseChildNode.getRule(), parseChildNode.getToken(),
							parseChildNode.getSymbol(), parseChildNode.getValue(),
							parseChildNode.isNegated());
					optimizedParentNode.addChild(optimizedChildNode);
					
					// Crawl children and add to optimized CHILD
					optimizedRecursionNode = optimizedChildNode;
				}
				else {
					// No new optimized node here
					// Crawl children and add to optimized PARENT
					optimizedRecursionNode = optimizedParentNode;
				}
				
				// If this is not a leaf, recur
				if (parseChildNode.getChildCount() > 0) {
					crawlChildren(parseChildNode, optimizedRecursionNode);
				}
				
				// If this Element type has a special binding,
				// execute binding as necessary
				// Does not apply to PASS or STOP
				if (!Element.PASS.equals(basicElement) && !Element.STOP.equals(basicElement)) {
					executeBinding(basicElement, optimizedRecursionNode, true);
				}
			}
		}
		return;
	}
	
	/**
	 * Execute any special bindings from Element.bindings
	 * At time of writing:
	 * 		Merge Left
	 * 			Using left optimized sibling as a target
	 * 			Merge this optimized node into the target
	 * 			Result Elemen type may be modified 
	 * 	
	 *		Merge Left to Child
	 *			Remove this optimized node from the tree
	 *			And add it as a child to the previous optimized node
	 * 
	 * @param type
	 * @param source
	 * @throws Exception
	 */
	private static void executeBinding(final Element sourceType, final Node source, final boolean allowMerge) throws Exception {
		try {
			// Leftward bindings
			Node target = source.getPreviousSibling();
			if (target != null) {
				Element leftSiblingType = (target != null ? target.getElementType() : null);
		
				// Merge this node into the next node
				// With the Element result type found from Element.bindings 
				Element resultType = Element.getMergeLeft(sourceType, leftSiblingType);
				boolean isMergeLeft = resultType != null;
				if (allowMerge && isMergeLeft) {
					// If we have a matching pattern, merge the node left
					Node parent = source.getParent();
					target.pop();
					source.pop();
					
					// Create new Node with left's properties but right's Element type
					// No reason we shouldn't properly XOR negations
					// NonTerminal type no longer applies, superceded by basicElement type
					Node mergedNode = new Node(
							resultType, target.getParent(), 
							null, target.getToken(),
							target.getSymbol(), target.getValue(), target.isNegated() ^ source.isNegated()
							); 
					parent.addChild(mergedNode);
					
					// Add all children from target, then all children from source
					Node[] nodes = new Node[] { target, source };
					for (Node n : nodes) {
						for (Node c : n) {
							mergedNode.addChild(c);
						}
					}
					
					// Source has changed, recur executeBinding and return
					executeBinding(resultType, mergedNode, false);
					return;
				}
					
				// Move this element to the child of the previous sibling ?
				resultType = Element.getMergeLeftToChild(
						sourceType, leftSiblingType
						);
				boolean isMergeLeftToChild = (resultType != null);
				if (isMergeLeftToChild) {
					// Add this element into the left sibling's children
					target.addChild(source.pop());
					// Do any left merges as necessary
					// Do not allow any downward shifting (toChild) merges
					executeBinding(sourceType, source, true);
					return;
				}
			}
		}
		catch (Exception err) {
			throw new Exception("Fatal error: Improper binding definitions for language.");
		}
	}
	
	/**
	 * Stops are used to indicate divisions between possible Element.bindings
	 * For example:
	 * 		IF     VAROUT
	 * 	  /  |  \
	 *   ..  ..  ..
	 *   
	 *   VAROUT will merge into IF as a child
	 *   But we do not want this, so we place a stop
	 *
	 * 		IF     STOP    VAROUT
	 * 	  /  |  \
	 *   ..  ..  ..
	 *   
	 *   Now the pattern (IF <-- VAROUT) does not exist.
	 *   This method removes all stops from the tree
	 *   
	 * @param optimizedNode
	 */
	private static Node removeStops(Node optimizedNode) { 
		Node nextNode;
		Element nodeElement = optimizedNode.getElementType();
		if (Element.STOP.equals(nodeElement)) {
			// Stop element found
			// Remove all children and place as right siblings to STOP
			Node lastChild = optimizedNode.getLastChild();
			while (lastChild != null) {
				// Pop last child from node
				// Add as the immediate right sibling of STOP
				// Builds children into siblings right to left
				optimizedNode.addRightSibling(lastChild.pop());
				lastChild = optimizedNode.getLastChild();
			}
			nextNode = optimizedNode.getNextSibling();
			// All children have been made right siblings in order
			// Remove STOP
			optimizedNode.pop();
		}
		else {
			// Remove all STOP progeny from non-STOP node
			Node child = optimizedNode.getFirstChild();
			while (child != null) {
				child = removeStops(child);
			}
			nextNode = optimizedNode.getNextSibling();
		}
		return nextNode;
	}
}

