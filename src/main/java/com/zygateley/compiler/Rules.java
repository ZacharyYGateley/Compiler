package com.zygateley.compiler;

import java.util.*;

// Renaming for readability
class TokenPair {
	private final Token token;
	private final Action action;
	
	TokenPair(Token token) {
		this.token = token;
		this.action = null;
	}
	TokenPair(Token token, Action action) {
		this.token = token;
		this.action = action;
	}
	
	public Token getToken() {
		return this.token;
	}
	public Action getAction() {
		return this.action;
	}
}

/**
 * Non-terminal rule
 * 
 * @author Zachary Gateley
 *
 */
class Rule {
	// Each new id corresponds to initialization order
	private static int ruleCount;
	protected final int id;
	private final String name;
	private final TokenPair[] first;
	private final TokenPair[] follow;
	
	public Rule(
			String name, 
			TokenPair[] first, 
			TokenPair[] follow
			) {
		this.id = Rule.ruleCount;
		Rule.ruleCount++;
		
		this.name = name;
		// Do not need to copy object, assume anonymous
		this.first = first;
		this.follow = follow;
	}
	
	public int getId() {
		return this.id;
	}
	
	public String getName() {
		return this.name;
	}
	
	public Action inFirst(Token token) {
		for (TokenPair tp : this.first) {
			if (tp.getToken() == token) {
				return tp.getAction();
			}
		}
		return null;
	}
	
	public Action inFollow(Token token) {
		for (TokenPair tp : this.follow) {
			if (tp.getToken() == token) {
				return tp.getAction();
			}
		}
		return null;
	}
}

/**
 * Rule
 * 
 * All CFG (Context Free Grammar) rules stored here
 * as class variables. See CFG.xlsx.
 * 
 * @author Zachary Gateley
 *
 */
public class Rules extends ArrayList<Rule> {
	// probably should be looking into what this means...
	public static final long serialVersionUID = 10934810;
	
	public Rules() {
		Object[][][] rules = {
				{
					{
						"STMT"
					}, {
						new TokenPair(Token.VAR),
						new TokenPair(Token.ECHO),
						new TokenPair(Token.EMPTY)
					}, {
						new TokenPair(Token.EOF)
					}
				},
				{
					{
						"DEF"
					}, {
						new TokenPair(Token.VAR)
					}, {
						new TokenPair(Token.EOF)
					}
				},
				{
					{
						"EXPR"
					}, {
						new TokenPair(Token.PARAN_OPEN),
						new TokenPair(Token.VAR),
						new TokenPair(Token.INT)
					}, {
						new TokenPair(Token.EOF),
						new TokenPair(Token.PLUS),
						new TokenPair(Token.MINUS),
						new TokenPair(Token.ASTERISK),
						new TokenPair(Token.SLASH),
						new TokenPair(Token.PARAN_CLOSE)
					}
				},
				{
					{
						"EXPR0"
					}, {
						new TokenPair(Token.PARAN_OPEN),
						new TokenPair(Token.VAR),
						new TokenPair(Token.INT)
					}, {
						new TokenPair(Token.EOF),
						new TokenPair(Token.PLUS),
						new TokenPair(Token.MINUS),
						new TokenPair(Token.ASTERISK),
						new TokenPair(Token.SLASH),
						new TokenPair(Token.PARAN_CLOSE)
					}
				},
				{
					{
						"EXPR1"
					}, {
						new TokenPair(Token.PARAN_OPEN),
						new TokenPair(Token.VAR),
						new TokenPair(Token.INT)
					}, {
						new TokenPair(Token.EOF),
						new TokenPair(Token.PLUS),
						new TokenPair(Token.MINUS),
						new TokenPair(Token.ASTERISK),
						new TokenPair(Token.SLASH),
						new TokenPair(Token.PARAN_CLOSE)
					}
				},
				{
					{
						"EXPR2"
					}, {
						new TokenPair(Token.PARAN_OPEN),
						new TokenPair(Token.VAR),
						new TokenPair(Token.INT)
					}, {
						new TokenPair(Token.EOF),
						new TokenPair(Token.PLUS),
						new TokenPair(Token.MINUS),
						new TokenPair(Token.ASTERISK),
						new TokenPair(Token.SLASH),
						new TokenPair(Token.PARAN_CLOSE)
					}
				}
		};
		
		for (Object[][] rule : rules) {
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
}