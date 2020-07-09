// Function definitions have JavaScript style
function console_out(first, second, third, which) {
	// Conditions must be included in parentheses
	if (which == "first") {
		// Output has a shell or php style, without the dollar signs
		echo first;
	}
	else if (which == "second") {
		// if, else if, and else follow Java syntax
		echo second;
	}
	// Conditional blocks can be enclosed in curly braces or can be single lines
	else echo third;
}

// All statements must end in a semicolon
echo "\nPlease enter which to echo: \"first\" or \"second\"";

// Input follows the style of dBase
input which;

_selection_ = "\nYou selected ";
selection_1 = _selection_ + "first.";
selection_2 = _selection_ + "second.";

// Function calls include parentheses
// If there are parameters, they are comma delimited
// Arguments may be expressions instead of just literals or variables
console_out(selection_1, selection_2, "\nWe " + "don't know what that is.", which);

// There exist few operators, but for those that are included,
// order of operations holds
calc = 1 *2 + -3 / (6 - -4);
echo "\nSimple calculation: ";
echo calc;