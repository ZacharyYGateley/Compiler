package com.zygateley.compiler;

import java.util.*;


/**
 * Rule
 * 
 * All CFG (Context Free Grammar) rules stored here
 * as class variables. See CFG.xlsx.
 * 
 * @author Zachary Gateley
 *
 */
/*
public class Rules extends ArrayList<NonTerminal> {
	// probably should be looking into what this means...
	public static final long serialVersionUID = 10934810;
	
	public Rules() {
		// From Token.java
		Token[] rules = NonTerminal.values();		
		for (Token rule : rules) {
			for (NonTerminal.Pattern pattern : rule.patterns) {
				
			}
			String name = (String) rule[0][0];
			// FIRST
			List<TokenPair> FIRST = new ArrayList<>();
			for (Object first : rule[1]) {
				FIRST.add((TokenPair) first);
			}
			// FOLLOW
			List<TokenPair> FOLLOW = new ArrayList<>(); 
			for (Object follow : rule[2]) {
				FOLLOW.add((TokenPair) follow);
			}
			
			// Skip check for rule uniqueness
			// First additions guarantee uniqueness
			Rule ruleObject = new Rule(
					name, 
					FIRST.toArray(new TokenPair[FIRST.size()]), 
					FOLLOW.toArray(new TokenPair[FOLLOW.size()]));
			this.add(ruleObject);
		}
	}
}*/