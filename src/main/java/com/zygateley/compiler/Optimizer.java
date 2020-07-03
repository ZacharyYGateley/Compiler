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
		return optimizedTree;
	}
	
	/**
	 * We are working with two trees at once.
	 * We crawl parseTree and build optimizedTree
	 * Recursion will always go to next level in parseTree
	 * But may not go to next level in optimizedTree (Element.PASS ignores parse node)
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
				if (!basicElement.equals(Element.PASS) && !basicElement.equals(Element.STOP)) {
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
				if (!basicElement.equals(Element.PASS)) {
					executeBinding(basicElement, optimizedRecursionNode);
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
	private static void executeBinding(Element type, Node source) throws Exception {
		try {
			// Leftward bindings
			Node target = source.getPreviousSibling();
			if (target != null) {
				Element leftSiblingType = (target != null ? target.getElementType() : null);
		
				// Merge this node into the next node
				// With the Element result type found from Element.bindings 
				Element resultType = Element.getMergeLeft(type, leftSiblingType);
				boolean isMergeLeft = resultType != null;
				if (isMergeLeft) {
					// If we have a matching pattern, merge the node left
					Node parent = source.getParent();
					target.pop();
					source.pop();
					
					// Create new Node with left's properties but right's Element type
					// No reason we shouldn't properly XOR negations
					// NonTerminal type no longer applies, superceded by basicElement type
					source = new Node(
							resultType, target.getParent(), 
							null, target.getToken(),
							target.getSymbol(), target.getValue(), target.isNegated() ^ source.isNegated()
							); 
					parent.addChild(source);
					
					// Add all children from target, then all children from source
					Node[] nodes = new Node[] { target, source };
					for (Node n : nodes) for (Node c : n) {
						source.addChild(c);
					}
				}
					
				// Move this element to the child of the previous sibling ?
				resultType = Element.getMergeLeftToChild(
						type, leftSiblingType
						);
				boolean isMergeLeftToChild = (resultType != null);
				if (isMergeLeftToChild) {
					// Add this element into the left sibling's children
					target.addChild(source.pop());
				}
			}
		}
		catch (Exception err) {
			throw new Exception("Fatal error: Improper binding definitions for language.");
		}
	}
}

