require ["fileinto", "reject", "tag", "flag"];

# Negative and positive
if allof (not header :contains ["to"] "band@who.com",
  header :contains ["to"] "pete@who.com") {
    fileinto "/Pete";
    stop;
}

# Positive and negative
if allof (header :contains ["to"] "band@who.com",
  not header :contains ["to"] "roger@who.com") {
    fileinto "/Roger";
    stop;
}

# Negative and negative
if allof (not header :contains ["to"] "band@who.com",
  not header :contains ["to"] "john@who.com") {
    fileinto "/Other";
    stop;
}

# Positive and positive
if allof (header :contains ["to"] "keith@who.com",
  header :contains ["to"] "band@who.com") {
    fileinto "/Keith";
    stop;
}
