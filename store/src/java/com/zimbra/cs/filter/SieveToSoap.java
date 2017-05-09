/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

import java.util.Date;
import java.util.List;

import org.apache.jsieve.parser.generated.Node;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.zimbra.common.filter.Sieve;
import com.zimbra.common.service.ServiceException;
import com.zimbra.soap.mail.type.FilterAction;
import com.zimbra.soap.mail.type.FilterRule;
import com.zimbra.soap.mail.type.FilterTest;
import com.zimbra.soap.mail.type.FilterTests;
import com.zimbra.soap.mail.type.FilterVariable;
import com.zimbra.soap.mail.type.FilterVariables;
import com.zimbra.soap.mail.type.NestedRule;

/**
 * Converts a Sieve node tree to the SOAP representation of
 * filter rules.
 */
public final class SieveToSoap extends SieveVisitor {

    private final List<FilterRule> rules = Lists.newArrayList();
    private final List<String> ruleNames;
    private FilterRule currentRule;
    private int currentRuleIndex = 0;
    private int nunOfIfProcessingStarted = 0; // For counting num of started If processings
    private int numOfIfProcessingDone = 0; // For counting num of finished If processings
    private NestedRule currentNestedRule; // keep the pointer to nested rule being processed
    private List<FilterVariable> currentVariables = null;
    private List<FilterVariable> actionVariables = null;

    public SieveToSoap(List<String> ruleNames) {
        this.ruleNames = ruleNames;
    }

    public List<FilterRule> toFilterRules() {
        return rules;
    }

    private String getCurrentRuleName() {
        if (ruleNames == null || currentRuleIndex >= ruleNames.size()) {
            return null;
        }
        return ruleNames.get(currentRuleIndex);
    }

    @Override
    protected void visitRule(Node ruleNode, VisitPhase phase, RuleProperties props) {
        if (phase == VisitPhase.end) {
            return;
        }

        if (!isNestedRule()){
            currentRule = new FilterRule(getCurrentRuleName(), props.isEnabled);
            if (currentVariables != null) {
                if(currentRule.getFilterVariables() == null) {
                    currentRule.setFilterVariables(new FilterVariables());
                }
                currentRule.getFilterVariables().setVariables(currentVariables);
                currentVariables = null;
            }
            currentRule.setFilterTests(new FilterTests(props.condition.toString()));
            rules.add(currentRule);
            currentRuleIndex++;
            // When start working on the root rule, initialise the pointer to nested rule
            currentNestedRule = null;
        } else {
            // set new Nested Rule instance as child of current one.
            NestedRule nestedRule = new NestedRule(new FilterTests(props.condition.toString()));
            if(currentNestedRule != null){  // some nested rule has been already processed
                // set it as child of previous one
                currentNestedRule.setChild(nestedRule);
            } else {               // first nested rule
                // set it as child of root rule
                currentRule.setChild(nestedRule);
            }
            currentNestedRule = nestedRule;
        }
    }

    @Override
    protected void visitVariable(Node ruleNode, VisitPhase phase, RuleProperties props, String name, String value) {
        if (phase == VisitPhase.begin) {
            if (isNestedRule()) {
                if (actionVariables == null) {
                    actionVariables = Lists.newArrayList();
                }
                actionVariables.add(FilterVariable.createFilterVariable(name, value));
            } else {
                if (currentVariables == null) {
                    currentVariables = Lists.newArrayList();
                }
                currentVariables.add(FilterVariable.createFilterVariable(name, value));
            }
        }
    }

    @Override
    protected void visitIfControl(Node ruleNode, VisitPhase phase, RuleProperties props) {
        if (phase == VisitPhase.end) {
            numOfIfProcessingDone++; // number of if for which process is done
            if (actionVariables != null && !actionVariables.isEmpty()) {
                FilterAction action = new FilterVariables(actionVariables);
                addAction(action);
                actionVariables = null;
            }
            return;
        }
        nunOfIfProcessingStarted++;   // number of if for which process is started.
    }

    private boolean isNestedRule(){
        // in non nested case, only one process is started but not done.
        if(nunOfIfProcessingStarted == numOfIfProcessingDone+1){
            return false;
        }
        return true;
    }

    private <T extends FilterTest> T addTest(T test, RuleProperties props) {
        FilterTests tests;
        if (currentNestedRule != null) { // Nested Rule is being processed
            tests = currentNestedRule.getFilterTests();
        } else {                     // Root Rule is being processed
            tests = currentRule.getFilterTests();
        }
        test.setIndex(tests.size());
        tests.addTest(test);
        if (props.isNegativeTest) {
            test.setNegative(true);
        }
        return test;
    }

    private <T extends FilterAction> T addAction(T action) {
        if(currentNestedRule != null){ // Nested Rule is being processed
            action.setIndex(currentNestedRule.getActionCount());
            currentNestedRule.addFilterAction(action);
        } else {                     // Root Rule is being processed
            action.setIndex(currentRule.getActionCount());
            currentRule.addFilterAction(action);
        }
        return action;
    }

    @Override
    protected void visitAttachmentTest(Node node, VisitPhase phase, RuleProperties props) {
        if (phase == VisitPhase.begin) {
            addTest(new FilterTest.AttachmentTest(), props);
        }
    }

    @Override
    protected void visitBodyTest(Node node, VisitPhase phase, RuleProperties props,
            boolean caseSensitive, String value) {
        if (phase == VisitPhase.begin) {
            FilterTest.BodyTest test = addTest(new FilterTest.BodyTest(), props);
            if (caseSensitive) {
                test.setCaseSensitive(true);
            }
            test.setValue(value);
        }
    }

    @Override
    protected void visitDateTest(Node node, VisitPhase phase, RuleProperties props,
            Sieve.DateComparison comparison, Date date) {
        if (phase == VisitPhase.begin) {
            FilterTest.DateTest test = addTest(new FilterTest.DateTest(), props);
            test.setDateComparison(comparison.toString());
            test.setDate(date.getTime() / 1000L);
        }
    }

    @Override
    protected void visitCurrentTimeTest(Node node, VisitPhase phase, RuleProperties props,
            Sieve.DateComparison comparison, String time) {
        if (phase == VisitPhase.begin) {
            FilterTest.CurrentTimeTest test = addTest(new FilterTest.CurrentTimeTest(), props);
            test.setDateComparison(comparison.toString());
            test.setTime(time);
        }
    }

    @Override
    protected void visitCurrentDayOfWeekTest(Node node, VisitPhase phase, RuleProperties props, List<String> days) {
        if (phase == VisitPhase.begin) {
            FilterTest.CurrentDayOfWeekTest test = addTest(new FilterTest.CurrentDayOfWeekTest(), props);
            test.setValues(Joiner.on(',').join(days));
        }
    }

    @Override
    protected void visitTrueTest(Node node, VisitPhase phase, RuleProperties props) {
        if (phase == VisitPhase.begin) {
            addTest(new FilterTest.TrueTest(), props);
        }
    }

    @Override
    protected void visitHeaderExistsTest(Node node, VisitPhase phase, RuleProperties props, String header) {
        if (phase == VisitPhase.begin) {
            addTest(new FilterTest.HeaderExistsTest(header), props);
        }
    }

    @Override
    protected void visitHeaderTest(Node node, VisitPhase phase, RuleProperties props,
            List<String> headers, Sieve.StringComparison comparison, boolean caseSensitive, String value) {
        if (phase == VisitPhase.begin) {
            FilterTest.HeaderTest test = addTest(new FilterTest.HeaderTest(), props);
            test.setHeaders(Joiner.on(',').join(headers));
            test.setStringComparison(comparison.toString());
            if (caseSensitive) {
                test.setCaseSensitive(true);
            }
            test.setValue(value);
        }
    }

    @Override
    protected void visitHeaderTest(Node node, VisitPhase phase, RuleProperties props,
            List<String> headers, Sieve.ValueComparison comparison, boolean isCount, String value) {
        if (phase == VisitPhase.begin) {
            FilterTest.HeaderTest test = addTest(new FilterTest.HeaderTest(), props);
            test.setHeaders(Joiner.on(',').join(headers));
            if (isCount) {
               test.setCountComparison(comparison.toString());
            } else {
               test.setValueComparison(comparison.toString());
            }
            test.setValue(value);
        }
    }

    @Override
    protected void visitMimeHeaderTest(Node node, VisitPhase phase, RuleProperties props,
            List<String> headers, Sieve.StringComparison comparison, boolean caseSensitive, String value) {
        if (phase == VisitPhase.begin) {
            FilterTest.MimeHeaderTest test = addTest(new FilterTest.MimeHeaderTest(), props);
            test.setHeaders(Joiner.on(',').join(headers));
            test.setStringComparison(comparison.toString());
            if (caseSensitive) {
                test.setCaseSensitive(true);
            }
            test.setValue(value);
        }
    }

    @Override
    protected void visitAddressTest(Node node, VisitPhase phase, RuleProperties props, List<String> headers,
            Sieve.AddressPart part, Sieve.ValueComparison comparison, boolean isCount, String value) {
        FilterTest.AddressTest test = addTest(new FilterTest.AddressTest(), props);
        visitAddress(test, phase, props, headers, part, comparison, isCount, value);
    }

    @Override
    protected void visitAddressTest(Node node, VisitPhase phase, RuleProperties props, List<String> headers,
            Sieve.AddressPart part, Sieve.StringComparison comparison, boolean caseSensitive, String value) {
        FilterTest.AddressTest test = addTest(new FilterTest.AddressTest(), props);
        visitAddress(test, phase, props, headers, part, comparison, caseSensitive, value);
    }

    @Override
    protected void visitEnvelopeTest(Node node, VisitPhase phase, RuleProperties props, List<String> headers,
            Sieve.AddressPart part, Sieve.StringComparison comparison, boolean caseSensitive, String value) {
        FilterTest.EnvelopeTest test = addTest(new FilterTest.EnvelopeTest(), props);
        visitAddress(test, phase, props, headers, part, comparison, caseSensitive, value);
    }

    @Override
    protected void visitEnvelopeTest(Node node, VisitPhase phase, RuleProperties props, List<String> headers,
            Sieve.AddressPart part, Sieve.ValueComparison comparison, boolean isCount, String value) {
        FilterTest.EnvelopeTest test = addTest(new FilterTest.EnvelopeTest(), props);
        visitAddress(test, phase, props, headers, part, comparison, isCount, value);
    }

    private void visitAddress(FilterTest.AddressTest test, VisitPhase phase, RuleProperties props, List<String> headers, Sieve.AddressPart part,
        Sieve.ValueComparison comparison, boolean isCount, String value) {
        if (test != null && phase == VisitPhase.begin) {
            test.setHeader(Joiner.on(',').join(headers));
            test.setPart(part.toString());
            if (isCount) {
                test.setCountComparison(comparison.toString());
             } else {
                test.setValueComparison(comparison.toString());
             }
            test.setValue(value);
        }
    }

    private void visitAddress(FilterTest.AddressTest test, VisitPhase phase, RuleProperties props, List<String> headers, Sieve.AddressPart part,
        Sieve.StringComparison comparison, boolean caseSensitive, String value) {
        if (test != null && phase == VisitPhase.begin) {
            test.setHeader(Joiner.on(',').join(headers));
            test.setPart(part.toString());
            test.setStringComparison(comparison.toString());
            if (caseSensitive) {
                test.setCaseSensitive(true);
            }
            test.setValue(value);
        }
    }

    @Override
    protected void visitSizeTest(Node node, VisitPhase phase, RuleProperties props,
            Sieve.NumberComparison comparison, int size, String sizeString) {
        if (phase == VisitPhase.begin) {
            FilterTest.SizeTest test = addTest(new FilterTest.SizeTest(), props);
            test.setNumberComparison(comparison.toString());
            test.setSize(sizeString);
        }
    }

    @Override
    protected void visitAddressBookTest(Node node, VisitPhase phase, RuleProperties props, String header) {
        if (phase == VisitPhase.begin) {
            addTest(new FilterTest.AddressBookTest(header), props);
        }
    }

    @Override
    protected void visitContactRankingTest(Node node, VisitPhase phase, RuleProperties props, String header) {
        if (phase == VisitPhase.begin) {
            addTest(new FilterTest.ContactRankingTest(header), props);
        }
    }

    @Override
    protected void visitMeTest(Node node, VisitPhase phase, RuleProperties props, String header) {
        if (phase == VisitPhase.begin) {
            addTest(new FilterTest.MeTest(header), props);
        }
    }

    @Override
    protected void visitInviteTest(Node node, VisitPhase phase, RuleProperties props, List<String> methods) {
        if (phase == VisitPhase.begin) {
            FilterTest.InviteTest test = addTest(new FilterTest.InviteTest(), props);
            test.addMethod(methods);
        }
    }

    @Override
    protected void visitConversationTest(Node node, VisitPhase phase, RuleProperties props, String where) {
        if (phase == VisitPhase.begin) {
            FilterTest.ConversationTest test = addTest(new FilterTest.ConversationTest(), props);
            test.setWhere(where);
        }
    }

    @Override
    protected void visitFacebookTest(Node node, VisitPhase phase, RuleProperties props) {
        if (phase == VisitPhase.begin) {
            addTest(new FilterTest.FacebookTest(), props);
        }
    }

    @Override
    protected void visitLinkedInTest(Node node, VisitPhase phase, RuleProperties props) {
        if (phase == VisitPhase.begin) {
            addTest(new FilterTest.LinkedInTest(), props);
        }
    }

    @Override
    protected void visitSocialcastTest(Node node, VisitPhase phase, RuleProperties props) {
        if (phase == VisitPhase.begin) {
            addTest(new FilterTest.SocialcastTest(), props);
        }
    }

    @Override
    protected void visitTwitterTest(Node node, VisitPhase phase, RuleProperties props) {
        if (phase == VisitPhase.begin) {
            addTest(new FilterTest.TwitterTest(), props);
        }
    }

    @Override
    protected void visitCommunityRequestsTest(Node node, VisitPhase phase, RuleProperties props) {
        if (phase == VisitPhase.begin) {
            addTest(new FilterTest.CommunityRequestsTest(), props);
        }
    }
    @Override
    protected void visitCommunityContentTest(Node node, VisitPhase phase, RuleProperties props) {
        if (phase == VisitPhase.begin) {
            addTest(new FilterTest.CommunityContentTest(), props);
        }
    }
    @Override
    protected void visitCommunityConnectionsTest(Node node, VisitPhase phase, RuleProperties props) {
        if (phase == VisitPhase.begin) {
            addTest(new FilterTest.CommunityConnectionsTest(), props);
        }
    }

    @Override
    protected void visitListTest(Node node, VisitPhase phase, RuleProperties props) {
        if (phase == VisitPhase.begin) {
            addTest(new FilterTest.ListTest(), props);
        }
    }

    @Override
    protected void visitBulkTest(Node node, VisitPhase phase, RuleProperties props) {
        if (phase == VisitPhase.begin) {
            addTest(new FilterTest.BulkTest(), props);
        }
    }

    @Override
    protected void visitImportanceTest(Node node, VisitPhase phase, RuleProperties props,
            FilterTest.Importance importance) {
        if (phase == VisitPhase.begin) {
            addTest(new FilterTest.ImportanceTest(importance), props);
        }
    }

    @Override
    protected void visitFlaggedTest(Node node, VisitPhase phase, RuleProperties props, Sieve.Flag flag) {
        if (phase == VisitPhase.begin) {
            addTest(new FilterTest.FlaggedTest(flag.toString()), props);
        }
    }

    @Override
    protected void visitDiscardAction(Node node, VisitPhase phase, RuleProperties props) {
        if (phase == VisitPhase.begin) {
            addAction(new FilterAction.DiscardAction());
        }
    }

    @Override
    protected void visitFileIntoAction(Node node, VisitPhase phase, RuleProperties props, String folderPath, boolean copy) {
        if (phase == VisitPhase.begin) {
            addAction(new FilterAction.FileIntoAction(folderPath, copy));
        }
    }

    @Override
    protected void visitFileIntoAction(Node node, VisitPhase phase, RuleProperties props, String folderPath) {
        visitFileIntoAction(node, phase, props, folderPath, false);
    }

    @Override
    protected void visitFlagAction(Node node, VisitPhase phase, RuleProperties props, Sieve.Flag flag) {
        if (phase == VisitPhase.begin) {
            addAction(new FilterAction.FlagAction(flag.toString()));
        }
    }

    @Override
    protected void visitKeepAction(Node node, VisitPhase phase, RuleProperties props) {
        if (phase == VisitPhase.begin) {
            addAction(new FilterAction.KeepAction());
        }
    }

    @Override
    protected void visitRedirectAction(Node node, VisitPhase phase, RuleProperties props, String address) {
        visitRedirectAction(node, phase, props, address, false);
    }

    @Override
    protected void visitRedirectAction(Node node, VisitPhase phase, RuleProperties props, String address, boolean copy) {
        if (phase == VisitPhase.begin) {
            addAction(new FilterAction.RedirectAction(address, copy));
        }
    }

    @Override
    protected void visitReplyAction(Node node, VisitPhase phase, RuleProperties props, String bodyTemplate) {
        if (phase == VisitPhase.begin) {
            addAction(new FilterAction.ReplyAction(bodyTemplate));
        }
    }

    @Override
    protected void visitNotifyAction(Node node, VisitPhase phase, RuleProperties props,
            String emailAddr, String subjectTemplate, String bodyTemplate, int maxBodyBytes, List<String> origHeaders) {
        if (phase == VisitPhase.begin) {
            FilterAction.NotifyAction action = addAction(new FilterAction.NotifyAction());
            action.setAddress(emailAddr);
            if (!Strings.isNullOrEmpty(subjectTemplate)) {
                action.setSubject(subjectTemplate);
            }
            if (!Strings.isNullOrEmpty(bodyTemplate)) {
                action.setContent(bodyTemplate);
            }
            if (maxBodyBytes != -1) {
                action.setMaxBodySize(maxBodyBytes);
            }
            if (origHeaders != null && !origHeaders.isEmpty()) {
                action.setOrigHeaders(Joiner.on(',').join(origHeaders));
            }
        }
    }

    @Override
    protected void visitRFCCompliantNotifyAction(Node node, VisitPhase phase, RuleProperties props,
            String from, String importance, String options, String message, String method) {
        if (phase == VisitPhase.begin) {
            FilterAction.RFCCompliantNotifyAction action = addAction(new FilterAction.RFCCompliantNotifyAction());
            if (!Strings.isNullOrEmpty(from)) {
                action.setFrom(from);
            }
            if (!Strings.isNullOrEmpty(importance)) {
                action.setImportance(importance);
            }
            if (!Strings.isNullOrEmpty(options)) {
                action.setOptions(options);
            }
            if (!Strings.isNullOrEmpty(message)) {
                action.setMessage(message);
            }
            if (!Strings.isNullOrEmpty(method)) {
                action.setMethod(method);
            }
        }
    }

    @Override
    protected void visitStopAction(Node node, VisitPhase phase, RuleProperties props) {
        if (phase == VisitPhase.begin) {
            addAction(new FilterAction.StopAction());
        }
    }

    @Override
    protected void visitTagAction(Node node, VisitPhase phase, RuleProperties props, String tag) {
        if (phase == VisitPhase.begin) {
            addAction(new FilterAction.TagAction(tag));
        }
    }

    @Override
    protected void visitRejectAction(Node node, VisitPhase phase, RuleProperties props, String content) throws ServiceException {
        if (phase == VisitPhase.begin) {
            if (!Strings.isNullOrEmpty(content)) {
                addAction(new FilterAction.RejectAction(content));
            } else {
                throw ServiceException.PARSE_ERROR("Invalid reject action: Missing reject message", null);
            }
        }
    }

    @Override
    protected void visitErejectAction(Node node, VisitPhase phase, RuleProperties props, String content) throws ServiceException {
        if (phase == VisitPhase.begin) {
            if (!Strings.isNullOrEmpty(content)) {
                addAction(new FilterAction.ErejectAction(content));
            } else {
                throw ServiceException.PARSE_ERROR("Invalid ereject action: Missing ereject message", null);
            }
        }
    }

    @Override
    protected void visitLogAction(Node node, VisitPhase phase, RuleProperties props, FilterAction.LogAction.LogLevel logLevel, String logText) throws ServiceException {
        if (phase == VisitPhase.begin) {
            if (!Strings.isNullOrEmpty(logText)) {
                addAction(new FilterAction.LogAction(logLevel, logText));
            } else {
                throw ServiceException.PARSE_ERROR("Invalid log action: Missing log message", null);
            }
        }
    }
}
