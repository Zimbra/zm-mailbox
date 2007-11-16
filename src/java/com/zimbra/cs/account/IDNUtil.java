/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
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

package com.zimbra.cs.account;

import gnu.inet.encoding.IDNA;
import gnu.inet.encoding.IDNAException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;

public class IDNUtil {
    public static final String ACE_PREFIX = "xn--";
    
    /*
     * convert an unicode domain name to ACE(ASCII Compatible Encoding)
     */
    public static String toAsciiDomainName(String name) {
        String ascii = name;
        try {
            ascii = IDNA.toASCII(name);
        } catch (IDNAException e) {
            ZimbraLog.account.info("domain " + name + " cannot be converted to ASCII", e);
        }
        return ascii;
    }
    
    /*
     * convert an  ASCII domain name to unicode
     */
    public static String toUnicodeDomainName(String name) {
        return IDNA.toUnicode(name);
    }
    
    public static String toAsciiEmail(String emailAddress) throws ServiceException {
        String parts[] = emailAddress.split("@");
        
        if (parts.length != 2)
            throw ServiceException.INVALID_REQUEST("must be valid list address: "+emailAddress, null);

        String localPart = parts[0];
        String domain = parts[1];
        emailAddress = localPart + "@" + IDNUtil.toAsciiDomainName(domain);
        return emailAddress;
    }
    
    public static String toUnicodeEmail(String emailAddress) throws ServiceException {
        String parts[] = emailAddress.split("@");
        
        if (parts.length != 2)
            throw ServiceException.INVALID_REQUEST("must be valid list address: "+emailAddress, null);

        String localPart = parts[0];
        String domain = parts[1];
        emailAddress = localPart + "@" + IDNUtil.toUnicodeDomainName(domain);
        return emailAddress;
    }
    
    public static String toAscii(String name) {
        if (name == null)
            return null;
        
        try {
            if (name.contains("@"))
                return toAsciiEmail(name);
            else
                return toAsciiDomainName(name);
        } catch (ServiceException e) {
            return name;
        }
    }
    
    public static String toUnicode(String name) {
        if (name == null)
            return null;
        
        try {
            if (name.contains("@"))
                return toUnicodeEmail(name);
            else
                return toUnicodeDomainName(name);
        } catch (ServiceException e) {
            return name;
        }
    }
    
    public static void main(String arsg[]) {
        // String u1 = "abc.\u5f35\u611b\u73b2" + ".jp";
        // String u1 = "abc.XYZ" + ".jp";
        // String u1 = "my.xyz\u4e2d\u6587abc.com";
        String u1 = "\u4e2d\u6587.xyz\u4e2d\u6587abc.com";
        String a1 = toAsciiDomainName(u1);
        System.out.println("u1: " + u1);
        System.out.println("a1: " + a1);
        
        String u2 = toUnicodeDomainName(u1);
        String a2 = toAsciiDomainName(u2);
        System.out.println("a2: " + a2);
        if (a1.equals(a2) && u1.equals(u2))
            System.out.println("\nyup!");
        else
            System.out.println("\nbad!");
        
    }
}
