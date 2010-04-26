require ["fileinto", "reject", "tag", "flag"];

# Negative and positive test
if allof (not header :contains "to" "pete@who.com",
  header :contains "to" "pete@who.com") {
    fileinto "/Pete";
    stop;
}
