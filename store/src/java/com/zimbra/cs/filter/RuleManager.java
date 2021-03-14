/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016, 2017 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.filter;

import com.zimbra.common.service.DeliveryServiceException;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.filter.jsieve.ErejectException;
import com.zimbra.cs.filter.ZimbraMailAdapter.KeepType;
import com.zimbra.cs.lmtpserver.LmtpEnvelope;
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
    private static final String ADMIN_FILTER_RULES_BEFORE_CACHE_KEY =
            RuleManager.class.getSimpleName() + ".ADMIN_FILTER_RULES_BEFORE_CACHE";
    private static final String ADMIN_FILTER_RULES_AFTER_CACHE_KEY =
            RuleManager.class.getSimpleName() + ".ADMIN_FILTER_RULES_AFTER_CACHE";
    private static final String ADMIN_OUTGOING_FILTER_RULES_BEFORE_CACHE_KEY =
            RuleManager.class.getSimpleName() + ".ADMIN_OUTGOING_FILTER_RULES_BEFORE_CACHE";
    private static final String ADMIN_OUTGOING_FILTER_RULES_AFTER_CACHE_KEY =
            RuleManager.class.getSimpleName() + ".ADMIN_OUTGOING_FILTER_RULES_AFTER_CACHE";
    public static final String editHeaderUserScriptError = "EDIT_HEADER_NOT_SUPPORTED_FOR_USER_SCRIPT";

    public static enum FilterType {INCOMING, OUTGOING};
    public static enum AdminFilterType {
        BEFORE,
        AFTER;
        public String getType() {
            return name().toLowerCase();
        }
    };

    private static SieveFactory SIEVE_FACTORY = createSieveFactory();

    private RuleManager() {
    }

    private static SieveFactory createSieveFactory() {
        // Initialize custom jSieve extensions
        ConfigurationManager mgr;
        try {
            mgr = new ZimbraConfigurationManager();
        } catch (SieveException e) {
            ZimbraLog.filter.error("Unable to initialize mail filtering extensions.", e);
            return null;
        }

        Map<String, String> commandMap = mgr.getCommandMap();
        commandMap.putAll(JsieveConfigMapHandler.getCommandMap());

        Map<String, String> testMap = mgr.getTestMap();
        testMap.putAll(JsieveConfigMapHandler.getTestMap());

        return mgr.build();
    }

    public static SieveFactory getSieveFactory() {
        return SIEVE_FACTORY;
    }

    /**
     * Saves the filter rules.
     *
     * @param entry the account/domain/cos/server for which the rules are to be saved
     * @param script the sieve script, or <code>null</code> or empty string if
     * all rules should be deleted
     * @param sieveScriptAttrName
     * @param rulesCacheKey
     * @throws ServiceException
     */
    private static void setRules(Entry entry, String script, String sieveScriptAttrName, String rulesCacheKey)
            throws ServiceException {
        if (entry instanceof Account) {
            ZimbraLog.filter.debug("Setting filter rules for account %s:\n%s", ((Account)entry).getName(), script);
        } else if (entry instanceof Domain) {
            ZimbraLog.filter.debug("Setting filter rules for domain %s:\n%s", ((Domain)entry).getName(), script);
        } else if (entry instanceof Cos) {
            ZimbraLog.filter.debug("Setting filter rules for cos %s:\n%s", ((Cos)entry).getName(), script);
        } else if (entry instanceof Server) {
            ZimbraLog.filter.debug("Setting filter rules for server %s:\n%s", ((Server)entry).getName(), script);
        }
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
            Provisioning.getInstance().modifyAttrs(entry, attrs);
            entry.setCachedData(rulesCacheKey, node);
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
        account.setCachedData(ADMIN_FILTER_RULES_BEFORE_CACHE_KEY, null);
        account.setCachedData(ADMIN_FILTER_RULES_AFTER_CACHE_KEY, null);
        account.setCachedData(ADMIN_OUTGOING_FILTER_RULES_BEFORE_CACHE_KEY, null);
        account.setCachedData(ADMIN_OUTGOING_FILTER_RULES_AFTER_CACHE_KEY, null);
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
     * @param account the owner account of the filter rule
     * @param rulesCacheKey key name for the rule node cache
     *
     * @see Account#setCachedData(String, Object)
     * @throws ParseException if there was an error while parsing the Sieve script
     * @throws ServiceException
     */
    public static Node getRulesNode(Account account, String rulesCacheKey)
        throws ParseException, ServiceException {

        String sieveScriptAttrName = getScriptAttributeName(rulesCacheKey);

        Node node = (Node) account.getCachedData(rulesCacheKey);
        if (null == node) {
            String script = getRules(account, sieveScriptAttrName);

            if (null == script) {
                script  = "";
            }

            ZimbraLog.filter.debug("attrName[%s] rule[%s]", sieveScriptAttrName, script);

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
        return getRulesAsXML(account, FilterType.INCOMING);
    }

    /**
     * Returns the XML representation of a user's outgoing filter rules.
     *
     * @param account the user account
     */
    public static List<FilterRule> getOutgoingRulesAsXML(Account account) throws ServiceException {
        return getRulesAsXML(account, FilterType.OUTGOING);
    }

    private static List<FilterRule> getRulesAsXML(Account account, FilterType filterType)
        throws ServiceException {
        Node node;
        String sieveScriptAttrName = null;
        try {
            if (filterType == FilterType.INCOMING) {
                node = getRulesNode(account, FILTER_RULES_CACHE_KEY);
                sieveScriptAttrName = Provisioning.A_zimbraMailSieveScript;
            } else {
                node = getRulesNode(account, OUTGOING_FILTER_RULES_CACHE_KEY);
                sieveScriptAttrName = Provisioning.A_zimbraMailOutgoingSieveScript;
            }
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
    public static Pair<String, String> getRuleByName(String script, String ruleName) {
        if (script == null) {
            return null;
        }
        StringBuilder scriptBuf = new StringBuilder();
        StringBuilder requireBuf = new StringBuilder();
        boolean found = false;
        BufferedReader reader = new BufferedReader(new StringReader(script));
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("require")) {
                    requireBuf.append(line).append("\r\n");
                    continue;
                }
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
                    scriptBuf.append(line).append("\r\n");
                }
            }
        } catch (IOException e) {
            ZimbraLog.filter.warn("Unable to get rule %s from script:\n%s.", ruleName, script, e);
        }
        Pair<String, String> requireScriptPair = new Pair<String, String>(requireBuf.toString(),
            scriptBuf.toString());
        if (scriptBuf.length() > 0) {
            return requireScriptPair;
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
        return applyRulesToIncomingMessage(octxt, mailbox, pm, size, recipient, null, sharedDeliveryCtxt, incomingFolderId, noICal, true);
    }

    public static List<ItemId> applyRulesToIncomingMessage(
            OperationContext octxt,
            Mailbox mailbox, ParsedMessage pm, int size, String recipient,
            DeliveryContext sharedDeliveryCtxt, int incomingFolderId, boolean noICal,
            boolean allowFilterToMountpoint)
        throws ServiceException {
        return applyRulesToIncomingMessage(octxt, mailbox, pm, size, recipient, null, sharedDeliveryCtxt, incomingFolderId, noICal, allowFilterToMountpoint);
    }

    public static List<ItemId> applyRulesToIncomingMessage(
            OperationContext octxt, Mailbox mailbox, ParsedMessage pm, int size, String recipient,
            LmtpEnvelope env,
            DeliveryContext sharedDeliveryCtxt, int incomingFolderId, boolean noICal)
        throws ServiceException {
            return applyRulesToIncomingMessage(octxt, mailbox, pm, size, recipient, env, sharedDeliveryCtxt, incomingFolderId, noICal, true);
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
        LmtpEnvelope env,
        DeliveryContext sharedDeliveryCtxt, int incomingFolderId, boolean noICal,
        boolean allowFilterToMountpoint)
    throws ServiceException {
        List<ItemId> addedMessageIds = null;
        IncomingMessageHandler handler = new IncomingMessageHandler(
            octxt, sharedDeliveryCtxt, mailbox, recipient, pm, size, incomingFolderId, noICal);
        ZimbraMailAdapter mailAdapter = new ZimbraMailAdapter(mailbox, handler);
        mailAdapter.setEnvelope(env);
        mailAdapter.setAllowFilterToMountpoint(allowFilterToMountpoint);

        String [] filters = {ADMIN_FILTER_RULES_BEFORE_CACHE_KEY,
                FILTER_RULES_CACHE_KEY,
                ADMIN_FILTER_RULES_AFTER_CACHE_KEY
        };

        try {
            boolean applyRules = true;
            Account account = mailbox.getAccount();
            for (String filter : filters) {
                // Determine whether to apply rules
                Node node = getRulesNode(account, filter);

                if (null == node) {
                    applyRules = false;
                }
                if (SpamHandler.isSpam(handler.getMimeMessage()) &&
                        !account.getBooleanAttr(Provisioning.A_zimbraSpamApplyUserFilters, false)) {
                    // Don't apply user filters to spam by default
                    applyRules = false;
                    break;
                }
                if (applyRules) {
                    if (filter.equals(FILTER_RULES_CACHE_KEY)) {
                        mailAdapter.setUserScriptExecuting(true);
                    }
                    boolean proceed = evaluateScript(mailAdapter, node);
                    if (!proceed) {
                        continue;
                    }
                    if (mailAdapter.isStop()) {
                        break;
                    }
                    mailAdapter.resetValues();
                    mailAdapter.resetCapabilities();
                }
            }
            if (applyRules) {
                mailAdapter.executeAllActions();
                addedMessageIds = mailAdapter.getAddedMessageIds();
            }
        } catch (ErejectException ex) {
            throw DeliveryServiceException.DELIVERY_REJECTED(ex.getMessage(), ex);
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
            Message msg = mailAdapter.keep(KeepType.IMPLICIT_KEEP);
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

        String[] filters = {ADMIN_OUTGOING_FILTER_RULES_BEFORE_CACHE_KEY, 
                OUTGOING_FILTER_RULES_CACHE_KEY,
                ADMIN_OUTGOING_FILTER_RULES_AFTER_CACHE_KEY};

        try {
            Account account = mailbox.getAccount();
            for (String filter : filters) {
                Node node = getRulesNode(account, filter);
                if (null != node) {
                    if (filter.equals(OUTGOING_FILTER_RULES_CACHE_KEY)) {
                        mailAdapter.setUserScriptExecuting(true);
                    }
                    boolean proceed = evaluateScript(mailAdapter, node);
                    if (!proceed) {
                        continue;
                    }
                    if (mailAdapter.isStop()) {
                        break;
                    }
                    mailAdapter.resetValues();
                    mailAdapter.resetCapabilities();
                }
            }
            mailAdapter.executeAllActions();
            // multiple fileinto may result in multiple copies of the messages in different folders
            addedMessageIds = mailAdapter.getAddedMessageIds();
        } catch (Exception e) {
            ZimbraLog.filter.warn("An error occurred while processing filter rules. Filing message to %s.",
                handler.getDefaultFolderPath(), e);
        } catch (TokenMgrError e) {
            ZimbraLog.filter.warn("An error occurred while processing filter rules. Filing message to %s.",
                handler.getDefaultFolderPath(), e);
        }
        if (addedMessageIds == null) {
            // Filter rules were not processed.  File to the default folder.
            Message msg = mailAdapter.keep(KeepType.IMPLICIT_KEEP);
            addedMessageIds = new ArrayList<ItemId>(1);
            addedMessageIds.add(new ItemId(msg));
        }
        return addedMessageIds;
    }

    private static boolean evaluateScript(ZimbraMailAdapter mailAdapter, Node node) throws SieveException {
        try {
            SIEVE_FACTORY.evaluate(mailAdapter, node);
        } catch (SieveException e) {
            if (editHeaderUserScriptError.equals(e.getMessage())) {
                ZimbraLog.filter.info(
                    "Sieve edit header feature not supported for user script. Ignoring the script.");
                mailAdapter.resetValues();
                mailAdapter.resetCapabilities();
                return false;
            } else {
                throw e;
            }
        }
        if (!mailAdapter.getAccount().isSieveEditHeaderEnabled()) {
            if (mailAdapter.isAddHeaderPresent()) {
                ZimbraLog.filter.info(
                    "Sieve edit header feature disabled. Add header command ignored.");
            }
            if (mailAdapter.isDeleteHeaderPresent()) {
                ZimbraLog.filter.info(
                    "Sieve edit header feature disabled. Delete header command ignored.");
            }
            if (mailAdapter.isReplaceHeaderPresent()) {
                ZimbraLog.filter.info(
                    "Sieve edit header feature disabled. Replace header command ignored.");
            }
        }
        return true;
    }

    public static boolean applyRulesToExistingMessage(OperationContext octxt, Mailbox mbox, int messageId, Node node)
    throws ServiceException {
        Message msg = mbox.getMessageById(octxt, messageId);
        ExistingMessageHandler handler = new ExistingMessageHandler(octxt, mbox, messageId, (int) msg.getSize());
        ZimbraMailAdapter mailAdapter = new ZimbraMailAdapter(mbox, handler);

        try {
            SIEVE_FACTORY.evaluate(mailAdapter, node);
            mailAdapter.executeAllActions();
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
        Node node = null;
        try {
            node = SIEVE_FACTORY.parse(sin);
        } catch (TokenMgrError e) {
            // Due to the jsieve library's bug, the tokenizer does not handle correctly
            // most characters after the backslash. According to the RFC 5228 Section 2.4.2,
            // "An undefined escape sequence is interpreted as if there were no backslash",
            // but the parse() method throws the TokenMgrError exception when it encounters
            // an undefined escape sequence (such as "\a" in a context where "a" has no
            // special meaning). Here is the workaround to re-try parsing using the same
            // filter string without any undefined escape sequences.
            try {
                sin = new ByteArrayInputStream(ignoreBackslash(script).getBytes("UTF-8"));
            } catch (UnsupportedEncodingException uee) {
                throw new ParseException(uee.getMessage());
            }
            node = SIEVE_FACTORY.parse(sin);
        }
        return node;
    }

    /**
     * Eliminate the undefined escape sequences from the sieve filter script string.
     * Only \\ (backslash backslash) and \" (backslash double-quote) are defined as
     * a escape sequences.
     *
     * @param script filter string which may include some undefined escape sequences
     * @return filter string without undefined escape sequence
     */
    private static String ignoreBackslash(String script) {
        StringBuilder result = new StringBuilder();
        boolean backslash = false;
        for (int i = 0; i < script.length(); i++) {
            char c = script.charAt(i);
            if (!backslash && c == '\\') {
                backslash = true;
            } else if (backslash && (c == '\\' || c == '"')) {
                result.append('\\').append(c);
                backslash = false;
            } else {
                result.append(c);
                backslash = false;
            }
        }
        return result.toString();
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

    private static String getScriptAttributeName(String rulesCacheKey) throws ServiceException {
        if (ADMIN_FILTER_RULES_BEFORE_CACHE_KEY.equalsIgnoreCase(rulesCacheKey)) {
            return getAdminScriptAttributeName(FilterType.INCOMING,  AdminFilterType.BEFORE);
        } else if (ADMIN_FILTER_RULES_AFTER_CACHE_KEY.equalsIgnoreCase(rulesCacheKey)) {
            return getAdminScriptAttributeName(FilterType.INCOMING,  AdminFilterType.AFTER);
        } else if (ADMIN_OUTGOING_FILTER_RULES_BEFORE_CACHE_KEY.equalsIgnoreCase(rulesCacheKey)) {
            return getAdminScriptAttributeName(FilterType.OUTGOING,  AdminFilterType.BEFORE);
        } else if (ADMIN_OUTGOING_FILTER_RULES_AFTER_CACHE_KEY.equalsIgnoreCase(rulesCacheKey)) {
            return getAdminScriptAttributeName(FilterType.OUTGOING,  AdminFilterType.AFTER);
        } else if (FILTER_RULES_CACHE_KEY.equalsIgnoreCase(rulesCacheKey)) {
            return Provisioning.A_zimbraMailSieveScript;
        } else if (OUTGOING_FILTER_RULES_CACHE_KEY.equalsIgnoreCase(rulesCacheKey)) {
            return Provisioning.A_zimbraMailOutgoingSieveScript;
        } else {
            StringBuilder msg = new StringBuilder();
            msg.append("FilterKey: ").append(rulesCacheKey).append(" is invalid");
            throw ServiceException.INVALID_REQUEST(msg.toString(), null);
        }
    }

    private static String getAdminScriptAttributeName(FilterType filterType, AdminFilterType afType) throws ServiceException {
        if (filterType == FilterType.INCOMING && afType == AdminFilterType.BEFORE) {
            return Provisioning.A_zimbraAdminSieveScriptBefore;
        } else if (filterType == FilterType.INCOMING && afType == AdminFilterType.AFTER) {
            return Provisioning.A_zimbraAdminSieveScriptAfter;
        } else if (filterType == FilterType.OUTGOING && afType == AdminFilterType.BEFORE) {
            return Provisioning.A_zimbraAdminOutgoingSieveScriptBefore;
        } else if (filterType == FilterType.OUTGOING && afType == AdminFilterType.AFTER) {
            return Provisioning.A_zimbraAdminOutgoingSieveScriptAfter;
        } else {
            StringBuilder msg = new StringBuilder();
            msg.append("FilterType: ").append(filterType).append(" or AdminFilterType: ").append(afType).append(" is invalid");
            throw ServiceException.INVALID_REQUEST(msg.toString(), null);
        }
    }

    public static String getAdminScriptCacheKey(FilterType filterType, AdminFilterType afType) throws ServiceException {
        if (filterType == FilterType.INCOMING && afType == AdminFilterType.BEFORE) {
            return ADMIN_FILTER_RULES_BEFORE_CACHE_KEY;
        } else if (filterType == FilterType.INCOMING && afType == AdminFilterType.AFTER) {
            return ADMIN_FILTER_RULES_AFTER_CACHE_KEY;
        } else if (filterType == FilterType.OUTGOING && afType == AdminFilterType.BEFORE) {
            return ADMIN_OUTGOING_FILTER_RULES_BEFORE_CACHE_KEY;
        } else if (filterType == FilterType.OUTGOING && afType == AdminFilterType.AFTER) {
            return ADMIN_OUTGOING_FILTER_RULES_AFTER_CACHE_KEY;
        } else {
            StringBuilder msg = new StringBuilder();
            msg.append("FilterType: ").append(filterType).append(" or AdminFilterType: ").append(afType).append(" is invalid");
            throw ServiceException.INVALID_REQUEST(msg.toString(), null);
        }
    }

    public static List<FilterRule> getAdminRulesAsXML(Entry entry, FilterType filterType, AdminFilterType afType) throws ServiceException {
        Node node;
        try {
            node = getRulesNode(entry, filterType, afType);
        } catch (ParseException | TokenMgrError e) {
            throw ServiceException.PARSE_ERROR("parsing Sieve script", e);
        }
        String sieveScriptAttrName = getAdminScriptAttributeName(filterType, afType);
        SieveToSoap sieveToSoap = new SieveToSoap(getRuleNames(entry.getAttr(sieveScriptAttrName)));
        sieveToSoap.accept(node, true);
        return sieveToSoap.toFilterRules();
    }

    /**
     * Returns the parsed filter rules for the given domain.  If no cached
     * copy of the parsed rules exists, parses the script returned by
     * {@link #getRules(com.zimbra.cs.account.Entry, String)} and caches the result on the <tt>Account</tt>.
     *
     * @param entry the owner domain/cos/server of the filter rule
     * @param filterType <tt>FilterType.INCOMING</tt> or <tt>FilterType.OUTGOING</tt>
     * @param afType <tt>AdminFilterType.BEFORE</tt> or <tt>AdminFilterType.AFTER</tt>
     *
     * @see Entry#setCachedData(String, Object)
     * @throws ParseException if there was an error while parsing the Sieve script
     * @throws ServiceException 
     */
    private static Node getRulesNode(Entry entry, FilterType filterType, AdminFilterType afType)
            throws ParseException, ServiceException {
        String rulesCacheKey = getAdminScriptCacheKey(filterType, afType);
        String adminRuleAttrName = getAdminScriptAttributeName(filterType, afType);

        Node node = (Node) entry.getCachedData(rulesCacheKey);
        if (null == node) {
            String adminRule = entry.getAttr(adminRuleAttrName);

            if (null == adminRule) {
                adminRule = "";
            }
            ZimbraLog.filter.debug("filterType[%s] rule[%s]", filterType == FilterType.INCOMING ? "incoming" : "outgoing", adminRule);
            node = parse(adminRule);
            entry.setCachedData(rulesCacheKey, node);
        }
        return node;
    }

    public static void setAdminRulesFromXML(Entry entry, List<FilterRule> rules, FilterType filterType, AdminFilterType afType) throws ServiceException {
        String sieveScriptAttrName = getAdminScriptAttributeName(filterType, afType);
        String rulesCacheKey = getAdminScriptCacheKey(filterType, afType);
        SoapToSieve soapToSieve = new SoapToSieve(rules);
        String script = soapToSieve.getAdminSieveScript();
        setRules(entry, script, sieveScriptAttrName, rulesCacheKey);
    }
}
