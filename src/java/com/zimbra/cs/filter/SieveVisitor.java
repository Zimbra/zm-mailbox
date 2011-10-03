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

import com.google.common.collect.ImmutableSet;
import com.zimbra.common.filter.Sieve;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.soap.mail.type.FilterTest;
import org.apache.jsieve.TagArgument;
import org.apache.jsieve.parser.SieveNode;
import org.apache.jsieve.parser.generated.ASTcommand;
import org.apache.jsieve.parser.generated.ASTtest;
import org.apache.jsieve.parser.generated.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Iterates a Sieve node tree and calls callbacks at various
 * points.  A subclass can override whichever <tt>visitXXX()</tt>
 * callbacks it is interested in.
 */
public abstract class SieveVisitor {

    protected enum VisitPhase { begin, end }

    @SuppressWarnings("unused")
    protected void visitNode(Node node, VisitPhase phase, RuleProperties props) throws ServiceException {
    }

    @SuppressWarnings("unused")
    protected void visitRule(Node node, VisitPhase phase, RuleProperties props) throws ServiceException {
    }

    @SuppressWarnings("unused")
    protected void visitTest(Node node, VisitPhase phase, RuleProperties props) throws ServiceException {
    }

    @SuppressWarnings("unused")
    protected void visitAction(Node node, VisitPhase phase, RuleProperties props) throws ServiceException {
    }

    @SuppressWarnings("unused")
    protected void visitHeaderTest(Node node, VisitPhase phase, RuleProperties props, List<String> headers,
            Sieve.StringComparison comparison, boolean caseSensitive, String value) throws ServiceException {
    }

    @SuppressWarnings("unused")
    protected void visitMimeHeaderTest(Node node, VisitPhase phase, RuleProperties props, List<String> headers,
            Sieve.StringComparison comparison, boolean caseSensitive, String value) throws ServiceException {
    }

    @SuppressWarnings("unused")
    protected void visitAddressTest(Node node, VisitPhase phase, RuleProperties props, List<String> headers,
            Sieve.AddressPart part, Sieve.StringComparison comparison, boolean caseSensitive, String value)
            throws ServiceException {
    }

    @SuppressWarnings("unused")
    protected void visitHeaderExistsTest(Node node, VisitPhase phase, RuleProperties props, String header)
            throws ServiceException {
    }

    @SuppressWarnings("unused")
    protected void visitSizeTest(Node node, VisitPhase phase, RuleProperties props,
            Sieve.NumberComparison comparison, int size, String sizeString) throws ServiceException {
    }

    @SuppressWarnings("unused")
    protected void visitDateTest(Node node, VisitPhase phase, RuleProperties props,
            Sieve.DateComparison comparison, Date date) throws ServiceException {
    }

    @SuppressWarnings("unused")
    protected void visitCurrentTimeTest(Node node, VisitPhase phase, RuleProperties props,
            Sieve.DateComparison comparison, String timeStr) throws ServiceException {
    }

    @SuppressWarnings("unused")
    protected void visitCurrentDayOfWeekTest(Node node, VisitPhase phase, RuleProperties props, List<String> days)
            throws ServiceException {
    }

    @SuppressWarnings("unused")
    protected void visitConversationTest(Node node, VisitPhase phase, RuleProperties props, String where)
            throws ServiceException {
    }

    @SuppressWarnings("unused")
    protected void visitFacebookTest(Node node, VisitPhase phase, RuleProperties props) throws ServiceException {
    }

    @SuppressWarnings("unused")
    protected void visitLinkedInTest(Node node, VisitPhase phase, RuleProperties props) throws ServiceException {
    }

    @SuppressWarnings("unused")
    protected void visitSocialcastTest(Node node, VisitPhase phase, RuleProperties props) throws ServiceException {
    }

    @SuppressWarnings("unused")
    protected void visitTwitterTest(Node node, VisitPhase phase, RuleProperties props) throws ServiceException {
    }

    @SuppressWarnings("unused")
    protected void visitListTest(Node node, VisitPhase phase, RuleProperties props) throws ServiceException {
    }

    @SuppressWarnings("unused")
    protected void visitBulkTest(Node node, VisitPhase phase, RuleProperties props) throws ServiceException {
    }

    @SuppressWarnings("unused")
    protected void visitImportanceTest(Node node, VisitPhase phase, RuleProperties props,
            FilterTest.Importance importance) throws ServiceException {
    }

    @SuppressWarnings("unused")
    protected void visitFlaggedTest(Node node, VisitPhase phase, RuleProperties props, Sieve.Flag flag)
            throws ServiceException {
    }

    @SuppressWarnings("unused")
    protected void visitTrueTest(Node node, VisitPhase phase, RuleProperties props) throws ServiceException {
    }

    @SuppressWarnings("unused")
    protected void visitAddressBookTest(Node node, VisitPhase phase, RuleProperties props, String header)
            throws ServiceException {
    }

    @SuppressWarnings("unused")
    protected void visitContactRankingTest(Node node, VisitPhase phase, RuleProperties props, String header)
            throws ServiceException {
    }

    @SuppressWarnings("unused")
    protected void visitMeTest(Node node, VisitPhase phase, RuleProperties props, String header)
            throws ServiceException {
    }

    @SuppressWarnings("unused")
    protected void visitBodyTest(Node node, VisitPhase phase, RuleProperties props,
            boolean caseSensitive, String value) throws ServiceException {
    }

    @SuppressWarnings("unused")
    protected void visitAttachmentTest(Node node, VisitPhase phase, RuleProperties props) throws ServiceException {
    }

    @SuppressWarnings("unused")
    protected void visitInviteTest(Node node, VisitPhase phase, RuleProperties props,
            List<String> methods) throws ServiceException {
    }

    @SuppressWarnings("unused")
    protected void visitKeepAction(Node node, VisitPhase phase, RuleProperties props) throws ServiceException {
    }

    @SuppressWarnings("unused")
    protected void visitDiscardAction(Node node, VisitPhase phase, RuleProperties props) throws ServiceException {
    }

    @SuppressWarnings("unused")
    protected void visitFileIntoAction(Node node, VisitPhase phase, RuleProperties props, String folderPath)
        throws ServiceException {
    }

    @SuppressWarnings("unused")
    protected void visitFlagAction(Node node, VisitPhase phase, RuleProperties props, Sieve.Flag flag)
            throws ServiceException {
    }

    @SuppressWarnings("unused")
    protected void visitTagAction(Node node, VisitPhase phase, RuleProperties props, String tagName)
            throws ServiceException {
    }

    @SuppressWarnings("unused")
    protected void visitRedirectAction(Node node, VisitPhase phase, RuleProperties props, String address)
            throws ServiceException {
    }

    @SuppressWarnings("unused")
    protected void visitReplyAction(Node node, VisitPhase phase, RuleProperties props, String bodyTemplate)
            throws ServiceException {
    }

    @SuppressWarnings("unused")
    protected void visitNotifyAction(Node node, VisitPhase phase, RuleProperties props, String emailAddr,
            String subjectTemplate, String bodyTemplate, int maxBodyBytes, List<String> origHeaders)
    throws ServiceException { }

    @SuppressWarnings("unused")
    protected void visitStopAction(Node node, VisitPhase phase, RuleProperties props) throws ServiceException {
    }

    private static final Set<String> RULE_NODE_NAMES = ImmutableSet.of("if", "disabled_if");

    public class RuleProperties {
        boolean isEnabled = true;
        boolean isNegativeTest = false;
        Sieve.Condition condition = Sieve.Condition.allof;
    }

    public void accept(Node node) throws ServiceException {
        accept(node, null);
    }

    private void accept(Node parent, RuleProperties props) throws ServiceException {
        visitNode(parent, VisitPhase.begin, props);

        int numChildren = parent.jjtGetNumChildren();
        for (int i = 0; i < numChildren; i++) {
            Node node = parent.jjtGetChild(i);

            if (isRuleNode(node)) {
                // New rule tree.
                RuleProperties newProps = new RuleProperties();
                if ("disabled_if".equalsIgnoreCase(getNodeName(node))) {
                    newProps.isEnabled = false;
                }
                accept(node, newProps);
            } else if (node instanceof ASTtest) {
                acceptTest(node, props);
            } else if (node instanceof ASTcommand) {
                acceptAction(node, props);
            } else {
                accept(node, props);
            }
        }

        visitNode(parent, VisitPhase.end, props);
    }

    private void acceptTest(Node node, RuleProperties props) throws ServiceException {
        visitTest(node, VisitPhase.begin, props);
        String nodeName = getNodeName(node);

        if ("not".equalsIgnoreCase(nodeName)) {
            props.isNegativeTest = true;
            accept(node, props);
        } else {
            if ("allof".equalsIgnoreCase(nodeName)) {
                props.condition = Sieve.Condition.allof;
                visitRule(node, VisitPhase.begin, props);
                accept(node, props);
                visitRule(node, VisitPhase.end, props);
            } else if ("anyof".equalsIgnoreCase(nodeName)) {
                props.condition = Sieve.Condition.anyof;
                visitRule(node, VisitPhase.begin, props);
                accept(node, props);
                visitRule(node, VisitPhase.end, props);
            } else if ("header".equalsIgnoreCase(nodeName) || "mime_header".equalsIgnoreCase(nodeName)) {
                Sieve.StringComparison comparison = Sieve.StringComparison.is;
                boolean caseSensitive = false;
                List<String> headers;
                String value;

                int headersArgIndex = 0;
                // There can be up to two tag arguments
                SieveNode firstTagArgNode, secondTagArgNode;
                firstTagArgNode = (SieveNode) getNode(node, 0, 0);
                if (firstTagArgNode.getValue() instanceof TagArgument) {
                    String argStr = stripLeadingColon(firstTagArgNode.getValue().toString());
                    try {
                        // assume that the first tag arg is match-type arg
                        comparison = Sieve.StringComparison.valueOf(argStr);
                        headersArgIndex ++;
                        secondTagArgNode = (SieveNode) getNode(node, 0 , 1);
                        if (secondTagArgNode.getValue() instanceof TagArgument) {
                            caseSensitive = Sieve.Comparator.ioctet == Sieve.Comparator.fromString(getValue(node, 0, 2, 0, 0));
                            headersArgIndex += 2;
                        }
                    } catch (IllegalArgumentException e) {
                        // so the first tag arg is not match-type arg, it must be :comparator arg then
                        caseSensitive = Sieve.Comparator.ioctet == Sieve.Comparator.fromString(getValue(node, 0, 1, 0, 0));
                        headersArgIndex += 2;
                        secondTagArgNode = (SieveNode) getNode(node, 0 , 2);
                        if (secondTagArgNode.getValue() instanceof TagArgument) {
                            argStr = stripLeadingColon(secondTagArgNode.getValue().toString());
                            comparison = Sieve.StringComparison.fromString(argStr);
                            headersArgIndex ++;
                        }
                    }
                }

                headers = getMultiValue(node, 0, headersArgIndex, 0);
                value = getValue(node, 0, headersArgIndex + 1, 0, 0);

                if ("header".equalsIgnoreCase(nodeName)) {
                    visitHeaderTest(node, VisitPhase.begin, props, headers, comparison, caseSensitive, value);
                    accept(node, props);
                    visitHeaderTest(node, VisitPhase.end, props, headers, comparison, caseSensitive, value);
                } else {
                    visitMimeHeaderTest(node, VisitPhase.begin, props, headers, comparison, caseSensitive, value);
                    accept(node, props);
                    visitMimeHeaderTest(node, VisitPhase.end, props, headers, comparison, caseSensitive, value);
                }
            } else if ("address".equalsIgnoreCase(nodeName)) {
                Sieve.AddressPart part = Sieve.AddressPart.all;
                Sieve.StringComparison comparison = Sieve.StringComparison.is;
                boolean caseSensitive = false;
                List<String> headers;
                String value;

                int nextArgIndex = 0;
                SieveNode argNode = (SieveNode) getNode(node, 0, nextArgIndex);
                // There can be up to three tag arguments
                for (int i = 0; i < 3 && argNode.getValue() instanceof TagArgument; i ++) {
                    TagArgument tagArg = (TagArgument) argNode.getValue();
                    if (tagArg.isComparator()) {
                        caseSensitive =
                                Sieve.Comparator.ioctet == Sieve.Comparator.fromString(getValue(node, 0, nextArgIndex + 1, 0, 0));
                        nextArgIndex += 2;
                    } else {
                        String argStr = stripLeadingColon(argNode.getValue().toString());
                        try {
                            // first assume that the next tag arg is match-type arg
                            comparison = Sieve.StringComparison.valueOf(argStr);
                        } catch (IllegalArgumentException e) {
                            // so the next tag arg is not match-type arg, it must be address-part arg then
                            part = Sieve.AddressPart.fromString(argStr);
                        }
                        nextArgIndex ++;
                    }
                    argNode = (SieveNode) getNode(node, 0, nextArgIndex);
                }

                headers = getMultiValue(node, 0, nextArgIndex, 0);
                value = getValue(node, 0, nextArgIndex + 1, 0, 0);

                visitAddressTest(node, VisitPhase.begin, props, headers, part, comparison, caseSensitive, value);
                accept(node, props);
                visitAddressTest(node, VisitPhase.end, props, headers, part, comparison, caseSensitive, value);
            } else if ("exists".equalsIgnoreCase(nodeName)) {
                String header = getValue(node, 0, 0, 0, 0);

                visitHeaderExistsTest(node, VisitPhase.begin, props, header);
                accept(node, props);
                visitHeaderExistsTest(node, VisitPhase.end, props, header);
            } else if ("size".equalsIgnoreCase(nodeName)) {
                String s = stripLeadingColon(getValue(node, 0, 0));
                Sieve.NumberComparison comparison = Sieve.NumberComparison.fromString(s);
                SieveNode sizeNode = (SieveNode) getNode(node, 0, 1);
                String sizeString = sizeNode.getFirstToken().toString();
                int size;
                try {
                    size = FilterUtil.parseSize(sizeString);
                } catch (NumberFormatException e) {
                    throw ServiceException.INVALID_REQUEST("Invalid size value " + sizeString, e);
                }

                visitSizeTest(node, VisitPhase.begin, props, comparison, size, sizeString);
                accept(node, props);
                visitSizeTest(node, VisitPhase.end, props, comparison, size, sizeString);
            } else if ("date".equalsIgnoreCase(nodeName)) {
                String s = stripLeadingColon(getValue(node, 0, 0));
                Sieve.DateComparison comparison = Sieve.DateComparison.fromString(s);
                String dateString = getValue(node, 0, 1, 0, 0);
                Date date = Sieve.DATE_PARSER.parse(dateString);
                if (date == null) {
                    throw ServiceException.PARSE_ERROR("Invalid date value: " + dateString, null);
                }

                visitDateTest(node, VisitPhase.begin, props, comparison, date);
                accept(node, props);
                visitDateTest(node, VisitPhase.end, props, comparison, date);
            } else if ("body".equalsIgnoreCase(nodeName)) {
                boolean caseSensitive = false;
                String value;
                if (getNode(node, 0, 1).jjtGetNumChildren() == 0) {
                    // must be :comparator
                    if (!":comparator".equals(getValue(node, 0, 1)))
                        throw ServiceException.PARSE_ERROR("Expected :comparator argument", null);
                    caseSensitive = Sieve.Comparator.ioctet == Sieve.Comparator.fromString(getValue(node, 0, 2, 0, 0));
                    value = getValue(node, 0, 3, 0, 0);
                } else {
                    value = getValue(node, 0, 1, 0, 0);
                }

                visitBodyTest(node, VisitPhase.begin, props, caseSensitive, value);
                accept(node, props);
                visitBodyTest(node, VisitPhase.end, props, caseSensitive, value);
            } else if ("attachment".equalsIgnoreCase(nodeName)) {
                visitAttachmentTest(node, VisitPhase.begin, props);
                accept(node, props);
                visitAttachmentTest(node, VisitPhase.end, props);
            } else if ("addressbook".equalsIgnoreCase(nodeName)) {
                String header = getValue(node, 0, 1, 0, 0);
                visitAddressBookTest(node, VisitPhase.begin, props, header);
                accept(node, props);
                visitAddressBookTest(node, VisitPhase.end, props, header);
            } else if ("contact_ranking".equalsIgnoreCase(nodeName)) {
                String header = getValue(node, 0, 1, 0, 0);
                visitContactRankingTest(node, VisitPhase.begin, props, header);
                accept(node, props);
                visitContactRankingTest(node, VisitPhase.end, props, header);
            } else if ("me".equalsIgnoreCase(nodeName)) {
                String header = getValue(node, 0, 1, 0, 0);
                visitMeTest(node, VisitPhase.begin, props, header);
                accept(node, props);
                visitMeTest(node, VisitPhase.end, props, header);
            } else if ("invite".equalsIgnoreCase(nodeName)) {
                List<String> methods = Collections.emptyList();
                if (getNode(node, 0).jjtGetNumChildren() > 0) {
                    // Arguments node has children.
                    methods = getMultiValue(node, 0, 1, 0);
                }
                visitInviteTest(node, VisitPhase.begin, props, methods);
                accept(node, props);
                visitInviteTest(node, VisitPhase.end, props, methods);
            } else if ("current_time".equalsIgnoreCase(nodeName)) {
                String s = stripLeadingColon(getValue(node, 0, 0));
                Sieve.DateComparison comparison = Sieve.DateComparison.fromString(s);
                String timeString = getValue(node, 0, 1, 0, 0);

                visitCurrentTimeTest(node, VisitPhase.begin, props, comparison, timeString);
                accept(node, props);
                visitCurrentTimeTest(node, VisitPhase.end, props, comparison, timeString);
            } else if ("current_day_of_week".equalsIgnoreCase(nodeName)) {
                List<String> days = getMultiValue(node, 0, 1, 0);

                visitCurrentDayOfWeekTest(node, VisitPhase.begin, props, days);
                accept(node, props);
                visitCurrentDayOfWeekTest(node, VisitPhase.end, props, days);
            } else if ("conversation".equalsIgnoreCase(nodeName)) {
                String where = getValue(node, 0, 1, 0, 0);
                visitConversationTest(node, VisitPhase.begin, props, where);
                accept(node, props);
                visitConversationTest(node, VisitPhase.end, props, where);
            } else if ("facebook".equalsIgnoreCase(nodeName)) {
                visitFacebookTest(node, VisitPhase.begin, props);
                accept(node, props);
                visitFacebookTest(node, VisitPhase.end, props);
            } else if ("linkedin".equalsIgnoreCase(nodeName)) {
                visitLinkedInTest(node, VisitPhase.begin, props);
                accept(node, props);
                visitLinkedInTest(node, VisitPhase.end, props);
            } else if ("socialcast".equalsIgnoreCase(nodeName)) {
                visitSocialcastTest(node, VisitPhase.begin, props);
                accept(node, props);
                visitSocialcastTest(node, VisitPhase.end, props);
            } else if ("twitter".equalsIgnoreCase(nodeName)) {
                visitTwitterTest(node, VisitPhase.begin, props);
                accept(node, props);
                visitTwitterTest(node, VisitPhase.end, props);
            } else if ("list".equalsIgnoreCase(nodeName)) {
                visitListTest(node, VisitPhase.begin, props);
                accept(node, props);
                visitListTest(node, VisitPhase.end, props);
            } else if ("bulk".equalsIgnoreCase(nodeName)) {
                visitBulkTest(node, VisitPhase.begin, props);
                accept(node, props);
                visitBulkTest(node, VisitPhase.end, props);
            } else if ("importance".equalsIgnoreCase(nodeName)) {
                FilterTest.Importance importance = FilterTest.Importance.fromString(getValue(node, 0, 0, 0, 0));
                visitImportanceTest(node, VisitPhase.begin, props, importance);
                accept(node, props);
                visitImportanceTest(node, VisitPhase.end, props, importance);
            } else if ("flagged".equalsIgnoreCase(nodeName)) {
                Sieve.Flag flag = Sieve.Flag.fromString(getValue(node, 0, 0, 0, 0));
                visitFlaggedTest(node, VisitPhase.begin, props, flag);
                accept(node, props);
                visitFlaggedTest(node, VisitPhase.end, props, flag);
            } else if ("true".equalsIgnoreCase(nodeName)) {
                visitTrueTest(node, VisitPhase.begin, props);
                accept(node, props);
                visitTrueTest(node, VisitPhase.end, props);
            } else {
                ZimbraLog.filter.debug("Ignoring unrecognized test type '%s'.", nodeName);
                accept(node, props);
            }

            // Done processing the current test.  Reset the negative test flag for
            // the next test (bug 46007).
            props.isNegativeTest = false;
        }

        visitTest(node, VisitPhase.end, props);
    }

    private void acceptAction(Node node, RuleProperties props) throws ServiceException {
        visitAction(node, VisitPhase.begin, props);
        String nodeName = getNodeName(node);

        if ("keep".equalsIgnoreCase(nodeName)) {
            visitKeepAction(node, VisitPhase.begin, props);
            accept(node, props);
            visitKeepAction(node, VisitPhase.end, props);
        } else if ("discard".equalsIgnoreCase(nodeName)) {
            visitDiscardAction(node, VisitPhase.begin, props);
            accept(node, props);
            visitDiscardAction(node, VisitPhase.end, props);
        } else if ("fileinto".equalsIgnoreCase(nodeName)) {
            String folderPath = getValue(node, 0, 0, 0, 0);
            visitFileIntoAction(node, VisitPhase.begin, props, folderPath);
            accept(node, props);
            visitFileIntoAction(node, VisitPhase.end, props, folderPath);
        } else if ("flag".equalsIgnoreCase(nodeName)) {
            String s = getValue(node, 0, 0, 0, 0);
            Sieve.Flag flag = Sieve.Flag.fromString(s);

            visitFlagAction(node, VisitPhase.begin, props, flag);
            accept(node, props);
            visitFlagAction(node, VisitPhase.end, props, flag);
        } else if ("tag".equalsIgnoreCase(nodeName)) {
            String tagName = getValue(node, 0, 0, 0, 0);
            visitTagAction(node, VisitPhase.begin, props, tagName);
            accept(node, props);
            visitTagAction(node, VisitPhase.end, props, tagName);
        } else if ("redirect".equalsIgnoreCase(nodeName)) {
            String address = getValue(node, 0, 0, 0, 0);
            visitRedirectAction(node, VisitPhase.begin, props, address);
            accept(node, props);
            visitRedirectAction(node, VisitPhase.end, props, address);
        } else if ("reply".equalsIgnoreCase(nodeName)) {
            String bodyTemplate = getValue(node, 0, 0, 0, 0);
            visitReplyAction(node, VisitPhase.begin, props, bodyTemplate);
            accept(node, props);
            visitReplyAction(node, VisitPhase.end, props, bodyTemplate);
        } else if ("notify".equalsIgnoreCase(nodeName)) {
            String emailAddr = getValue(node, 0, 0, 0, 0);
            String subjectTemplate = getValue(node, 0, 1, 0, 0);
            String bodyTemplate = getValue(node, 0, 2, 0, 0);
            int numArgs = getNode(node, 0).jjtGetNumChildren();
            int maxBodyBytes = -1;
            List<String> origHeaders = null;
            if (numArgs == 4) {
                if (getNode(node, 0, 3).jjtGetNumChildren() == 0) {
                    maxBodyBytes = Integer.valueOf(getValue(node, 0, 3));
                } else {
                    origHeaders = getMultiValue(node, 0, 3, 0);
                }
            } else if (numArgs == 5) {
                maxBodyBytes = Integer.valueOf(getValue(node, 0, 3));
                origHeaders = getMultiValue(node, 0, 4, 0);
            }
            visitNotifyAction(
                    node, VisitPhase.begin, props, emailAddr, subjectTemplate, bodyTemplate, maxBodyBytes, origHeaders);
            accept(node, props);
            visitNotifyAction(
                    node, VisitPhase.end, props, emailAddr, subjectTemplate, bodyTemplate, maxBodyBytes, origHeaders);
        } else if ("stop".equalsIgnoreCase(nodeName)) {
            visitStopAction(node, VisitPhase.begin, props);
            accept(node, props);
            visitStopAction(node, VisitPhase.end, props);
        } else {
            accept(node, props);
        }

        visitAction(node, VisitPhase.end, props);
    }

    /**
     * Returns the given node's name in lower case.
     */
    static String getNodeName(Node node) {
        if (node == null || !(node instanceof SieveNode)) {
            return null;
        }
        String name = ((SieveNode) node).getName();
        if (name != null) {
            name = name.toLowerCase();
        }
        return name;
    }

    protected Node getNode(Node parent, int ... indexes) throws ServiceException {
        Node node = parent;
        for (int index : indexes) {
            if (node.jjtGetNumChildren() == 0) {
                throw ServiceException.PARSE_ERROR(
                        "Subnode " + getNodeName(node) + " of node " + getNodeName(parent) + " has no children.", null);
            }

            if (index >= node.jjtGetNumChildren()) {
                throw ServiceException.PARSE_ERROR(
                        "Subnode " + getNodeName(node) + " of node " + getNodeName(parent) + " has " +
                                node.jjtGetNumChildren() + " children.  Requested child " + index + ".", null);
            }
            node = node.jjtGetChild(index);
        }
        return node;
    }

    private String getValue(Node parent, int ... indexes) throws ServiceException {
        Node child = getNode(parent, indexes);
        Object value = ((SieveNode) child).getValue();
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    private List<String> getMultiValue(Node parent, int ... indexes) throws ServiceException {
        Node child = getNode(parent, indexes);
        List<String> values = new ArrayList<String>();
        for (int i = 0; i < child.jjtGetNumChildren(); i++) {
            Object value = ((SieveNode) child.jjtGetChild(i)).getValue();
            values.add(value == null ? null : value.toString());
        }
        return values;
    }


    private String stripLeadingColon(String s) {
        if (s == null || s.length() == 0 || s.charAt(0) != ':') {
            return s;
        }
        return s.substring(1, s.length());
    }

    /**
     * Returns <tt>true</tt> if the given node is the root of a rule node
     * hierarchy.
     */
    private boolean isRuleNode(Node node) {
        if (node == null) {
            return false;
        }
        if (!(node instanceof ASTcommand)) {
            return false;
        }
        String name = getNodeName(node);
        return RULE_NODE_NAMES.contains(name);
    }
}

