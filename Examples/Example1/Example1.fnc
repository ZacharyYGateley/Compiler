// Selected functionalities demonstrated in this example:
//	Concatenating strings, storing them in the heap
// 	Working with strings of lengths unknown at compile time
//	Save input to variables
//	Outputting the concatenation of a string and a variable
//	Conditional jumps
// 	Arithmetic and boolean operations
//	Outputting numbers and boolean values, converted from binary to ASCII
// 	Integer and boolean promotion for string concatenation
//	String comparison
echo "Demonstrate string concatenation and output:\n";
var first = "First, ";
var second = "concatenate" + " a few strings" + " and store the result to a variable.";
echo first + second + "\nLater, concatenate the variable with an additional string, and output result.\n\n";

echo "What is your favorite color? ";
var color;
input color;
if (color == "green" || color == "Green" || color == "GREEN") {
	echo "I like green, too.";
}
else 	echo color + " is a nice color.";
echo "\n\n";

echo "Demonstrate arithmetic operations:\n";
echo "1 + 2 - 3 * 4 / 5 = ";
var a;
a = 1 + 2 - 3 * 4 / 5;
echo a;
echo "\n\n";

echo "Demonstrate FOR loop, boolean operations, and boolean/integer promotion to string for concatenation:\n";
var initialValue = -3;
var myOtherVariable = 5;
var comparator = -1;
// Remember that only integer division is currently supported.
for (i = initialValue to myOtherVariable/4 step 2) {
	echo i + "==" + comparator +" = " + (i==comparator) + "\n";
	echo i + "!=" + comparator +" = " + (i!=comparator) + "\n";
	echo i + "< " + comparator +" = " + (i< comparator) + "\n";
	echo i + "<=" + comparator +" = " + (i<=comparator) + "\n";
	echo i + "> " + comparator +" = " + (i> comparator) + "\n";
	echo i + ">=" + comparator +" = " + (i>=comparator) + "\n\n";
}