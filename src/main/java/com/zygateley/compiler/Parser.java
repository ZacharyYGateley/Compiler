package com.zygateley.compiler;


public class Parser {
	 public static boolean parse(TokenStream ts, Rules rules) {
	     // Build output
		 StringBuilder parsed = new StringBuilder();
	     
	     // Get token and consume stream until stream is empty
	     while (ts.length() > 0) {
	         // Get token
	         Token t = ts.peek().token;
	         
	         // Find first applicable rule
	         for (Rule rule : rules) {
	        	 Action action = rule.inFirst(t);
	        	 if (action != null) {
	        		 // Let Action parse starting from before token
	        		 ts.ungettoken();
	        		 // Parse this rule
	        		 parsed.append(action.apply(ts));
	        	 }
	         }
	     }
	     
	     return true;
	 }
}