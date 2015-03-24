/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright  = C) 2015 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.servicelocator;

import com.zimbra.cs.account.Provisioning;



/**
 * Service names registered with the Service Locator.
 */
public interface ZimbraServiceNames {
    String IMAP = "zimbra-imap";
    String LMTP = "zimbra-lmtp";
    String MAILSTORE = "zimbra-mailstore";
    String MAILSTOREADMIN = "zimbra-mailstoreadmin";
    String POP = "zimbra-pop3";
    String WEB = "zimbra-web";
    String WEBADMIN = "zimbra-webadmin";


    public static String getNameForProvisioningServiceConst(String serviceConst) {
        if (Provisioning.SERVICE_ADMINCLIENT.equals(serviceConst)) {
            return WEBADMIN;
        } else if (Provisioning.SERVICE_MAILCLIENT.equals(serviceConst)) {
            return WEB;
        } else if (Provisioning.SERVICE_MAILBOX.equals(serviceConst)) {
            return MAILSTORE;
        } else if (Provisioning.SERVICE_WEBCLIENT.equals(serviceConst)) {
            return WEB;
        }
        return null;
    }

    public static String toProvisioningServiceConst(String name) {
        if (IMAP.equals(name)) {
            return Provisioning.SERVICE_MAILBOX;
        } else if (LMTP.equals(name)) {
            return Provisioning.SERVICE_MAILBOX;
        } else if (MAILSTORE.equals(name)) {
            return Provisioning.SERVICE_MAILBOX;
        } else if (MAILSTOREADMIN.equals(name)) {
            return Provisioning.SERVICE_MAILBOX;
        } else if (POP.equals(name)) {
            return Provisioning.SERVICE_MAILBOX;
        } else if (WEB.equals(name)) {
            return Provisioning.SERVICE_WEBCLIENT;
        } else if (WEBADMIN.equals(name)) {
            return Provisioning.SERVICE_ADMINCLIENT;
        }
        return null;
    }
}
