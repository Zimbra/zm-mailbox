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
            return new ParseMailboxID(idStr);
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
    public String getString() { return initialString; };
    public String toString()  { return getString(); };
    
    /**
     * @return TRUE if the mailbox is owned by this server
     */
    public boolean isLocal() { return isLocal; }
    
    
    /**
     * @return the server ID.  This ALWAYS returns NULL if isLocal is true! 
     */
    public String getServer() { return hostName; } 
    
    
    /**
     * @return the integer ID part of the mailbox, if we have one.  Note that if the
     * mailbox is nonlocal, then we may not have this value...
     */
    public int getMailboxId() { return mailboxId; }
    
    
    /**
     * Only possible if isLocal() is true.
     * 
     * @return mailbox
     */
    public Mailbox getMailbox() { return mailbox; }
    
    /**
     * @return true if The specifier was a "*"ed wildcard which means ALL mailbox ID's. 
     */
    public boolean isAllMailboxIds() { return allMailboxIds; }
    
    
    /**
     * @return true if the specifier was a "*"ed wildcard which means ALL servers.
     */
    public boolean isAllServers() { return allServers; };
    
    
    protected String hostName = null; // if not localhost
    protected Mailbox mailbox = null;
    protected int mailboxId = 0;
    protected boolean isLocal = false;
    protected boolean allMailboxIds = false;
    protected boolean allServers = false;
    protected String initialString;
    
    protected ParseMailboxID(String idStr) throws ServiceException, IllegalArgumentException {
        initialString = idStr;
        
        Account acct = null;  
        if (idStr.indexOf('@') >= 0) {
            // account
            acct = Provisioning.getInstance().getAccountByName(idStr);
            if (acct == null) {
                throw AccountServiceException.NO_SUCH_ACCOUNT(idStr);
            }
            
            if (acct.isCorrectHost()) {
                isLocal = true;
                mailbox = Mailbox.getMailboxByAccountId(acct.getId());
                mailboxId = mailbox.getId();
            } else {
                hostName = acct.getAttr(Provisioning.A_liquidMailHost);
            }
            
        } else if (idStr.indexOf('-') >= 0) {
            // UID
            acct = Provisioning.getInstance().getAccountById(idStr);
            
            if (acct.isCorrectHost()) {
                isLocal = true;
                mailbox = Mailbox.getMailboxByAccountId(acct.getId());
                mailboxId = mailbox.getId();
            } else {
                hostName = acct.getAttr(Provisioning.A_liquidMailHost);
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
            
            hostName = substrs[1];
            if (hostName.equals("*")) {
                allServers = true;
            }
            
            if (substrs[2].startsWith("*")) {
                allMailboxIds = true;
            } else {
                if (allServers==true) {
                    throw new IllegalArgumentException("Invalid mailboxID (\"*/number is not allowed): "+ idStr);
                }
                mailboxId = Integer.parseInt(substrs[2]);
            }
                
            
            String localhost = Provisioning.getInstance().getLocalServer().getAttr(Provisioning.A_liquidServiceHostname);
            if (hostName.equals(localhost)) {
                isLocal = true;
                hostName = null;
                if (!allMailboxIds) {
                    mailbox = Mailbox.getMailboxById(mailboxId);
                }
            }
        } else {
            if (idStr.equals("*")) {
                hostName = "*";
                allMailboxIds = true;
                allServers = true;
            } else {
                mailboxId = Integer.parseInt(idStr);
                isLocal = true;
                mailbox = Mailbox.getMailboxById(mailboxId);
            }
        }
    }
}
