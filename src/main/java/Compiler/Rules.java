package Compiler;

import java.util.ArrayList;

class Rule {
	// Each new id corresponds to initialization order
	private static int ruleCount;
	protected final int __id__;
	private final String __name__;
	private final String __first__;
	private final String __follow__;
	
	public Rule(String name, String first, String follow) {
		this.__id__ = Rule.ruleCount;
		Rule.ruleCount++;
		
		this.__name__ = name;
		this.__first__ = first;
		this.__follow__ = follow;
	}
	
	public int getId() {
		return this.__id__;
	}
	
	public String getName() {
		return this.__name__;
	}
	
	public String getFirst() {
		return this.__first__;
	}
	
	public String getFollow() {
		return this.__follow__;
	}
	
	public boolean equals(String name, String first, String follow) {
		return  this.__name__ == name &&
				this.__first__ == first &&
				this.__follow__ == follow;
	}
	public boolean equals(Rule comparator) {
		return this.__id__ == comparator.__id__;
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
	
	public Rules(String ruleFileName) {
		// Open rules file
		
		this.__rules__ = new ArrayList<Rule>();
		// Parse rules file and add to __rules__
		
		// Close rules file
	}
	
	public void add(final String name, final String first, final String follow) {
		// Check for rule existence
		int ruleIndex = this.indexOf(name, first, follow);
		if (ruleIndex < 0) {
			return;
		}
		
		Rule rule = new Rule(name, first, follow);
		this.__add__(rule);
	}
	public void add(Rule newRule) {
		int ruleIndex = this.indexOf(newRule);
		if (ruleIndex < 0) {
			return;
		}
		
		this.__add__(newRule);
	}
	private void __add__(Rule newRule) {
		this.__rules__.add(newRule);
	}
	
	public int indexOf(final String name, final String first, final String follow) {
		for (int i = 0; i < this.__rules__.size(); i++) {
			Rule rule = this.__rules__.get(i);
			if (rule.equals(name, first, follow)) {
				return i;
			}
		}
		return -1;
	}
	public int indexOf(final Rule comparator) {
		for (int i = 0; i < this.__rules__.size(); i++) {
			Rule rule = this.__rules__.get(i);
			if (rule.equals(comparator)) {
				return i;
			}
		}
		return -1;
	}
}