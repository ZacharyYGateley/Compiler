package Compiler;

import java.util.ArrayList;

class Rule {
	// Each new id corresponds to initialization order
	private static int ruleCount;
	private int __id__;
	private String __name__;
	
	public Rule(String name) {
		this.__id__ = Rule.ruleCount;
		Rule.ruleCount++;
		
		this.__name__ = name;
	}
	
	public int getId() {
		return this.__id__;
	}
	
	public String getName() {
		return this.__name__;
	}
}

/**
 * Rule
 * 
 * All CFG (Context Free Grammar) rules stored here
 * as class variables
 * 
 * @author Zachary Gateley
 *
 */
public class Rules {
	private ArrayList<Rule> __rules__;
	
	public Rules() {
		this.__rules__ = new ArrayList<Rule>();
	}
	
	public void add(String name) {
		// Check for rule existence
		int ruleIndex = this.indexOf(name);
		if (ruleIndex < 0) {
			return;
		}
		
		Rule rule = new Rule(name);
		this.__rules__.add(rule);
	}
	
	public int indexOf(String name) {
		for (int i = 0; i < this.__rules__.size(); i++) {
			Rule rule = this.__rules__.get(i);
			if (rule.getName().equals(name)) {
				return i;
			}
		}
		return -1;
	}
}