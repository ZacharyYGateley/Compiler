package Compiler;

import java.util.ArrayDeque;

enum Token {
	EMPTY,
	LINE,
	DEF,
	EXPR,
	ECHO,
	VAR,
	LITERAL,
	EQUALS,
	PARAN_OPEN,
	PARAN_CLOSE,
	ASTERISK,
	SLASH,
	PLUS,
	MINUS;
}

public class Tokens {
	 private ArrayDeque<Token> __tokens__;
	 private int __len__;
	 private int __pos__ = 0;
	 
	 public Tokens(String s) {
	     this.__tokens__ = new ArrayDeque<Token>();
	     this.__len__ = s.length();
	     this.__pos__ = 0;
	 }
	 
	 public Token peek() {
	     if (this.isEmpty()) {
	         return Token.EMPTY;
	     }
	     return this.__tokens__.pollFirst();
	 }
	 
	 public void addtoken(Token newToken) {
		 this.__tokens__.add(newToken);
	 }
	 
	 public Token gettoken() {
	     Token next = this.peek();
	     if (next == Token.EMPTY) return next;
	     this.__pos__++;
	     return next;
	 }
	 
	 public Token contains(Token t) {
		 for (Token token : this.__tokens__) {
			 if (token.equals(t)) {
				 return token;
			 }
		 }
		 return t;
	 }
	 
	 public boolean isEmpty() {
	     return this.__len__ == this.__pos__;
	 }
	 
	 public int length() {
	     return this.__len__ - this.__pos__;
	 }
	}