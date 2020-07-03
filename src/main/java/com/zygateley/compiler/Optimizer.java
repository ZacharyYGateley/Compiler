package com.zygateley.compiler;

import com.zygateley.compiler.Element.Relationship;

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
		Node optimizedTree = new Node(Element.PASS);
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
				if (!basicElement.equals(Element.PASS)) {
					executeBinding(basicElement, optimizedRecursionNode);
				}
			}
		}
		return;
	}
	
	private static Element toChildLeftSource = null;
	private static void executeBinding(Element basicElement, Node optimizedNode) throws Exception {
		try {
			if (basicElement.equals(toChildLeftSource)) {
				// Add this element into the left sibling's children
				Node leftNode = optimizedNode.getPreviousSibling();
				leftNode.addChild(optimizedNode.pop());
			}
			else if (toChildLeftSource != null) {
				// Only pull immediate siblings
				toChildLeftSource = null;
			}
			
			if (Element.isSpecialBinding(basicElement)) {
				Relationship bindLeft = Element.getBindLeft(basicElement);
				Relationship pullChildRight = Element.getPullChildRight(basicElement);
				if (bindLeft != null) {
					// Combine with left
					
					// Get parent and pop target and source
					Node parent = optimizedNode.getParent();
					Node target = optimizedNode.getPreviousSibling();
					Node source = optimizedNode;
					target.pop();
					optimizedNode.pop();
					
					// Create new Node with left's properties but right's Element type
					// No reason we shouldn't properly XOR negations
					Node boundNode = new Node(
							basicElement, parent, 
							target.getRule(), target.getToken(),
							target.getSymbol(), target.getValue(), target.isNegated() ^ source.isNegated()
							); 
					parent.addChild(boundNode);
					
					// Add all children from target, then all children from source
					Node[] nodes = new Node[] { target, source };
					for (Node n : nodes) for (Node c : n) {
						boundNode.addChild(c);
					}
				}
				if (pullChildRight != null) {
					// Pull all immediate right siblings of type (Element) Relationship.target
					// into the children of this optimizedRecursionNode
					// Source right child, target into this child
					toChildLeftSource = pullChildRight.target;
				}
			}
		}
		catch (Exception err) {
			throw new Exception("Fatal error: Improper binding definitions for language.");
		}
	}
}

