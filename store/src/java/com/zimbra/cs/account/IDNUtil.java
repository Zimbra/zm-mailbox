/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account;

import java.io.UnsupportedEncodingException;
import java.lang.IllegalAccessException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// import java.net.IDN;  JDK1.6 only

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import gnu.inet.encoding.IDNA;
import gnu.inet.encoding.IDNAException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.mime.shim.JavaMailInternetAddress;
import com.zimbra.cs.account.AttributeManager.IDNType;

public class IDNUtil {
    public static final String ACE_PREFIX = "xn--";
    
    private static abstract class ZimbraIDN {
        private static final boolean sAllowUnassigned = true;
        private static final boolean sUseSTD3ASCIIRules = false;
        
        private static final ZimbraIDN INSTANCE = ZimbraIDN.getInstance();
        
        private static ZimbraIDN getInstance() {
            
            ZimbraIDN instance = JavaIDN.getInstance(sAllowUnassigned, sUseSTD3ASCIIRules);
            if (instance == null)
                instance = new GnuIDN(sAllowUnassigned, sUseSTD3ASCIIRules);
            
            return instance;
        }
        
        abstract String toASCII(String input) throws ServiceException;
        abstract String toUnicode(String input) throws ServiceException;
                
        private static String convertToASCII(String input) {
            try {
                return INSTANCE.toASCII(input);
            } catch (ServiceException e) {
                // if for any reason it cannot be converted, just INFO log and return the input as is
                ZimbraLog.account.info("domain [" + input + "] cannot be converted to ASCII", e);
                return input;
            }
        }
        
        private static String convertToUnicode(String input) {
            try {
                return INSTANCE.toUnicode(input);
            } catch (ServiceException e) {
                // if for any reason it cannot be converted, just INFO log and return the input as is
                ZimbraLog.account.info("domain [" + input + "] cannot be converted to Unicode", e);
                return input;
            }
        }
    }
    
    private static class JavaIDN extends ZimbraIDN {
        private static final String SEGMENT_DELIM = ".";
        
        private int mFlags;
        private Method mMethodToASCII;
        private Method mMethodToUnicode;
        
        // keep constant field value defined in java.net.IDN, since we are using java.reflect 
        private int ALLOW_UNASSIGNED;      
        private int USE_STD3_ASCII_RULES;
        
        private static JavaIDN getInstance(boolean allowUnassigned, boolean useSTD3ASCIIRules) {
            try {
                return new JavaIDN(allowUnassigned, useSTD3ASCIIRules);
            } catch (ServiceException e) {
            }
            return null;
        }
        
        /*
         * Java IDN support is only in JDK 1.6
         */
        private JavaIDN(boolean allowUnassigned, boolean useSTD3ASCIIRules) throws ServiceException {
            try {
                Class cls = Class.forName("java.net.IDN");
                mMethodToASCII = cls.getMethod("toASCII", String.class, Integer.TYPE);
                mMethodToUnicode = cls.getMethod("toUnicode", String.class, Integer.TYPE);
                Field fieldAllowUnassigned = cls.getField("ALLOW_UNASSIGNED");
                Field fieldUseSTD3ASCIIRules = cls.getField("USE_STD3_ASCII_RULES");
                
                // just in case, should not happen if we get here
                if (mMethodToASCII == null || mMethodToUnicode == null ||
                    fieldAllowUnassigned == null || fieldUseSTD3ASCIIRules == null)
                    throw ServiceException.FAILURE("JavaIDN not supported", null);
                        
                ALLOW_UNASSIGNED = fieldAllowUnassigned.getInt(null);
                USE_STD3_ASCII_RULES = fieldUseSTD3ASCIIRules.getInt(null);
                
                mFlags = 0;
                if (allowUnassigned)
                    mFlags |= ALLOW_UNASSIGNED;
                
                if (useSTD3ASCIIRules)
                    mFlags |= USE_STD3_ASCII_RULES;
                
                // all is well
                return;
                
            } catch (ClassNotFoundException e) {
            } catch (NoSuchMethodException e) {
            } catch (NoSuchFieldException e) {
            } catch (IllegalAccessException e) {
            }
            
            // nope, no Java IDN
            throw ServiceException.FAILURE("JavaIDN not supported", null);
        }
        
        String toASCII(String input) throws ServiceException {
            
            /*
             * change to just:  java.net.IDN.toASCII(input, mFlags) 
             * after dev is all on 1.6.  Currently only production is on 1.6
             */
            
            try {
                /*
                 * bug 68964
                 * 
                 * if there is no segment after the last dot, the last dot is not 
                 * included in the output by java.net.IDN.toASCII.  
                 * e.g. "." would return "", expected: "."
                 *      ".." would return ".", expected: ".."
                 *      ".a." would return ".a", expected: ".a."
                 *      
                 * To work around, split input to segments, convert each segment
                 * separately, then join them.
                 */
                StringBuffer sb = new StringBuffer();
                StringTokenizer tokenizer = new StringTokenizer(input, SEGMENT_DELIM, true);
                while (tokenizer.hasMoreTokens()) {
                    String token = tokenizer.nextToken();
                    if (!SEGMENT_DELIM.equals(token)) {
                        token = (String)mMethodToASCII.invoke(null, token, mFlags);
                    }
                    sb.append(token);
                }
                return sb.toString();
            } catch (IllegalAccessException e) {
                throw ServiceException.FAILURE("cannot convert to ASCII", e);
            } catch (IllegalArgumentException e) {
                throw ServiceException.FAILURE("cannot convert to ASCII", e);
            } catch (InvocationTargetException e) {
                throw ServiceException.FAILURE("cannot convert to ASCII", e);
            }
        }
        
        String toUnicode(String input) throws ServiceException {
            
            /*
             * change to just:  java.net.IDN.toUnicode(input, mFlags) 
             * after dev is all on 1.6.  Currently only production is on 1.6
             */
            
            try {
                StringBuffer sb = new StringBuffer();
                StringTokenizer tokenizer = new StringTokenizer(input, SEGMENT_DELIM, true);
                while (tokenizer.hasMoreTokens()) {
                    String token = tokenizer.nextToken();
                    if (!SEGMENT_DELIM.equals(token)) {
                        token = (String)mMethodToUnicode.invoke(null, token, mFlags);
                    }
                    sb.append(token);
                }
                return sb.toString();
            } catch (IllegalAccessException e) {
                throw ServiceException.FAILURE("cannot convert to Unicode", e);
            } catch (IllegalArgumentException e) {
                throw ServiceException.FAILURE("cannot convert to Unicode", e);
            } catch (InvocationTargetException e) {
                throw ServiceException.FAILURE("cannot convert to Unicode", e);
            }
        }

    }
    
    /**
     * Gateway to gnu IDNA methods.
     * 
     * gnu libind-1.0 methods:
       public static String toASCII(String input,
                             boolean allowUnassigned,
                             boolean useSTD3ASCIIRules)

       public static String toUnicode(String input,
                               boolean allowUnassigned,
                               boolean useSTD3ASCIIRules)
                      
       expects the input is only a label, NOT the entire domain name,
       which is wrong according their javadoc.
       
       We copy-paste:       
       public static String toASCII(String input)
       public static String toUnicode(String input) 
       
       from IDNA.java, and make them sensitive to allowUnassigned/useSTD3ASCIIRules.
       
       A bit hacky.  The real fix should be using JDK1.6, which has IDN support 
       in the java.net.IDN class.   In GnR, 1.6 is the JDK in production, but in 
       dev JDK 1.5 is still around.   We should get rid of GNU libidn and just use 
       the one in JDK1.6 at some point. 
                               
     */
    private static class GnuIDN extends ZimbraIDN {
        private boolean mAllowUnassigned;
        private boolean mUseSTD3ASCIIRules;
        
        private GnuIDN(boolean allowUnassigned, boolean useSTD3ASCIIRules) {
            mAllowUnassigned = allowUnassigned;
            mUseSTD3ASCIIRules = useSTD3ASCIIRules;
        }
        
        String toASCII(String input) throws ServiceException {
            try {
                StringBuffer o = new StringBuffer();
                StringBuffer h = new StringBuffer();
        
                for (int i = 0; i < input.length(); i++) {
                    char c = input.charAt(i);
                    if (c == '.' || c == '\u3002' || c == '\uff0e' || c == '\uff61') {
                        o.append(IDNA.toASCII(h.toString(), mAllowUnassigned, mUseSTD3ASCIIRules));
                        o.append('.');
                        h = new StringBuffer();
                    } else {
                        h.append(c);
                    }
                }
                o.append(IDNA.toASCII(h.toString(), mAllowUnassigned, mUseSTD3ASCIIRules));
                return o.toString();
            } catch (IDNAException e) {
                throw ServiceException.FAILURE("cannot convert to ASCII", e);
            }
        }
        
        String toUnicode(String input) {
            StringBuffer o = new StringBuffer();
            StringBuffer h = new StringBuffer();

            for (int i = 0; i < input.length(); i++) {
                char c = input.charAt(i);
                if (c == '.' || c == '\u3002' || c == '\uff0e' || c == '\uff61') {
                    o.append(IDNA.toUnicode(h.toString(), mAllowUnassigned, mUseSTD3ASCIIRules));
                    o.append(c);
                    h = new StringBuffer();
                } else {
                    h.append(c);
                }
          }
          o.append(IDNA.toUnicode(h.toString(), mAllowUnassigned, mUseSTD3ASCIIRules));
          return o.toString();
        }
    }

    
    /*
     * convert an unicode domain name to ACE(ASCII Compatible Encoding)
     */
    public static String toAsciiDomainName(String name) {
        return ZimbraIDN.convertToASCII(name);
    }
    
    /*
     * convert an  ASCII domain name to unicode
     */
    public static String toUnicodeDomainName(String name) {
        return ZimbraIDN.convertToUnicode(name);
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
        if (domain.indexOf('>') != 1) {
            domain = domain.substring(0, domain.length() - 1);
            emailAddress = localPart + "@" + IDNUtil.toUnicodeDomainName(domain) + ">";
        } else {
            emailAddress = localPart + "@" + IDNUtil.toUnicodeDomainName(domain);
        }
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

    /*
    this doesn't work since JavaMail1.4 if name contains IDN, even when strict is false.
    We have to parse ourselves, convert the domain to ACE, then put together an InternetAddress,
    then use its toString method.
    */
    /*
    private static String toAsciiWithPersonalPart(String name) {
        try {
            InternetAddress ia = new InternetAddress(name, true);
            
            String addr = ia.getAddress();
            String asciiAddr = toAscii(addr);
            try {
                ia = new InternetAddress(asciiAddr, ia.getPersonal(), MimeConstants.P_CHARSET_UTF8);
                
                //
                // note, if personal part contains non-ascii chars, it will be 
                // converted (by InternetAddress.toString()) to RFC 2047 encoded address.
                // The resulting string contains only US-ASCII characters, and
                // hence is mail-safe.
                // 
                // e.g. a personal part: \u4e2d\u6587
                //      will be converted to =?utf-8?B?5Lit5paH?=
                //      and stored in LDAP in the encoded form
                //
                return ia.toString(); 
            } catch (UnsupportedEncodingException e) {
                ZimbraLog.account.info("cannot convert to ascii, returning original addr: [" + name + "]", e);
            }
        } catch (AddressException e) {
            ZimbraLog.account.info("cannot convert to ascii, returning original addr: [" + name + "]", e);
        }
        
        return name;
    }
    */
    
    /*
     * parse the address into 4 parts:
     *   1. chars before the @
     *   2. the @
     *   3. chars after the @ and before the next >
     *   4. remaining chars (will be empty string if no remaining chars)
     *   
     *   If an input is matched, part 3 is the domain name
     *   
     *  e.g.
        test.com ==> don't match
        @test.com ==> don't match
        @test.com>aaa ==> don't match
        user@test.com>aaa ==> [1 user][2 @][3 test.com][4 >aaa]
        foo bar <user@test.com> ==> [1 foo bar <user][2 @][3 test.com][4 >]
        foo bar <user@test.com ==> [1 foo bar <user][2 @][3 test.com][4 ]
     *   
     */
    private static Pattern S_DOMAIN = Pattern.compile("(.+)(@)([^>]*)(.*)");
    
    private static String toAsciiWithPersonalPart(String name) {
        
        String asciiName = name;
            
        // extract out the domain, convert it to ACE, and put it back
        Matcher m = S_DOMAIN.matcher(name);
        if (m.matches() && m.groupCount() == 4) {
            // if matches(), then groupCount() must be 4 according to our regex, just to be safe
            String domain = m.group(3);
            String asciiDomain = toAsciiDomainName(domain);
                
            // put everything back
            asciiName = m.group(1) + m.group(2) + asciiDomain + m.group(4);
        }
        try {
            InternetAddress ia = new JavaMailInternetAddress(asciiName);
            
            String personal = ia.getPersonal();
            if (personal != null)
                ia =  new JavaMailInternetAddress(ia.getAddress(), personal, MimeConstants.P_CHARSET_UTF8);
                
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
            InternetAddress ia = new JavaMailInternetAddress(name, true);
            /*
             * InternetAddress.toUnicodeString only deals with 
             * non-ascii chars in personal part, it has nothing 
             * to do with IDN.
             */
            // return ia.toUnicodeString();   
            
            String addr = ia.getAddress();
            String unicodeAddr = toUnicode(addr);
            try {
                ia = new JavaMailInternetAddress(unicodeAddr, ia.getPersonal(), MimeConstants.P_CHARSET_UTF8);
                
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
    
    

    
    private static void regexTest(String input) {
        System.out.print(input + " ==> ");
        Matcher m = S_DOMAIN.matcher(input);
        if (m.matches()) {
            int groupsMatched = m.groupCount();
            for (int i = 1; i <= groupsMatched; i++)
                System.out.print("[" + i + " " + m.group(i) + "]");
            System.out.println();
        } else
            System.out.println("don't match");
        
    }
    
    public static void main(String[] args) {
        regexTest("test.com");
        regexTest("@test.com");
        regexTest("@test.com>aaa");
        regexTest("user@test.com>aaa");
        regexTest("foo bar <user@test.com>");
        regexTest("foo bar <user@test.com");
    }

}
