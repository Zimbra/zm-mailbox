/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account;

import java.io.UnsupportedEncodingException;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import gnu.inet.encoding.IDNA;
import gnu.inet.encoding.IDNAException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AttributeManager.IDNType;
import com.zimbra.cs.mime.Mime;

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
            ZimbraLog.account.info("domain [" + name + "] cannot be converted to ASCII", e);
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
            throw ServiceException.INVALID_REQUEST("must be valid email address: "+emailAddress, null);

        String localPart = parts[0];
        String domain = parts[1];
        emailAddress = localPart + "@" + IDNUtil.toAsciiDomainName(domain);
        return emailAddress;
    }
    
    public static String toUnicodeEmail(String emailAddress) throws ServiceException {
        String parts[] = emailAddress.split("@");
        
        if (parts.length != 2)
            throw ServiceException.INVALID_REQUEST("must be valid email address: "+emailAddress, null);

        String localPart = parts[0];
        String domain = parts[1];
        emailAddress = localPart + "@" + IDNUtil.toUnicodeDomainName(domain);
        return emailAddress;
    }
    
    public static String toAscii(String name, IDNType idnType) {
        switch (idnType) {
        case email:
            return toAscii(name);
        case emailp:
            return toAsciiWithPersonalPart(name);
        case cs_emailp:
            String[] names = name.split(",");
            StringBuilder out = new StringBuilder(); 
            boolean first = true;
            for (String n: names) {
                if (first)
                    first = false;
                else
                    out.append(", ");
                out.append(toAsciiWithPersonalPart(n.trim()));
            }
            return out.toString();
        default:
            return name;
        }
    }
    
    private static String toAsciiWithPersonalPart(String name) {
        try {
            InternetAddress ia = new InternetAddress(name, true);
            
            String addr = ia.getAddress();
            String asciiAddr = toAscii(addr);
            try {
                ia = new InternetAddress(asciiAddr, ia.getPersonal(), Mime.P_CHARSET_UTF8);
                
                /*
                 * note, if personal part contains non-ascii chars, it will be 
                 * converted (by InternetAddress.toString()) to RFC 2047 encoded address.
                 * The resulting string contains only US-ASCII characters, and
                 * hence is mail-safe.
                 * 
                 * e.g. a personal part: \u4e2d\u6587
                 *      will be converted to =?utf-8?B?5Lit5paH?=
                 *      and stored in LDAP in the encoded form
                 */
                return ia.toString(); 
            } catch (UnsupportedEncodingException e) {
                ZimbraLog.account.info("cannot convert to ascii, returning original addr: [" + name + "]", e);
            }
        } catch (AddressException e) {
            ZimbraLog.account.info("cannot convert to ascii, returning original addr: [" + name + "]", e);
        }
        
        return name;
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
    
    public static String toUnicode(String name, IDNType idnType) {
       
        switch (idnType) {
        case email:
            return toUnicode(name);
        case emailp:
            return toUnicodeWithPersonalPart(name);
        case cs_emailp:
            String[] names = name.split(",");
            StringBuilder out = new StringBuilder(); 
            boolean first = true;
            for (String n: names) {
                if (first)
                    first = false;
                else
                    out.append(", ");
                out.append(toUnicodeWithPersonalPart(n.trim()));
            }
            return out.toString();
        default:
            return name;
        }
    }
    
    private static String toUnicodeWithPersonalPart(String name) {
        try {
            InternetAddress ia = new InternetAddress(name, true);
            /*
             * InternetAddress.toUnicodeString only deals with 
             * non-ascii chars in personal part, it has nothing 
             * to do with IDN.
             */
            // return ia.toUnicodeString();   
            
            String addr = ia.getAddress();
            String unicodeAddr = toUnicode(addr);
            try {
                ia = new InternetAddress(unicodeAddr, ia.getPersonal(), Mime.P_CHARSET_UTF8);
                
                /*
                 *  call InternetAddress.toUnicodeString instead of 
                 *  InternetAddress.toString so non-ascii chars in 
                 *  personal part can be returned in Unicode instead of 
                 *  RFC 2047 encoded.
                 */
                return ia.toUnicodeString();
            } catch (UnsupportedEncodingException e) {
                ZimbraLog.account.info("cannot convert to unicode, returning original addr: [" + name + "]", e);
            }
        } catch (AddressException e) {
            ZimbraLog.account.info("cannot convert to unicode, returning original addr: [" + name + "]", e);
        }
        
        return name;
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
    
    
    private static void testEmailp(String unicode, IDNType idnType) {
        System.out.println("\nTesting email with personal part, idn type = " + idnType + "\n");
        
        String emailp_u1 = unicode;
        String emailp_a1 = toAscii(emailp_u1, idnType);
        System.out.println("emailp_u1: " + emailp_u1);
        System.out.println("emailp_a1: " + emailp_a1);
        
        String emailp_u2 = toUnicode(emailp_a1, idnType);
        String emailp_a2 = toAscii(emailp_u2, idnType);
        System.out.println("emailp_u2: " + emailp_u2);
        System.out.println("emailp_a2: " + emailp_a2);
        
        if (emailp_u1.equals(emailp_u2) && emailp_a1.equals(emailp_a2))
            System.out.println("\nyup!");
        else
            System.out.println("\nbad!");
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
        
        // System.out.println(toAscii("abc@xyz"));
        
        // with personal name
        testEmailp("foo bar <test@\u4e2d\u6587.xyz\u4e2d\u6587abc.com>", IDNType.emailp);
        testEmailp("\u4e2d\u6587 <test@\u4e2d\u6587.xyz\u4e2d\u6587abc.com>", IDNType.emailp);
        testEmailp("foo bar <test@\u4e2d\u6587.xyz\u4e2d\u6587abc.com>", IDNType.cs_emailp);
        testEmailp("foo bar <test@\u4e2d\u6587.xyz\u4e2d\u6587abc.com>, cat dog <test@xyz\u4e2d\u6587abc.com>", IDNType.cs_emailp);
    }
}
