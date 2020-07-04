function www(what, when, where) {
	echo "What:\t" + what;
	echo "When:\t" + when;
	echo "Where:\t" + where;
}

echo "What, when, or where? ";
input inp;

dun = "Dunno";
if (inp == "This") {
	www(inp, dun, dun);
}
else if (inp == "Now") {
	www(dun, inp, dun);
}
else if (inp == "Here") {
	www(dun, dun, inp);
}
else {
	www("I " + "wish " + "I " + "knew", dun, dun);
}

if (true) {
	echo "\nIt is working.";
}

b = (4 * 2 + 8 / 2) / 6 - 1;
echo b;