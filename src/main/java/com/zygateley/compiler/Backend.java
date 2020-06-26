package com.zygateley.compiler;

import java.util.*;

public class Backend {
	private StringBuilder sb;
	private  ArrayList<ParseNode> parseTrees;
	
	public Backend(ArrayList<ParseNode> parseTrees) {
		this.sb = new StringBuilder();
		this.parseTrees = parseTrees;
	}
	
	/**
	 * "Compile" code into Java 
	 * @return compiled code
	 */
	public String java() {
		// Restart string builder if some parse method has already been called
		sb.setLength(0);
		__java__all__(parseTrees);
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
			sb.append(pn.getSymbol().name);
			break;
		case EQUALS:
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
		__python__all__(parseTrees);
		return sb.toString();
	}
	private void __python__all__(ArrayList<ParseNode> parseTrees) {
		for (ParseNode pn : parseTrees) {
			__python__(pn);
		}
	}
	private void __python__(ParseNode pn) {
		ArrayList<ParseNode> params = pn.getParam();
		
		// Try NonTerminal
		NonTerminal nt = pn.getRule();
		if (nt != null) {
			switch (nt) {
			case _STMT_:
				__python__all__(params);
				sb.append("\r\n");
				break;
			case _DEF_:
				__python__all__(params);
				break;
			case _ECHO_:
				sb.append("print (");
				__python__all__(params);
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
					sb.append(" (");
					__python__(params.get(1));
					sb.append(action);
					__python__(params.get(2));
					sb.append(") ");
				}
				
				// Otherwise, nothing out of the ordinary
				else {
					sb.append(" (");
					__python__all__(params);
					sb.append(") ");
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
			sb.append(pn.getSymbol().name);
			break;
		case EQUALS:
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
}
