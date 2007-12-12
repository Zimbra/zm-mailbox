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

/*
 * Created on Mar 29, 2005
 *
 */
package com.zimbra.cs.service.util;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;

/**
 * @author tim
 *
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
 */
public class ParseMailboxID 
{
    private static Log mLog = LogFactory.getLog(ParseMailboxID.class);
    
    /** 
     * Parse the ID from a string
     * 
     * @param idStr
     * @return
     * @throws ServiceException
     */
    public static ParseMailboxID parse(String idStr) throws ServiceException {
        try {
        	ZimbraLog.misc.info("Parsing id string %s", idStr);
            return new ParseMailboxID(idStr, false);
        } catch (IllegalArgumentException e) {
            throw ServiceException.FAILURE("Error parsing MailboxID specifier: "+idStr, e);
        }
    }

    /** 
     * Parse the ID from a string
     * 
     * @param idStr
     * @return
     * @throws ServiceException
     */
    public static ParseMailboxID parseForceRemote(String idStr) throws ServiceException {
        try {
            return new ParseMailboxID(idStr, true);
        } catch (IllegalArgumentException e) {
            throw ServiceException.FAILURE("Error parsing MailboxID specifier: "+idStr, e);
        }
    }
      
    /**
     * Create an ID which represents ALL mailboxes on a specified server
     * (same as the string "/serverid/*")
     * 
     * @param serverID
     * @return
     * @throws ServiceException
     */
    public static ParseMailboxID serverAll(String serverID) throws ServiceException 
    {
        return parse("/" + serverID + "/*");
    }
    
    /**
     * @return the specified mailbox identifier - useful if after checking isLocal() you decide
     * you need to pass this request on to another server.
     */
    public String getString() { return mInitialString; };
    public String toString()  { return getString(); };
    
    /**
     * @return TRUE if the mailbox is owned by this server
     */
    public boolean isLocal() { return mIsLocal; }
    
    
    /**
     * @return the server ID.  This ALWAYS returns NULL if isLocal is true! 
     */
    public String getServer() { return mHostName; } 
    
    
    /**
     * @return the integer ID part of the mailbox, if we have one.  Note that if the
     * mailbox is nonlocal, then we may not have this value...
     */
    public int getMailboxId() { return mMailboxId; }
    
    
    /**
     * Only possible if isLocal() is true.
     * 
     * @return mailbox
     */
    public Mailbox getMailbox() { return mMailbox; }
    
    /**
     * @return true if The specifier was a "*"ed wildcard which means ALL mailbox ID's. 
     */
    public boolean isAllMailboxIds() { return mAllMailboxIds; }
    
    
    /**
     * @return true if the specifier was a "*"ed wildcard which means ALL servers.
     */
    public boolean isAllServers() { return mAllServers; }
    
    /**
     * @return the account email address, if we have one.  Account email address is available only if the 
     * object is instanciated from an email address or account's zimbraId.
     */
    public String getEmailAddress() { return mEmailAddress; }
    
    protected String mHostName = null; // if not localhost
    protected Mailbox mMailbox = null;
    protected int mMailboxId = 0;
    protected boolean mIsLocal = false;
    protected boolean mAllMailboxIds = false;
    protected boolean mAllServers = false;
    protected String mInitialString;
    protected String mEmailAddress = null;
    
    protected ParseMailboxID(Account account, boolean forceRemote) throws ServiceException, IllegalArgumentException {
        this.initFromAccount(account, null, forceRemote);
    }
    
    protected void initFromAccount(Account account, String idStr, boolean forceRemote) throws ServiceException, IllegalArgumentException {
        mHostName = account.getAttr(Provisioning.A_zimbraMailHost);
        mInitialString = (idStr==null)?account.getId():idStr;
        mEmailAddress = account.getName();
    	if (!forceRemote &&  Provisioning.onLocalServer(account)) {
    		ZimbraLog.misc.info("Account %s is local", account.getId());
            mIsLocal = true;
            mMailbox = MailboxManager.getInstance().getMailboxByAccountId(account.getId());
            mMailboxId = mMailbox.getId();
    		ZimbraLog.misc.info("Account id %s, mailbox id %s", account.getId(),mMailbox.getId());
        } else {
        	ZimbraLog.misc.info("Account %s is not local", account.getId());
        }
    }
    
    
    protected ParseMailboxID(String idStr, boolean forceRemote) throws ServiceException, IllegalArgumentException {
           
        Account acct = null;  
        if (idStr.indexOf('@') >= 0) {
            // account
            acct = Provisioning.getInstance().get(AccountBy.name, idStr);
            if (acct == null) {
                throw AccountServiceException.NO_SUCH_ACCOUNT(idStr);
            }
            
            this.initFromAccount(acct,idStr,forceRemote);
            
        } else if (idStr.indexOf('/') >= 0) {
            /* /server/mailboxid */
            
            String[] substrs = idStr.split("/");
            
            if ((substrs.length != 3) || (substrs[0].length() != 0)) {
                if (substrs.length == 2) {
                    throw new IllegalArgumentException("Invalid mailboxID (missing initial '/' ?): "+idStr);
                } else {
                    throw new IllegalArgumentException("Invalid MailboxID: "+idStr);
                }
            }
            
            mHostName = substrs[1];
            if (mHostName.equals("*")) {
                mAllServers = true;
            }
            
            if (substrs[2].startsWith("*")) {
                mAllMailboxIds = true;
            } else {
                if (mAllServers==true) {
                    throw new IllegalArgumentException("Invalid mailboxID (\"*/number is not allowed): "+ idStr);
                }
                mMailboxId = Integer.parseInt(substrs[2]);
            }
                
            
            String localhost = Provisioning.getInstance().getLocalServer().getAttr(Provisioning.A_zimbraServiceHostname);
            if (mHostName.equals(localhost)) {
                mIsLocal = true;
                mHostName = null;
                if (!mAllMailboxIds) {
                    mMailbox = MailboxManager.getInstance().getMailboxById(mMailboxId);
                }
            }
        } else if (idStr.indexOf('-') >= 0) {
            // UID
            acct = Provisioning.getInstance().get(AccountBy.id, idStr);
            if (acct == null)
                throw AccountServiceException.NO_SUCH_ACCOUNT(idStr);

            this.initFromAccount(acct,idStr,forceRemote);
            
        }  else {
            if (idStr.equals("*")) {
                mHostName = "*";
                mAllMailboxIds = true;
                mAllServers = true;
            } else {
                mMailboxId = Integer.parseInt(idStr);
                mIsLocal = true;
                mMailbox = MailboxManager.getInstance().getMailboxById(mMailboxId);
            }
        }
    }
    
    
    // =========================================
    // =========================================
    // =========================================
    
    private ParseMailboxID() {
    }
    
    private ParseMailboxID(Account acct, String idStr, boolean forceRemote) throws ServiceException {
        initFromAccount(acct,idStr,forceRemote);
    }
    
    private void initAllMailboxes() {
        mHostName = "*";
        mAllMailboxIds = true;
        mAllServers = true;
    }
        
    public static ParseMailboxID allMailboxes() {
        ParseMailboxID pmid = new ParseMailboxID();
        pmid.initAllMailboxes();
        return pmid;
    }
    
    public static ParseMailboxID byAccount(Account acct) throws ServiceException {
        mLog.debug("byAccount %s %s", acct.getName(), acct.getId());
        return new ParseMailboxID(acct, null, false);
    }
    
    public static ParseMailboxID byEmailAddress(String idStr) throws ServiceException {
        mLog.debug("byEmailAddress %s", idStr);
        return byEmailAddress(idStr, false);
    }
    
    public static ParseMailboxID byAccountId(String idStr) throws ServiceException {
        mLog.debug("byAccountId %s", idStr);
        return byAccountId(idStr, false);
    }
    
    private static ParseMailboxID byEmailAddress(String idStr, boolean forceRemote) throws ServiceException {
        
        Account acct = Provisioning.getInstance().get(AccountBy.name, idStr);
        if (acct == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(idStr);
        
        return new ParseMailboxID(acct, idStr, forceRemote);
    }
    
    private static ParseMailboxID byAccountId(String idStr, boolean forceRemote) throws ServiceException {
        Account acct = Provisioning.getInstance().get(AccountBy.id, idStr);
        if (acct == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(idStr);

        return new ParseMailboxID(acct, idStr, forceRemote);
    }

}
