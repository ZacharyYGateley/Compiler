function inTheBeginning(what) {
	echo what;
}

if (true) {
	inTheBeginning("abc");
}
else if (false) {
	inTheBeginning("abc" + "3");
}
else {
	input what;
	inTheBeginning(what);
}