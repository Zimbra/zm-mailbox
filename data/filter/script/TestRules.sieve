#
# This is the testing Sieve script that exercises the mail filter tests/actions.
# Test steps: 
#   (1) save this script into LDAP for an account; restart tomcat to make sure
#		the script is reread.
#   (2) create the following tags in the system:
#		  FilterTag1, FilterTag2, FilterTag3
#       create the following folders in the system:
#		  FilterFolder1, FilterFolder2
#   (3) run LmtpInject to inject testing emails for filtering into the system
#   (4) verify appropriate actions have taken place
#
require ["fileinto", "reject", "tag", "flag"];

# Test 1: From
# from rule
if header :contains "From" "test1@filter.org"
{
    fileinto "/Drafts";
    stop;
}

# Test 2: To
# to rule
if header :contains "To" "test2@filter.org"
{
    fileinto "/Drafts";
    stop;
}

# Test 3: Cc
# cc rule
if header :contains "cc" "test3@filter.org"
{
    fileinto "/Drafts";
    stop;
}

# Test 4: Subject
# subject rule
if header :contains "Subject" "test4"
{
    fileinto "/Drafts";
    stop;
}

# Test 5: From or To or Cc
# header combo rule 1
if header :contains ["From", "To", "Cc"] "test5"
{
	fileinto "/Drafts";
	stop;
}

# Test 6: From and Subject
# header combo rule 2
if allof (header :contains "From" "test6",
		header :contains "subject" "test6")
{
	fileinto "/Drafts";
	stop;
}
		
# Test 7: Header Sender
if header :contains "sender" "test7@filter.org" 
{
	fileinto "/Drafts";
	stop;
}

# Test 13: Attachment presence
# attachment rule
if attachment
{
	tag "FilterTag3";
	stop;
}

# Test 9: Date range
# date rule 2
if allof (date :before "12/1/2004", date :after "6/1/2004")
{
	flag "flagged";
	stop;
}

# Test 10: Size
# size rule 1
if size :over 2048
{
	flag "flagged";
    stop;
}

# Test 11: Size range
# size rule 2
if allof (size :over 1024, size :under 2048)
{
	flag "flagged";
    stop;
}

# Test 12: Body
# body rule
if (body :contains "test12")
{
	flag "flagged";
	stop;
}

# Test 14: From in contacts
# addressbook rule
if addressbook :in "From" "contacts"
{
	flag "flagged";
	stop;
}

# Test 15: From Address
# Test 16: multiple fileinto actions
# fileinto test rule
if header :contains "subject" "test16 action" 
{
	fileinto "/Drafts";
	fileinto "/Spam";
	stop;
}

# Test 17: multiple keep, fileinto actions
if header :contains "subject" "test17 action" 
{
	keep;
	fileinto "/FilterFolder1";
	fileinto "/FilterFolder2";
	stop;
}

# Test 18: multiple tag actions
if header :contains "subject" "test18 action" 
{
	tag "FilterTag1";
	tag "FilterTag2";
	stop;
}
# Test 19: multiple tag, flag actions
if header :contains "subject" "test19 action" 
{
	tag "FilterTag1";
	flag "read";
	tag "FilterTag2";
	flag "flagged";
	stop;
}

# Test 20: mix of fileinto, tag actions
if header :contains "subject" "test20 action" 
{
	tag "FilterTag1";
	fileinto "/FilterFolder1";
	tag "FilterTag2";
	fileinto "/FilterFolder2";
	tag "FilterTag3";
	stop;
}

# Test 21: mix of discard and other actions
if header :contains "subject" "test21 action" 
{
	discard;
	fileinto "/FilterFolder1";
	fileinto "/FilterFolder2";
	keep;
	stop;
}

# Test 22: matches comparator 
# matches rule
if header :matches ["From", "To"] "*@test22.com"
{
    flag "flagged"; 
    stop;
}

# Test 23: exists comparator
# exists rule
if exists ["X-test23-0", "X-test23-1"] {
	fileinto "/Spam";
	stop;
}        

# Test 8: Date
# date rule 1
if date :before "6/1/2004"
{
	flag "flagged";
	stop;
}
