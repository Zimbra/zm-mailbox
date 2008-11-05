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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.MessagingException;

import org.apache.jsieve.ConfigurationManager;
import org.apache.jsieve.SieveFactory;
import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.parser.generated.Node;
import org.apache.jsieve.parser.generated.ParseException;
import org.apache.jsieve.parser.generated.TokenMgrError;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.ElementFactory;
import com.zimbra.common.soap.Element.XMLElement;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.SharedDeliveryContext;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.SpamHandler;

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

    static {
        // Initialize custom jSieve extensions
        try {
            @SuppressWarnings("unchecked")
            Map<String, String> commandMap = ConfigurationManager.getInstance().getCommandMap();
            commandMap.put("disabled_if", com.zimbra.cs.filter.jsieve.DisabledIf.class.getName());
            commandMap.put("tag", com.zimbra.cs.filter.jsieve.Tag.class.getName());
            commandMap.put("flag", com.zimbra.cs.filter.jsieve.Flag.class.getName());
            
            @SuppressWarnings("unchecked")
            Map<String, String> testMap = ConfigurationManager.getInstance().getTestMap();
            testMap.put("date", com.zimbra.cs.filter.jsieve.DateTest.class.getName());
            testMap.put("body", com.zimbra.cs.filter.jsieve.BodyTest.class.getName());
            testMap.put("attachment", com.zimbra.cs.filter.jsieve.AttachmentTest.class.getName());
            testMap.put("addressbook", com.zimbra.cs.filter.jsieve.AddressBookTest.class.getName());
        } catch (SieveException e) {
            ZimbraLog.filter.error("Unable to initialize mail filtering extensions.", e);
        }
    }
    
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
    public static void setRules(Account account, String script) throws ServiceException {
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
    public static String getRules(Account account) {
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
     * Returns the <tt>Account</tt>'s filter rules as an XML element tree.  Uses
     * the old Sieve-style response format.
     * 
     * @param factory used to create new XML elements
     * @param account the account
     */
    public Element getRulesAsXML(ElementFactory factory, Account account) throws ServiceException {
        return getRulesAsXML(factory, account, false);
    }
    
    /**
     * Returns the XML representation of a user's filter rules.
     * 
     * @param factory used to create elements
     * @param account the user account
     * @param useNewFormat if <tt>true</tt>, returns the new response format instead of
     *                     the old Sieve-style one
     */
    public Element getRulesAsXML(ElementFactory factory, Account account, boolean useNewFormat)
    throws ServiceException {
        Node node = null;
        try {
            node = getRulesNode(account);
        } catch (ParseException e) {
            throw ServiceException.PARSE_ERROR("parsing Sieve script", e);
        } catch (TokenMgrError e) {
            throw ServiceException.PARSE_ERROR("parsing Sieve script", e);
        }

        String script = account.getAttr(Provisioning.A_zimbraMailSieveScript);
        List<String> ruleNames = getRuleNames(script);

        if (!useNewFormat) {
            RuleRewriter t = RuleRewriterFactory.getInstance().createRuleRewriter(factory, node, ruleNames);
            return t.getElement();
        } else {
            SieveToSoap sieveToSoap = new SieveToSoap(factory, ruleNames);
            sieveToSoap.accept(node);
            return sieveToSoap.getRootElement();
        }
    }
    
    private static final Pattern PAT_RULE_NAME = Pattern.compile("# (.*)");
    
    /**
     * Kind of hacky, but works for now.  Rule names are encoded into the comment preceding
     * the rule.  Return the values of all lines that begin with <tt>"# "</tt>.
     */
    public static List<String> getRuleNames(String script) {
        List<String> names = new ArrayList<String>();
        if (script != null) {
            BufferedReader reader = new BufferedReader(new StringReader(script));
            String line = null;
            try {
                while ((line = reader.readLine()) != null){
                    Matcher matcher = PAT_RULE_NAME.matcher(line);
                    if (matcher.matches()) {
                        names.add(matcher.group(1));
                    }
                }
            } catch (IOException e) {
                ZimbraLog.filter.warn("Unable to determine filter rule names.", e);
            }
        }
        return names;
    }

    /**
     * Sets filter rules, specified as an XML element tree.  Uses the old
     * Sieve-style XML format.
     */
    public void setXMLRules(Account account, Element eltRules) throws ServiceException {
        setXMLRules(account, eltRules, false);
    }
    
    public void setXMLRules(Account account, Element eltRules, boolean useNewFormat)
    throws ServiceException {
        if (!useNewFormat) {
            RuleRewriter t = RuleRewriterFactory.getInstance().createRuleRewriter(eltRules, MailboxManager.getInstance().getMailboxByAccount(account));
            String script = t.getScript();
            setRules(account, script);
        } else {
            SoapToSieve soapToSieve = new SoapToSieve(eltRules);
            String script = soapToSieve.getSieveScript();
            setRules(account, script);
        }
    }
    
    /**
     * Adds a message to a mailbox.  If filter rules exist, processes
     * the filter rules.  Otherwise, files to <tt>Inbox</tt> or <tt>Spam</tt>.
     * 
     * @return the list of message id's that were added, or an empty list.
     */
    public List<ItemId> applyRules(Mailbox mailbox, ParsedMessage pm,
            String recipient, SharedDeliveryContext sharedDeliveryCtxt, int incomingFolderId)
    throws IOException, MessagingException, ServiceException {
        List<ItemId> addedMessageIds = null;
        
        try {
            Account account = mailbox.getAccount();
            Node node = getRulesNode(account);
            ZimbraMailAdapter mailAdapter = new ZimbraMailAdapter(
                    mailbox, pm, recipient, sharedDeliveryCtxt, incomingFolderId);
            
            // Determine whether to apply rules
            boolean applyRules = true;
            if (node == null) {
            	applyRules = false;
            }
            if (SpamHandler.isSpam(pm.getMimeMessage()) &&
            		!account.getBooleanAttr(Provisioning.A_zimbraSpamApplyUserFilters, false)) {
            	// Don't apply user filters to spam by default
            	applyRules = false;
            }
            
            if (applyRules) {
                SieveFactory.getInstance().evaluate(mailAdapter, node);
                // multiple fileinto may result in multiple copies of the messages in different folders
                addedMessageIds = mailAdapter.getAddedMessageIds(); 
            } else {
                addedMessageIds = new ArrayList<ItemId>(1);
                Message msg = mailAdapter.doDefaultFiling();
                if (msg != null) {
                    addedMessageIds.add(new ItemId(msg));
                }
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
                Message msg = mailbox.addMessage(null, pm, Mailbox.ID_FOLDER_INBOX,
                        false, Flag.BITMASK_UNREAD, null, recipient, sharedDeliveryCtxt);
                addedMessageIds = new ArrayList<ItemId>(1);
                addedMessageIds.add(new ItemId(msg));
            }
        } catch (ParseException e) {
            ZimbraLog.filter.warn("Unable to parse Sieve script.  Filing message in Inbox.", e);
            // filtering system generates errors; 
            // ignore filtering and file the message into INBOX
            Message msg = mailbox.addMessage(null, pm, Mailbox.ID_FOLDER_INBOX,
                    false, Flag.BITMASK_UNREAD, null, recipient, sharedDeliveryCtxt);
            addedMessageIds = new ArrayList<ItemId>(1);
            addedMessageIds.add(new ItemId(msg));
        } catch (TokenMgrError e) {
            ZimbraLog.filter.warn("Unable to parse Sieve script.  Filing message in Inbox.", e);
            // filtering system generates errors; 
            // ignore filtering and file the message into INBOX
            Message msg = mailbox.addMessage(null, pm, Mailbox.ID_FOLDER_INBOX,
                    false, Flag.BITMASK_UNREAD, null, recipient, sharedDeliveryCtxt);
            addedMessageIds = new ArrayList<ItemId>(1);
            addedMessageIds.add(new ItemId(msg));
        }
        return addedMessageIds;
    }
    
    /**
     * Parses the sieve script and returns the result. 
     */
    private static Node parse(String script) throws ParseException {
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
    public static void folderRenamed(Account account, String originalPath, String newPath)
    throws ServiceException {
        String script = getRules(account);
        if (script != null) {
            Node node = null;
            try {
                node = parse(script);
            } catch (ParseException e) {
                ZimbraLog.filter.warn("Unable to update filter rules with new folder path '%s'.", e);
                return;
            }
            FolderRenamer renamer = new FolderRenamer(originalPath, newPath);
            renamer.accept(node);
            if (renamer.renamed()) {
                // Kind of a hacky way to convert a Node tree to a script.  We
                // convert to XML first, and then to a String.  Unfortunately
                // jSieve 0.2 doesn't have an API that generates a script from
                // a Node tree.
                List<String> ruleNames = getRuleNames(script);
                SieveToSoap sieveToSoap = new SieveToSoap(XMLElement.mFactory, ruleNames);
                sieveToSoap.accept(node);
                SoapToSieve soapToSieve = new SoapToSieve(sieveToSoap.getRootElement());
                String newScript = soapToSieve.getSieveScript();
                setRules(account, newScript);
                ZimbraLog.filter.info("Updated filter rules due to folder move or rename from %s to %s.",
                    originalPath, newPath);
                ZimbraLog.filter.debug("Old rules:\n%s, new rules:\n%s", script, newScript);
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
                ZimbraLog.filter.debug("Old rules:\n%s, new rules:\n%s", rules, newRules);
            }
        }
    }
}
