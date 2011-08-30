require ["fileinto", "reject", "tag", "flag"];

# Header, anyof, enabled
if anyof (header :contains ["to"] "devel-db",
 header :contains ["cc"] "devel-db")
{
    fileinto "devel-db";
    stop;
}
# Header, allof, disabled
disabled_if allof (header :contains ["to"] "james.apache.org",
 header :contains ["cc"] "james.apache.org")
{
    fileInto "jSieve";
    stop;
}
# Exists
if allof (exists "Return-Path")
{
    discard;
}
# Size, not under
if allof (not size :under 512000)
{
    fileinto "big";
    stop;
}
# Size, over
if allof (not size :over 2097152)
{
    fileinto "bigger";
    stop;
}
# Date, before
if allof (date :before "20080101")
{
    flag "read";
}
# Date, after
if allof (date :after "20101006")
{
    tag "sayonara";
}
# Body
if allof (body :contains "wookie")
{
    redirect "chewbacca@starwars.com";
}
# Attachment
if allof (attachment)
{
    keep;
    stop;
}
# address book
if anyof (addressbook :in "from")
{
    flag "flagged";
    stop;
}
# Invite without method
if allof (invite)
{
    keep;
}
# Invite with method
if anyof (invite :method ["reply", "refresh", "counter"])
{
    keep;
}
# MIME header
if allof (mime_header :contains ["to"] "james.apache.org",
 mime_header :contains ["cc"] "james.apache.org")
{
    fileInto "jSieve";
    stop;
}
# backslash
if allof (header :contains ["to"] "\\") {
    keep;
}
# double quote
if anyof (header :contains ["from"] "\"") {
    keep;
}
# Mutlple header names
if allof (header :contains ["to","cc"] "james.apache.org")
{
    keep;
}
