/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
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

package com.zimbra.common.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author bburtin
 */
public class EmailUtil
{
    /**
     * Splits email address of the form "foo@bar.com" into "foo" and "bar.com".
     * Do NOT use this method in RFC/protocol validation. Use only for simple
     * sanity split into two strings on either side of the '@'. RFC822 allows
     * local-part to contain '@' in quotes etc, and we do not deal with that
     * here (eg.: foo"@"bar@bar.com).
     * 
     * @return a 2-element array. Element 0 is local-part and element 1 is
     *         domain. Returns null if either local-part or domain were not
     *         found.
     */
    public static String[] getLocalPartAndDomain(String address) {
        if (address == null) {
            return null;
        }
        
        int at = address.indexOf('@');
        if (at == -1) {
            return null;
        }

        String localPart = address.substring(0, at);
        if (localPart.length() == 0) {
            return null;
        }

        String domain = address.substring(at + 1, address.length());
        if (domain.length() == 0) {
            return null;
        }

        return new String[] { localPart, domain };
    }
    
    public static String getValidDomainPart(String address) {
        String parts[] = getLocalPartAndDomain(address);
        if (parts == null)
            return null;
        String domain = parts[1];
        if (validDomain(domain))
            return domain;
        return null;
    }
    
    private static final String DOMAIN_PATTERN = "[-a-zA-Z0-9\\.]+";
    
    private static final Pattern DOMAIN_REGEX = Pattern.compile(DOMAIN_PATTERN);
    	
    public static boolean validDomain(String domain) {
    	int len = domain.length();
    	Matcher matcher = DOMAIN_REGEX.matcher(domain);
    	return len > 0 && domain.charAt(0) != '.' && domain.charAt(len-1) != '.' &&
    			matcher.matches() && domain.indexOf("..") == -1;
    }
}
