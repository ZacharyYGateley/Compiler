package com.zygateley.compiler;

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
	public static Node optimize(Node syntaxTree) {
		Node optimizedTree = new Node(Element.PASS, null, false);
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
	private static void crawlChildren(Node parseParentNode, Node optimizedParentNode) {
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
			if (basicElement != Element.NULL) {
				Node optimizedRecursionNode = null;
				if (basicElement != Element.PASS) {
					// Non-pass elements have optimized types
					Node optimizedChildNode = new Node(basicElement, optimizedParentNode, parseChildNode.isNegated());
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
			}
		}
		return;
	}
}
