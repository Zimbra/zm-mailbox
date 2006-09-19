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
package com.zimbra.cs.jsp.bean;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.zimbra.cs.zclient.ZEmailAddress;

public class BeanUtils {

    private static void addAddr(StringBuilder sb, ZEmailAddress email, int size) {
        if (email == null) return;
        if (sb.length() > 0) sb.append(", ");
        if (size > 1 && email.getDisplay() != null)
            sb.append(email.getDisplay());        
        else if (email.getPersonal() != null)
            sb.append(email.getPersonal());
        else if (email.getAddress() != null)
            sb.append(email.getAddress());
    }
    
    public static String getAddrs(List<ZEmailAddress> addrs) {
        if ( addrs == null) return null;
        int len = addrs.size();
        StringBuilder sb = new StringBuilder();
        for (ZEmailAddress addr: addrs) {
            addAddr(sb, addr, addrs.size());
        }
        String result = sb.toString();
        return result.length() == 0 ? null : result; 
    }
    
    public static String getHeaderAddrs(List<ZEmailAddress> addrs, String type) {
        if ( addrs == null) return null;
        int len = addrs.size();
        StringBuilder sb = new StringBuilder();
        for (ZEmailAddress addr: addrs) {
            if (type != null && addr.getType().equals(type)) {
                if (sb.length() > 0) sb.append("; ");
                String p = addr.getPersonal();
                boolean useP = p!= null && p.length() > 0;
                if (useP) sb.append(p);
                String a = addr.getAddress();
                if (a != null && a.length() > 0) {
                    if (useP) sb.append(" <");
                    sb.append(a);
                    if (useP) sb.append('>');
                }
            }
        }
        String result = sb.toString();
        return result.length() == 0 ? null : result; 
    }

    public static String getAddr(ZEmailAddress addr) {
        String result = null;
        if ( addr == null) return null;
        else if (addr.getPersonal() != null)
            result = addr.getPersonal();
        else if (addr.getAddress() != null)
            result = addr.getAddress();
        else
            return null;
        return result.length() == 0 ? null : result;         
    }

    private static String replaceAll(String text, Pattern pattern, String replace) {
        Matcher m = pattern.matcher(text);
        StringBuffer sb = null;
        while (m.find()) {
            if (sb == null) sb = new StringBuffer();
            m.appendReplacement(sb, replace);
        }
        if (sb != null) m.appendTail(sb);
        return sb == null ? text : sb.toString();
    }
    
    private static final Pattern sAMP = Pattern.compile("&", Pattern.MULTILINE);
    private static final Pattern sTWO_SPACES = Pattern.compile("  ", Pattern.MULTILINE);
    private static final Pattern sLEADING_SPACE = Pattern.compile("^ ", Pattern.MULTILINE);
    private static final Pattern sTAB = Pattern.compile("\\t", Pattern.MULTILINE);
    private static final Pattern sLT = Pattern.compile("<", Pattern.MULTILINE);
    private static final Pattern sGT = Pattern.compile(">", Pattern.MULTILINE);
    private static final Pattern sNL = Pattern.compile("\\r?\\n", Pattern.MULTILINE);    
    
    public static String textToHtml(String text) {
        if (text == null || text.length() == 0) return "";
        String s = replaceAll(text, sAMP, "&amp;");
        s = replaceAll(s, sTWO_SPACES, " &nbsp;");
        s = replaceAll(s, sLEADING_SPACE, "&nbsp;");
        s = replaceAll(s, sTAB, "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
        s = replaceAll(s, sLT, "&lt;");
        s = replaceAll(s, sGT, "&gt;");
        s = replaceAll(s, sNL, "<br />");
        return s;
    }

}
