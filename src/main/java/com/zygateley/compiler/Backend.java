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
	 * "Compile" code into Java 
	 * @return compiled code
	 */
	public String java() {
		// Restart string builder if some parse method has already been called
		sb.setLength(0);
		ArrayList<ParseNode> top = new ArrayList<ParseNode>();
		top.add(parseTree);
		__java__all__(top);
		return sb.toString();
	}
	private void __java__all__(ArrayList<ParseNode> parseTrees) {
		for (ParseNode pn : parseTrees) {
			__java__(pn);
		}
	}
	private void __java__(ParseNode pn) {
		ArrayList<ParseNode> params = pn.getParam();
		
		// Try NonTerminal
		NonTerminal nt = pn.getRule();
		if (nt != null) {
			switch (nt) {
			case _STMT_:
				__java__all__(params);
				sb.append(";\r\n");
				break;
			case _DEF_:
				__java__all__(params);
				break;
			case _ECHO_:
				sb.append("System.out.println(");
				__java__all__(params);
				sb.append(")");
				break;
			case _EXPR_:
				// Does the expression correspond to arithmetic?
				ParseNode firstChild = params.get(0);
				Terminal nextToken = firstChild.getToken();
				char action = ' ';
				switch (nextToken) {
				case PLUS:
					action = '+';
					break;
				case MINUS:
					action = '-';
					break;
				case ASTERISK:
					action = '*';
					break;
				case SLASH:
					action = '/';
					break;
				default:
				}
				
				// Arithmetic -> Switch order
				// + a b -> a + b
				if (action != ' ') {
					sb.append("(");
					__java__(params.get(1));
					sb.append(action);
					__java__(params.get(2));
					sb.append(")");
				}
				
				// Otherwise, nothing out of the ordinary
				else {
					sb.append("(");
					__java__all__(params);
					sb.append(")");
				}
				break;
			default:
				break;
			}
			return;
		}
		
		// Try Terminal
		Terminal t = pn.getToken();
		switch (t) {
		case ECHO:
			// This handled by _ECHO_
			break;
		case VAR:
		case INT:
		case STRING:
			sb.append(pn.getSymbol().name);
			break;
		case DEF:
			sb.append(" =");
			break;
		case PAREN_OPEN:
			sb.append(" (");
			break;
		case PAREN_CLOSE:
			sb.append(") ");
			break;
		case ASTERISK:
		case SLASH:
		case PLUS:
		case MINUS:
			// This handled by _EXPR_
			break;
		default:
			// Do nothing
		}
		
		return;
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
			case _PROGRAM_:
			case _STMTS_:
			case _THEN_:
			case _ELSETHEN_:
			case _DEF_:
			case _OPEXPR_:
			case _OP_:
			case _LITERAL_:
				__pythonParseList__(params);
				break;
			case _BLOCK_:
				endLine();
				addLine("if True:");
				currentIndent++;
				__pythonParseList__(params);
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
				// Can go into elseif 
				// or into nonterminal else
				// or be empty
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
				else {
					// May be else condition
					// or may be empty string
					__pythonParseList__(params);
				}
				break;
			case _ELSE_:
				firstChild = params.get(0);
				if (firstChild.getToken() == Terminal.ELSE) {
					// Else condition exists
					endLine();
					currentIndent--;
					addLine("else:");
					currentIndent++;
					__pythonParseNode__(params.get(1));
				}
				// Otherwise, empty string. Ignore
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
				break;
			}
			return;
		}
		
		// Try Terminal
		Terminal t = pn.getToken();
		switch (t) {
		case ECHO:
			// This handled by _ECHO_
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
		case DEF:
			add(" =");
			break;
		case PAREN_OPEN:
			add(" (");
			break;
		case PAREN_CLOSE:
			add(") ");
			break;
		case ASTERISK:
		case SLASH:
		case PLUS:
		case MINUS:
		case NEQ:
		case LTEQ:
		case GTEQ:
		case LT:
		case GT:
		case EQ:
			add(" " + pn.getToken().exactString);
			break;
		default:
			// Do nothing
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
