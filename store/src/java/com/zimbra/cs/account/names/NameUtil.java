/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2012, 2013, 2014, 2016, 2018 Synacor, Inc.
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
package com.zimbra.cs.account.names;

import java.io.UnsupportedEncodingException;
import javax.mail.internet.InternetAddress;

import com.google.common.base.Strings;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.EmailUtil;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;

public class NameUtil {
    private static final int MAX_DOMAIN_NAME_LEN = 255; // bug 15919, (RFC 1035)

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
            InternetAddress ia = new InternetAddress(addr, "", "UTF-8");
            if (ia.getPersonal() != null && !ia.getPersonal().equals(""))
                throw ServiceException.INVALID_REQUEST("invalid email address", null);
        } catch (UnsupportedEncodingException e) {
            throw ServiceException.INVALID_REQUEST("invalid email address", e);
        }
    }

    /*
     * returns if a domain can be created or renamed into the given name
     *
     * Note: this has to be called with an ASCII domain name.
     *       i.e. for IDN, it has to converted to ACE first before calling
     *       this method.
     */
    public static void validNewDomainName(String domain) throws ServiceException {

        // bug 15919, domain name should be restricted to 255 chars (RFC 1035)
        if (domain.length() > MAX_DOMAIN_NAME_LEN)
            throw ServiceException.INVALID_REQUEST("invalid domain name " + domain +
                    ", domain name cannot exceed " + MAX_DOMAIN_NAME_LEN + " chars", null);

        String email = "test" + "@" + domain;
        try {
            validEmailAddress(email);
        } catch (ServiceException e) {
            throw ServiceException.INVALID_REQUEST("invalid domain name " + domain, e);
        }

        Config config = Provisioning.getInstance().getConfig();
        boolean allowNonLDH = config.getBooleanAttr(Provisioning.A_zimbraAllowNonLDHCharsInDomain, true);
        if (!allowNonLDH && containsNonLDH(domain))
            throw ServiceException.INVALID_REQUEST("invalid domain name " + domain +
                    ": " + " containing non-LDH characters and " +
                    Provisioning.A_zimbraAllowNonLDHCharsInDomain + " is false", null);
    }


    /*
     * A simple email address parser that splits the address into localpart and domain name
     */
    public static class EmailAddress {

        private String localPart;
        private String domain;

        public EmailAddress(String addr) throws ServiceException {
            this(addr, true);
        }

        public EmailAddress(String addr, boolean strict) throws ServiceException {
            int index = addr.indexOf('@');
            if (index == -1) {
                localPart = addr;
                domain = null;
            } else {
                localPart = addr.substring(0, index);
                domain = addr.substring(index+1);
            }

            if (strict && Strings.isNullOrEmpty(domain)) {
                throw ServiceException.INVALID_REQUEST("must be valid email address: " + addr, null);
            }
        }

        public static String getAddress(String localPart, String domain) {
            return localPart + "@" + domain;
        }

        public static String getDomainNameFromEmail(String addr) throws ServiceException {
            EmailAddress email = new EmailAddress(addr);
            return email.getDomain();
        }

        public static String getLocalPartFromEmail(String addr) throws ServiceException {
            EmailAddress email = new EmailAddress(addr);
            return email.getLocalPart();
        }

        public String getLocalPart() {
            return localPart;
        }

        public String getDomain() {
            return domain;
        }
    }
}
