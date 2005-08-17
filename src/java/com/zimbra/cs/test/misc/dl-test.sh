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
run ddl blah@liquidsys.com
run gdl haha
run gdl haha@liquidsys.com
 
run cdl haha@liquidsys.com
run cdl hehe@liquidsys.com

run adlm haha@liquidsys.com
run adlm haha@liquidsys.com anandp
 
run adlm haha@liquidsys.com anandp@quickmonkey.com
run adlm haha@liquidsys.com anand_palaniswamy@yahoo.com
run adlm haha@liquidsys.com anandp@gmail.com
run adlm haha@liquidsys.com user1@liquidsys.com
 
run adlm hehe@liquidsys.com anandp@quickmonkey.com.invalid
run adlm hehe@liquidsys.com anand_palaniswamy@yahoo.com.invalid
run adlm hehe@liquidsys.com anandp@gmail.com.invalid
run adlm hehe@liquidsys.com user2@liquidsys.com.invalid
 
run gadl 
run gadl -v
 
run gdl haha@liquidsys.com
 
run ddl hehe@liquidsys.com

run gadl
