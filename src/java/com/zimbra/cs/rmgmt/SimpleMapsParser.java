/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.rmgmt;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 
 * Parse a list of simple key_string=value_string\n maps from standard input.
 * Maps are seperated any lines that do not contain a = seperator.
 */
public class SimpleMapsParser {
    
    public interface Visitor {
        public void handle(int lineNo, Map<String, String> map);
    }
    
    private static final Pattern KEY_VALUE_PATTERN = Pattern.compile("^([^=]+)=(.*)$");
    private static final int KEY_GROUP = 1;
    private static final int VALUE_GROUP = 2;
    
    public static void parse(Reader reader, Visitor visitor) throws IOException {
        BufferedReader in = new BufferedReader(reader);
        String line;
        int lineNumber = 0;
        
        Map<String,String> current = new HashMap<String, String>();
        int currentMapStartLineNumber = 1;
        
        while ((line = in.readLine()) != null) {
            lineNumber++;
            Matcher matcher = KEY_VALUE_PATTERN.matcher(line);
            if (!matcher.find()) {
                visitor.handle(currentMapStartLineNumber, current);
                current = new HashMap<String, String>();
                currentMapStartLineNumber = lineNumber + 1;
            } else {
                current.put(matcher.group(KEY_GROUP), matcher.group(VALUE_GROUP));
            }
        }
        if (!current.isEmpty()) {
            visitor.handle(currentMapStartLineNumber, current);
        }
    }
    
    public static void main(String[] args) throws IOException {
        InputStream is;
        if (args.length == 0) {
            is = System.in;
        } else {
            is = new FileInputStream(args[0]);
        }
        
        SimpleMapsParser.parse(new InputStreamReader(is), new Visitor() {
            
            public void handle(int lineNumber, Map<String, String> map) {
                for (String key : map.keySet()) {
                    System.out.print(key);
                    System.out.print("=");
                    System.out.println(map.get(key));
                }
                System.out.println();
            }
        });
    }
}
