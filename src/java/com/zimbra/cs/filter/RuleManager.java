/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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

package com.zimbra.cs.filter;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.DeliveryContext;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.SpamHandler;
import com.zimbra.soap.mail.type.FilterRule;

import org.apache.jsieve.ConfigurationManager;
import org.apache.jsieve.SieveFactory;
import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.parser.generated.Node;
import org.apache.jsieve.parser.generated.ParseException;
import org.apache.jsieve.parser.generated.TokenMgrError;

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

/**
 * Handles setting and getting filter rules for an <tt>Account</tt>,
 * and executing filter rules on a message.
 */
public final class RuleManager {
    /**
     * Keys used to save the parsed version of a Sieve script in an <tt>Account</tt>'s
     * cached data.  The cache is invalidated whenever an <tt>Account</tt> attribute
     * is modified, so the script and parsed rules won't get out of sync.
     */
    private static final String FILTER_RULES_CACHE_KEY =
        RuleManager.class.getSimpleName() + ".FILTER_RULES_CACHE";
    private static final String OUTGOING_FILTER_RULES_CACHE_KEY =
        RuleManager.class.getSimpleName() + ".OUTGOING_FILTER_RULES_CACHE";

    private static final SieveFactory SIEVE_FACTORY = createSieveFactory();

    private RuleManager() {
    }

    private static SieveFactory createSieveFactory() {
        // Initialize custom jSieve extensions
        ConfigurationManager mgr;
        try {
            mgr = new ConfigurationManager();
        } catch (SieveException e) {
            ZimbraLog.filter.error("Unable to initialize mail filtering extensions.", e);
            return null;
        }

        Map<String, String> commandMap = mgr.getCommandMap();
        commandMap.put("disabled_if", com.zimbra.cs.filter.jsieve.DisabledIf.class.getName());
        commandMap.put("tag", com.zimbra.cs.filter.jsieve.Tag.class.getName());
        commandMap.put("flag", com.zimbra.cs.filter.jsieve.Flag.class.getName());
        commandMap.put("reply", com.zimbra.cs.filter.jsieve.Reply.class.getName());
        commandMap.put("notify", com.zimbra.cs.filter.jsieve.Notify.class.getName());
        commandMap.put("discard", com.zimbra.cs.filter.jsieve.Discard.class.getName());

        Map<String, String> testMap = mgr.getTestMap();
        testMap.put("date", com.zimbra.cs.filter.jsieve.DateTest.class.getName());
        testMap.put("body", com.zimbra.cs.filter.jsieve.BodyTest.class.getName());
        testMap.put("attachment", com.zimbra.cs.filter.jsieve.AttachmentTest.class.getName());
        testMap.put("addressbook", com.zimbra.cs.filter.jsieve.AddressBookTest.class.getName());
        testMap.put("contact_ranking", com.zimbra.cs.filter.jsieve.ContactRankingTest.class.getName());
        testMap.put("me", com.zimbra.cs.filter.jsieve.MeTest.class.getName());
        testMap.put("invite", com.zimbra.cs.filter.jsieve.InviteTest.class.getName());
        testMap.put("mime_header", com.zimbra.cs.filter.jsieve.MimeHeaderTest.class.getName());
        testMap.put("current_time", com.zimbra.cs.filter.jsieve.CurrentTimeTest.class.getName());
        testMap.put("current_day_of_week", com.zimbra.cs.filter.jsieve.CurrentDayOfWeekTest.class.getName());
        testMap.put("conversation", com.zimbra.cs.filter.jsieve.ConversationTest.class.getName());
        testMap.put("facebook", com.zimbra.cs.filter.jsieve.FacebookTest.class.getName());
        testMap.put("linkedin", com.zimbra.cs.filter.jsieve.LinkedInTest.class.getName());
        testMap.put("socialcast", com.zimbra.cs.filter.jsieve.SocialcastTest.class.getName());
        testMap.put("twitter", com.zimbra.cs.filter.jsieve.TwitterTest.class.getName());
        testMap.put("list", com.zimbra.cs.filter.jsieve.ListTest.class.getName());
        testMap.put("bulk", com.zimbra.cs.filter.jsieve.BulkTest.class.getName());
        testMap.put("importance", com.zimbra.cs.filter.jsieve.ImportanceTest.class.getName());
        testMap.put("flagged", com.zimbra.cs.filter.jsieve.FlaggedTest.class.getName());
        return mgr.build();
    }

    public static SieveFactory getSieveFactory() {
        return SIEVE_FACTORY;
    }

    /**
     * Saves the filter rules.
     *
     * @param account the account for which the rules are to be saved
     * @param script the sieve script, or <code>null</code> or empty string if
     * all rules should be deleted
     * @param sieveScriptAttrName
     * @param rulesCacheKey
     * @throws ServiceException
     */
    private static void setRules(Account account, String script, String sieveScriptAttrName, String rulesCacheKey)
            throws ServiceException {
        String accountId = account.getId();
        ZimbraLog.filter.debug("Setting filter rules for account %s:\n%s", accountId, script);
        if (script == null) {
            script = "";
        }
        try {
            Node node = parse(script);
            // evaluate against dummy mail adapter to catch more errors
            SIEVE_FACTORY.evaluate(new DummyMailAdapter(), node);
            // save
            Map<String, Object> attrs = new HashMap<String, Object>();
            attrs.put(sieveScriptAttrName, script);
            Provisioning.getInstance().modifyAttrs(account, attrs);
            account.setCachedData(rulesCacheKey, node);
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
    public static void clearCachedRules(Account account) {
        account.setCachedData(FILTER_RULES_CACHE_KEY, null);
        account.setCachedData(OUTGOING_FILTER_RULES_CACHE_KEY, null);
    }

    /**
     * Returns the incoming filter rules Sieve script for the given account.
     */
    public static String getIncomingRules(Account account) {
        return getRules(account, Provisioning.A_zimbraMailSieveScript);
    }

    /**
     * Returns the outgoing filter rules Sieve script for the given account.
     */
    public static String getOutgoingRules(Account account) {
        return getRules(account, Provisioning.A_zimbraMailOutgoingSieveScript);
    }

    private static String getRules(Account account, String sieveScriptAttrName) {
        return account.getAttr(sieveScriptAttrName);
    }

    /**
     * Returns the parsed filter rules for the given account.  If no cached
     * copy of the parsed rules exists, parses the script returned by
     * {@link #getRules(com.zimbra.cs.account.Account, String)} and caches the result on the <tt>Account</tt>.
     *
     * @see Account#setCachedData(String, Object)
     * @throws ParseException if there was an error while parsing the Sieve script
     */
    private static Node getRulesNode(Account account, String sieveScriptAttrName, String rulesCacheKey)
    throws ParseException {
        Node node = (Node) account.getCachedData(rulesCacheKey);
        if (node == null) {
            String script = getRules(account, sieveScriptAttrName);
            if (script == null) {
                script = "";
            }
            node = parse(script);
            account.setCachedData(rulesCacheKey, node);
        }
        return node;
    }

    /**
     * Returns the XML representation of a user's incoming filter rules.
     *
     * @param account the user account
     */
    public static List<FilterRule> getIncomingRulesAsXML(Account account) throws ServiceException {
        return getRulesAsXML(account, Provisioning.A_zimbraMailSieveScript, FILTER_RULES_CACHE_KEY);
    }

    /**
     * Returns the XML representation of a user's outgoing filter rules.
     *
     * @param account the user account
     */
    public static List<FilterRule> getOutgoingRulesAsXML(Account account) throws ServiceException {
        return getRulesAsXML(account, Provisioning.A_zimbraMailOutgoingSieveScript, OUTGOING_FILTER_RULES_CACHE_KEY);
    }

    private static List<FilterRule> getRulesAsXML(Account account, String sieveScriptAttrName,
            String rulesCacheKey) throws ServiceException {
        Node node;
        try {
            node = getRulesNode(account, sieveScriptAttrName, rulesCacheKey);
        } catch (ParseException e) {
            throw ServiceException.PARSE_ERROR("parsing Sieve script", e);
        } catch (TokenMgrError e) {
            throw ServiceException.PARSE_ERROR("parsing Sieve script", e);
        }
        SieveToSoap sieveToSoap = new SieveToSoap(getRuleNames(account.getAttr(sieveScriptAttrName)));
        sieveToSoap.accept(node);
        return sieveToSoap.toFilterRules();
    }

    private static final Pattern PAT_RULE_NAME = Pattern.compile("# (.+)");

    /**
     * Kind of hacky, but works for now.  Rule names are encoded into the comment preceding
     * the rule.  Return the values of all lines that begin with <tt>"# "</tt>.
     */
    public static List<String> getRuleNames(String script) {
        List<String> names = new ArrayList<String>();
        if (script != null) {
            BufferedReader reader = new BufferedReader(new StringReader(script));
            String line;
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
     * Returns the portion of the Sieve script for the rule with the given name,
     * or <tt>null</tt> if it doesn't exist.
     */
    public static String getRuleByName(String script, String ruleName) {
        if (script == null) {
            return null;
        }

        StringBuilder buf = new StringBuilder();
        boolean found = false;
        BufferedReader reader = new BufferedReader(new StringReader(script));
        String line;

        try {
            while ((line = reader.readLine()) != null) {
                Matcher matcher = PAT_RULE_NAME.matcher(line);
                if (matcher.matches()) {
                    String currentName = matcher.group(1);
                    if (currentName.equals(ruleName)) {
                        // First line of rule.
                        found = true;
                    } else if (found) {
                        // First line of next rule.
                        break;
                    }
                }
                if (found) {
                    buf.append(line).append("\r\n");
                }
            }
        } catch (IOException e) {
            ZimbraLog.filter.warn("Unable to get rule %s from script:\n%s.", ruleName, script, e);
        }

        if (buf.length() > 0) {
            return buf.toString();
        } else {
            return null;
        }
    }

    public static void setIncomingXMLRules(Account account, List<FilterRule> rules) throws ServiceException {
        setXMLRules(account, rules, Provisioning.A_zimbraMailSieveScript, FILTER_RULES_CACHE_KEY);
    }

    public static void setOutgoingXMLRules(Account account, List<FilterRule> rules) throws ServiceException {
        setXMLRules(account, rules, Provisioning.A_zimbraMailOutgoingSieveScript, OUTGOING_FILTER_RULES_CACHE_KEY);
    }

    private static void setXMLRules(Account account,List<FilterRule> rules, String sieveScriptAttrName,
            String rulesCacheKey) throws ServiceException {
        SoapToSieve soapToSieve = new SoapToSieve(rules);
        String script = soapToSieve.getSieveScript();
        setRules(account, script, sieveScriptAttrName, rulesCacheKey);
    }

    public static List<ItemId> applyRulesToIncomingMessage(
        OperationContext octxt, Mailbox mailbox, ParsedMessage pm, int size, String recipient,
        DeliveryContext sharedDeliveryCtxt, int incomingFolderId, boolean noICal)
    throws ServiceException {
        return applyRulesToIncomingMessage(octxt, mailbox, pm, size, recipient, sharedDeliveryCtxt, incomingFolderId, noICal, true);
    }

    /**
     * Adds a message to a mailbox.  If filter rules exist, processes
     * the filter rules.  Otherwise, files to <tt>Inbox</tt> or <tt>Spam</tt>.
     *
     * @param allowFilterToMountpoint if <tt>false</tt>, rules
     * @return the list of message id's that were added, or an empty list.
     */
    public static List<ItemId> applyRulesToIncomingMessage(
        OperationContext octxt,
        Mailbox mailbox, ParsedMessage pm, int size, String recipient,
        DeliveryContext sharedDeliveryCtxt, int incomingFolderId, boolean noICal,
        boolean allowFilterToMountpoint)
    throws ServiceException {
        List<ItemId> addedMessageIds = null;
        IncomingMessageHandler handler = new IncomingMessageHandler(
            octxt, sharedDeliveryCtxt, mailbox, recipient, pm, size, incomingFolderId, noICal);
        ZimbraMailAdapter mailAdapter = new ZimbraMailAdapter(mailbox, handler);
        mailAdapter.setAllowFilterToMountpoint(allowFilterToMountpoint);

        try {
            Account account = mailbox.getAccount();
            Node node = getRulesNode(account, Provisioning.A_zimbraMailSieveScript, FILTER_RULES_CACHE_KEY);

            // Determine whether to apply rules
            boolean applyRules = true;
            if (node == null) {
                applyRules = false;
            }
            if (SpamHandler.isSpam(handler.getMimeMessage()) &&
                    !account.getBooleanAttr(Provisioning.A_zimbraSpamApplyUserFilters, false)) {
                // Don't apply user filters to spam by default
                applyRules = false;
            }

            if (applyRules) {
                SIEVE_FACTORY.evaluate(mailAdapter, node);
                // multiple fileinto may result in multiple copies of the messages in different folders
                addedMessageIds = mailAdapter.getAddedMessageIds();
            }
        } catch (Exception e) {
            ZimbraLog.filter.warn("An error occurred while processing filter rules. Filing message to %s.",
                handler.getDefaultFolderPath(), e);
        } catch (TokenMgrError e) {
            // Workaround for bug 19576.  Certain parse errors can cause JavaCC to throw an Error
            // instead of an Exception.  Woo.
            ZimbraLog.filter.warn("An error occurred while processing filter rules. Filing message to %s.",
                handler.getDefaultFolderPath(), e);
        }
        if (addedMessageIds == null) {
            // Filter rules were not processed.  File to the default folder.
            Message msg = mailAdapter.doDefaultFiling();
            addedMessageIds = new ArrayList<ItemId>(1);
            addedMessageIds.add(new ItemId(msg));
        }
        return addedMessageIds;
    }


    public static List<ItemId> applyRulesToOutgoingMessage(
            OperationContext octxt, Mailbox mailbox, ParsedMessage pm, int sentFolderId,
            boolean noICal, int flags, String[] tags, int convId)
            throws ServiceException {
        List<ItemId> addedMessageIds = null;
        OutgoingMessageHandler handler = new OutgoingMessageHandler(
            mailbox, pm, sentFolderId, noICal, flags, tags, convId, octxt);
        ZimbraMailAdapter mailAdapter = new ZimbraMailAdapter(mailbox, handler);
        try {
            Account account = mailbox.getAccount();
            Node node = getRulesNode(account, Provisioning.A_zimbraMailOutgoingSieveScript, OUTGOING_FILTER_RULES_CACHE_KEY);
            if (node != null) {
                SIEVE_FACTORY.evaluate(mailAdapter, node);
                // multiple fileinto may result in multiple copies of the messages in different folders
                addedMessageIds = mailAdapter.getAddedMessageIds();
            }
        } catch (Exception e) {
            ZimbraLog.filter.warn("An error occurred while processing filter rules. Filing message to %s.",
                handler.getDefaultFolderPath(), e);
        } catch (TokenMgrError e) {
            ZimbraLog.filter.warn("An error occurred while processing filter rules. Filing message to %s.",
                handler.getDefaultFolderPath(), e);
        }
        if (addedMessageIds == null) {
            // Filter rules were not processed.  File to the default folder.
            Message msg = mailAdapter.doDefaultFiling();
            addedMessageIds = new ArrayList<ItemId>(1);
            addedMessageIds.add(new ItemId(msg));
        }
        return addedMessageIds;
    }

    public static boolean applyRulesToExistingMessage(OperationContext octxt, Mailbox mbox, int messageId, Node node)
    throws ServiceException {
        Message msg = mbox.getMessageById(octxt, messageId);
        ExistingMessageHandler handler = new ExistingMessageHandler(octxt, mbox, messageId, (int) msg.getSize());
        ZimbraMailAdapter mailAdapter = new ZimbraMailAdapter(mbox, handler);

        try {
            SIEVE_FACTORY.evaluate(mailAdapter, node);
        } catch (SieveException e) {
            throw ServiceException.FAILURE("Unable to evaluate script", e);
        }

        return handler.filtered();
    }

    /**
     * Parses the sieve script and returns the root to the resulting node tree.
     */
    public static Node parse(String script) throws ParseException {
        ByteArrayInputStream sin;
        try {
            sin = new ByteArrayInputStream(script.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new ParseException(e.getMessage());
        }
        return SIEVE_FACTORY.parse(sin);
    }

    /**
     * When a folder is renamed, updates any filter rules that reference
     * that folder.
     */
    public static void folderRenamed(Account account, String originalPath, String newPath)
    throws ServiceException {
        folderRenamed(account, originalPath, newPath, Provisioning.A_zimbraMailSieveScript, FILTER_RULES_CACHE_KEY);
        folderRenamed(account, originalPath, newPath, Provisioning.A_zimbraMailOutgoingSieveScript, OUTGOING_FILTER_RULES_CACHE_KEY);
    }

    private static void folderRenamed(Account account, String originalPath, String newPath, String sieveScriptAttrName, String rulesCacheKey)
            throws ServiceException {
        String script = getRules(account, sieveScriptAttrName);
        if (script != null) {
            Node node;
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
                SieveToSoap sieveToSoap = new SieveToSoap(ruleNames);
                sieveToSoap.accept(node);
                SoapToSieve soapToSieve = new SoapToSieve(sieveToSoap.toFilterRules());
                String newScript = soapToSieve.getSieveScript();
                setRules(account, newScript, sieveScriptAttrName, rulesCacheKey);
                ZimbraLog.filter.info("Updated %s due to folder move or rename from %s to %s.",
                    sieveScriptAttrName, originalPath, newPath);
                ZimbraLog.filter.debug("Old rules:\n%s, new rules:\n%s", script, newScript);
            }
        }
    }

    /**
     * When a folder is deleted, disables any filter rules that reference that folder.
     */
    public static void folderDeleted(Account account, String originalPath)
    throws ServiceException {
        folderDeleted(account, originalPath, Provisioning.A_zimbraMailSieveScript, FILTER_RULES_CACHE_KEY);
        folderDeleted(account, originalPath, Provisioning.A_zimbraMailOutgoingSieveScript, OUTGOING_FILTER_RULES_CACHE_KEY);
    }

    private static void folderDeleted(Account account, String originalPath, String sieveScriptAttrName, String rulesCacheKey)
            throws ServiceException {
        String script = getRules(account, sieveScriptAttrName);
        if (script != null) {
            Node node;
            try {
                node = parse(script);
            } catch (ParseException e) {
                ZimbraLog.filter.warn("Unable to update filter rules after folder '%s' was deleted.", originalPath, e);
                return;
            }
            FolderDeleted deleted = new FolderDeleted(originalPath);

            deleted.accept(node);
            if (deleted.modified()) {
                // Kind of a hacky way to convert a Node tree to a script.  We
                // convert to XML first, and then to a String.  Unfortunately
                // jSieve 0.2 doesn't have an API that generates a script from
                // a Node tree.
                List<String> ruleNames = getRuleNames(script);
                SieveToSoap sieveToSoap = new SieveToSoap(ruleNames);
                sieveToSoap.accept(node);
                SoapToSieve soapToSieve = new SoapToSieve(sieveToSoap.toFilterRules());
                String newScript = soapToSieve.getSieveScript();
                setRules(account, newScript, sieveScriptAttrName, rulesCacheKey);
                ZimbraLog.filter.info("Updated %s filter rules after folder %s was deleted.", sieveScriptAttrName, originalPath);
                ZimbraLog.filter.debug("Old rules:\n%s, new rules:\n%s", script, newScript);
            }
        }
    }

    /**
     * When a tag is renamed, updates any filter rules that reference
     * that tag.
     */
    public static void tagRenamed(Account account, String originalName, String newName)
    throws ServiceException {
        tagRenamed(account, originalName, newName, Provisioning.A_zimbraMailSieveScript, FILTER_RULES_CACHE_KEY);
        tagRenamed(account, originalName, newName, Provisioning.A_zimbraMailOutgoingSieveScript, OUTGOING_FILTER_RULES_CACHE_KEY);
    }

    private static void tagRenamed(Account account, String originalName, String newName, String sieveScriptAttrName, String rulesCacheKey)
            throws ServiceException {
        String rules = getRules(account, sieveScriptAttrName);
        if (rules != null) {
            String newRules = rules.replace("tag \"" + originalName + "\"", "tag \"" + newName + "\"");
            if (!newRules.equals(rules)) {
                setRules(account, newRules, sieveScriptAttrName, rulesCacheKey);
                ZimbraLog.filter.info("Updated %s due to tag rename from %s to %s.",
                    sieveScriptAttrName, originalName, newName);
                ZimbraLog.filter.debug("Old rules:\n%s, new rules:\n%s", rules, newRules);
            }
        }
    }

    public static void tagDeleted(Account account, String tagName)
    throws ServiceException {
        tagDeleted(account, tagName, Provisioning.A_zimbraMailSieveScript, FILTER_RULES_CACHE_KEY);
        tagDeleted(account, tagName, Provisioning.A_zimbraMailOutgoingSieveScript, OUTGOING_FILTER_RULES_CACHE_KEY);
    }

    private static void tagDeleted(Account account, String tagName, String sieveScriptAttrName, String rulesCacheKey)
            throws ServiceException {
        String script = getRules(account, sieveScriptAttrName);
        if (script != null) {
            Node node;
            try {
                node = parse(script);
            } catch (ParseException e) {
                ZimbraLog.filter.warn("Unable to update %s after tag '%s' was deleted.", sieveScriptAttrName, tagName, e);
                return;
            }
            TagDeleted deleted = new TagDeleted(tagName);

            deleted.accept(node);
            if (deleted.modified()) {
                // Kind of a hacky way to convert a Node tree to a script.  We
                // convert to XML first, and then to a String.  Unfortunately
                // jSieve 0.2 doesn't have an API that generates a script from
                // a Node tree.
                List<String> ruleNames = getRuleNames(script);
                SieveToSoap sieveToSoap = new SieveToSoap(ruleNames);
                sieveToSoap.accept(node);
                SoapToSieve soapToSieve = new SoapToSieve(sieveToSoap.toFilterRules());
                String newScript = soapToSieve.getSieveScript();
                setRules(account, newScript, sieveScriptAttrName, rulesCacheKey);
                ZimbraLog.filter.info("Updated %s after tag %s was deleted.", sieveScriptAttrName, tagName);
                ZimbraLog.filter.debug("Old rules:\n%s, new rules:\n%s", script, newScript);
            }
        }
    }
}
