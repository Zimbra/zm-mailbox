/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.html;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts HTML-specific entities (e.g. &nbsp;) to equivalent unicode character entities (&#!60;)
 * and vice-versa.  Also converts unicode characters to HTML-entity equivalents
 * 
 * This is useful for converting HTML to XML and vice-versa.
 */
public class HtmlEntityMapper {
    private static final String[][] entities = 
    {
     { "nbsp", "160"},
    };
    
    private static Map<String, String> sHtmlToEntityMap;
    private static Map<String, String> sEntityToHtmlMap;
    private static Map<Character, String> sUnicodeEntityToHtmlEntityMap; 
    
    static {
        sHtmlToEntityMap = new HashMap<String, String>();
        sEntityToHtmlMap = new HashMap<String, String>();
        sUnicodeEntityToHtmlEntityMap = new HashMap<Character, String>();
        
        for (String[] data : entities) {
            sHtmlToEntityMap.put(data[0], data[1]);
            sEntityToHtmlMap.put(data[1], data[0]);
            sUnicodeEntityToHtmlEntityMap.put(new Character((char)Integer.parseInt(data[1])), data[0]);
        }
    }
    
    static Pattern htmlEntityPat = Pattern.compile("&([A-Za-z0-9]{2,6});");
    static Pattern numEntityPat = Pattern.compile("&#([0-9]{1,5});");
    
    private static String replaceInternal(String s, Pattern regex, Map<String,String> map, String prefix) {
        StringBuilder toRet = new StringBuilder();
        
        // while text left {
        //   find first substring that matches regex
        //   have one?
        //       YES!
        //          copy stuff before match into output
        //          in entity map?
        //             NO
        //                put orig string into output
        //             YES
        //                put replacement string into output
        //          move input to end of regex match
        //       NO!
        //          copy rest of string into output
        //          move to end of input
        //     
        
        int curIdx = 0;
        
        Matcher m = regex.matcher(s);

        while (curIdx < s.length()) {
            if (m.find(curIdx)) {
                toRet.append(s.substring(curIdx, m.start()));
                String id = m.group(1);
                if (map.containsKey(id)) {
                    toRet.append(prefix).append(map.get(id)).append(";");
                } else {
                    toRet.append(m.group(0));
                }
                curIdx=m.end();
            } else {
                toRet.append(s.substring(curIdx));
                curIdx = s.length();
            }
        }
        
        return toRet.toString();
    }
    
    /**
     * Given an input string, replace all HTML-entities with the numberic versions
     *    &nbsp; --> &#160;
     * 
     * @param s
     * @return
     */
    public static String htmlEntitiesToNumeric(String s) {
        return replaceInternal(s, htmlEntityPat, sHtmlToEntityMap, "&#");
    }
    
    /**
     * Given an input string, replace all numberic entities with known HTML versions
     *     &#160; --> &nbsp;
     *  
     * TODO FIXME: Does NOT currently support hexadecimal entities      
     *     
     * @param s
     * @return
     */
    public static String numericEntitiesToHtml(String s) {
        return replaceInternal(s, numEntityPat, sEntityToHtmlMap, "&");
    }
    
    
    /**
     * Given a Unicode string, replace all the unicode chars with known HTML entities
     *     (char 160) --> &nbsp;
     * @param s
     * @return
     */
    public static String unicodeToHtmlEntity(String s) {
        StringBuilder toRet = new StringBuilder();
        for (char c : s.toCharArray()) {
            String rep = sUnicodeEntityToHtmlEntityMap.get(c);
            if (rep != null) 
                toRet.append("&").append(rep).append(";");
            else
                toRet.append(c);
        }
        return toRet.toString();
    }
    
    public static void main(String[] argv) {
        String test1 = "abc&nbsp;def&nbsp;&nbsp;ghi&nbsp;&foo;&#1234;zug";
//        String test1 = "abcdef&#nbsp;&nbsp;ghi&nbsp;";
        System.out.println(test1);
        String result = htmlEntitiesToNumeric(test1);
        System.out.println(result);
        String result2 = numericEntitiesToHtml(result);
        System.out.println(result2);
        if (!result2.equals(test1)) {
            System.out.println("Final result NOT EQUAL to initial!");
        }
    }
}
