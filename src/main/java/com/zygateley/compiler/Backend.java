package com.zygateley.compiler;

import java.util.*;

public class Backend {
	private StringBuilder sb;
	private ParseNode parseTree;
	private int currentIndent;
	private boolean newLine;
	
	public Backend(ParseNode parseTree) {
		this.sb = new StringBuilder();
		this.parseTree = parseTree;
	}
	
	/**
	 * "Compile" code into Python 
	 * @return compiled code
	 */
	public String python() {
		// Restart string builder if some parse method has already been called
		sb.setLength(0);
		currentIndent = 0;
		__pythonParseNode__(parseTree);
		return sb.toString();
	}
	private void __pythonParseList__(ArrayList<ParseNode> parseTrees) {
		for (ParseNode pn : parseTrees) {
			__pythonParseNode__(pn);
		}
	}
	private void __pythonParseNode__(ParseNode pn) {
		ArrayList<ParseNode> params = pn.getParam();
		
		// Try NonTerminal
		NonTerminal nt = pn.getRule();
		ParseNode firstChild;
		if (nt != null) {
			switch (nt) {
			case _FUNCDEF_:
				endLine();
				add("def ");
				for (int i = 0; i < params.size() - 1; i++) {
					__pythonParseNode__(params.get(i));
				}
				addLine(":");
				currentIndent++;
				__pythonParseNode__(params.get(params.size()-1));
				currentIndent--;
				endLine();
				break;
			case _IF_:
				endLine();
				add("if");
				// Expression
				__pythonParseNode__(params.get(2));
				addLine(":");
				currentIndent++;
				__pythonParseNode__(params.get(4));
				currentIndent--;
				endLine();
				break;
			case _ELSEIF_:
				// Can go into pattern starting with terminal elseif 
				// or into pattern starting with terminal else
				// or may be empty
				firstChild = params.get(0);
				if (firstChild.getToken() == Terminal.ELSEIF) {
					// Else if condition exists
					endLine();
					currentIndent--;
					add("elif");
					__pythonParseNode__(params.get(2));
					addLine(":");
					currentIndent++;
					__pythonParseNode__(params.get(4));
				}
				else if (firstChild.getToken() == Terminal.ELSE) {
					// Else condition exists
					endLine();
					currentIndent--;
					addLine("else:");
					currentIndent++;
					__pythonParseNode__(params.get(1));
				}
				break;
			case _BLOCK_:
				endLine();
				addLine("if True:");
				currentIndent++;
				__pythonParseList__(params);
				currentIndent--;
				endLine();
				break;
			case _STMT_:
				__pythonParseList__(params);
				endLine();
				break;
			case _ECHO_:
				// Expressions all enclosed in parentheses,
				// So no need to place them here
				add("print");
				__pythonParseList__(params);
				break;
			case _EXPR_:
				add(" (");
				__pythonParseList__(params);
				add(") ");
				break;
			default:
				__pythonParseList__(params);
				break;
			}
			return;
		}
		
		// Try Terminal
		Terminal t = pn.getToken();
		switch (t) {
		case FUNCTION:
		case ECHO:
		case CURLY_OPEN:
		case CURLY_CLOSE:
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
		case VAR:
		case INT:
		case STRING:
			add(pn.getSymbol().name);
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
			add(" " + pn.getToken().exactString);
			break;
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
	private void addLine(String output) {
		add(output);
		sb.append("\r\n");
		newLine = true;
	}
	private void endLine() {
		addLine("");
	}
}
