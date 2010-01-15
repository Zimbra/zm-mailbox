/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.names;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;

public class NameUtil {
    
    private static boolean isDot(int c) {
        if (c == '.' || c == '\u3002' || c == '\uff0e' || c == '\uff61')
            return true;
        else
            return false;
    }

    private static boolean containsNonLDH(String input) {
        for (int i = 0; i < input.length(); i++) {
            int c = input.charAt(i);
            
            // skip dot characters
            // The following characters are recognized as dots: 
            //     \u002E (full stop), \u3002 (ideographic full stop), \uFF0E (fullwidth full stop), and \uFF61 (halfwidth ideographic full stop)
            //
            if (isDot(c))
                continue;
            
            if ((c <= 0x2c) || 
                (c >= 0x2e && c <= 0x2f) || 
                (c >= 0x3a && c <= 0x40) ||
                (c >= 0x5b && c <= 0x60) ||
                (c >= 0x7b && c <= 0x7f)) {
                return true;
            }
        }
        return false;
    }
    
    public static void validEmailAddress(String addr) throws ServiceException {
        try {
            InternetAddress ia = new InternetAddress(addr, true);
            // is this even needed?
            // ia.validate();
            if (ia.getPersonal() != null && !ia.getPersonal().equals(""))
                throw ServiceException.INVALID_REQUEST("invalid email address", null);
        } catch (AddressException e) {
            throw ServiceException.INVALID_REQUEST("invalid email address", e);
        }
    }
    
    /*
     * returns if a domain can be created or renamed into the given name
     */
    public static void validNewDomainName(String domain) throws ServiceException {
        String email = "test" + "@" + domain;
        try {
            validEmailAddress(email); 
        } catch (ServiceException e) {
            throw ServiceException.INVALID_REQUEST("invalid domain name " + domain, null);
        }
        
        Config config = Provisioning.getInstance().getConfig();
        boolean allowNonLDH = config.getBooleanAttr(Provisioning.A_zimbraAllowNonLDHCharsInDomain, true);
        if (!allowNonLDH && containsNonLDH(domain))
            throw ServiceException.INVALID_REQUEST("invalid domain name " + domain + 
                    ": " + " containing non-LDH characters and " + 
                    Provisioning.A_zimbraAllowNonLDHCharsInDomain + " is false", null);
    }
    
}
