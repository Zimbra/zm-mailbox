/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
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
 * Created on Oct 28, 2004
 */
package com.zimbra.cs.filter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;

import org.apache.jsieve.SieveException;
import org.apache.jsieve.SieveFactory;
import org.apache.jsieve.parser.generated.Node;
import org.apache.jsieve.parser.generated.ParseException;
import org.apache.jsieve.parser.generated.TokenMgrError;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.ElementFactory;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.filter.jsieve.ActionFlag;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.SharedDeliveryContext;
import com.zimbra.cs.mime.ParsedMessage;

/**
 * Handles setting and getting filter rules for an <tt>Account</tt>.
 */
public class RuleManager {
    /**
     * Key used to save the parsed version of a Sieve script in an <tt>Account</tt>'s
     * cached data.  The cache is invalidated whenever an <tt>Account</tt> attribute
     * is modified, so the script and parsed rules won't get out of sync.
     */
    private static final String FILTER_RULES_CACHE_KEY =
        StringUtil.getSimpleClassName(RuleManager.class.getName()) + ".FILTER_RULES_CACHE";
    private static RuleManager mInstance = new RuleManager();

    public static RuleManager getInstance() {
        return mInstance;
    }
    
    private RuleManager() {
    }

    /**
     * Saves the filter rules.
     * 
     * @param account the account for which the rules are to be saved
     * @param script the sieve script, or <code>null</code> or empty string if
     * all rules should be deleted
     * @throws ServiceException
     */
    public void setRules(Account account, String script) throws ServiceException {
        String accountId = account.getId();
        ZimbraLog.filter.debug("Setting filter rules for account %s:\n%s", accountId, script);
        if (script == null) {
            script = "";
        }
        try {
            Node node = parse(script);
            SieveFactory sieveFactory = SieveFactory.getInstance();
            // evaluate against dummy mail adapter to catch more errors
            sieveFactory.evaluate(new DummyMailAdapter(), node);
            // save 
            Map<String, Object> attrs = new HashMap<String, Object>();
            attrs.put(Provisioning.A_zimbraMailSieveScript, script);
            Provisioning.getInstance().modifyAttrs(account, attrs);
            account.setCachedData(FILTER_RULES_CACHE_KEY, node);
        } catch (ParseException e) {
            ZimbraLog.filter.error("Unable to parse script:\n" + script);
            throw ServiceException.PARSE_ERROR("parsing Sieve script", e);
        } catch (TokenMgrError e) {
            ZimbraLog.filter.error("Unable to parse script:\n" + script);
            throw ServiceException.PARSE_ERROR("parsing Sieve script", e);
        } catch (SieveException e) {
            ZimbraLog.filter.error("Unable to evaluate script:\n" + script);
            throw ServiceException.PARSE_ERROR("evaluating Sieve script", e);
        }
    }
    
    /**
     * Clears the in memory parsed filter rule cache
     * 
     * @param account the account for which the cached parsed rules are to be cleared
     */
    public void clearCachedRules(Account account) {
        account.setCachedData(FILTER_RULES_CACHE_KEY, null);
    }

    /**
     * Returns the filter rules Sieve script for the given account. 
     */
    public String getRules(Account account) {
        String script = account.getAttr(Provisioning.A_zimbraMailSieveScript);
        return script;
    }

    /**
     * Returns the parsed filter rules for the given account.  If no cached
     * copy of the parsed rules exists, parses the script returned by
     * {@link #getRules(Account)} and caches the result on the <tt>Account</tt>.
     *  
     * @see Account#setCachedData(String, Object)
     * @throws ParseException if there was an error while parsing the Sieve script
     */
    private Node getRulesNode(Account account)
    throws ParseException {
        Node node = (Node) account.getCachedData(FILTER_RULES_CACHE_KEY);
        if (node == null) {
            String script = getRules(account);
            if (script == null) {
                script = "";
            }
            node = parse(script);
            account.setCachedData(FILTER_RULES_CACHE_KEY, node);
        }
        return node;
    }
    
    /**
     * Returns the <tt>Account</tt>'s filter rules as an XML element tree.
     * 
     * @param factory used to create new XML elements
     * @param account the account
     */
    public Element getRulesAsXML(ElementFactory factory, Account account) throws ServiceException {
        Node node = null;
        try {
            node = getRulesNode(account);
        } catch (ParseException e) {
            throw ServiceException.PARSE_ERROR("parsing Sieve script", e);
        } catch (TokenMgrError e) {
            throw ServiceException.PARSE_ERROR("parsing Sieve script", e);
        }
        RuleRewriter t = new RuleRewriter(factory, node);
        return t.getElement();
    }

    /**
     * Sets filter rules, specified as an XML element tree.
     */
    public void setXMLRules(Account account, Element eltRules) throws ServiceException {
        RuleRewriter t = new RuleRewriter(eltRules, MailboxManager.getInstance().getMailboxByAccount(account));
        String script = t.getScript();
        setRules(account, script);
    }
    
    public Message applyRules(Account account, Mailbox mailbox, ParsedMessage pm, int size, 
            String recipient, SharedDeliveryContext sharedDeliveryCtxt)
		throws IOException, MessagingException, ServiceException {
    	return applyRules(account, mailbox, pm, size, recipient, sharedDeliveryCtxt, Flag.BITMASK_UNREAD);
    }
    
    //we call this api in offline for IMAP accounts' local filter rules
    //because IMAP messages could already have flags different from UNREAD synced from server
    public Message applyRules(Account account, Mailbox mailbox, ParsedMessage pm, int size, 
            String recipient, SharedDeliveryContext sharedDeliveryCtxt, int flags) 
    	throws IOException, MessagingException, ServiceException
    {
        Message msg = null;
        try {
            Node node = getRulesNode(account);
            ZimbraMailAdapter mailAdapter = new ZimbraMailAdapter(
                    mailbox, pm, recipient, sharedDeliveryCtxt);
            addFlagActions(mailAdapter, flags);
            
            // Determine whether to apply rules
            boolean applyRules = true;
            if (node == null) {
            	applyRules = false;
            }
            if (mailAdapter.isSpam() &&
            		!account.getBooleanAttr(Provisioning.A_zimbraSpamApplyUserFilters, false)) {
            	// Don't apply user filters to spam by default
            	applyRules = false;
            }
            
            if (applyRules) {
                SieveFactory.getInstance().evaluate(mailAdapter, node);
                // multiple fileinto may result in multiple copies of the messages in different folders
                Message[] msgs = mailAdapter.getProcessedMessages();
                // return only the last filed message
                if (msgs.length > 0)
                    msg = msgs[msgs.length - 1];
            } else {
                msg = mailAdapter.doDefaultFiling();
            }
        } catch (SieveException e) {
            if (e instanceof ZimbraSieveException) {
                Throwable t = ((ZimbraSieveException) e).getCause();
                if (t instanceof ServiceException) {
                    throw (ServiceException) t;
                } else if (t instanceof IOException) {
                    throw (IOException) t;
                } else if (t instanceof MessagingException) {
                    throw (MessagingException) t;
                }
            } else {
                ZimbraLog.filter.warn("Sieve error:", e);
                // filtering system generates errors; 
                // ignore filtering and file the message into INBOX
                msg = mailbox.addMessage(null, pm, Mailbox.ID_FOLDER_INBOX,
                        false, flags, null, recipient, sharedDeliveryCtxt);
            }
        } catch (ParseException e) {
            ZimbraLog.filter.warn("Unable to parse Sieve script.  Filing message in Inbox.", e);
            // filtering system generates errors; 
            // ignore filtering and file the message into INBOX
            msg = mailbox.addMessage(null, pm, Mailbox.ID_FOLDER_INBOX,
                    false, flags, null, recipient, sharedDeliveryCtxt);
        } catch (TokenMgrError e) {
            ZimbraLog.filter.warn("Unable to parse Sieve script.  Filing message in Inbox.", e);
            // filtering system generates errors; 
            // ignore filtering and file the message into INBOX
            msg = mailbox.addMessage(null, pm, Mailbox.ID_FOLDER_INBOX,
                    false, flags, null, recipient, sharedDeliveryCtxt);
        }
        return msg;
    }
    
    private void addFlagActions(ZimbraMailAdapter mailAdapter, int flags) {
    	List<Integer> flagIds = Flag.bitmaskToFlagIds(flags);
    	for (int flagId : flagIds) {
    		mailAdapter.addAction(new ActionFlag(flagId, true, ""));
    	}
    	//by default the ZimbraMailAdapter will assume UNREAD so if the flags don't contain UNREAD
    	//we need to unset the UNREAD flag
    	if ((flags & Flag.BITMASK_UNREAD) == 0)
    		mailAdapter.addAction(new ActionFlag(Flag.ID_FLAG_UNREAD, false, ""));
    }
    
    /**
     * Parses the sieve script and returns the result. 
     */
    private Node parse(String script) throws ParseException {
        ByteArrayInputStream sin = null;
        try {
            sin = new ByteArrayInputStream(script.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new ParseException(e.getMessage());
        }
        Node node = SieveFactory.getInstance().parse(sin);
        return node;
    }

    /**
     * When a folder is renamed, updates any filter rules that reference
     * that folder.
     */
    public void folderRenamed(Account account, String originalPath, String newPath)
    throws ServiceException {
        String rules = getRules(account);
        if (rules != null) {
            // Assume that we always put quotes around folder paths.  Replace
            // any paths that start with this folder's original path.  This will
            // take care of rules for children affected by a parent's move or rename.
            String newRules = rules.replace("\"" + originalPath, "\"" + newPath);
            if (!newRules.equals(rules)) {
                setRules(account, newRules);
                ZimbraLog.filter.info("Updated filter rules due to folder move or rename from %s to %s.",
                    originalPath, newPath);
                ZimbraLog.filter.debug("Old rules:\n%s, new rules:\n", rules, newRules);
            }
        }
    }
    
    /**
     * When a tag is renamed, updates any filter rules that reference
     * that tag.
     */
    public void tagRenamed(Account account, String originalName, String newName)
    throws ServiceException {
        String rules = getRules(account);
        if (rules != null) {
            String newRules = rules.replace("tag \"" + originalName + "\"", "tag \"" + newName + "\"");
            if (!newRules.equals(rules)) {
                setRules(account, newRules);
                ZimbraLog.filter.info("Updated filter rules due to tag rename from %s to %s.",
                    originalName, newName);
                ZimbraLog.filter.debug("Old rules:\n%s, new rules:\n", rules, newRules);
            }
        }
    }
}
