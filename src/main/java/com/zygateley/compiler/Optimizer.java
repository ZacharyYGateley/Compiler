package com.zygateley.compiler;

import java.io.FileWriter;
import java.io.IOException;

import com.zygateley.compiler.Element.Reflow;

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
		buildOptimizedTreeFrom(syntaxTree, optimizedTree);
		this.depth = 1;
		this.log("<!-- Begin: Intermediate optimized syntax tree -->\n");
		this.logTree(optimizedTree, true);
		this.log("");
		this.log("<!-- End: Intermediate optimized syntax tree -->\n");
		
		// Condense tree to optimized by removing STOPs
		cleanOptimizedTree(optimizedTree);

		this.depth = 1;
		this.log("");
		this.log("<!-- Begin: Fully optimized syntax tree-->\n");
		this.logTree(optimizedTree, false);
		this.log("");
		this.log("<!-- End: Fully optimized syntax tree -->\n");

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
	private void buildOptimizedTreeFrom(Node parseTree, Node optimizedTree) throws Exception {
		// crawlChildren does not process parent node, 
		// so the top-level node in a tree is not processed unless
		// we explicitly make it a child to a dummy parent
		Node treeHolder = new Node(Element.REFLOW_LIMIT);
		treeHolder.addChild(parseTree);
		buildOptimizedSubtree(treeHolder, optimizedTree);
	}
	
	/**
	 * 
	 * Crawl the parse tree<br /><br />
	 * 
	 * Build new tree optimizedTree BESIDE parseTree<br />
	 * 1) ParseTree is unmodified.<br />
	 * 2) OptimizedTree is built from the ground up using
	 * 			the following rules.
	 * <ul>
	 * <li>Crawl parseTree while observing Element basicElement types
	 * 		<ul>
	 * 		<li>
	 * 		Element.PASS:
	 * 			Skip this node
	 * 		</li>
	 * 		<li>
	 * 		Element.REFLOW_LIMIT:
	 * 			New node in optimized tree
	 * 				No reflow bindings may traverse this node
	 * 		</li>
	 * 		<li>
	 * 		Otherwise: 
	 * 			New node in optimized tree
	 * 
	 * 		</li>
	 * 		</ul
	 * </li>
	 * <li>Schema :: new nodes in the optimized tree
	 * 		<ul>
	 * 		<li> 
	 * 		Direct descendents in parse tree
	 * 			correspond to direct discendents in optimized tree
	 * 		</li>
	 * 		<li>
	 * 		First cousins in parse tree, 
	 * 			no matter how many times removed
	 * 			correspond to siblings in optimized tree
	 * 
	 * 		</li>
	 * 		</ul>
	 * </li>
	 * </ul>
	 * <br />
	 * <strong>Additional notes:</strong>
	 * <ul>
	 * <li> 
	 * 		Recursion will always go to next level in parseTree
	 * 			but may not go to next level in optimizedTree (Element.PASS ignores parse node)
	 * </li>
	 * <li>
	 * 		Temporary STOP nodes prevent improper Element.bindings
	 * </li>
	 * </ul>
	 * 
	 * @param parseParentNode subtree corresponding to a node from the parse tree
	 * @param optimizedParentNode node from the optimized tree to build in place
	 */
	private void buildOptimizedSubtree(Node parseParentNode, Node optimizedParentNode) throws Exception {
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
					buildOptimizedSubtree(parseChildNode, optimizedRecursionNode);
				}
				
				
				// If this Element type has a special binding,
				// execute binding as necessary
				// Does not apply to PASS or STOP
				if (!Element.PASS.equals(basicElement) && !Element.REFLOW_LIMIT.equals(basicElement)) {
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
	 * REFLOW_LIMITs are used to indicate divisions between possible Element.reflowBindings<br /><br />
	 * 
	 * For example:
	 * <pre>
	 * 		IF     VAROUT
	 * 	  /  |  \
	 *   ..  ..  ..
	 * </pre>
	 * <br />
	 *   VAROUT will merge into IF as a child.
	 *   If we do not want this, we place a stop.
	 * <br />
	 *
	 * <pre>
	 * 		 REFLOW_LIMIT
	 * 		  /        \
	 * 		IF        VAROUT
	 * 	  /  |  \
	 *   ..  ..  ..
	 * </pre>
	 * <br />
	 *   Now the pattern (IF <-- VAROUT) does not exist.
	 *   This method removes all stops from the tree
	 *   
	 * @param optimizedNode node corresponding to root of subtree of optimized tree
	 */
	private Node cleanOptimizedTree(Node optimizedNode) { 
		Node nextNode;
		Element nodeElement = optimizedNode.getElementType();
		if (Element.REFLOW_LIMIT.equals(nodeElement)) {
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
				child = cleanOptimizedTree(child);
			}
			nextNode = optimizedNode.getNextSibling();
		}
		return nextNode;
	}
	
	/**
	 * Output the XML structure of this tree in indented format. <br />
	 * Depends on Optimizer.depth to be accurate.
	 * 
	 * @param optimizedNode node corresponding to root of subtree in optimized tree 
	 * @param showToken true: show Terminal or NonTerminal which correspond to node
	 * @throws IOException
	 */
	private void logTree(Node optimizedNode, boolean showToken) throws IOException {
		if (!this.verbose || this.logFileWriter == null) return;
		
		for (Node child : optimizedNode) {
			if (child.getChildCount() == 0) {
				// Leave node
				log(child, showToken);
			}
			else {
				// Open branch
				log(child, true, showToken);
				depth++;
				logTree(child, showToken);
				depth--;
				// Close branch
				log(child, false, showToken);
			}
		}
	}
	
	private void log(Node optimizedNode, boolean showToken) throws IOException {
		// Terminal node
		log(optimizedNode, true, true, showToken);
	}
	private void log(Node optimizedNode, boolean openNode, boolean showToken) throws IOException {
		// NonTerminal node
		log(optimizedNode, openNode, false, showToken);
	}
	private void log(Node optimizedNode, boolean openNode, boolean noChildren, boolean showToken) throws IOException {
		if (!this.verbose || this.logFileWriter == null) return;
		
		Element element = optimizedNode.getElementType();
		if (!openNode) {
			this.log("</" + element + ">");
			return;
		}
		
		StringBuilder output = new StringBuilder();
		output.append("<" + optimizedNode.getElementType());
		if (showToken) {
			String tokenName = "";
			String tokenValue = "";
			NonTerminal nonTerminal = optimizedNode.getRule();
			if (nonTerminal != null) {
				tokenName = "NonTerminal";
				tokenValue = nonTerminal + "";
			}
			Terminal terminal = optimizedNode.getToken();
			if (terminal != null) {
				tokenName = "Terminal";
				tokenValue = terminal + "";
			}
			if (!tokenName.isBlank()) {
				output.append(Node.getParameterString(tokenName, tokenValue));
			}
		}
		output.append(optimizedNode.getAllParametersString());
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

