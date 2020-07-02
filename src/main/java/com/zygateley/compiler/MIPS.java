/**
 * 
 */
package com.zygateley.compiler;

import java.util.HashMap;

import com.zygateley.compiler.Assembler.Writer;
import com.zygateley.compiler.AssyLanguage.*;

/**
 * @author Zachary Gateley
 *
 */
public final class MIPS extends AssyLanguage {
	protected final String stackPush = "subu $sp, $sp, 4\n" +
			"sw %s, 0($sp)\n";
	/**
	 * @param io
	 * @param scope
	 * @param stack
	 * @param symbolTable
	 * @param globalVariables
	 */
	public MIPS(Writer io, Scope scope, Stack stack, SymbolTable symbolTable, HashMap<Symbol, String> globalVariables) {
		super(io, scope, stack, symbolTable, globalVariables);
		// TODO Auto-generated constructor stub
	}

	@Override
	public Variable allocateMemory(int numBytes) {
		// TODO Auto-generated method stub
		return null;
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
	public void assembleTerminal(Node leafNode) {
		// TODO Auto-generated method stub

	}

	@Override
	public void assembleGlobalString(String name, Symbol symbol) {
		io.println("%s:\t.asciiz %s", name, symbol.getValue());
	}

	@Override
	public void assembleFunctions() {
		// TODO Auto-generated method stub

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
	public Variable assemblePush(Variable variable) {
		// TODO Auto-generated method stub
		return null;
	}

}
