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

import com.google.common.base.Strings;
import com.zimbra.common.filter.Sieve;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.ElementFactory;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.StringUtil;
import org.apache.jsieve.parser.generated.Node;

import java.util.Date;
import java.util.List;

/**
 * Converts a Sieve node tree to the SOAP representation of
 * filter rules.
 */
public final class SieveToSoap extends SieveVisitor {

    private final Element root;
    private final List<String> ruleNames;
    private Element currentRule;
    private int currentRuleIndex = 0;

    public SieveToSoap(ElementFactory factory, List<String> ruleNames) {
        this.root = factory.createElement(MailConstants.E_FILTER_RULES);
        this.ruleNames = ruleNames;
    }

    public Element getRootElement() {
        return root;
    }

    @Override
    protected void visitRule(Node ruleNode, VisitPhase phase, RuleProperties props) {
        if (phase == VisitPhase.end) {
            return;
        }

        // rule element
        currentRule = root.addElement(MailConstants.E_FILTER_RULE);
        String name = getCurrentRuleName();
        if (name != null) {
            currentRule.addAttribute(MailConstants.A_NAME, name);
        }
        currentRule.addAttribute(MailConstants.A_ACTIVE, props.isEnabled);

        // filterTests element
        Element filterTests = currentRule.addElement(MailConstants.E_FILTER_TESTS);
        filterTests.addAttribute(MailConstants.A_CONDITION, props.condition.toString());

        // filterActions element
        currentRule.addElement(MailConstants.E_FILTER_ACTIONS);

        currentRuleIndex++;
    }

    private Element addTest(String elementName, RuleProperties props) throws ServiceException {
        Element tests = currentRule.getElement(MailConstants.E_FILTER_TESTS);
        int index = tests.listElements().size();
        Element test = tests.addElement(elementName);
        if (props.isNegativeTest) {
            test.addAttribute(MailConstants.A_NEGATIVE, "1");
        }
        test.addAttribute(MailConstants.A_INDEX, index);
        return test;
    }

    private Element addAction(String elementName) throws ServiceException {
        Element actions = currentRule.getElement(MailConstants.E_FILTER_ACTIONS);
        int index = actions.listElements().size();
        Element action = actions.addElement(elementName);
        action.addAttribute(MailConstants.A_INDEX, Integer.toString(index));
        return action;
    }

    @Override
    protected void visitAttachmentTest(Node node, VisitPhase phase, RuleProperties props) throws ServiceException {
        if (phase == VisitPhase.begin) {
            addTest(MailConstants.E_ATTACHMENT_TEST, props);
        }
    }

    @Override
    protected void visitBodyTest(Node node, VisitPhase phase, RuleProperties props, boolean caseSensitive, String value)
            throws ServiceException {
        if (phase == VisitPhase.begin) {
            Element test = addTest(MailConstants.E_BODY_TEST, props);
            if (caseSensitive) {
                test.addAttribute(MailConstants.A_CASE_SENSITIVE, caseSensitive);
            }
            test.addAttribute(MailConstants.A_VALUE, value);
        }
    }

    @Override
    protected void visitDateTest(Node node, VisitPhase phase, RuleProperties props,
            Sieve.DateComparison comparison, Date date) throws ServiceException {
        if (phase == VisitPhase.begin) {
            Element test = addTest(MailConstants.E_DATE_TEST, props);
            test.addAttribute(MailConstants.A_DATE_COMPARISON, comparison.toString());
            test.addAttribute(MailConstants.A_DATE, date.getTime() / 1000);
        }
    }

    @Override
    protected void visitCurrentTimeTest(Node node, VisitPhase phase, RuleProperties props,
            Sieve.DateComparison comparison, String timeStr) throws ServiceException {
        if (phase == VisitPhase.begin) {
            Element test = addTest(MailConstants.E_CURRENT_TIME_TEST, props);
            test.addAttribute(MailConstants.A_DATE_COMPARISON, comparison.toString());
            test.addAttribute(MailConstants.A_TIME, timeStr);
        }
    }

    @Override
    protected void visitCurrentDayOfWeekTest(Node node, VisitPhase phase, RuleProperties props, List<String> days)
            throws ServiceException {
        if (phase == VisitPhase.begin) {
            Element test = addTest(MailConstants.E_CURRENT_DAY_OF_WEEK_TEST, props);
            test.addAttribute(MailConstants.A_VALUE, StringUtil.join(",", days));
        }
    }

    @Override
    protected void visitTrueTest(Node node, VisitPhase phase, RuleProperties props) throws ServiceException {
        if (phase == VisitPhase.begin) {
            addTest(MailConstants.E_TRUE_TEST, props);
        }
    }

    @Override
    protected void visitHeaderExistsTest(Node node, VisitPhase phase, RuleProperties props, String header)
            throws ServiceException {
        if (phase == VisitPhase.begin) {
            Element test = addTest(MailConstants.E_HEADER_EXISTS_TEST, props);
            test.addAttribute(MailConstants.A_HEADER, header);
        }
    }

    @Override
    protected void visitHeaderTest(String testEltName, Node node, VisitPhase phase, RuleProperties props,
            List<String> headers, Sieve.StringComparison comparison, boolean caseSensitive, String value)
            throws ServiceException {
        if (phase == VisitPhase.begin) {
            Element test = addTest(testEltName, props);
            test.addAttribute(MailConstants.A_HEADER, StringUtil.join(",", headers));
            test.addAttribute(MailConstants.A_STRING_COMPARISON, comparison.toString());
            if (caseSensitive) {
                test.addAttribute(MailConstants.A_CASE_SENSITIVE, caseSensitive);
            }
            test.addAttribute(MailConstants.A_VALUE, value);
        }
    }

    @Override
    protected void visitAddressTest(Node node, VisitPhase phase, RuleProperties props, List<String> headers,
            Sieve.AddressPart part, Sieve.StringComparison comparison, boolean caseSensitive, String value)
            throws ServiceException {
        if (phase == VisitPhase.begin) {
            Element test = addTest(MailConstants.E_ADDRESS_TEST, props);
            test.addAttribute(MailConstants.A_HEADER, StringUtil.join(",", headers));
            test.addAttribute(MailConstants.A_PART, part.toString());
            test.addAttribute(MailConstants.A_STRING_COMPARISON, comparison.toString());
            if (caseSensitive) {
                test.addAttribute(MailConstants.A_CASE_SENSITIVE, caseSensitive);
            }
            test.addAttribute(MailConstants.A_VALUE, value);
        }
    }

    @Override
    protected void visitSizeTest(Node node, VisitPhase phase, RuleProperties props,
            Sieve.NumberComparison comparison, int size, String sizeString) throws ServiceException {
        if (phase == VisitPhase.begin) {
            Element test = addTest(MailConstants.E_SIZE_TEST, props);
            test.addAttribute(MailConstants.A_NUMBER_COMPARISON, comparison.toString());
            test.addAttribute(MailConstants.A_SIZE, sizeString);
        }
    }

    @Override
    protected void visitAddressBookTest(Node node, VisitPhase phase, RuleProperties props, String header, String type)
            throws ServiceException {
        if (phase == VisitPhase.begin) {
            Element test = addTest(MailConstants.E_ADDRESS_BOOK_TEST, props);
            test.addAttribute(MailConstants.A_HEADER, header);
            test.addAttribute(MailConstants.A_CONTACT_TYPE, type);
        }
    }

    @Override
    protected void visitInviteTest(Node node, VisitPhase phase, RuleProperties props, List<String> methods)
            throws ServiceException {
        if (phase == VisitPhase.begin) {
            Element test = addTest(MailConstants.E_INVITE_TEST, props);
            for (String method : methods) {
                test.addElement(MailConstants.E_METHOD).setText(method);
            }
        }
    }

    @Override
    protected void visitConversationTest(Node node, VisitPhase phase, RuleProperties props, String where)
            throws ServiceException {
        if (phase == VisitPhase.begin) {
            Element test = addTest(MailConstants.E_CONVERSATION_TEST, props);
            test.addAttribute(MailConstants.A_WHERE, where);
        }
    }

    @Override
    protected void visitSocialcastTest(Node node, VisitPhase phase, RuleProperties props) throws ServiceException {
        if (phase == VisitPhase.begin) {
            addTest(MailConstants.E_SOCIALCAST_TEST, props);
        }
    }

    @Override
    protected void visitListTest(Node node, VisitPhase phase, RuleProperties props) throws ServiceException {
        if (phase == VisitPhase.begin) {
            addTest(MailConstants.E_LIST_TEST, props);
        }
    }

    @Override
    protected void visitBulkTest(Node node, VisitPhase phase, RuleProperties props) throws ServiceException {
        if (phase == VisitPhase.begin) {
            addTest(MailConstants.E_BULK_TEST, props);
        }
    }

    private String getCurrentRuleName() {
        if (ruleNames == null || currentRuleIndex >= ruleNames.size()) {
            return null;
        }
        return ruleNames.get(currentRuleIndex);
    }

    @Override
    protected void visitDiscardAction(Node node, VisitPhase phase, RuleProperties props) throws ServiceException {
        if (phase == VisitPhase.begin) {
            addAction(MailConstants.E_ACTION_DISCARD);
        }
    }

    @Override
    protected void visitFileIntoAction(Node node, VisitPhase phase, RuleProperties props, String folderPath)
            throws ServiceException {
        if (phase == VisitPhase.begin) {
            addAction(MailConstants.E_ACTION_FILE_INTO).addAttribute(MailConstants.A_FOLDER_PATH, folderPath);
        }
    }

    @Override
    protected void visitFlagAction(Node node, VisitPhase phase, RuleProperties props, Sieve.Flag flag)
            throws ServiceException {
        if (phase == VisitPhase.begin) {
            addAction(MailConstants.E_ACTION_FLAG).addAttribute(MailConstants.A_FLAG_NAME, flag.toString());
        }
    }

    @Override
    protected void visitKeepAction(Node node, VisitPhase phase, RuleProperties props) throws ServiceException {
        if (phase == VisitPhase.begin) {
            addAction(MailConstants.E_ACTION_KEEP);
        }
    }

    @Override
    protected void visitRedirectAction(Node node, VisitPhase phase, RuleProperties props, String address)
            throws ServiceException {
        if (phase == VisitPhase.begin) {
            addAction(MailConstants.E_ACTION_REDIRECT).addAttribute(MailConstants.A_ADDRESS, address);
        }
    }

    @Override
    protected void visitReplyAction(Node node, VisitPhase phase, RuleProperties props, String bodyTemplate)
            throws ServiceException {
        if (phase == VisitPhase.begin) {
            addAction(MailConstants.E_ACTION_REPLY).addElement(MailConstants.E_CONTENT).addText(bodyTemplate);
        }
    }

    @Override
    protected void visitNotifyAction(Node node, VisitPhase phase, RuleProperties props, String emailAddr,
            String subjectTemplate, String bodyTemplate, int maxBodyBytes) throws ServiceException {
        if (phase == VisitPhase.begin) {
            Element action = addAction(MailConstants.E_ACTION_NOTIFY);
            action.addAttribute(MailConstants.A_ADDRESS, emailAddr);
            if (!Strings.isNullOrEmpty(subjectTemplate)) {
                action.addAttribute(MailConstants.A_SUBJECT, subjectTemplate);
            }
            if (!Strings.isNullOrEmpty(bodyTemplate)) {
                action.addElement(MailConstants.E_CONTENT).addText(bodyTemplate);
            }
            if (maxBodyBytes != -1) {
                action.addAttribute(MailConstants.A_MAX_BODY_SIZE, maxBodyBytes);
            }
        }
    }

     @Override
    protected void visitStopAction(Node node, VisitPhase phase, RuleProperties props) throws ServiceException {
        if (phase == VisitPhase.begin) {
            addAction(MailConstants.E_ACTION_STOP);
        }
    }

    @Override
    protected void visitTagAction(Node node, VisitPhase phase, RuleProperties props, String tagName)
            throws ServiceException {
        if (phase == VisitPhase.begin) {
            addAction(MailConstants.E_ACTION_TAG).addAttribute(MailConstants.A_TAG_NAME, tagName);
        }
    }
}
