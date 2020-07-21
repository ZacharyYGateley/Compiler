// Selected functionalities demonstrated in this example:
//	Concatenating strings, storing them in the heap
// 	Working with strings of lengths unknown at compile time
//	Save input to variables
//	Outputting the concatenation of a string and a variable
//	Conditional jumps
// 	Arithmetic and boolean operations
//	Outputting number, converted from binary to ASCII
//	String comparison
if (true) {
	echo "Demonstrate string concatenation and output:\n";
	var first = "First, ";
	var second = "concatenate" + " a few strings" + " and store the result to a variable.";
	echo first + second + "\nLater, concatenate the variable with an additional string, and output result.\n\n";

	echo "Anything else you would like to say? ";
	var extra;
	input extra;
	echo "\n\"" + extra + "\"\n\n";
}

echo "Demonstrate arithmetic operations:\n";
echo "1 + 2 - 3 * 4 / 5 = ";
var a;
a = 1 + 2 - 3 * 4 / 5;
echo a;
echo "\n\n";

echo "Demonstrate boolean operations:\n";
echo "-1 ? -2 ... (==, !=, <, <=, >, >=)\n";
echo -1 == -2;
echo "\n";
echo -1 != -2;
echo "\n";
echo -1 < -2;
echo "\n";
echo -1 <= -2;
echo "\n";
echo -1 > -2;
echo "\n";
echo -1 >= -2;
echo "\n\n";

echo "Demonstrate operator precedence:\n";
echo "4 >= 5 && 6 < 7 - 2 && true || !false = ";
echo 4>=5 && 6<7-2 && true || !false;
echo "\n\n";

echo "Demonstrate string comparison:";
echo "\n\"abc\" == \"def\" = ";
echo "abc" == "def";
echo "\n\"abc\" != \"def\" = ";
echo "abc" != "def";
echo "\n\"abc\" == \"abc\" = ";
echo "abc" == "abc";
echo "\n\"a\"   == \"abc\" = ";
echo "a" == "abc";
echo "\n\"abc\" == \"a\"   = ";
echo "a" == "abc";
echo "\n";