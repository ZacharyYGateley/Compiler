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
public class MIPS extends AssyLanguage {

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
	public Variable allocateMemory() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Variable assembleCalculation() {
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
	public void assembleTerminal(Node node) {
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

}
