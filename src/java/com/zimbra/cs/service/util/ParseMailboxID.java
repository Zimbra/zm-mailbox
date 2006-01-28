/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Mar 29, 2005
 *
 */
package com.zimbra.cs.service.util;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Provisioning;

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
    /** 
     * Parse the ID from a string
     * 
     * @param idStr
     * @return
     * @throws ServiceException
     */
    public static ParseMailboxID parse(String idStr) throws ServiceException {
        try {
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
     * Return a ParseMailboxID which represents a single mailbox on the local (local to whoever 
     * processes the request!) server
     * 
     * @param id
     * @return
     * @throws ServiceException
     */
    public static ParseMailboxID localMailbox(int id) throws ServiceException
    {
        return parse(Integer.toString(id));
    }
    
    /**
     * Return a ParseMailboxID which represents a single mailbox on any server
     * 
     * @param id
     * @return
     * @throws ServiceException
     */
    public static ParseMailboxID remoteMailbox(String serverID, int id) throws ServiceException
    {
        return parse("/" + serverID + "/" + id);
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
    public boolean isAllServers() { return mAllServers; };
    
    
    protected String mHostName = null; // if not localhost
    protected Mailbox mMailbox = null;
    protected int mMailboxId = 0;
    protected boolean mIsLocal = false;
    protected boolean mAllMailboxIds = false;
    protected boolean mAllServers = false;
    protected String mInitialString;
    
    protected ParseMailboxID(String idStr, boolean forceRemote) throws ServiceException, IllegalArgumentException {
        mInitialString = idStr;
        
        Account acct = null;  
        if (idStr.indexOf('@') >= 0) {
            // account
            acct = Provisioning.getInstance().getAccountByName(idStr);
            if (acct == null) {
                throw AccountServiceException.NO_SUCH_ACCOUNT(idStr);
            }
            
            if (!forceRemote && acct.isCorrectHost()) {
                mIsLocal = true;
                mMailbox = Mailbox.getMailboxByAccountId(acct.getId());
                mMailboxId = mMailbox.getId();
            } else {
                mHostName = acct.getAttr(Provisioning.A_zimbraMailHost);
            }
            
        } else if (idStr.indexOf('-') >= 0) {
            // UID
            acct = Provisioning.getInstance().getAccountById(idStr);
            if (acct == null)
                throw AccountServiceException.NO_SUCH_ACCOUNT(idStr);

            if (!forceRemote && acct.isCorrectHost()) {
                mIsLocal = true;
                mMailbox = Mailbox.getMailboxByAccountId(acct.getId());
                mMailboxId = mMailbox.getId();
            } else {
                mHostName = acct.getAttr(Provisioning.A_zimbraMailHost);
            }
            
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
                    mMailbox = Mailbox.getMailboxById(mMailboxId);
                }
            }
        } else {
            if (idStr.equals("*")) {
                mHostName = "*";
                mAllMailboxIds = true;
                mAllServers = true;
            } else {
                mMailboxId = Integer.parseInt(idStr);
                mIsLocal = true;
                mMailbox = Mailbox.getMailboxById(mMailboxId);
            }
        }
    }
}
