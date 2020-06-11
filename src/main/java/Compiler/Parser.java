package Compiler;

import java.util.Stack;
import java.util.List;
import java.util.Arrays;


public class Parser {
	 // First characters in each production rule
	 public final static Rules __rules__ = new Rules("Rules.txt");
	 
	 public boolean isValid(String s) {
	     // Speed up string processing
	     Tokens t = new Tokens(s);
	     
	     /*
	      * From previous work, much modification needed
	      * 
	     // In case of large program, use stack
	     // to prevent recursive program stack overflow
	     Stack<Rule> program = new Stack<Rule>();
	     
	     // Get token and destruct string until string is empty
	     while (t.length() > 0) {
	         // Get token
	         Token c = t.peek();
	         
	         // Check for open rule
	         Token open_rule = t.contains(c);
	         if (open_rule > -1) {
	             t.gettoken();
	             program.push(RULES[open_rule]);
	             continue;
	         }
	         
	         // Check for closed rule
	         int close_rule = CLOSE.indexOf(c);
	         if (close_rule > -1) {
	             if (program.size() == 0) {
	                 return false;
	             }
	             Rule lastRule = program.pop();
	             if (RULES[close_rule] != lastRule) {
	                 return false;
	             }
	             // Destruct
	             t.gettoken();
	         }
	     }
	     
	     // If no syntax error up to here, string is valid
	     return program.size() == 0;
	     */
	     
	     return true;
	 }
}