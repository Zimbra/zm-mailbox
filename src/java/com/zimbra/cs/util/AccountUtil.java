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

package com.zimbra.cs.util;

import java.io.UnsupportedEncodingException;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.EmailUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.servlet.ZimbraServlet;

public class AccountUtil {

    public static InternetAddress getFriendlyEmailAddress(Account acct) {
        // check "displayName" for personal part, and fall back to "cn" if not present
        String personalPart = acct.getAttr(Provisioning.A_displayName);
        if (personalPart == null)
            personalPart = acct.getAttr(Provisioning.A_cn);
        // catch the case where no real name was present and so cn was defaulted to the username
        if (personalPart == null || personalPart.trim().equals("") || personalPart.equals(acct.getAttr("uid")))
            personalPart = null;

        String address;
        try {
            address = getCanonicalAddress(acct);
        } catch (ServiceException se) {
            ZimbraLog.misc.warn("unexpected exception canonicalizing address, will use account name", se);
            address = acct.getName();
        }

        try {
            return new InternetAddress(address, personalPart, Mime.P_CHARSET_UTF8);
        } catch (UnsupportedEncodingException e) { }

        // UTF-8 should *always* be supported (i.e. this is actually unreachable)
        try {
            // fall back to using the system's default charset (also pretty much guaranteed not to be "unsupported")
            return new InternetAddress(address, personalPart);
        } catch (UnsupportedEncodingException e) { }

        // if we ever reached this point (which we won't), just return an address with no personal part
        InternetAddress ia = new InternetAddress();
        ia.setAddress(address);
        return ia;
    }

    public static boolean isDirectRecipient(Account acct, MimeMessage mm) throws ServiceException, MessagingException {
        return isDirectRecipient(acct, null, mm, -1);
    }
    
    public static boolean isDirectRecipient(Account acct, String[] otherAccountAddrs, MimeMessage mm, int maxToCheck) throws ServiceException, MessagingException {
        String accountAddress = acct.getName();
        String canonicalAddress = getCanonicalAddress(acct);
        String[] accountAliases = acct.getMailAlias();
        Address[] recipients = mm.getAllRecipients();
        
        if (recipients == null) {
            return false;
        }

        int numRecipientsToCheck = (maxToCheck <= 0 ? recipients.length : Math.min(recipients.length, maxToCheck));
        for (int i = 0; i < numRecipientsToCheck; i++) {
            String msgAddress = ((InternetAddress) recipients[i]).getAddress();
            if (addressMatchesAccount(accountAddress, canonicalAddress, accountAliases, msgAddress)) 
                return true;
            
            if (otherAccountAddrs != null) {
                for (String otherAddr: otherAccountAddrs) {
                    if (otherAddr.equalsIgnoreCase(msgAddress)) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }

    /* We do a more lightweight canonicalization that postfix, because
     * we except to set the LDAP attributes only in certain ways.  For
     * instance we do not canonicalize the local part by itself.
     */
    public static String getCanonicalAddress(Account account) throws ServiceException {
        // If account has a canonical address, let's use that.
        String ca = account.getAttr(Provisioning.A_zimbraMailCanonicalAddress);
        
        // But we still have to canonicalize domain names, so do that with account address
        if (ca == null)
            ca = account.getName();

        String[] parts = EmailUtil.getLocalPartAndDomain(ca);
        if (parts == null)
            return ca;

        Domain domain = Provisioning.getInstance().get(DomainBy.name, parts[1]);
        if (domain == null)
            return ca;

        String domainCatchAll = domain.getAttr(Provisioning.A_zimbraMailCatchAllCanonicalAddress);
        if (domainCatchAll != null)
            return parts[0] + domainCatchAll;

        return ca;
    }

    /**
     * Check if given account is allowed to set given from header.
     */
    public static boolean allowFromAddress(Account acct, String fromAddr) throws ServiceException {
        if (fromAddr == null)
            return false;

        if (acct.getBooleanAttr(Provisioning.A_zimbraAllowAnyFromAddress, false))
            return true;
        if (addressMatchesAccount(acct, fromAddr))
            return true;

        String[] allowedAddrs = acct.getMultiAttr(Provisioning.A_zimbraAllowFromAddress);
        if (allowedAddrs == null)
            return false;
        for (String addr : allowedAddrs) {
            if (fromAddr.equalsIgnoreCase(addr))
                return true;
        }
        return false;
    }

    /**
     * True if this address matches some address for this account (aliases, domain re-writes, etc)
     */
    public static boolean addressMatchesAccount(Account acct, String givenAddress) throws ServiceException {
        if (givenAddress == null)
            return false;
        String accountAddress = acct.getName();
        String canonicalAddress = getCanonicalAddress(acct);
        String[] accountAliases = acct.getMailAlias();
        return addressMatchesAccount(accountAddress, canonicalAddress, accountAliases, givenAddress);
    }
    
    private static boolean addressMatchesAccount(String accountAddress, String canonicalAddress, String[] accountAliases, String givenAddress) {
        if (givenAddress == null)
            return false;

        if (givenAddress.equalsIgnoreCase(accountAddress))
            return true;
        if (givenAddress.equalsIgnoreCase(canonicalAddress))
            return true;

        for (int j = 0; j < accountAliases.length; j++) {
            if (givenAddress.equalsIgnoreCase(accountAliases[j]))
                return true;
        }
        return false;
    }


    public static String getSoapUri(Account account) {
        String base = getBaseUri(account);
        return (base == null ? null : base + ZimbraServlet.USER_SERVICE_URI);
    }

    public static String getBaseUri(Account account) {
        if (account == null)
            return null;

        try {
            Server server = Provisioning.getInstance().getServer(account);
            if (server == null) {
                ZimbraLog.account.warn("no server associated with acccount " + account.getName());
                return null;
            }
            return getBaseUri(server);
        } catch (ServiceException e) {
            ZimbraLog.account.warn("error fetching SOAP URI for account " + account.getName(), e);
            return null;
        }
    }

    public static String getBaseUri(Server server) {
        if (server == null)
            return null;

        String host = server.getAttr(Provisioning.A_zimbraServiceHostname);
        String mode = server.getAttr(Provisioning.A_zimbraMailMode, "http");
        int port = server.getIntAttr(Provisioning.A_zimbraMailPort, 0);
        if (port > 0 && !mode.equalsIgnoreCase("https") && !mode.equalsIgnoreCase("redirect")) {
            return "http://" + host + ':' + port;
        } else if (!mode.equalsIgnoreCase("http")) {
            port = server.getIntAttr(Provisioning.A_zimbraMailSSLPort, 0);
            if (port > 0)
                return "https://" + host + ':' + port;
        }
        ZimbraLog.account.warn("no service port available on host " + host);
        return null;
    }

//    /**
//     * True if this mime message has at least one recipient that is NOT the same as the specified account
//     * 
//     * @param acct
//     * @param mm
//     * @return
//     * @throws ServiceException
//     */
//    public static boolean hasExternalRecipient(Account acct, MimeMessage mm) throws ServiceException, MessagingException
//    {
//        int maxToCheck = -1;
//        String accountAddress = acct.getName();
//        String canonicalAddress = getCanonicalAddress(acct);
//        String[] accountAliases = acct.getMailAlias();
//        Address[] recipients = mm.getAllRecipients();
//        
//        if (recipients != null) {
//            int numRecipientsToCheck = (maxToCheck <= 0 ? recipients.length : Math.min(recipients.length, maxToCheck));
//            for (int i = 0; i < numRecipientsToCheck; i++) {
//                String msgAddress = ((InternetAddress) recipients[i]).getAddress();
//                if (!addressMatchesAccount(accountAddress, canonicalAddress, accountAliases, msgAddress)) 
//                    return true;
//            }
//        }
//        return false;
//    }

    /**
     *
     * @param id account id to lookup
     * @param nameKey name key to add to context if account lookup is ok
     * @param idOnlyKey id key to add to context if account lookup fails
     */
    public static void addAccountToLogContext(Provisioning prov, String id, String nameKey, String idOnlyKey, AuthToken authToken) {
        Account acct = null;
        try {
            acct = prov.get(Provisioning.AccountBy.id, id, authToken);
        } catch (ServiceException se) {
            ZimbraLog.misc.warn("unable to lookup account for log, id: " + id, se);
        }
        if (acct == null) {
            ZimbraLog.addToContext(idOnlyKey, id);
        } else {
            ZimbraLog.addToContext(nameKey, acct.getName());
        }
    }
}
