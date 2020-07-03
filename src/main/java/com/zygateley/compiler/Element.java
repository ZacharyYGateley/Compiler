package com.zygateley.compiler;

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
public enum Element {
	// End of branch
	NULL,
	
	// Skip this, continue into its children
	PASS,
	
	// Control
	SCOPE, IF, THEN, ELSE, 
	// Definitions
	FUNCDEF, PARAM, VARDEF,
	// IO
	OUTPUT, INPUT,
	// Execution
	CALCULATION, FUNCCALL,
	// Logical
	OR, AND, 
	// Arithmetic
	ADD, SUB, MULT, INTDIV,
	// Comparison
	EQEQ, NEQ, LT, LTEQ, GT, GTEQ, 
	// Unary 
	NOT,
	
	// Terminals
	VAROUT, LITERAL
}