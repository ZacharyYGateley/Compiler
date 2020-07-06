# Compiler / Translator

The first and main goal of this project is to present a working compiler from a simple made-up language. To such an end, there is a **front end**, a **middle stage**, and a **back end**. 

The **front end** is composed of a <u>lexer</u> and a <u>parser</u>. The lexer reads predefined tokens from a file and builds a stream that the parser can read. Additionally, any variables or string literals found are added to a symbol table that is used throughout the compilation process. The parser reads through the token stream and builds a syntax tree out of the stream. There are two different sets of rules for the parser. One set is a context free grammar (CFG), which relies on first-token sets that dictate unique left-to-right pathways through any valid token stream. The other set is also a CFG, but it is instead dictated by unique middle tokens. This second set allows for appropriate application of precedence rules in expressions.

The **middle stage** crawls the bulky syntax tree that results from the parser and builds an optimized, bare-bones syntax tree that represents the entirety of the program without containing any elements that will not be used by the back end. This is achieved by binding predefined elements that will be used by the compiler with different parse rules and tokens. In this way, the syntax tree can be crawled to find these basic elements, building a valid, optimized tree meanwhile. To accommodate grammar rules that do not lend themselves easily to syntax trees as the back-end expects them, the language can define reflow rules adjust the primary syntax tree to the final optimized counterpart. 

The **back end** is composed of an <u>assembler</u> and an interface with the <u>compiler of the assembler</u>. This stage is TBD. At the moment, the aim is to create a generalized assembler with links to assembly language Java classes, which with carefully chosen methods can output the code from the assembler without knowing the specific assembly language in question. Finally, the assembly language in question will indicate the assembly compiler command line directive, which will generate the final executable file.

To assist in development, there is also a Python translator. 

<br />

## See also

* There is a [full listing of the grammar](README/LANGUAGE.md "Prepackaged Language Information") for the prepackaged language with all sorts of interesting additional information on how parsing occurs.
* The [grammar development spreadsheet](README/CFG.xlsx "Grammar Development Spreadsheet") can also be found in the README folder.
* Finally, there is a [collection of flow diagrams](README/CFG.drawio "Grammar Flow Diagrams") that can be opened with [draw.io (external link)](http://draw.io). The results from this can be found found on the [grammar explanation page](README/LANGUAGE.md "Prepackaged Language Information").

## Goals
Below are listed the goals of the program.

1. Create a working compiler corresponding to a simple, made-up language.

2. Allow for custom-designed of language plug-ins, so that the compiler can work with any language, given a properly-designed plug-in. This would include:

<div style="margin-left:40px;">
<ul>
<li>Tokens as the designer desires.</li>
<li>Any CFG rules to utilize the custom token set, with their relations to the elemental code constructs.</li>
</ul>
</div>


3. Allow for the selection of any back-end compiler, given a properly-created assembly language plug-in.

<br />

## Front End

### Lexer

Consider the following code.

	// Set a variable to a value then output it
	b = "Hello, World";
	echo b;

The lexer will step through the code file and convert it to tokens. Along the way, it will add any variables to the symbol table. Additionally, it adds strings to the symbol table, which will later be gathered into a global string pool. 

After the lexer has run, there will be a stream of tokens ready to be read. For the above code, it is represented by the following.

	Token: COMMENT   	( // Set a variable to a value then output it )
	Token: VAR       	( b )
	Token: EQ        	( = )
	Token: STRING    	( "Hello, World" )
	Token: SEMICOLON 	( ; )
	Token: ECHO      	( echo )
	Token: VAR       	( b )
	Token: SEMICOLON 	( ; )
	Token: EOF       	(   )

Of course the tokens found by the lexer do not represent the actual language constructs. They are raw building blocks from which the parser can construct a syntax tree.

### Parser

The parser will then take the above tokens, and using the CFG rules, will construct a raw syntax tree. For the above code, it can be represented by the following XML structure.

	// Raw parse tree
	
	<_PROGRAM_ element="SCOPE">
	  <_STMTS_ element="REFLOW_LIMIT">
	    <_BLOCKSTMT_ element="PASS">
	      <_STMT_ element="PASS">
	      <COMMENT value="// Set a variable to a value then output it" />
	      </_STMT_>
	    </_BLOCKSTMT_>
	    <_STMTS_ element="REFLOW_LIMIT">
	      <_BLOCKSTMT_ element="PASS">
	        <_STMT_ element="PASS">
	        <VAR element="VAROUT" name="b" />
	          <_VARSTMT_ element="PASS">
	            <_VARDEF_ element="VARDEF">
	            <EQ value="=" />
	              <_EXPR_ element="PASS">
	                <_VALUEOREXPR_ element="PASS">
	                  <_VALUE_ element="PASS">
	                    <_LITERAL_ element="PASS">
	                    <STRING element="LITERAL" value="&quot;Hello, World&quot;" type="STRING" />
	                    </_LITERAL_>
	                  </_VALUE_>
	                </_VALUEOREXPR_>
	              </_EXPR_>
	            </_VARDEF_>
	          </_VARSTMT_>
	        <SEMICOLON value=";" />
	        </_STMT_>
	      </_BLOCKSTMT_>
	      <_STMTS_ element="REFLOW_LIMIT">
	        <_BLOCKSTMT_ element="PASS">
	          <_STMT_ element="PASS">
	            <_ECHO_ element="OUTPUT">
	            <ECHO value="echo" />
	              <_EXPR_ element="PASS">
	                <_VALUEOREXPR_ element="PASS">
	                  <_VALUE_ element="PASS">
	                    <_VAR_ element="PASS">
	                    <VAR element="VAROUT" name="b" />
	                      <VAREXPR />
	                    </_VAR_>
	                  </_VALUE_>
	                </_VALUEOREXPR_>
	              </_EXPR_>
	            </_ECHO_>
	          <SEMICOLON value=";" />
	          </_STMT_>
	        </_BLOCKSTMT_>
	        <STMTS />
	      </_STMTS_>
	    </_STMTS_>
	  </_STMTS_>
	</_PROGRAM_>

That is a bulky structure for such a simple program! However, this is an unambiguous syntax tree that results directly from the CFG rules set for this language. Nevertheless, it would be quite a challenge for an assembler to make sense of such a bulky structure. For this reason, we create an optimizer to clean the tree before passing it to the assembler.

For more information on the grammar, see GRAMMAR.md.

<br />

## Middle Stage

### Optimizer

You may have noticed that some of the XML elements above have a parameter called "element". This is the key to optimization. CFG rules bind to elemental programming concepts that the assembler can understand. The optimizer crawls the syntax tree, finds these elements, and builds a simplified tree from them. 

	// Optimized syntax tree, stage 1
	
	<SCOPE NonTerminal="_PROGRAM_">
	  <REFLOW_LIMIT NonTerminal="_STMTS_">
	    <REFLOW_LIMIT NonTerminal="_STMTS_">
	      <VARDEF Terminal="VAR" name="b">
	        <LITERAL Terminal="STRING" value="&quot;Hello, World&quot;" type="STRING" />
	      </VARDEF>
	      <REFLOW_LIMIT NonTerminal="_STMTS_">
	        <OUTPUT NonTerminal="_ECHO_">
	          <VAROUT Terminal="VAR" name="b" />
	        </OUTPUT>
	      </REFLOW_LIMIT>
	    </REFLOW_LIMIT>
	  </REFLOW_LIMIT>
	</SCOPE>

This is certainly better, but what are these `<REFLOW_LIMIT>` elements? There are certain language constructs that do not lend themselves nicely to an easy-to-assemble tree. Thus, languages may be retrofitted with "reflow bindings". These are rules that tell the optimizer how to rearrange the syntax tree such that the language constructs are properly understood. These reflow rules have now been used, so the optimizer removes them for a final optimized tree.

	// Optimized syntax tree, final stage
	
	<SCOPE>
	  <VARDEF name="b">
	    <LITERAL value="&quot;Hello, World&quot;" type="STRING" />
	  </VARDEF>
	  <OUTPUT>
	    <VAROUT name="b" />
	  </OUTPUT>
	</SCOPE>

What an improvement over the raw syntax tree that resulted directly from the parser! This tree is ready to go on to the next stage.

<br />

## Back End

This step is currently in progress. It is being built with 

[GoAsm]: http://www.godevtool.com/	"GoAsm Homepage"

, which is compilable into Windows executables as well as linux executables. To be updated!

<br />

## Translator

To assist in development, a Python translator was created. The above code will result in the following code. It's about as simple as you can get, but it proves (or disproves) the accuracy of the syntax tree!

	b = "Hello, World"
	print (b)


## In Development

There are quite a few items still to be done. See the .TODO file for an updated list. 