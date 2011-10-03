/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011 Zimbra, Inc.
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

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.zimbra.common.filter.Sieve;
import com.zimbra.soap.mail.type.FilterAction;
import com.zimbra.soap.mail.type.FilterRule;
import com.zimbra.soap.mail.type.FilterTest;
import com.zimbra.soap.mail.type.FilterTests;

import org.apache.jsieve.parser.generated.Node;

import java.util.Date;
import java.util.List;

/**
 * Converts a Sieve node tree to the SOAP representation of
 * filter rules.
 */
public final class SieveToSoap extends SieveVisitor {

    private final List<FilterRule> rules = Lists.newArrayList();
    private final List<String> ruleNames;
    private FilterRule currentRule;
    private int currentRuleIndex = 0;

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
        currentRule = new FilterRule(getCurrentRuleName(), props.isEnabled);
        currentRule.setFilterTests(new FilterTests(props.condition.toString()));
        rules.add(currentRule);
        currentRuleIndex++;
    }

    private <T extends FilterTest> T addTest(T test, RuleProperties props) {
        FilterTests tests = currentRule.getFilterTests();
        test.setIndex(tests.size());
        tests.addTest(test);
        if (props.isNegativeTest) {
            test.setNegative(true);
        }
        return test;
    }

    private <T extends FilterAction> T addAction(T action) {
        action.setIndex(currentRule.getActionCount());
        currentRule.addFilterAction(action);
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
            Sieve.AddressPart part, Sieve.StringComparison comparison, boolean caseSensitive, String value) {
        if (phase == VisitPhase.begin) {
            FilterTest.AddressTest test = addTest(new FilterTest.AddressTest(), props);
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
    protected void visitFileIntoAction(Node node, VisitPhase phase, RuleProperties props, String folderPath) {
        if (phase == VisitPhase.begin) {
            addAction(new FilterAction.FileIntoAction(folderPath));
        }
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
        if (phase == VisitPhase.begin) {
            addAction(new FilterAction.RedirectAction(address));
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
}
