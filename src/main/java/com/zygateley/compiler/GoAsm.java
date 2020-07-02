/**
 * 
 */
package com.zygateley.compiler;

import java.util.HashMap;

import com.zygateley.compiler.Assembler.Writer;

/**
 * @author Zachary Gateley
 *
 */
public final class GoAsm extends AssyLanguage {
	// Since EAX is used as a return value, do not allow it auto allocate
	protected String[] tempRegisters = new String[] { "ebx", "ecx", "edx", "esi", "edi" };
	
	/**
	 * @param io
	 * @param scope
	 * @param stack
	 * @param symbolTable
	 * @param globalVariables
	 */
	public GoAsm(Writer io, Scope scope, Stack stack, SymbolTable symbolTable,
			HashMap<Symbol, String> globalVariables) {
		super(io, scope, stack, symbolTable, globalVariables);
		// TODO Auto-generated constructor stub
	}

	@Override
	public Variable assembleCalculation(Node operation) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Variable assembleExpression(Node parseTree) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Variable assembleFooter() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Variable assembleHeader() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Variable assembleInputHandle() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Variable assembleOutputHandle() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void assembleTerminal(Node leafNode) {
		// TODO Auto-generated method stub

	}

	@Override
	public void assembleGlobalString(String name, Symbol symbol) {
		// TODO Auto-generated method stub

	}

	@Override
	public void assembleFunctions() {
		// TODO Auto-generated method stub

	}

	@Override
	public Variable assemblePush(Variable variable) {
		// TODO Auto-generated method stub
		return null;
	}

}
