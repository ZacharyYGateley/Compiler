package com.zygateley.compiler;

public enum TypeSystem {
	BOOLEAN,
	INTEGER,
	STRING;
	
	/**
	 * Crawl tree (depth first) and assign types to data and calculations.
	 * If types are mismatched, throw a syntax error.
	 * @param syntaxTree
	 * @return
	 */
	public static void typeAssignAndCheck(Node syntaxTree) throws SyntaxError {
		// Process depth first
		for (Node subtree : syntaxTree) {
			typeAssignAndCheck(subtree);
		}
		
		Construct basicElement = syntaxTree.getConstruct();
		Node leftChild = syntaxTree.getFirstChild();
		Node nextChild = (leftChild == null ? null : leftChild.getNextSibling());
		TypeSystem leftType = (leftChild == null ? null : leftChild.getType());
		TypeSystem nextType = (nextChild == null ? null : nextChild.getType());
		Symbol symbol;
		switch (basicElement) {
		case SCOPE:
			break;
		case LOOP:
			if (syntaxTree.getChildCount() == 2) {
				// While loop
			}
			else {
				TypeSystem type = leftChild.getType();
				if (type != null && type != INTEGER) {
					fatalError(String.format("Bad loop variable type %s (%s)", leftChild.getSymbol(), type));
				}
				leftChild.getVariable().setType(INTEGER);
			}
			break;
		case IF:
			if (leftChild == null || leftChild.getType() != BOOLEAN) {
				fatalError("Mismatched types at " + syntaxTree);
			}
			if (nextChild == null) {
				fatalError("If command has no body at " + syntaxTree);
			}
			break;
		case FUNCDEF:
			symbol = (leftChild == null ? null : leftChild.getSymbol());
			if (leftChild == null || symbol == null) {
				fatalError("No name found for function at " + syntaxTree);
			}
			if (nextChild == null) {
				fatalError("Bad function definition at " + syntaxTree);
			}
			
			// Set variable information
			symbol.setIsFunction(true);
			int parameterCount = syntaxTree.getChildCount() - 2;
			for (int i = 0; i < parameterCount; i++) {
				symbol.addParameter(nextChild.getType());
				nextChild = nextChild.getNextSibling();
			}
			break;
		case VARDECL:
			if (nextChild != null && nextChild.getType() != null) {
				leftChild.setType(nextChild.getType());
			}
			break;
		case VARSET:
			if (leftChild == null || leftChild.getVariable() == null) {
				fatalError("Bad variable definition at " + syntaxTree);
			}
			if (nextChild == null) {
				fatalError("No variable contents in variable definition at " + syntaxTree);
			}
			Variable variable = leftChild.getVariable();
			TypeSystem variableType = variable.getType();
			TypeSystem assignType = nextChild.getType();
			if (variableType != null && assignType != null && !assignType.equals(variableType)) {
				fatalError("Variable %s was initially declared as %s but is trying to be assigned type %s", variable.getSymbol(), variableType, assignType);
			}
			
			leftChild.setType(assignType);
			variable.setType(assignType);
			break;
		case FUNCCALL:
			symbol = (leftChild == null ? null : leftChild.getSymbol());
			if (leftChild == null || symbol == null || !symbol.isFunction()) {
				fatalError("Cannot call " + symbol+ ". It is not a function.");
			}
			int argumentCount = syntaxTree.getChildCount() - 1;
			if (symbol.getParameterCount() != argumentCount) {
				fatalError("Incorrect number of parameters on call to " + symbol + ".");
			}
			for (int i = 0; i < argumentCount; i++) {
				if (nextChild.getType() != symbol.getParameter(i)) {
					fatalError("Incorrect type for argument %d in call to " + symbol, i);
				}
			}
			break;
		case OUTPUT:
		case INPUT:
			if (leftChild == null) {
				fatalError("No operand at " + syntaxTree);
			}
			break;
		case ADD:
			if (leftChild == null || nextChild == null) {
				fatalError("Incorrect number of operand for operation " + syntaxTree);
			}
			// Allow automatic string promotion for concatenation
			if (leftType == STRING || nextType == STRING) {
				syntaxTree.setType(STRING);
			}
			else if (leftType != null && nextType != null && leftType != nextType) {
				fatalError("Incorrect operand types on " + syntaxTree);
			}
			break;
		case SUB:
		case MULT:
		case INTDIV:
			if (leftChild == null || nextChild == null) {
				fatalError("Incorrect number of operands for operation " + syntaxTree);
			}
			if (leftType != null && nextType != null && leftType != nextType) {
				fatalError("Incorrect operand types on " + syntaxTree);
			}
			
			// Good to go
			syntaxTree.setType((leftType != null ? leftType : nextType));
			break;
		case AND:
		case OR:
			if (leftChild == null || nextChild == null) {
				fatalError("Incorrect number of operands for operation " + syntaxTree);
			}
			if (leftType != null && leftType != BOOLEAN ||
					nextType != null && nextType != BOOLEAN) {
				fatalError("Incorrect operand types on " + syntaxTree);
			}
			
			// Good to go
			syntaxTree.setType(BOOLEAN);
			break;
		case EQEQ:
		case NEQ:
		case LT:
		case GT:
		case LTEQ:
		case GTEQ:
			if (leftChild == null || nextChild == null) {
				fatalError("Incorrect number of operands for operation " + syntaxTree);
			}
			if (leftType != null && nextType != null && leftType != nextType) {
				fatalError("Incorrect operand types on " + syntaxTree);
			}
			
			// Good to go
			syntaxTree.setType(BOOLEAN);
			break;
		case NOT:
			if (leftChild == null || nextChild != null) {
				fatalError("Incorrect number of operands for operation " + syntaxTree);
			}
			if (leftType != null && leftType != BOOLEAN) {
				fatalError("Incorrect operand type on " + syntaxTree);
			}
			
			syntaxTree.setType(BOOLEAN);
			break;
		case FALSE:
		case TRUE:
		case LITERAL:
			syntaxTree.setType(syntaxTree.getToken().type);
			break;
		case VARIABLE:
			// Set by VARDEF
			break;
		default:
			break;
		}
	}
	
	private static void fatalError(String message, Object... args) throws SyntaxError {
		if (args != null) {
			message = String.format(message, args);
		}
		throw new SyntaxError("Syntax error: " + message);
	}
}
