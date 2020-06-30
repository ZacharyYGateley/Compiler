package com.zygateley.compiler;

import java.util.*;

public class Translator {
	private StringBuilder sb;
	private Node syntaxTree;
	private int currentIndent;
	private boolean newLine;
	
	public Translator(Node syntaxTree) {
		this.sb = new StringBuilder();
		this.syntaxTree = syntaxTree;
	}
	
	/**
	 * "Compile" code into Python 
	 * @return compiled code
	 */
	public String toPython() {
		// Restart string builder if translator has already been called
		sb.setLength(0);
		currentIndent = 0;
		__pythonTranslateNode__(syntaxTree);
		return sb.toString();
	}
	private void __pythonCrawlList__(ArrayList<Node> syntaxNodes) {
		for (Node pn : syntaxNodes) {
			__pythonTranslateNode__(pn);
		}
	}
	private void __pythonTranslateNode__(Node pn) {
		ArrayList<Node> childNodes = pn.childNodes();
		
		if (pn.isNegated()) {
			add("(-");
		}
		
		// Try NonTerminal
		NonTerminal nt = pn.getRule();
		Node firstChild;
		if (nt != null) {
			switch (nt) {
			case _FUNCDEF_:
				endLine();
				add("def ");
				for (int i = 0; i < childNodes.size() - 1; i++) {
					__pythonTranslateNode__(childNodes.get(i));
				}
				addLine(":");
				currentIndent++;
				__pythonTranslateNode__(childNodes.get(childNodes.size()-1));
				currentIndent--;
				endLine();
				break;
			case _IF_:
				endLine();
				add("if ");
				// Expression
				__pythonTranslateNode__(childNodes.get(2));
				addLine(":");
				currentIndent++;
				__pythonTranslateNode__(childNodes.get(4));
				currentIndent--;
				endLine();
				break;
			case _ELSEIF_:
				// Can go into pattern starting with terminal elseif 
				// or into pattern starting with terminal else
				// or may be empty
				firstChild = childNodes.get(0);
				if (firstChild.getToken() == Terminal.ELSEIF) {
					// Else if condition exists
					endLine();
					currentIndent--;
					add("elif");
					__pythonTranslateNode__(childNodes.get(2));
					addLine(":");
					currentIndent++;
					__pythonTranslateNode__(childNodes.get(4));
				}
				else if (firstChild.getToken() == Terminal.ELSE) {
					// Else condition exists
					endLine();
					currentIndent--;
					addLine("else:");
					currentIndent++;
					__pythonTranslateNode__(childNodes.get(1));
				}
				break;
			case _BLOCK_:
				// Python does not contain if-then scopes, so just process normally
				__pythonCrawlList__(childNodes);
				break;
			case _STMT_:
				__pythonCrawlList__(childNodes);
				endLine();
				break;
			case _ECHO_:
				// Expressions all enclosed in parentheses,
				// So no need to place them here
				add("print (");
				__pythonCrawlList__(childNodes);
				addLine(")");
				break;
			case _INPUT_:
				__pythonTranslateNode__(childNodes.get(1));
				add(" = input() ");
				break;
			case __OP__:
				add("(");
				__pythonTranslateNode__(childNodes.get(0));
				add(" ");
				addTerminal(pn.getToken());
				add(" ");
				__pythonTranslateNode__(childNodes.get(1));
				add(")");
				break;
			default:
				__pythonCrawlList__(childNodes);
				break;
			}
		}
		// Try Terminal
		else {
			Terminal t = pn.getToken();
			switch (t) {
			case INT:
				String value = pn.getValue();
				add(value);
				break;
			case STRING:
				add(pn.getSymbol().getValue());
				break;
			case VAR:
				add(pn.getSymbol().getName());
			default:
				addTerminal(t);
				break;
			}
		}
		
		if (pn.isNegated()) {
			add(")");
		}
		
		return;
	}
	
	private void add(String output) {
		if (newLine) {
			sb.append("    ".repeat(currentIndent));
			newLine = false;
		}
		sb.append(output);
	}
	private void addTerminal(Terminal t) {
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
			add("True");
			break;
		case FALSE:
			add("False");
			break;
		case EQ:
			add(" =");
			break;
		case PAREN_OPEN:
			add(" (");
			break;
		case PAREN_CLOSE:
			add(") ");
			break;
		case COMMA:
			add(", ");
			break;
		default:
			add(t.exactString);
			break;
		}
	}
	private void addLine(String output) {
		add(output);
		sb.append("\r\n");
		newLine = true;
	}
	private void endLine() {
		addLine("");
	}
}
