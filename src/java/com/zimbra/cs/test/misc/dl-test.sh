#!/bin/bash

if [ ! -x "$1" ]; then
    echo "Usage: dl-test.sh <ProvUtilCommand>"
    exit 1
fi
provutil="$1"

run() {
    echo === Testing: $@ ===
    $provutil "$@"
}
     
run cdl hfdsfsdf
run cdl dkfjsdfss@sdfldksfs.com
run ddl dsdkjfsd
run ddl ffsdfsdf@fsdfsdf.com
run ddl blah@example.zimbra.com
run gdl haha
run gdl haha@example.zimbra.com
 
run cdl haha@example.zimbra.com
run cdl hehe@example.zimbra.com

run adlm haha@example.zimbra.com
run adlm haha@example.zimbra.com anandp
 
run adlm haha@example.zimbra.com anandp@quickmonkey.com
run adlm haha@example.zimbra.com anand_palaniswamy@yahoo.com
run adlm haha@example.zimbra.com anandp@gmail.com
run adlm haha@example.zimbra.com user1@example.zimbra.com
 
run adlm hehe@example.zimbra.com anandp@quickmonkey.com.invalid
run adlm hehe@example.zimbra.com anand_palaniswamy@yahoo.com.invalid
run adlm hehe@example.zimbra.com anandp@gmail.com.invalid
run adlm hehe@example.zimbra.com user2@example.zimbra.com.invalid
 
run gadl 
run gadl -v
 
run gdl haha@example.zimbra.com
 
run ddl hehe@example.zimbra.com

run gadl
