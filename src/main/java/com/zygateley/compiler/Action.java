package com.zygateley.compiler;

import java.util.function.Function;

/**
 * Convert action to string
 * 
 * List of methods for Parser, referenced in Rules
 * 
 * @author Zachary Gateley
 *
 */
public class Action implements Function<Parser, String> {
	private final Function<Parser, String> which;
	
	public Action(Function<Parser, String> which) {
		this.which = which;
	}
	
	public static String echo(Parser p) {
		p.tokenStream.gettoken();
		TokenSymbol ts = p.tokenStream.gettoken();
		return "System.out.println(" + ts.symbol.getName() + ");";
	}

	@Override
	public String apply(Parser p) {
		// TODO Auto-generated method stub
		try {
			return this.which.apply(p);
		}
		catch (Exception e) {
			System.err.println("Fatal parse error.");
		}
		return null;
	}
}
