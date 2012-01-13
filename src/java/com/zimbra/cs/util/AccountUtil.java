/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.util;

import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.util.EmailUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.SystemUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.mime.shim.JavaMailInternetAddress;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.DomainBy;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.soap.admin.type.DataSourceType;

public class AccountUtil {

    /**
     * Returns effective quota for an account which is calculated as the minimum of account level quota and domain
     * max mailbox quota. Returns zero for unlimited effective quota.
     *
     * @param acct
     * @return
     * @throws ServiceException
     */
    public static long getEffectiveQuota(Account acct) throws ServiceException {
        long acctQuota = acct.getLongAttr(Provisioning.A_zimbraMailQuota, 0);
        Domain domain = Provisioning.getInstance().getDomain(acct);
        long domainQuota = 0;
        if (domain != null) {
            domainQuota = domain.getLongAttr(Provisioning.A_zimbraMailDomainQuota, 0);
        }
        if (acctQuota == 0) {
            return domainQuota;
        } else if (domainQuota == 0) {
            return acctQuota;
        } else {
            return Math.min(acctQuota, domainQuota);
        }
    }

    public static boolean isOverAggregateQuota(Domain domain) {
        long quota = domain.getDomainAggregateQuota();
        return quota != 0 && domain.getLongAttr(Provisioning.A_zimbraAggregateQuotaLastUsage, 0) > quota;
    }

    public static boolean isSendAllowedOverAggregateQuota(Domain domain) {
        return domain.getDomainAggregateQuotaPolicy().isALLOWSENDRECEIVE();
    }

    public static boolean isReceiveAllowedOverAggregateQuota(Domain domain) {
        return !domain.getDomainAggregateQuotaPolicy().isBLOCKSENDRECEIVE();
    }

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
            return new JavaMailInternetAddress(address, personalPart, MimeConstants.P_CHARSET_UTF8);
        } catch (UnsupportedEncodingException e) { }

        // UTF-8 should *always* be supported (i.e. this is actually unreachable)
        try {
            // fall back to using the system's default charset (also pretty much guaranteed not to be "unsupported")
            return new JavaMailInternetAddress(address, personalPart);
        } catch (UnsupportedEncodingException e) { }

        // if we ever reached this point (which we won't), just return an address with no personal part
        InternetAddress ia = new JavaMailInternetAddress();
        ia.setAddress(address);
        return ia;
    }

    /**
     * Returns the <tt>From</tt> address used for an outgoing message from the given account.
     * Takes all account attributes into consideration, including user preferences.
     */
    public static InternetAddress getFromAddress(Account acct) {
        if (acct == null) {
            return null;
        }
        String address = SystemUtil.coalesce(acct.getPrefFromAddress(), acct.getName());
        String personal = SystemUtil.coalesce(acct.getPrefFromDisplay(), acct.getDisplayName(), acct.getCn());
        try {
            return new JavaMailInternetAddress(address, personal, MimeConstants.P_CHARSET_UTF8);
        } catch (UnsupportedEncodingException e) {
            ZimbraLog.system.error("Unable to encode address %s <%s>", personal, address);
            InternetAddress ia = new JavaMailInternetAddress();
            ia.setAddress(address);
            return ia;
        }
    }

    /**
     * Returns the <tt>Reply-To</tt> address used for an outgoing message from the given
     * account, based on user preferences, or <tt>null</tt> if <tt>zimbraPrefReplyToEnabled</tt>
     * is <tt>FALSE</tt>.
     */
    public static InternetAddress getReplyToAddress(Account acct) {
        if (acct == null) {
            return null;
        }
        if (!acct.isPrefReplyToEnabled()) {
            return null;
        }
        String address = acct.getPrefReplyToAddress();
        if (address == null) {
            return null;
        }
        String personal = acct.getPrefReplyToDisplay();
        try {
            return new JavaMailInternetAddress(address, personal, MimeConstants.P_CHARSET_UTF8);
        } catch (UnsupportedEncodingException e) {
            ZimbraLog.system.error("Unable to encode address %s <%s>", personal, address);
            InternetAddress ia = new JavaMailInternetAddress();
            ia.setAddress(address);
            return ia;
        }
    }

    public static boolean isDirectRecipient(Account acct, MimeMessage mm) throws ServiceException, MessagingException {
        return isDirectRecipient(acct, null, mm, -1);
    }

    public static boolean isDirectRecipient(Account acct, String[] otherAccountAddrs, MimeMessage mm, int maxToCheck) throws ServiceException, MessagingException {
        Address[] recipients = mm.getAllRecipients();
        if (recipients == null) {
            return false;
        }

        AccountAddressMatcher acctMatcher = new AccountAddressMatcher(acct);
        int numRecipientsToCheck = (maxToCheck <= 0 ? recipients.length : Math.min(recipients.length, maxToCheck));
        for (int i = 0; i < numRecipientsToCheck; i++) {
            String msgAddress = ((InternetAddress) recipients[i]).getAddress();
            if (acctMatcher.matches(msgAddress))
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

        Domain domain = Provisioning.getInstance().getDomain(Key.DomainBy.name, parts[1], true);
        if (domain == null)
            return ca;

        String domainCatchAll = domain.getAttr(Provisioning.A_zimbraMailCatchAllCanonicalAddress);
        if (domainCatchAll != null)
            return parts[0] + domainCatchAll;

        return ca;
    }

    /**
     * True if this address matches some address for this account (aliases, domain re-writes, etc) or address of
     * another account that this account may send as.
     */
    public static boolean addressMatchesAccountOrSendAs(Account acct, String givenAddress) throws ServiceException {
        return (new AccountAddressMatcher(acct, false, true)).matches(givenAddress);
    }

    /**
     * True if this address matches some address for this account (aliases, domain re-writes, etc).  Send-as addresses
     * are not considered a match.  A send-as address is an allow-from address that corresponds to an account different
     * from this account.
     */
    public static boolean addressMatchesAccount(Account acct, String givenAddress) throws ServiceException {
        return (new AccountAddressMatcher(acct)).matches(givenAddress);
    }

    /**
     * Returns all account email addresses in lower case in a hash set.
     * @param acct user's account
     * @return Set containing all account email addresses in lower case or, empty if no email address is found
     * @throws ServiceException
     */
    public static Set<String> getEmailAddresses(Account acct) throws ServiceException {
        Set<String> addrs = new HashSet<String> ();

        addrs.add(acct.getName().toLowerCase());
        addrs.add(AccountUtil.getCanonicalAddress(acct).toLowerCase());

        String[] accountAliases = acct.getMailAlias();
        for (String addr : accountAliases)
            addrs.add(addr.toLowerCase());

        String[] allowedFromAddrs = acct.getMultiAttr(Provisioning.A_zimbraAllowFromAddress);
        for (String addr : allowedFromAddrs)
            addrs.add(addr.toLowerCase());

        return addrs;
    }

    public static long getMaxInternalShareLifetime(Account account, MailItem.Type folderType) {
        switch (folderType) {
            case DOCUMENT:
                return account.getFileShareLifetime();
            case UNKNOWN:
                return AccountUtil.minShareLifetime(
                        account.getFileShareLifetime(), account.getShareLifetime());
            default:
                return account.getShareLifetime();
        }
    }

    public static long getMaxExternalShareLifetime(Account account, MailItem.Type folderType) {
        switch (folderType) {
            case DOCUMENT:
                return account.getFileExternalShareLifetime();
            case UNKNOWN:
                return AccountUtil.minShareLifetime(
                        account.getFileExternalShareLifetime(), account.getExternalShareLifetime());
            default:
                return account.getExternalShareLifetime();
        }
    }

    public static long getMaxPublicShareLifetime(Account account, MailItem.Type folderType) {
        switch (folderType) {
            case DOCUMENT:
                return account.getFilePublicShareLifetime();
            case UNKNOWN:
                return AccountUtil.minShareLifetime(
                        account.getFilePublicShareLifetime(), account.getPublicShareLifetime());
            default:
                return account.getPublicShareLifetime();
        }
    }

    private static long minShareLifetime(long shareLifetime1, long shareLifetime2) {
        // 0 means there's no limit on lifetime
        if (shareLifetime1 == 0) {
            return shareLifetime2;
        } else if (shareLifetime2 == 0) {
            return shareLifetime1;
        } else {
            return Math.min(shareLifetime1, shareLifetime2);
        }
    }

    public static class AccountAddressMatcher {
        private Account account;
        private boolean matchSendAs;
        private Set<String> addresses;

        public AccountAddressMatcher(Account account) throws ServiceException {
            this(account, false, false);
        }

        public AccountAddressMatcher(Account account, boolean internalOnly) throws ServiceException {
            this(account, internalOnly, false);
        }

        /**
         * 
         * @param account
         * @param internalOnly only match internal addresses, i.e. ignore zimbraAllowFromAddress values
         * @param matchSendAs match sendAs/sendAsDistList addresses granted
         * @throws ServiceException
         */
        public AccountAddressMatcher(Account account, boolean internalOnly, boolean matchSendAs) throws ServiceException {
            this.account = account;
            this.matchSendAs = matchSendAs;

            addresses = new HashSet<String>();
            String mainAddr = account.getName();
            if (!StringUtil.isNullOrEmpty(mainAddr)) {
                addresses.add(mainAddr.toLowerCase());
            }
            String canonAddr = getCanonicalAddress(account);
            if (!StringUtil.isNullOrEmpty(canonAddr)) {
                addresses.add(canonAddr.toLowerCase());
            }
            String[] aliases = account.getMailAlias();
            if (aliases != null) {
                for (String alias : aliases) {
                    if (!StringUtil.isNullOrEmpty(alias)) {
                        addresses.add(alias.toLowerCase());
                    }
                }
            }
            if (!internalOnly) {
                String[] addrs = account.getMultiAttr(Provisioning.A_zimbraAllowFromAddress);
                if (addrs != null) {
                    for (String addr : addrs) {
                        if (!StringUtil.isNullOrEmpty(addr)) {
                            addresses.add(addr.toLowerCase());
                        }
                    }
                }
            }
        }

        public boolean matches(String address) throws ServiceException {
            return matches(address, true);
        }

        private boolean matches(String address, boolean checkDomainAlias) throws ServiceException {
            if (StringUtil.isNullOrEmpty(address)) {
                return false;
            }
            if (addresses.contains(address.toLowerCase())) {
                return true;
            }
            boolean match = false;
            if (checkDomainAlias) {
                try {
                    String addrByDomainAlias = Provisioning.getInstance().getEmailAddrByDomainAlias(address);
                    if (addrByDomainAlias != null) {
                        match = matches(addrByDomainAlias, false);  // Assume domain aliases are never chained.
                    }
                } catch (ServiceException e) {
                    ZimbraLog.account.warn("unable to get addr by alias domain" + e);
                }
            }
            if (!match && matchSendAs) {
                match = AccessManager.getInstance().canSendAs(account, account, address, false);
            }
            if (match) {
                addresses.add(address.toLowerCase());  // Cache for later matches() calls.
            }
            return match;
        }
    }

    public static String getSoapUri(Account account) {
        String base = getBaseUri(account);
        return (base == null ? null : base + AccountConstants.USER_SERVICE_URI);
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
            acct = prov.get(Key.AccountBy.id, id, authToken);
        } catch (ServiceException se) {
            ZimbraLog.misc.warn("unable to lookup account for log, id: " + id, se);
        }
        if (acct == null) {
            ZimbraLog.addToContext(idOnlyKey, id);
        } else {
            ZimbraLog.addToContext(nameKey, acct.getName());
        }
    }
    
    /**
     * Check if given account is a galsync account
     * @param account to lookup
     * @return true if account is galsync account, false otherwise.
     */
    
    public static boolean isGalSyncAccount(Account account) {
        boolean isGalSync = false;    	
        try {
            Domain domain = Provisioning.getInstance().getDomain(account);
            if (domain != null) {
                for (String galAcctId : domain.getGalAccountId()) {
                    if (galAcctId.equals(account.getId())) {
                        isGalSync = true;
                        break;
                    }
                }
            }
        } catch (ServiceException e) {
            ZimbraLog.misc.warn("unable to lookup domain for account, id: " + account.getId());
        }
        return isGalSync;
    }

    /**
     * True if accountId is the "local@host.local" special account of ZDesktop.
     * @param accountId
     * @return
     */
    public static boolean isZDesktopLocalAccount(String accountId) {
        String zdLocalAcctId = LC.zdesktop_local_account_id.value();
        return zdLocalAcctId != null && zdLocalAcctId.equalsIgnoreCase(accountId);
    }

    public static String[] getAllowedSendAddresses(NamedEntry grantor) {
        String[] addrs = grantor.getMultiAttr(Provisioning.A_zimbraPrefAllowAddressForDelegatedSender);
        if (addrs.length == 0) {
            addrs = new String[] { grantor.getName() };
        }
        return addrs;
    }

    public static boolean isAllowedSendAddress(NamedEntry grantor, String address) {
        String[] addrs = getAllowedSendAddresses(grantor);
        for (String a : addrs) {
            if (a.equalsIgnoreCase(address)) {
                return true;
            }
        }
        return false;
    }

    // returns true if address is the From address of an enabled pop/imap/caldav data source
    public static boolean isAllowedDataSourceSendAddress(Account account, String address) throws ServiceException {
        if (address == null || addressHasInternalDomain(address)) {
            // only external addresses are allowed because internal addresses require the address owner to grant send rights
            return false;
        }
        List<DataSource> dsList = Provisioning.getInstance().getAllDataSources(account);
        for (DataSource ds : dsList) {
            DataSourceType dsType = ds.getType();
            if (ds.isEnabled() &&
                (DataSourceType.pop3.equals(dsType) || DataSourceType.imap.equals(dsType) || DataSourceType.caldav.equals(dsType)) &&
                address.equalsIgnoreCase(ds.getEmailAddress())) {
                return true;
            }
        }
        return false;
    }

    // returns true if address's domain is a local domain
    // it doesn't necessarily imply an account with the address exists
    public static boolean addressHasInternalDomain(String address) throws ServiceException {
        String domain = EmailUtil.getValidDomainPart(address);
        if (domain != null) {
            Provisioning prov = Provisioning.getInstance();
            Domain internalDomain = prov.getDomain(DomainBy.name, domain, true);
            if (internalDomain != null) {
                return true;
            }
        }
        return false;
    }
}
