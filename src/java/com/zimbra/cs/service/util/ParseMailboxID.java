/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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
package com.zimbra.cs.service.util;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;

/**
 * Helper class to parse out a mailbox specifier in any of the following forms:
 *
 *      MAILBOXID         -- integer for the mailbox id
 *      foo@bar.com       -- email address for the account
 *      1230231-ac231-..  -- UID for account
 *      /SERVER/MAILBOXID -- Specific mailbox on a specific server
 *      /SERVER/*         -- ALL mailboxes on a particular server
 *      *                 -- ALL mailboxes in the system!
 *
 *  NOTENOTENOTE --  If server ID is *, then mailbox ID must be * (for simplicity sake)
 *
 * @since Mar 29, 2005
 * @author tim
 */
public final class ParseMailboxID {

    private String hostname = null; // if not localhost
    private int mailboxId = 0;
    private boolean isLocal = false;
    private boolean allMailboxIds = false;
    private boolean allServers = false;
    private String initialString;
    private Account account;

    private ParseMailboxID() {
    }

    private ParseMailboxID(Account acct, String idStr) throws ServiceException {
        init(acct,idStr);
    }

    /**
     * Parse the ID from a string
     */
    public static ParseMailboxID parse(String idStr) throws ServiceException {
        try {
            return new ParseMailboxID(idStr);
        } catch (IllegalArgumentException e) {
            throw ServiceException.FAILURE("Error parsing MailboxID specifier: "+idStr, e);
        }
    }

    /**
     * Create an ID which represents ALL mailboxes on a specified server (same as the string "/serverid/*")
     */
    public static ParseMailboxID serverAll(String serverID) throws ServiceException {
        return parse("/" + serverID + "/*");
    }

    /**
     * @return the specified mailbox identifier - useful if after checking isLocal() you decide
     * you need to pass this request on to another server.
     */
    public String getString() {
        return initialString;
    };

    @Override
    public String toString()  {
        return getString();
    };

    /**
     * @return TRUE if the mailbox is owned by this server
     */
    public boolean isLocal() {
        return isLocal;
    }

    public Account getAccount() {
        return account;
    }

    /**
     * @return the server ID.  This ALWAYS returns NULL if isLocal is true!
     */
    public String getServer() {
        return hostname;
    }

    /**
     * @return the integer ID part of the mailbox, if we have one.  Note that if the
     * mailbox is nonlocal, then we may not have this value...
     */
    public int getMailboxId() {
        return mailboxId;
    }

    /**
     * @return true if The specifier was a "*"ed wildcard which means ALL mailbox ID's.
     */
    public boolean isAllMailboxIds() {
        return allMailboxIds;
    }

    /**
     * @return true if the specifier was a "*"ed wildcard which means ALL servers.
     */
    public boolean isAllServers() {
        return allServers;
    }

    private ParseMailboxID(Account account) throws ServiceException, IllegalArgumentException {
        init(account, null);
    }

    private void init(Account account, String idStr) throws ServiceException, IllegalArgumentException {
        this.account = account;
        hostname = account.getAttr(Provisioning.A_zimbraMailHost);
        initialString = (idStr==null)?account.getId():idStr;
        if (Provisioning.onLocalServer(account)) {
            isLocal = true;
        }
    }

    private ParseMailboxID(String idStr) throws ServiceException, IllegalArgumentException {
        initialString = idStr;
        Account acct = null;
        if (idStr.indexOf('@') >= 0) { // email
            acct = Provisioning.getInstance().get(AccountBy.name, idStr);
            if (acct == null) {
                throw AccountServiceException.NO_SUCH_ACCOUNT(idStr);
            }
            init(acct, idStr);
        } else if (idStr.indexOf('/') >= 0) { // /server/mailboxid

            String[] substrs = idStr.split("/");

            if ((substrs.length != 3) || (substrs[0].length() != 0)) {
                if (substrs.length == 2) {
                    throw new IllegalArgumentException("Invalid mailboxID (missing initial '/' ?): "+idStr);
                } else {
                    throw new IllegalArgumentException("Invalid MailboxID: "+idStr);
                }
            }

            hostname = substrs[1];
            if (hostname.equals("*")) {
                allServers = true;
            }

            if (substrs[2].startsWith("*")) {
                allMailboxIds = true;
            } else {
                if (allServers) {
                    throw new IllegalArgumentException("Invalid mailboxID (\"*/number is not allowed): "+ idStr);
                }
                mailboxId = Integer.parseInt(substrs[2]);
            }
            isLocal = hostname.equals(Provisioning.getInstance().getLocalServer().getServiceHostname());
        } else if (idStr.indexOf('-') >= 0) { // UID
            acct = Provisioning.getInstance().get(AccountBy.id, idStr);
            if (acct == null) {
                throw AccountServiceException.NO_SUCH_ACCOUNT(idStr);
            }
            init(acct, idStr);
        } else {
            if (idStr.equals("*")) {
                hostname = "*";
                allMailboxIds = true;
                allServers = true;
            } else {
                mailboxId = Integer.parseInt(idStr);
                isLocal = true;
            }
        }
    }

    private void initAllMailboxes() {
        hostname = "*";
        allMailboxIds = true;
        allServers = true;
    }

    public static ParseMailboxID allMailboxes() {
        ParseMailboxID pmid = new ParseMailboxID();
        pmid.initAllMailboxes();
        return pmid;
    }

    public static ParseMailboxID byAccount(Account acct) throws ServiceException {
        return new ParseMailboxID(acct, null);
    }

    public static ParseMailboxID byEmailAddress(String idStr) throws ServiceException {
        Account acct = Provisioning.getInstance().get(AccountBy.name, idStr);
        if (acct == null) {
            throw AccountServiceException.NO_SUCH_ACCOUNT(idStr);
        }
        return new ParseMailboxID(acct, idStr);
    }

    public static ParseMailboxID byAccountId(String idStr) throws ServiceException {
        Account acct = Provisioning.getInstance().get(AccountBy.id, idStr);
        if (acct == null) {
            throw AccountServiceException.NO_SUCH_ACCOUNT(idStr);
        }
        return new ParseMailboxID(acct, idStr);
    }

}
