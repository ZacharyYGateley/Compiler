package com.zygateley.compiler;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Basic assembly element for parse tree.
 * 
 * You can update your grammar to be any odd thing if the
 * Terminal and NonTerminal basicElement result in
 * a valid basic assembly parse tree.
 * 
 * @author Zachary Gateley
 *
 */
public enum Construct {
	/////
	// NONTERMINAL Elements
	
	// End of statement
	REFLOW_LIMIT (true),
	
	// For optimization,
	// Skip this node in raw parse tree, continue into its children
	// Most NonTerminals in a CFG should have this element type
	PASS (true),
	
	// Control
	SCOPE, LOOP, IF, 
	// Definitions
	FUNCDEF, VARDECL, 
	// Execution
	VARSET, FUNCCALL, OPERATION (true),
	// IO
	OUTPUT, INPUT,
	// Temporary holding variables for clarity
	PARAMETERS (true), ARGUMENTS (true), 
	
	
	
	/////
	// TERMINAL Elements
	
	// End of branch
	NULL (true),
	
	// Logical
	OR, AND, 
	// Arithmetic
	ADD, SUB, MULT, INTDIV,
	// Comparison
	EQEQ, NEQ, LT, LTEQ, GT, GTEQ,
	// Unary 
	NOT,
	
	// Values
	VARIABLE, LITERAL, FALSE, TRUE;
	
	public final boolean isTemporary;
	
	private Construct() {
		this.isTemporary = false;
	}
	private Construct(boolean isTemporary) {
		this.isTemporary = isTemporary;
	}
}
