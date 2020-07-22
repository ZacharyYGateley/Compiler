package com.zygateley.compiler;

import java.io.*;

public class PythonTranslator {
	private Node syntaxTree;
	private StringBuilder stringBuilder;
	private FileWriter fileWriter;
	private int depth;
	private boolean newLine;
	
	public PythonTranslator(Node syntaxTree) {
		this.syntaxTree = syntaxTree;
		this.stringBuilder = new StringBuilder();
	}
	public PythonTranslator(Node syntaxTree, FileWriter fileWriter) {
		this.syntaxTree = syntaxTree;
		this.fileWriter = fileWriter;
	}
	
	/**
	 * "Compile" code into Python 
	 * @return compiled code
	 */
	public String toPython() throws IOException {
		// Restart string builder if translator has already been called
		depth = 0;
		if (stringBuilder != null) {
			stringBuilder.setLength(0);
		}
		else if (fileWriter != null) {
			// Do not have access to file path from fileWriter
			// Cannot refresh stream
		}
		
		// Header
		println("# This Python code was automatically generated by ");
		println("# com.zygateley.compiler.PythonTranslator#toPython");
		println();
		
		// Crawl syntax tree
		translateNode(syntaxTree);
		
		// Return result
		if (stringBuilder != null) {
			return stringBuilder.toString();
		}
		else {
			return fileWriter.toString();
		}
	}
	private void crawlChildrenAndTranslate(Node parent) throws IOException {
		if (parent == null) return;
		for (Node child : parent) {
			if (child != null) translateNode(child);
		}
	}
	private void translateNode(Node node) throws IOException {
		if (node == null) return;
		
		
		if (node.isNegated()) {
			print("(-");
		}
		
		// Try NonTerminal
		Construct element = node.getConstruct();
		Node firstChild = (Node) node.getFirstChild();
		Node nextChild = null;
		if (firstChild != null) {
			nextChild = firstChild.getNextSibling();
		}
		int childCount = node.getChildCount();
		if (element != null) {
			switch (element) {
			case FUNCDEF:
				println();
				// Function signature
				print("def ");
				// Function name
				translateNode(firstChild);
				print("(");
				// Function parameters
				Node lastChild = node.getLastChild();
				// All following children except the last are parameters
				if (firstChild.getNextSibling() != lastChild) {
					printList(nextChild, node.getChildCount() - 2);
				}
				print(")");
				println(":");
				
				// Function body
				depth++;
				if (lastChild.getChildCount() == 0) {
					// No children
					print("pass");
				}
				else {
					// Output code
					translateNode(lastChild);
				}
				depth--;
				println();
				break;
			case LOOP:
				if (childCount == 2) {
					print("while ");
					translateNode(firstChild);
					println(":");
				}
				else {
					print("for ");
					
					// Variable
					translateNode(firstChild);
					print(" in range(");
					
					// Initial value
					translateNode(nextChild);
					print(", ");
					
					// High limit
					nextChild = nextChild.getNextSibling();
					translateNode(nextChild);
					
					// Step
					nextChild = nextChild.getNextSibling();
					if (nextChild != node.getLastChild()) {
						print(", ");
						translateNode(nextChild);
					}
					println("):");
				}

				// Body
				depth++;
				nextChild = node.getLastChild();
				if (nextChild.getChildCount() == 0) {
					print("pass");
				}
				else {
					translateNode(nextChild);
				}
				depth--;
				break;
			case IF:
				print("if ");
				// Condition
				translateNode(firstChild);
				println(":");

				// Body
				depth++;
				if (nextChild.getChildCount() == 0) {
					print("pass");
				}
				else {
					translateNode(nextChild);
				}
				depth--;
				
				// else / else if
				nextChild = nextChild.getNextSibling();
				if (nextChild != null) {
					println();
					if (Construct.IF.equals(nextChild.getConstruct())) {
						print("el");
						translateNode(nextChild);
					}
					else {
						print("else:");
						println();
						depth++;
						translateNode(nextChild);
						if (nextChild.getChildCount() == 0) {
							print("pass");
						}
						depth--;
						println();
					}
				}
				break;
			case OUTPUT:
				print("print (");
				crawlChildrenAndTranslate(node);
				println(")");
				break;
			case INPUT:
				translateNode(firstChild);
				println(" = input()");
				break;
			case OPERATION:
			case OR: case AND:
			case ADD: case SUB: case MULT: case INTDIV:
			case EQEQ: case NEQ: case LT: case LTEQ: case GT: case GTEQ:
			case NOT:
				if (nextChild == null) {
					// Unary
					printTerminal(node.getToken());
					translateNode(firstChild);
				}
				else {
					// Binary
					TypeSystem nodeType = node.getType();
					TypeSystem type0 = firstChild.getType();
					TypeSystem type1 = nextChild.getType();
					TypeSystem string = TypeSystem.STRING;
					
					print("(");
					boolean needStringConversion = nodeType == string && type0 != string;
					if (needStringConversion) {
						print("str(");
						translateNode(firstChild);
						print(")");
					}
					else {
						translateNode(firstChild);
					}
					print(" ");
					switch (element) {
					case OR: print("or"); break;
					case AND: print("and"); break;
					case ADD: print("+"); break;
					case SUB: print("-"); break;
					case MULT: print("*"); break;
					case INTDIV: print("//"); break;
					case EQEQ: print("=="); break;
					case NEQ: print("!="); break;
					case LT: print("<"); break;
					case LTEQ: print("<="); break;
					case GT: print(">"); break;
					case GTEQ: print(">="); break;
					default:
						printTerminal(node.getToken());
						break;
					}
					print(" ");
					needStringConversion = nodeType == string && type1 != string;
					if (needStringConversion) {
						print("str(");
						translateNode(nextChild);
						print(")");
					}
					else {
						translateNode(nextChild);
					}
					print(")");
				}
				break;
			case FUNCCALL:
				// Function name
				translateNode(firstChild);
				print("(");
				
				// Arguments
				// All following children are arguments
				printList(nextChild, childCount);
				
				println(")");
				break;
			case VARDECL:
				if (childCount == 1) {
					break;
				}
				// Go into varset
				firstChild = node.getFirstChild();
				nextChild = firstChild.getNextSibling();
			case VARSET:
				translateNode(firstChild);
				print(" = ");
				translateNode(nextChild);
				println();
				break;
			default:
				if (node.getChildCount() > 0) {
					crawlChildrenAndTranslate(node);
				}
				else {
					printValue(node);
				}
				break;
			}
		}
		
		if (node.isNegated()) {
			print(")");
		}
		
		return;
	}
	
	private void printList(Node nextChild, int limit) throws IOException {
		int count = 0;
		boolean isFirstArgument = true;
		while (nextChild != null && count++ < limit) {
			if (!isFirstArgument) {
				print(", ");
			}
			isFirstArgument = false;
			translateNode(nextChild);
			nextChild = nextChild.getNextSibling();
		}
	}
	
	private void print(String output) throws IOException {
		if (newLine) {
			String indent = "    ".repeat(depth);
			if (stringBuilder != null) {
				stringBuilder.append(indent);
			}
			if (fileWriter != null) {
				fileWriter.append(indent);
			}
			newLine = false;
		}
		if (stringBuilder != null) {
			stringBuilder.append(output);
		}
		if (fileWriter != null) {
			fileWriter.append(output);
		}
	}
	private void printValue(Node node) throws IOException {
		Terminal t = node.getToken();
		String value;
		switch (t) {
		case INTEGER:
			value = node.getValue();
			print(value);
			break;
		case STRING:
			print(node.getSymbol().getValue());
			break;
		case COMMENT:
			value = "#" + node.getValue().substring(2);
			print(value);
			break;
		case VARIABLE:
			print(node.getSymbol().toString());
			break;
		default:
			printTerminal(t);
			break;
		}
	}
	private void printTerminal(Terminal t) throws IOException {
		// Try Terminal
		switch (t) {
		case FUNCTION:
		case ECHO:
		case CURLY_OPEN:
		case CURLY_CLOSE:
		case SEMICOLON:
		case EMPTY:
		case EOF:
			// These handled by their wrapping NonTerminals
			break;
		case TRUE:
			print("True");
			break;
		case FALSE:
			print("False");
			break;
		case EQ:
			print(" =");
			break;
		case NEQ:
			print(" ~=");
			break;
		case AND:
			print(" and");
			break;
		case OR:
			print(" or");
			break;
		case NOT:
			print("~");
			break;
		case SLASH:
			// For the moment, it is integer division
			print("//");
			break;
		case PAREN_OPEN:
			print(" (");
			break;
		case PAREN_CLOSE:
			print(") ");
			break;
		case COMMA:
			print(", ");
			break;
		default:
			print(t.exactString);
			break;
		}
	}
	private void println(String output) throws IOException {
		print(output);
		print("\r\n");
		newLine = true;
	}
	private void println() throws IOException {
		println("");
	}
}
