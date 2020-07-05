package com.zygateley.compiler;

import java.io.FileWriter;
import java.io.IOException;

import com.zygateley.compiler.Element.Reflow;
import com.zygateley.compiler.Element.Reflow.*;

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
	private FileWriter logFileWriter;
	private boolean verbose;
	private int depth = 0;
	
	public Optimizer() {
		this(null);
	}
	public Optimizer(FileWriter logFileWriter) {
		this.logFileWriter = logFileWriter;
	}
	
	public Node optimize(Node syntaxTree) throws Exception {
		return optimize(syntaxTree, false);
	}
	public Node optimize(Node syntaxTree, boolean verbose) throws Exception {
		this.verbose = verbose;
		
		this.depth = 0;
		this.log("<!-- Middle stage optimization initiated -->\n");
		
		Node optimizedTree = new Node(Element.SCOPE);
		
		// Build intermediate optimized tree with STOPs
		crawlParseTreeBuildOptimizedTree(syntaxTree, optimizedTree);
		this.depth = 1;
		this.log("<!-- Intermediate optimized syntax tree :: -->\n");
		this.logTree(optimizedTree);
		this.log("");
		this.log("<!-- :: Intermediate optimized syntax tree -->\n");
		
		// Condense tree to optimized by removing STOPs
		amendOptimizedTreeRemoveStops(optimizedTree);

		this.depth = 1;
		this.log("");
		this.log("<!-- Fully optimized syntax tree :: -->\n");
		this.logTree(optimizedTree);
		this.log("");
		this.log("<!-- :: Fully optimized syntax tree -->\n");

		this.depth = 0;
		this.log("<!-- Middle stage optimization finished -->\n\n");
		
		return optimizedTree;
	}
	
	/**
	 * Build optimizedTree in place
	 * 
	 * Build new tree optimizedTree BESIDE parseTree
	 * 		parseTree is unmodified
	 * 		optimizedTree is built from the ground up
	 * 		see crawlChildren for rules
	 * 
	 */
	private void crawlParseTreeBuildOptimizedTree(Node parseTree, Node optimizedTree) throws Exception {
		// crawlChildren does not process parent node, 
		// so the top-level node in a tree is not processed unless
		// we explicitly make it a child to a dummy parent
		Node treeHolder = new Node(Element.STOP);
		treeHolder.addChild(parseTree);
		crawlParseChildrenBuildOptimizedSubtree(treeHolder, optimizedTree);
	}
	
	/**
	 * 
	 * Crawl the parse tree
	 * 
	 * Build new tree optimizedTree BESIDE parseTree
	 * 		parseTree is unmodified
	 * 		optimizedTree is built from the ground up using
	 * 			the following rules
	 * 		
	 * 	crawl parseTree while observing Element basicElement types
	 * 		Element.PASS:
	 * 			Skip this node
	 * 		Element.STOP:
	 * 			Add node, shows delineation between rules
	 * 		Otherwise:
	 * 			Add a new optimized node to the tree
	 * 				as a sibling to the last optimized node
	 * 				built from this same branch
	 * 			If the parse tree node has children,
	 * 				use the above rules on the parse sub tree
	 * 				adding to the children of the new optimized node
	 * 
	 * Notes: 
	 * 		Recursion will always go to next level in parseTree
	 * 			But may not go to next level in optimizedTree (Element.PASS ignores parse node)
	 * 		Temporary STOP nodes prevent improper Element.bindings
	 * 
	 * @param parseParentNode
	 * @param optimizedParentNode
	 */
	private void crawlParseChildrenBuildOptimizedSubtree(Node parseParentNode, Node optimizedParentNode) throws Exception {
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
					
					// Crawl children and add as childen to new optimized node
					optimizedRecursionNode = optimizedChildNode;
				}
				else {
					// No new optimized node here
					// Crawl children and add to optimized PARENT
					optimizedRecursionNode = optimizedParentNode;
				}
				
				
				// Output new XML structure
				// and recur (if appropriate)
				if (parseChildNode.getChildCount() > 0) {
					crawlParseChildrenBuildOptimizedSubtree(parseChildNode, optimizedRecursionNode);
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
	private void executeBinding(final Element sourceType, final Node source, final boolean allowMerge) throws Exception {
		try {
			// Leftward bindings
			Node target = source.getPreviousSibling();
			if (target != null) {
				Element leftSiblingType = (target != null ? target.getElementType() : null);
		
				// Merge this node into the next node
				// With the Element result type found from Element.bindings
				Element resultType1 = Element.getReflowResult(
						Reflow.MERGE_LEFT, 
						sourceType, leftSiblingType
						);
				boolean isMergeLeft = resultType1 != null;
				if (allowMerge && isMergeLeft) {
					// If we have a matching pattern, merge the node left
					Node parent = source.getParent();
					target.pop();
					source.pop();
					
					// Create new Node with left's properties but right's Element type
					// No reason we shouldn't properly XOR negations
					// NonTerminal type no longer applies, superceded by basicElement type
					Node mergedNode = new Node(
							resultType1, target.getParent(), 
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
					executeBinding(resultType1, mergedNode, false);
					return;
				}
					
				// Move this element to the child of the previous sibling ?
				Element resultType2 = Element.getReflowResult(
						Reflow.MERGE_LEFT_TO_CHILD,
						sourceType, leftSiblingType
						);
				boolean isMergeLeftToChild = (resultType2 != null);
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
	private Node amendOptimizedTreeRemoveStops(Node optimizedNode) { 
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
				child = amendOptimizedTreeRemoveStops(child);
			}
			nextNode = optimizedNode.getNextSibling();
		}
		return nextNode;
	}
	
	private void logTree(Node optimizedNode) throws IOException {
		if (!this.verbose || this.logFileWriter == null) return;
		
		for (Node child : optimizedNode) {
			if (child.getChildCount() == 0) {
				// Leave node
				log(child);
			}
			else {
				// Open branch
				log(child, true);
				depth++;
				logTree(child);
				depth--;
				// Close branch
				log(child, false);
			}
		}
	}
	
	private void log(Node optimizedNode) throws IOException {
		log(optimizedNode, true, true);
	}
	private void log(Node optimizedNode, boolean openNode) throws IOException {
		log(optimizedNode, openNode, false);
	}
	private void log(Node optimizedNode, boolean openNode, boolean noChildren) throws IOException {
		if (!this.verbose || this.logFileWriter == null) return;
		
		Element element = optimizedNode.getElementType();
		if (!openNode) {
			this.log("</" + element + ">");
			return;
		}
		
		StringBuilder output = new StringBuilder();
		output.append("<" + optimizedNode.getElementType() + optimizedNode.getParameterString());
		if (noChildren) {
			output.append(" /");
		}
		output.append(">");
		this.log(output.toString());
	}
		
	private void log(String message) throws IOException {
		if (!this.verbose || this.logFileWriter == null) return;
		
		StringBuilder output = new StringBuilder();
		for (int i = 0; i < depth; i++) output.append("  ");
		output.append(message);
		output.append("\n");
		if (this.verbose) {
			System.out.print(output);
		}
		if (this.logFileWriter != null) {
			this.logFileWriter.append(output);
		}
	}
}

