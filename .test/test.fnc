// "\nPlease enter which to echo: \"first\" or \"second\"";

function www(what, when, where) {
  echo "What:\t" + what;
  echo "When:\t" + when;
  echo "Where:\t" + where;
}

echo "What, when, or where? ";
input inp;

unk = "Not entered";
if (inp == "What") {
  www("This", unk, unk);
}
else if (inp == "When") {
  www(unk, "Now", unk);
}
else if (inp == "Where") {
  www(unk, unk, "Here");
}
else {
  www("It " + "is " + "an " + "unknown", unk, unk);
}

if (true) {
  echo "\nSecond IF statement after the first.";
}

b = (4 * 2 + 8 / 2) / 6 - 1;
echo b;