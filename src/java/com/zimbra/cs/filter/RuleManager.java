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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Oct 28, 2004
 */
package com.zimbra.cs.filter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.mail.MessagingException;

import org.apache.commons.collections.map.LRUMap;
import org.apache.jsieve.SieveException;
import org.apache.jsieve.SieveFactory;
import org.apache.jsieve.parser.generated.Node;
import org.apache.jsieve.parser.generated.ParseException;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.SharedDeliveryContext;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.ZimbraLog;
import com.zimbra.soap.Element;

/**
 * @author kchen
 */
public class RuleManager {
    // xxx: may need to store this in db so that it can be tuned
    static final int MAX_CACHED_SCRIPTS = 200;
    
    private static RuleManager mInstance = new RuleManager();
    /** maps account id to its parsed sieve rules */
    private Map mScriptCache;

    public static RuleManager getInstance() {
        return mInstance;
    }
    
    private RuleManager() {
        mScriptCache = Collections.synchronizedMap(new LRUMap(MAX_CACHED_SCRIPTS));
    }

    /**
     * Saves the filter rules.
     * 
     * @param account the account for which the rules are to be saved
     * @param script the sieve script
     * @throws ServiceException
     */
    public void setRules(Account account, String script) throws ServiceException {
        String accountId = account.getId();
        Mailbox mailbox =  Mailbox.getMailboxByAccount(account);
        try {
            synchronized (mailbox) {
                // invalidate the existing script
                mScriptCache.remove(accountId);
                Node node = parse(accountId, script);
                SieveFactory sieveFactory = SieveFactory.getInstance();
                // evaluate against dummy mail adapter to catch more errors
                sieveFactory.evaluate(new DummyMailAdapter(), node);
                // save 
                Map attrs = new HashMap(3);
                attrs.put(Provisioning.A_zimbraMailSieveScript, script);
                Provisioning.getInstance().modifyAttrs(account, attrs);
                mScriptCache.put(accountId, node);
            }

        } catch (ParseException e) {
            throw ServiceException.PARSE_ERROR("parsing Sieve script", e);
        } catch (SieveException e) {
            throw ServiceException.PARSE_ERROR("evaluating Sieve script", e);
        }
    }
     
    public String getRules(Account account) throws ServiceException {
        String script = account.getAttr(Provisioning.A_zimbraMailSieveScript);
        return script;
    }
    
    public Element getRulesAsXML(Element parent, Account account) throws ServiceException {
        try {
            Mailbox mbox = Mailbox.getMailboxByAccount(account);
            Node node = null;
            synchronized (mbox) {
                node = (Node) mScriptCache.get(account.getId());
                if (node == null) {
                    String script = getRules(account);
                    if (script == null) {
                        script = "";
                    }
                    String accountId = account.getId();
                    node = parse(accountId, script);
                    mScriptCache.put(accountId, node);
                }
            }
            RuleRewriter t = new RuleRewriter(parent, node);
            return t.getElement();
        } catch (ParseException e) {
            throw ServiceException.PARSE_ERROR("parsing Sieve script", e);
        }
    }

    public void setXMLRules(Account account, Element eltRules) throws ServiceException {
        RuleRewriter t = new RuleRewriter(eltRules, Mailbox.getMailboxByAccount(account));
        String script = t.getScript();
        setRules(account, script);
    }
    
    public Message applyRules(Account account, Mailbox mailbox, ParsedMessage pm, int size, 
            String recipient, SharedDeliveryContext sharedDeliveryCtxt) 
    	throws IOException, MessagingException, ServiceException
    {
        Message msg = null;
        try {
            Node node = null;
            synchronized (mailbox) {
                /*
                 * Need to hold lock on mailbox because the script may be modified and concurrently saved 
                 * by another thread. Inconsistent scenario without synchronization goes like this:
                 *   t1 is trying to save the script, t2 is trying to get the script to eval a mail delivery
                 *   t1 invalidates the cache
                 *   t2 doesn't find the script in the cache
                 *   t2 then fetches the old script from ldap and parse it
                 *   t1 finishes eval the new script and saves it to ldap
                 *   t1 caches the new script
                 *   t2 caches the old script
                 * subsequently all delivery threads will be using the cached old script
                 */
                node = (Node) mScriptCache.get(account.getId());
                if (node == null) {
                    String script = getRules(account);
                    if (script != null) {
                        String accountId = account.getId();
                        node = parse(accountId, script);
                        mScriptCache.put(accountId, node);
                    }
                }
            }
            
            ZimbraMailAdapter mailAdapter = new ZimbraMailAdapter(
                    mailbox, pm, recipient, sharedDeliveryCtxt);
            if (node != null) {
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
                        false, Flag.FLAG_UNREAD, null, recipient, sharedDeliveryCtxt);
            }
        } catch (ParseException e) {
            ZimbraLog.filter.warn("Sieve script parsing error:", e);
            // filtering system generates errors; 
            // ignore filtering and file the message into INBOX
            msg = mailbox.addMessage(null, pm, Mailbox.ID_FOLDER_INBOX,
                    false, Flag.FLAG_UNREAD, null, recipient, sharedDeliveryCtxt);
        }
        return msg;
    }
    
    /*
     * Parses the sieve script and cache the result. 
     */
    private Node parse(String accountId, String script) throws ParseException {
        // caller is already synchronized on mailbox.
        ByteArrayInputStream sin = new ByteArrayInputStream(script
                .getBytes());
        Node node = SieveFactory.getInstance().parse(sin);
        return node;
    }    
}
