/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.common.util;

import java.io.IOException;
import java.io.InputStream;
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
    
    /**
     * Returns <tt>true</tt> if the given stream returns an RFC 822 message.  Confirms
     * that the streams starts with a header name, followed by a colon (RFC 2822 2.2)
     * 
     * @param in the data stream.  Must support mark/reset with a buffer of at least
     * 998 bytes.
     */
    public static boolean isRfc822Message(InputStream in)
    throws IOException {
        in.mark(998);
        boolean gotHeaderName = false;
        for (int i = 1; i <= 998; i++) {
            int c = in.read();
            if (c < 33 || c > 126) {
                in.reset();
                return false;
            }
            if (c == ':') {
                in.reset();
                return gotHeaderName;
            }
            gotHeaderName = true;
        }
        in.reset();
        return false;
    }
}
