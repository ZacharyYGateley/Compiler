package com.zygateley.compiler;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.instrument.IllegalClassFormatException;

import com.zygateley.compiler.Grammar.Reflow;

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
		
		// Create placeholder for optimized tree
		Node optimizedTree = new Node(Construct.SCOPE);
		
		// Build stage 1 optimized tree with REFLOW_LIMITs
		buildOptimizedTreeFrom(syntaxTree, optimizedTree);
		this.depth = 1;
		this.log("<!-- Begin: Stage 1 optimized syntax tree -->\n");
		this.depth = 0;
		this.logTree(optimizedTree, true);
		this.log("");
		this.depth = 1;
		this.log("<!-- End: Stage 1 optimized syntax tree -->\n");
		
		// Execute any reflow bindings
		crawlAndApplyReflow(optimizedTree);
		this.depth = 1;
		this.log("<!-- Begin: Stage 2 optimized syntax tree -->\n");
		this.depth = 0;
		this.logTree(optimizedTree, true);
		this.log("");
		this.depth = 1;
		this.log("<!-- End: Stage 2 optimized syntax tree -->\n");
		
		// Condense tree to optimized by removing all temporary element nodes
		// (Node.basicElement.isTemporary --> remove)
		cleanOptimizedTree(optimizedTree);
		this.depth = 1;
		this.log("");
		this.log("<!-- Begin: Final optimized syntax tree-->\n");
		this.depth = 0;
		this.logTree(optimizedTree, false);
		this.log("");
		this.depth = 1;
		this.log("<!-- End: Final optimized syntax tree -->\n");

		this.depth = 0;
		this.log("<!-- Middle stage optimization finished -->\n\n");
		
		// Remove placeholder SCOPE
		if (optimizedTree.getChildCount() != 1 || !Construct.SCOPE.equals(optimizedTree.getFirstChild().getConstruct())) {
			throw new IllegalClassFormatException("The top level of the grammar must be a SCOPE construct.");
		}
		optimizedTree = optimizedTree.getFirstChild();
		
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
		Node treeHolder = new Node(Construct.REFLOW_LIMIT);
		treeHolder.addChild(parseTree);
		buildOptimizedSubtree(treeHolder, optimizedTree, false);
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
	 * @param isNextNegated a PASS element was negated, apply to the next optimized node
	 */
	private void buildOptimizedSubtree(Node parseParentNode, Node optimizedParentNode, boolean isNextNegated) throws Exception {
		NonTerminal nonTerminal;
		Terminal terminal;
		for (Node childNodeAsBasic : parseParentNode) {
			Node parseChildNode = (Node) childNodeAsBasic;
			// Get basic element type
			Construct basicElement = parseChildNode.getConstruct();
			// Backup, element should not be null
			if (basicElement == null) {
				nonTerminal = parseChildNode.getRule();
				if (nonTerminal != null) {
					basicElement = nonTerminal.basicElement;
				}
				else {
					terminal = parseChildNode.getToken();
					if (terminal == null) {
						continue;
					}
					basicElement = terminal.construct;
				}
			}
			
			// Skip NULL (non-process leaf branch)
			if (!basicElement.equals(Construct.NULL)) {
				Node optimizedRecursionNode = null;
				if (!basicElement.equals(Construct.PASS)) {
					// This parse tree node is a basic element
					// Create new optimized node, duplicating contents of parse tree node
					Node optimizedChildNode = new Node(basicElement, optimizedParentNode, 
							parseChildNode.getRule(), parseChildNode.getToken(),
							parseChildNode.getSymbol(), parseChildNode.getValue(),
							parseChildNode.getScope(), parseChildNode.getVariable(), 
							isNextNegated);
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
					buildOptimizedSubtree(parseChildNode, optimizedRecursionNode, parseChildNode.isNegated() ^ isNextNegated);
				}
			}
		}
		return;
	}
	
	private void crawlAndApplyReflow(Node optimizedNode) throws Exception {
		if (!this.verbose || this.logFileWriter == null) return;
		
		for (Node child : optimizedNode) {
			// Execute reflow on this node, if it applies
			// Does not apply to REFLOW_LIMIT
			// (or PASS, which has already been ignored--not in tree)
			Construct basicElement = child.getConstruct();
			if (!Construct.REFLOW_LIMIT.equals(basicElement)) {
				applyReflow(child, true);
			}
			
			if (child.getChildCount() == 0) {
				// Leave node
			}
			else {
				// Open branch
				depth++;
				
				crawlAndApplyReflow(child);
				depth--;
				// Close branch
			}
		}
	}
	
	/**
	 * Apply any special bindings from Element.bindings
	 * At time of writing:
	 * 		Merge Left
	 * 			Using left optimized sibling as a target
	 * 			Merge this optimized node into the target
	 * 			Result Element type may be modified 
	 * 	
	 *		Merge Left to Child
	 *			Remove this optimized node from the tree
	 *			And add it as a child to the previous optimized node
	 * 
	 * @param type
	 * @param source
	 * @throws Exception
	 */
	private void applyReflow(final Node source, final boolean allowMerge) throws Exception {
		try {
			Construct sourceType = source.getConstruct();
			
			// Upwards bindings
			Node parent = source.getParent();
			{
				Node target = parent;
				Construct targetType = (target != null ? target.getConstruct() : null);
				
				// Move this element to the child of the next sibling?
				Construct resultType = Grammar.getReflowResult(
						Reflow.MOVE_UPWARDS_AND_LEFT,
						sourceType, targetType
						);
				boolean isUpwardsRightToChild = (resultType != null);
				if (isUpwardsRightToChild) {
					// Add this element as a left sibling of its parent
					int targetIndex = target.getParent().indexOf(target); 
					target.getParent().insertChild(targetIndex, source.pop());
					// Do not allow any additional shifting
					return;
				}
			}
			
			// Rightward bindings
			Node rightTarget = source.getNextSibling();
			if (rightTarget != null) {
				Construct targetType = (rightTarget != null ? rightTarget.getConstruct() : null);
				
				// Move this element to the child of the next sibling?
				Construct resultType = Grammar.getReflowResult(
						Reflow.MOVE_RIGHT_TO_CHILD,
						sourceType, targetType
						);
				boolean isMergeRightToChild = (resultType != null);
				if (isMergeRightToChild) {
					// Add this element as first Node in right sibling's children
					rightTarget.insertChild(0, source.pop());
					// Do not allow any additional downward shifting (toChild) merges
					// MERGE_RIGHT not defined
					/*
					// Do any right merges as necessary
					executeReflow(source, true);
					*/
					return;
				}
			}
			
			// Leftward bindings
			Node leftTarget = source.getPreviousSibling();
			if (leftTarget != null) {
				Construct targetType = (leftTarget != null ? leftTarget.getConstruct() : null);
				
				/*
				// Merge this node into the next node
				// With the Element result type found from Element.bindings
				Element resultType1 = Element.getReflowResult(
						Reflow.MERGE_LEFT, 
						sourceType, targetType
						);
				boolean isMergeLeft = resultType1 != null;
				if (allowMerge && isMergeLeft) {
					// If we have a matching pattern, merge the node left
					int indexBookmark = parent.indexOf(leftTarget);
					leftTarget.pop();
					source.pop();
					
					// Create new Node with left's properties but right's Element type
					// No reason we shouldn't properly XOR negations
					// NonTerminal type no longer applies, superceded by basicElement type
					Node mergedNode = new Node(
							resultType1, leftTarget.getParent(), 
							null, leftTarget.getToken(),
							leftTarget.getSymbol(), leftTarget.getValue(), leftTarget.isNegated() ^ source.isNegated()
							); 
					parent.insertChild(indexBookmark, mergedNode);
					
					// Add all children from target, then all children from source
					Node[] childSources = new Node[] { leftTarget, source };
					for (Node n : childSources) {
						for (Node c : n) {
							mergedNode.addChild(c);
						}
					}
					
					// Source has changed, recur executeBinding and return
					executeReflow(mergedNode, false);
					return;
				}
				*/
				
				// Move this element to the child of the previous sibling ?
				Construct resultType = Grammar.getReflowResult(
						Reflow.MOVE_LEFT_TO_CHILD,
						sourceType, targetType
						);
				boolean isMergeLeftToChild = (resultType != null);
				if (isMergeLeftToChild) {
					// Add this element into the left sibling's children
					leftTarget.addChild(source.pop());
					// Do any left merges as necessary
					// true == Do not allow any additional downward shifting (toChild) merges
					applyReflow(source, true);
					return;
				}
			}
		}
		catch (Exception err) {
			throw new Exception("Fatal error: Improper binding definitions for language.");
		}
	}
	
	/**
	 * There are a handful of Elements that are temporary (Element.isTemporary).
	 * These elements are used for reflow bindings.
	 * All reflow bindings have been executed, 
	 * so all temporary elements may now be removed.
	 * <p>
	 * REFLOW_LIMITs are used to indicate divisions between possible Element.reflowBindings<br /><br />
	 * </p>
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
		Construct nodeElement = optimizedNode.getConstruct();
		if (nodeElement.isTemporary) {
			// Temporary element found
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
	/*
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
		if (Element.OPERATION.equals(element)) {
			// Show specific operation
			Terminal terminal = optimizedNode.getToken();
			if (terminal != null && terminal.basicElement != null) {
				output.append(Node.getParameterString("operation", terminal.basicElement + ""));
			}
		}
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
		output.append(optimizedNode.getStringAllParameters());
		if (noChildren) {
			output.append(" /");
		}
		output.append(">");
		this.log(output.toString());
	}

	*/
	
	private void logTree(Node optimizedNode, boolean showToken) throws IOException {
		if (!this.verbose || this.logFileWriter == null) return;
		
		this.log(optimizedNode.asXMLTree(0, showToken));
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

