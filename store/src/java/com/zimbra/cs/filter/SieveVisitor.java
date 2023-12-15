/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011, 2013, 2014, 2015, 2016 Synacor, Inc.
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

import com.google.common.collect.ImmutableSet;
import com.zimbra.common.filter.Sieve;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.HeaderConstants;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.filter.jsieve.NotifyMailto;
import com.zimbra.soap.mail.type.FilterAction;
import com.zimbra.soap.mail.type.FilterTest;
import org.apache.jsieve.TagArgument;
import org.apache.jsieve.comparators.ComparatorNames;
import org.apache.jsieve.comparators.MatchTypeTags;
import org.apache.jsieve.parser.SieveNode;
import org.apache.jsieve.parser.generated.ASTcommand;
import org.apache.jsieve.parser.generated.ASTtest;
import org.apache.jsieve.parser.generated.Node;
import org.apache.jsieve.tests.ComparatorTags;

import java.util.ArrayList;
import java.util.Collections;
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
        // empty method
    }

    @SuppressWarnings("unused")
    protected void visitRule(Node node, VisitPhase phase, RuleProperties props) throws ServiceException {
        // empty method
    }

    @SuppressWarnings("unused")
    protected void visitVariable(Node node, VisitPhase phase, RuleProperties props, String name, String value) throws ServiceException {
        // empty method
    }

    @SuppressWarnings("unused")
    protected void visitIfControl(Node node, VisitPhase phase, RuleProperties props) throws ServiceException {
        // empty method
    }

    @SuppressWarnings("unused")
    protected void visitTest(Node node, VisitPhase phase, RuleProperties props) throws ServiceException {
        // empty method
    }

    @SuppressWarnings("unused")
    protected void visitAction(Node node, VisitPhase phase, RuleProperties props) throws ServiceException {
        // empty method
    }

    @SuppressWarnings("unused")
    protected void visitHeaderTest(Node node, VisitPhase phase, RuleProperties props, List<String> headers,
            Sieve.StringComparison comparison, boolean caseSensitive, String value) throws ServiceException {
        // empty method
    }

    @SuppressWarnings("unused")
    protected void visitHeaderTest(Node node, VisitPhase phase, RuleProperties props, List<String> headers,
            Sieve.ValueComparison comparison, Sieve.Comparator comparator, boolean isCount, String value) throws ServiceException {
        // empty method
    }

    @SuppressWarnings("unused")
    protected void visitMimeHeaderTest(Node node, VisitPhase phase, RuleProperties props, List<String> headers,
            Sieve.StringComparison comparison, boolean caseSensitive, String value) throws ServiceException {
        // empty method
    }

    @SuppressWarnings("unused")
    protected void visitAddressTest(Node node, VisitPhase phase, RuleProperties props, List<String> headers,
        Sieve.AddressPart part, Sieve.StringComparison comparison, boolean caseSensitive, String value)
        throws ServiceException {
        // empty method
    }

    @SuppressWarnings("unused")
    protected void visitAddressTest(Node node, VisitPhase phase, RuleProperties props, List<String> headers,
        Sieve.AddressPart part, Sieve.ValueComparison comparison, Sieve.Comparator comparator, boolean isCount, String value)
        throws ServiceException {
        // empty method
    }

    protected void visitEnvelopeTest(Node node, VisitPhase phase, RuleProperties props, List<String> headers,
        Sieve.AddressPart part, Sieve.StringComparison comparison, boolean caseSensitive, String value)
        throws ServiceException {
        // empty method
    }

    protected void visitEnvelopeTest(Node node, VisitPhase phase, RuleProperties props, List<String> headers,
        Sieve.AddressPart part, Sieve.ValueComparison comparison, Sieve.Comparator comparator, boolean isCount, String value)
        throws ServiceException {
        // empty method
    }

    @SuppressWarnings("unused")
    protected void visitHeaderExistsTest(Node node, VisitPhase phase, RuleProperties props, String header)
            throws ServiceException {
        // empty method
    }

    @SuppressWarnings("unused")
    protected void visitSizeTest(Node node, VisitPhase phase, RuleProperties props,
            Sieve.NumberComparison comparison, int size, String sizeString) throws ServiceException {
        // empty method
    }

    @SuppressWarnings("unused")
    protected void  visitDateTest(Node node, VisitPhase phase, RuleProperties props,
            Sieve.DateComparison comparison, String date) throws ServiceException {
        // empty method
    }

    @SuppressWarnings("unused")
    protected void visitCurrentTimeTest(Node node, VisitPhase phase, RuleProperties props,
            Sieve.DateComparison comparison, String timeStr) throws ServiceException {
        // empty method
    }

    @SuppressWarnings("unused")
    protected void visitCurrentDayOfWeekTest(Node node, VisitPhase phase, RuleProperties props, List<String> days)
            throws ServiceException {
        // empty method
    }

    @SuppressWarnings("unused")
    protected void visitConversationTest(Node node, VisitPhase phase, RuleProperties props, String where)
            throws ServiceException {
        // empty method
    }

    @SuppressWarnings("unused")
    protected void visitFacebookTest(Node node, VisitPhase phase, RuleProperties props) throws ServiceException {
        // empty method
    }

    @SuppressWarnings("unused")
    protected void visitLinkedInTest(Node node, VisitPhase phase, RuleProperties props) throws ServiceException {
        // empty method
    }

    @SuppressWarnings("unused")
    protected void visitSocialcastTest(Node node, VisitPhase phase, RuleProperties props) throws ServiceException {
        // empty method
    }

    @SuppressWarnings("unused")
    protected void visitTwitterTest(Node node, VisitPhase phase, RuleProperties props) throws ServiceException {
        // empty method
    }

    @SuppressWarnings("unused")
    protected void visitListTest(Node node, VisitPhase phase, RuleProperties props) throws ServiceException {
        // empty method
    }

    @SuppressWarnings("unused")
    protected void visitAllOfTest(Node node, VisitPhase phase, RuleProperties props) throws ServiceException {
        // empty method
    }

    @SuppressWarnings("unused")
    protected void visitAnyOfTest(Node node, VisitPhase phase, RuleProperties props) throws ServiceException {
        // empty method
    }

    @SuppressWarnings("unused")
    protected void visitBulkTest(Node node, VisitPhase phase, RuleProperties props) throws ServiceException {
        // empty method
    }

    @SuppressWarnings("unused")
    protected void visitCommunityRequestsTest(Node node, VisitPhase phase, RuleProperties props) throws ServiceException {
        // empty method
    }

    @SuppressWarnings("unused")
    protected void visitCommunityContentTest(Node node, VisitPhase phase, RuleProperties props) throws ServiceException {
        // empty method
    }

    @SuppressWarnings("unused")
    protected void visitCommunityConnectionsTest(Node node, VisitPhase phase, RuleProperties props) throws ServiceException {
        // empty method
    }

    @SuppressWarnings("unused")
    protected void visitImportanceTest(Node node, VisitPhase phase, RuleProperties props,
            FilterTest.Importance importance) throws ServiceException {
        // empty method
    }

    @SuppressWarnings("unused")
    protected void visitFlaggedTest(Node node, VisitPhase phase, RuleProperties props, Sieve.Flag flag)
            throws ServiceException {
        // empty method
    }

    @SuppressWarnings("unused")
    protected void visitTrueTest(Node node, VisitPhase phase, RuleProperties props) throws ServiceException {
        // empty method
    }

    @SuppressWarnings("unused")
    protected void visitAddressBookTest(Node node, VisitPhase phase, RuleProperties props, String header)
            throws ServiceException {
        // empty method
    }

    @SuppressWarnings("unused")
    protected void visitContactRankingTest(Node node, VisitPhase phase, RuleProperties props, String header)
            throws ServiceException {
        // empty method
    }

    @SuppressWarnings("unused")
    protected void visitMeTest(Node node, VisitPhase phase, RuleProperties props, String header)
            throws ServiceException {
        // empty method
    }

    @SuppressWarnings("unused")
    protected void visitBodyTest(Node node, VisitPhase phase, RuleProperties props,
            boolean caseSensitive, String value) throws ServiceException {
        // empty method
    }

    @SuppressWarnings("unused")
    protected void visitAttachmentTest(Node node, VisitPhase phase, RuleProperties props) throws ServiceException {
        // empty method
    }

    @SuppressWarnings("unused")
    protected void visitInviteTest(Node node, VisitPhase phase, RuleProperties props,
            List<String> methods) throws ServiceException {
        // empty method
    }

    @SuppressWarnings("unused")
    protected void visitKeepAction(Node node, VisitPhase phase, RuleProperties props) throws ServiceException {
        // empty method
    }

    @SuppressWarnings("unused")
    protected void visitDiscardAction(Node node, VisitPhase phase, RuleProperties props) throws ServiceException {
        // empty method
    }

    @SuppressWarnings("unused")
    protected void visitFileIntoAction(Node node, VisitPhase phase, RuleProperties props, String folderPath)
        throws ServiceException {
        // empty method
    }

    @SuppressWarnings("unused")
    protected void visitFileIntoAction(Node node, VisitPhase phase, RuleProperties props, String folderPath, boolean copy)
        throws ServiceException {
        // empty method
    }

    @SuppressWarnings("unused")
    protected void visitFlagAction(Node node, VisitPhase phase, RuleProperties props, Sieve.Flag flag)
            throws ServiceException {
        // empty method
    }

    @SuppressWarnings("unused")
    protected void visitTagAction(Node node, VisitPhase phase, RuleProperties props, String tagName)
            throws ServiceException {
        // empty method
    }

    @SuppressWarnings("unused")
    protected void visitRedirectAction(Node node, VisitPhase phase, RuleProperties props, String address)
            throws ServiceException {
        // empty method
    }

    @SuppressWarnings("unused")
    protected void visitRedirectAction(Node node, VisitPhase phase, RuleProperties props, String address, boolean copy)
            throws ServiceException {
        // empty method
    }

    @SuppressWarnings("unused")
    protected void visitReplyAction(Node node, VisitPhase phase, RuleProperties props, String bodyTemplate)
            throws ServiceException {
        // empty method
    }

    @SuppressWarnings("unused")
    protected void visitNotifyAction(Node node, VisitPhase phase, RuleProperties props, String emailAddr,
            String subjectTemplate, String bodyTemplate, int maxBodyBytes, List<String> origHeaders)
            throws ServiceException {
        // empty method
    }

    @SuppressWarnings("unused")
    protected void visitRFCCompliantNotifyAction(Node node, VisitPhase phase, RuleProperties props, String from,
            String importance, String options, String message, String method)
        throws ServiceException {
        // empty method
    }

    @SuppressWarnings("unused")
    protected void visitStopAction(Node node, VisitPhase phase, RuleProperties props) throws ServiceException {
        // empty method
    }

    @SuppressWarnings("unused")
    protected void visitRejectAction(Node node, VisitPhase phase, RuleProperties props, String content) throws ServiceException {
        // empty method
    }

    @SuppressWarnings("unused")
    protected void visitErejectAction(Node node, VisitPhase phase, RuleProperties props, String content) throws ServiceException {
        // empty method
    }

    @SuppressWarnings("unused")
    protected void visitLogAction(Node node, VisitPhase phase, RuleProperties props, FilterAction.LogAction.LogLevel logLevel, String logText) throws ServiceException {
        // empty method
    }

    @SuppressWarnings("unused")
    protected void visitAddheaderAction(Node node, VisitPhase phase, RuleProperties props, String headerName, String headerValue, Boolean last) throws ServiceException {
        // empty method
    }

    @SuppressWarnings("unused")
    protected void visitDeleteheaderAction(Node node, VisitPhase phase, RuleProperties props, Boolean last, Integer offset, String matchType, Boolean countComparision,
            Boolean valueComparision, String relationalComparator, String comparator, String headerName, List<String> headerValue) throws ServiceException {
        // empty method
    }

    @SuppressWarnings("unused")
    protected void visitReplaceheaderAction(Node node, VisitPhase phase, RuleProperties props, Boolean last, Integer offset, String newName, String newValue,
            String matchType, Boolean countComparision, Boolean valueComparision, String relationalComparator, String comparator, String headerName,
            List<String> headerValue) throws ServiceException {
        // empty method
    }

    private static final Set<String> RULE_NODE_NAMES = ImmutableSet.of("if", "disabled_if", "elsif");
    private static final String COPY_EXT = ":copy";

    public class RuleProperties {
        private boolean isEnabled = true;
        private boolean isNegativeTest = false;
        private Sieve.Condition condition = null;

        public boolean isEnabled() {
            return isEnabled;
        }
        public void setEnabled(boolean isEnabled) {
            this.isEnabled = isEnabled;
        }
        public boolean isNegativeTest() {
            return isNegativeTest;
        }
        public void setNegativeTest(boolean isNegativeTest) {
            this.isNegativeTest = isNegativeTest;
        }
        public Sieve.Condition getCondition() {
            return condition;
        }
        public void setCondition(Sieve.Condition condition) {
            this.condition = condition;
        }
    }

    public void accept(Node node) throws ServiceException {
        accept(node, null);
    }

    public void accept(Node node, boolean isAdminScript) throws ServiceException {
        accept(node, null, isAdminScript);
    }

    private void accept(Node parent, RuleProperties props) throws ServiceException {
        accept(parent, props, false);
    }

    private void accept(Node parent, RuleProperties props, boolean isAdminScript) throws ServiceException {
        visitNode(parent, VisitPhase.begin, props);

        int numChildren = parent.jjtGetNumChildren();
        for (int i = 0; i < numChildren; i++) {
            Node node = parent.jjtGetChild(i);

            if (isRuleNode(node)) {
                // New rule tree or Nested if. New RuleProperties is created for each nested if
                RuleProperties newProps = new RuleProperties();
                if ("disabled_if".equalsIgnoreCase(getNodeName(node))) {
                    newProps.setEnabled(false);
                }
                visitIfControl(node,VisitPhase.begin,newProps);
                accept(node, newProps, isAdminScript);
                visitIfControl(node,VisitPhase.end,newProps);
            } else if (node instanceof ASTtest) {
                acceptTest(node, props);
            } else if (node instanceof ASTcommand) {
                acceptAction(node, props, isAdminScript);
            } else {
                accept(node, props, isAdminScript);
            }
        }

        visitNode(parent, VisitPhase.end, props);
    }

    private void acceptTest(Node node, RuleProperties props) throws ServiceException {
        visitTest(node, VisitPhase.begin, props);
        String nodeName = getNodeName(node);

        if ("not".equalsIgnoreCase(nodeName)) {
            props.setNegativeTest(true);
            accept(node, props);
        } else {
            if ("allof".equalsIgnoreCase(nodeName)) {
                props.setCondition(Sieve.Condition.allof);
                visitAllOfTest(node, VisitPhase.begin, props);
                accept(node, props);
                visitAllOfTest(node, VisitPhase.end, props);
            } else if ("anyof".equalsIgnoreCase(nodeName)) {
                props.setCondition(Sieve.Condition.anyof);
                visitAnyOfTest(node, VisitPhase.begin, props);
                accept(node, props);
                visitAnyOfTest(node, VisitPhase.end, props);
            } else if ("header".equalsIgnoreCase(nodeName) || "mime_header".equalsIgnoreCase(nodeName)) {
                boolean caseSensitive = false;
                List<String> headers;
                String value;
                Sieve.ValueComparison valueComparison = null;
                boolean isCount = false;
                Sieve.StringComparison comparison = null;
                Sieve.Comparator comparator = null;
                int headersArgIndex = 0;
                // There can be up to two tag arguments
                SieveNode firstTagArgNode, secondTagArgNode;
                firstTagArgNode = (SieveNode) getNode(node, 0, 0);
                if (firstTagArgNode.getValue() instanceof TagArgument) {
                    String argStr = stripLeadingColon(firstTagArgNode.getValue().toString());
                    try {
                         if ("count".equals(argStr) || "value".equals(argStr)) {
                            if ("count".equals(argStr)) {
                                isCount = true;
                            }
                            valueComparison = Sieve.ValueComparison.valueOf(getValue(node, 0, 1, 0, 0));
                            headersArgIndex += 2;
                            secondTagArgNode = (SieveNode) getNode(node, 0 , 2);
                            if (secondTagArgNode.getValue() instanceof TagArgument) {
                               comparator = Sieve.Comparator.fromString(getValue(node, 0, 3, 0, 0));
                               headersArgIndex += 2;
                            }
                         } else {
                            // assume that the first tag arg is match-type arg
                            comparison = Sieve.StringComparison.valueOf(argStr);
                            headersArgIndex ++;
                            secondTagArgNode = (SieveNode) getNode(node, 0 , 1);
                            if (secondTagArgNode.getValue() instanceof TagArgument) {
                                caseSensitive = Sieve.Comparator.ioctet == Sieve.Comparator.fromString(getValue(node, 0, 2, 0, 0));
                                headersArgIndex += 2;
                            }
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
                validateCountComparator(isCount, comparator);

                if ("header".equalsIgnoreCase(nodeName)) {
                    if (valueComparison != null) {
                        visitHeaderTest(node, VisitPhase.begin, props, headers, valueComparison, comparator, isCount, value);
                        accept(node, props);
                        visitHeaderTest(node, VisitPhase.end, props, headers, valueComparison, comparator, isCount, value);
                    } else {
                        visitHeaderTest(node, VisitPhase.begin, props, headers, comparison, caseSensitive, value);
                        accept(node, props);
                        visitHeaderTest(node, VisitPhase.end, props, headers, comparison, caseSensitive, value);
                    }
                } else {
                    visitMimeHeaderTest(node, VisitPhase.begin, props, headers, comparison, caseSensitive, value);
                    accept(node, props);
                    visitMimeHeaderTest(node, VisitPhase.end, props, headers, comparison, caseSensitive, value);
                }
            } else if ("address".equalsIgnoreCase(nodeName) || "envelope".equalsIgnoreCase(nodeName)) {
                Sieve.AddressPart part = Sieve.AddressPart.all;
                Sieve.StringComparison comparison = Sieve.StringComparison.is;
                boolean caseSensitive = false;
                List<String> headers;
                String value;

                int nextArgIndex = 0;
                boolean isCount = false;
                Sieve.ValueComparison valueComparison = null;
                Sieve.Comparator comparator = null;
                SieveNode firstTagArgNode;
                firstTagArgNode = (SieveNode) getNode(node, 0, 0);
                if (firstTagArgNode.getValue() instanceof TagArgument) {
                     String firstArgStr = firstTagArgNode.getValue().toString();
                     if (HeaderConstants.COUNT.equalsIgnoreCase(firstArgStr) || HeaderConstants.VALUE.equalsIgnoreCase(firstArgStr)) {
                        if (HeaderConstants.COUNT.equalsIgnoreCase(firstArgStr)) {
                             isCount = true;
                        }
                        valueComparison = Sieve.ValueComparison.valueOf(getValue(node, 0, 1, 0, 0));
                        nextArgIndex += 2;
                     }
                }
                SieveNode argNode = (SieveNode) getNode(node, 0, nextArgIndex);
                // There can be up to three tag arguments
                for (int i = 0; i < 3 && argNode.getValue() instanceof TagArgument; i ++) {
                    TagArgument tagArg = (TagArgument) argNode.getValue();
                    if (tagArg.isComparator()) {
                        comparator = Sieve.Comparator.fromString(getValue(node, 0, nextArgIndex + 1, 0, 0));
                        caseSensitive = Sieve.Comparator.ioctet == comparator;
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
                validateCountComparator(isCount, comparator);

                if ("envelope".equalsIgnoreCase(nodeName)) {
                    if (valueComparison != null) {
                        visitEnvelopeTest(node, VisitPhase.begin, props, headers, part, valueComparison, comparator, isCount,
                            value);
                        accept(node, props);
                        visitEnvelopeTest(node, VisitPhase.end, props, headers, part, valueComparison, comparator, isCount, value);
                    } else {
                        visitEnvelopeTest(node, VisitPhase.begin, props, headers, part, comparison, caseSensitive,
                            value);
                        accept(node, props);
                        visitEnvelopeTest(node, VisitPhase.end, props, headers, part, comparison, caseSensitive, value);
                    }
                } else {
                    if (valueComparison != null) {
                        visitAddressTest(node, VisitPhase.begin, props, headers, part, valueComparison, comparator, isCount, value);
                        accept(node, props);
                        visitAddressTest(node, VisitPhase.end, props, headers, part, valueComparison, comparator, isCount, value);
                    } else {
                        visitAddressTest(node, VisitPhase.begin, props, headers, part, comparison, caseSensitive,
                            value);
                        accept(node, props);
                        visitAddressTest(node, VisitPhase.end, props, headers, part, comparison, caseSensitive, value);
                    }
                }
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
                //Previously code was sending the timestamp, but now it is sending date in String so no parsing is needed.
                //Date date = Sieve.DATE_PARSER.parse(dateString);
                if (dateString == null) {
                    throw ServiceException.PARSE_ERROR("Invalid date value: " + dateString, null);
                }

                visitDateTest(node, VisitPhase.begin, props, comparison, dateString);
                accept(node, props);
                visitDateTest(node, VisitPhase.end, props, comparison, dateString);
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
                StringBuilder format = new StringBuilder();
                Node argNode = getNode(node, 0, 1, 0);
                int argNodeLen = argNode.jjtGetNumChildren();
                for (int i = 0; i < argNodeLen; i++) {
                    if (i > 0) {
                        format.append(",");
                    }
                    format.append(getValue(argNode, i));
                }
                String header = format.toString();
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
            } else if ("community_requests".equalsIgnoreCase(nodeName)) {
                visitCommunityRequestsTest(node, VisitPhase.begin, props);
                accept(node, props);
                visitCommunityRequestsTest(node, VisitPhase.end, props);
            } else if ("community_content".equalsIgnoreCase(nodeName)) {
                visitCommunityContentTest(node, VisitPhase.begin, props);
                accept(node, props);
                visitCommunityContentTest(node, VisitPhase.end, props);
            } else if ("community_connections".equalsIgnoreCase(nodeName)) {
                visitCommunityConnectionsTest(node, VisitPhase.begin, props);
                accept(node, props);
                visitCommunityConnectionsTest(node, VisitPhase.end, props);
            }else {
                ZimbraLog.filter.debug("Ignoring unrecognized test type '%s'.", nodeName);
                accept(node, props);
            }

            // Done processing the current test.  Reset the negative test flag for
            // the next test (bug 46007).
            props.isNegativeTest = false;
        }

        visitTest(node, VisitPhase.end, props);
    }

    private void acceptAction(Node node, RuleProperties props, boolean isAdminScript) throws ServiceException {
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
        } else if ("fileinto".equalsIgnoreCase(nodeName) || "redirect".equalsIgnoreCase(nodeName)) {
            int numArgs = getNode(node, 0).jjtGetNumChildren();
            boolean copy = false;
            String target = null;
            for (int i = 0; i < numArgs; i++) {
                boolean isTag = getNode(node, 0, i).jjtGetNumChildren() == 0 ? true : false;
                if (isTag) {
                    String value = getValue(node, 0, i++);
                    if (COPY_EXT.equals(value)) {
                        copy = true;
                    } else {
                        throw ServiceException.PARSE_ERROR("Invalid argument: " + value, null);
                    }
                    target = getValue(node, 0, i, 0, 0);
                } else {
                    target = getValue(node, 0, i, 0, 0);
                }
            }
            if ("fileinto".equalsIgnoreCase(nodeName)) {
                visitFileIntoAction(node, VisitPhase.begin, props, target, copy);
                accept(node, props);
                visitFileIntoAction(node, VisitPhase.end, props, target, copy);
            }
            if ("redirect".equalsIgnoreCase(nodeName)) {
                visitRedirectAction(node, VisitPhase.begin, props, target, copy);
                accept(node, props);
                visitRedirectAction(node, VisitPhase.end, props, target, copy);
            }
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
        } else if ("reply".equalsIgnoreCase(nodeName)) {
            String bodyTemplate = getValue(node, 0, 0, 0, 0);
            visitReplyAction(node, VisitPhase.begin, props, bodyTemplate);
            accept(node, props);
            visitReplyAction(node, VisitPhase.end, props, bodyTemplate);
        } else if ("notify".equalsIgnoreCase(nodeName)) {
            if (!isRFCCompliantNotifyAction(node)) {
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
            } else {
                // RFC 5435 compliant 'notify' format
                String from = null;
                String importance = null;
                String options = null;
                String message = null;
                String methodMailto = null;

                int numArgs = getNode(node, 0).jjtGetNumChildren();
                String tag = null;
                String value = null;
                for (int i = 0; i < numArgs; i++) {
                    boolean isTag = getNode(node, 0, i).jjtGetNumChildren() == 0 ? true : false;
                    if (isTag) {
                        tag = getValue(node, 0, i++);
                        value = getValue(node, 0, i, 0, 0);
                        if (null == from && NotifyMailto.NOTIFY_FROM.equalsIgnoreCase(tag)) {
                            from = value;
                        } else if (null == importance && NotifyMailto.NOTIFY_IMPORTANCE.equalsIgnoreCase(tag)) {
                            importance = value;
                        } else if (null == options && NotifyMailto.NOTIFY_OPTIONS.equalsIgnoreCase(tag)) {
                            options = value;
                        } else if (null == message && NotifyMailto.NOTIFY_MESSAGE.equalsIgnoreCase(tag)) {
                            message = value;
                        } else {
                            throw ServiceException.PARSE_ERROR("Invalid notify tag: "+ tag +", valid values: " + value, null);
                        }
                    } else {
                        value = getValue(node, 0, i, 0, 0);
                        if (null == methodMailto && null != value && value.toLowerCase().startsWith(NotifyMailto.NOTIFY_METHOD_MAILTO)) {
                            methodMailto = value;
                        } else {
                            throw ServiceException.PARSE_ERROR("Invalid notify value: " + value, null);
                        }
                    }
                }
                visitRFCCompliantNotifyAction(node, VisitPhase.begin, props, from, importance, options, message, methodMailto);
                accept(node, props);
                visitRFCCompliantNotifyAction(node, VisitPhase.end, props, from, importance, options, message, methodMailto);
            }
        } else if ("stop".equalsIgnoreCase(nodeName)) {
            visitStopAction(node, VisitPhase.begin, props);
            accept(node, props);
            visitStopAction(node, VisitPhase.end, props);
        } else if ("set".equalsIgnoreCase(nodeName)) {
            String name = getValue(node, 0, 0, 0, 0);
            String value = getValue(node, 0, 1, 0, 0);
            visitVariable(node, VisitPhase.begin, props, name, value);
            accept(node, props);
            visitVariable(node, VisitPhase.end, props, name, value);
        } else if ("reject".equalsIgnoreCase(nodeName)) {
            String content = null;
            String val1 = getValue(node, 0, 0, 0, 0);
            if (!StringUtil.isNullOrEmpty(val1)) {
                if (FilterAction.RejectAction.TEXT_TEMPLATE.equals(val1)) {
                    content = getValue(node, 0, 1, 0, 0);
                } else {
                    content = val1;
                }
                visitRejectAction(node, VisitPhase.begin, props, content);
                accept(node, props);
                visitRejectAction(node, VisitPhase.end, props, content);
            } else {
                throw ServiceException.PARSE_ERROR("Invalid reject action: Missing reject message", null);
            }
        } else if ("ereject".equalsIgnoreCase(nodeName)) {
            String content = null;
            String val1 = getValue(node, 0, 0, 0, 0);
            if (!StringUtil.isNullOrEmpty(val1)) {
                if (FilterAction.RejectAction.TEXT_TEMPLATE.equals(val1)) {
                    content = getValue(node, 0, 1, 0, 0);
                } else {
                    content = val1;
                }
                visitErejectAction(node, VisitPhase.begin, props, content);
                accept(node, props);
                visitErejectAction(node, VisitPhase.end, props, content);
            } else {
                throw ServiceException.PARSE_ERROR("Invalid ereject action: Missing ereject message", null);
            }
        } else if ("log".equalsIgnoreCase(nodeName)) {
            FilterAction.LogAction.LogLevel logLevel = null;
            boolean isTag = getNode(node, 0, 0) == null ? false : getNode(node, 0, 0).jjtGetNumChildren() == 0 ? true : false;
            String logText = null;
            if (isTag) {
                String level = getValue(node, 0, 0);
                if (!StringUtil.isNullOrEmpty(level)) {
                    level = level.substring(1);// remove preceding ":"
                    try {
                        logLevel = FilterAction.LogAction.LogLevel.fromString(level);
                    } catch (ServiceException se) {
                        ZimbraLog.filter.info("Invalid log level found: %s, reseting to %s", level, FilterAction.LogAction.LogLevel.info);
                    }
                    if (!(FilterAction.LogAction.LogLevel.fatal == logLevel
                            || FilterAction.LogAction.LogLevel.error == logLevel
                            || FilterAction.LogAction.LogLevel.warn == logLevel
                            || FilterAction.LogAction.LogLevel.info == logLevel
                            || FilterAction.LogAction.LogLevel.debug == logLevel
                            || FilterAction.LogAction.LogLevel.trace == logLevel
                            )) {
                        String message = "Invalid log action: Invalid log level found: " + logLevel.toString();
                        throw ServiceException.PARSE_ERROR(message, null);
                    }
                }
                logText = getValue(node, 0, 1, 0, 0);
            } else {
                logText = getValue(node, 0, 0, 0, 0);
            }
            if (!StringUtil.isNullOrEmpty(logText)) {
                visitLogAction(node, VisitPhase.begin, props, logLevel, logText);
                accept(node, props);
                visitLogAction(node, VisitPhase.end, props, logLevel, logText);
            } else {
                throw ServiceException.PARSE_ERROR("Invalid log action: Missing log message", null);
            }
        } else if ("addheader".equalsIgnoreCase(nodeName)) {
            if (isAdminScript) {
                parseAddheader(node, props);
            } else {
                throw ServiceException.PARSE_ERROR("Invalid addheader action: addheader action is not allowed in user scripts", null);
            }
        } else if ("deleteheader".equalsIgnoreCase(nodeName)) {
            if (isAdminScript) {
                parseDeleteheader(node, props);
            } else {
                throw ServiceException.PARSE_ERROR("Invalid deleteheader action: deleteheader action is not allowed in user scripts", null);
            }
        } else if ("replaceheader".equalsIgnoreCase(nodeName)) {
            if (isAdminScript) {
                parseReplaceheader(node, props);
            } else {
                throw ServiceException.PARSE_ERROR("Invalid replaceheader action: replaceheader action is not allowed in user scripts", null);
            }
        } else {
            accept(node, props);
        }

        visitAction(node, VisitPhase.end, props);
    }

    private Integer visitIndex(Node node, String tag, int i) throws ServiceException {
        String tempValue = getValue(node, 0, i);
        if (tempValue == null) {
            throw ServiceException.PARSE_ERROR("No value received with \"" + tag + "\"", null);
        } else {
            try {
                return Integer.valueOf(tempValue);
            } catch (NumberFormatException nfe) {
                throw ServiceException.PARSE_ERROR("Invalid value \"" + tempValue + "\" received with \"" + tag + "\"", nfe);
            }
        }
    }

    private void validateRelationalComparator(String tempValue, String tag) throws ServiceException {
        if (!(tempValue.equals(HeaderConstants.GT_OP)
                || tempValue.equals(HeaderConstants.GE_OP)
                || tempValue.equals(HeaderConstants.LT_OP)
                || tempValue.equals(HeaderConstants.LE_OP)
                || tempValue.equals(HeaderConstants.EQ_OP)
                || tempValue.equals(HeaderConstants.NE_OP))) {
            throw ServiceException.PARSE_ERROR("Invalid value \"" + tempValue + "\" received with \"" + tag + "\"", null);
        }
    }

    private String visitRelationalComparator(Node node, String tag, int i) throws ServiceException {
        String relationalComparator = getValue(node, 0, i, 0, 0);
        if (relationalComparator == null) {
            throw ServiceException.PARSE_ERROR("No value received with \"" + tag + "\"", null);
        }
        validateRelationalComparator(relationalComparator, tag);
        return relationalComparator;
    }

    private String visitComparator(Node node, String tag, int i) throws ServiceException {
        String tempValue = getValue(node, 0, i, 0, 0);
        if (tempValue == null) {
            throw ServiceException.PARSE_ERROR("No value received with \"" + tag + "\"", null);
        }
        if (!(tempValue.equals(HeaderConstants.I_ASCII_NUMERIC)
                || tempValue.equals(ComparatorNames.OCTET_COMPARATOR)
                || tempValue.equals(ComparatorNames.ASCII_CASEMAP_COMPARATOR)
                )) {
            throw ServiceException.PARSE_ERROR("Invalid value \"" + tempValue + "\" received with \"" + tag + "\"", null);
        }
        return tempValue;
    }

    private void parseAddheader(Node node, RuleProperties props) throws ServiceException {
        String headerName = null;
        String headerValue = null;
        Boolean last = false;
        boolean isTag = getNode(node, 0, 0) == null ? false : getNode(node, 0, 0).jjtGetNumChildren() == 0 ? true : false;
        if (isTag) {
            String value = getValue(node, 0, 0);
            if (!StringUtil.isNullOrEmpty(value)) {
                last = value.equals(HeaderConstants.LAST);
            } else {
                throw ServiceException.PARSE_ERROR("Invalid argument :" + value + " received with addheader", null);
            }
            headerName = getValue(node, 0, 1, 0, 0);
            headerValue = getValue(node, 0, 2, 0, 0);
        } else {
            headerName = getValue(node, 0, 0, 0, 0);
            headerValue = getValue(node, 0, 1, 0, 0);
        }

        if (!StringUtil.isNullOrEmpty(headerName) && !StringUtil.isNullOrEmpty(headerValue)) {
            visitAddheaderAction(node, VisitPhase.begin, props, headerName, headerValue, last);
            accept(node, props);
            visitAddheaderAction(node, VisitPhase.end, props, headerName, headerValue, last);
        } else {
            throw ServiceException.PARSE_ERROR("Invalid addheader action: Missing headerName or headerValue", null);
        }
    }

    private void parseDeleteheader(Node node, RuleProperties props) throws ServiceException {
        Boolean last = null;
        Integer offset = null;
        String matchType = null;
        Boolean countComparision = null;
        Boolean valueComparision = null;
        String relationalComparator = null;
        String comparator = null;
        String headerName = null;
        List<String> headerValue = null;
        int argCount = getNode(node, 0).jjtGetNumChildren();
        int i;
        for(i = 0; i < argCount; i++) {
            boolean isTag = getNode(node, 0, i) == null ? false : getNode(node, 0, i).jjtGetNumChildren() == 0 ? true : false;
            if (isTag) {
                String tag = getValue(node, 0, i);
                if (!StringUtil.isNullOrEmpty(tag)) {
                    switch(tag) {
                        case HeaderConstants.INDEX:
                            i++;
                            offset = visitIndex(node, tag, i);
                            break;
                        case HeaderConstants.LAST:
                            last = true;
                            break;
                        case HeaderConstants.COUNT:
                            if (valueComparision != null && valueComparision) {
                                throw ServiceException.PARSE_ERROR(":count and :value, both can not be received with deleteheader", null);
                            }
                            countComparision = true;
                            i++;
                            relationalComparator = visitRelationalComparator(node, tag, i);
                            break;
                        case HeaderConstants.VALUE:
                            if (countComparision != null && countComparision) {
                                throw ServiceException.PARSE_ERROR(":count and :value, both can not be received with deleteheader", null);
                            }
                            valueComparision = true;
                            i++;
                            relationalComparator = visitRelationalComparator(node, tag, i);
                            break;
                        case ComparatorTags.COMPARATOR_TAG:
                            i++;
                            comparator = visitComparator(node, tag, i);
                            break;
                        case MatchTypeTags.CONTAINS_TAG:
                        case MatchTypeTags.IS_TAG:
                        case MatchTypeTags.MATCHES_TAG:
                            if (matchType != null) {
                                throw ServiceException.PARSE_ERROR("Multiple matchTypes received : \":" + matchType + "\" and \"" + tag + "\"", null);
                            }
                            matchType = tag.substring(1);// trim preceding ":"
                            break;
                        default:
                            throw ServiceException.PARSE_ERROR("Invalid tag \"" + tag + "\" received with deleteheader", null);
                    }
                } else {
                    throw ServiceException.PARSE_ERROR("Invalid argument :" + tag + " received with deleteheader", null);
                }
            } else {
                if (i < argCount) {
                    headerName = getValue(node, 0, i, 0, 0);
                    i++;
                }
                if (i < argCount) {
                    headerValue = getMultiValue(node, 0, i, 0);
                }
            }
        }
        visitDeleteheaderAction(node, VisitPhase.begin, props, last, offset, matchType, countComparision, valueComparision,
                relationalComparator, comparator, headerName, headerValue);
        accept(node, props);
        visitDeleteheaderAction(node, VisitPhase.end, props, last, offset, matchType, countComparision, valueComparision,
                relationalComparator, comparator, headerName, headerValue);
    }

    private void parseReplaceheader(Node node, RuleProperties props) throws ServiceException {
        Boolean last = null;
        Integer offset = null;
        String newName = null;
        String newValue = null;
        String matchType = null;
        Boolean countComparision = null;
        Boolean valueComparision = null;
        String relationalComparator = null;
        String comparator = null;
        String headerName = null;
        List<String> headerValue = null;
        int argCount = getNode(node, 0).jjtGetNumChildren();
        int i;
        for(i = 0; i < argCount; i++) {
            boolean isTag = getNode(node, 0, i) == null ? false : getNode(node, 0, i).jjtGetNumChildren() == 0 ? true : false;
            if (isTag) {
                String tag = getValue(node, 0, i);
                if (!StringUtil.isNullOrEmpty(tag)) {
                    switch(tag) {
                        case HeaderConstants.INDEX:
                            i++;
                            offset = visitIndex(node, tag, i);
                            break;
                        case HeaderConstants.LAST:
                            last = true;
                            break;
                        case HeaderConstants.NEW_NAME:
                            i++;
                            newName = getValue(node, 0, i, 0, 0);
                            if (StringUtil.isNullOrEmpty(newName)) {
                                throw ServiceException.PARSE_ERROR("No value received with \"" + tag + "\" in replaceheader", null);
                            }
                            break;
                        case HeaderConstants.NEW_VALUE:
                            i++;
                            newValue = getValue(node, 0, i, 0, 0);
                            if (StringUtil.isNullOrEmpty(newValue)) {
                                throw ServiceException.PARSE_ERROR("No value received with \"" + tag + "\" in replaceheader", null);
                            }
                            break;
                        case HeaderConstants.COUNT:
                            if (valueComparision != null && valueComparision) {
                                throw ServiceException.PARSE_ERROR(":count and :value, both can not be received with replaceheader", null);
                            }
                            countComparision = true;
                            i++;
                            relationalComparator = visitRelationalComparator(node, tag, i);
                            break;
                        case HeaderConstants.VALUE:
                            if (countComparision != null && countComparision) {
                                throw ServiceException.PARSE_ERROR(":count and :value, both can not be received with replaceheader", null);
                            }
                            valueComparision = true;
                            i++;
                            relationalComparator = visitRelationalComparator(node, tag, i);
                            break;
                        case ComparatorTags.COMPARATOR_TAG:
                            i++;
                            comparator = visitComparator(node, tag, i);
                            break;
                        case MatchTypeTags.CONTAINS_TAG:
                        case MatchTypeTags.IS_TAG:
                        case MatchTypeTags.MATCHES_TAG:
                            if (matchType != null) {
                                throw ServiceException.PARSE_ERROR("Multiple matchTypes received : \":" + matchType + "\" and \"" + tag + "\"", null);
                            }
                            matchType = tag.substring(1);// trim preceding ":"
                            break;
                        default:
                            throw ServiceException.PARSE_ERROR("Invalid tag \"" + tag + "\" received with replaceheader", null);
                    }
                } else {
                    throw ServiceException.PARSE_ERROR("Invalid argument :" + tag + " received with replaceheader", null);
                }
            } else {
                if (i < argCount) {
                    headerName = getValue(node, 0, i, 0, 0);
                    i++;
                }
                if (i < argCount) {
                    headerValue = getMultiValue(node, 0, i, 0);
                }
            }
        }
        visitReplaceheaderAction(node, VisitPhase.begin, props, last, offset, newName, newValue, matchType, countComparision, valueComparision,
                relationalComparator, comparator, headerName, headerValue);
        accept(node, props);
        visitReplaceheaderAction(node, VisitPhase.end, props, last, offset, newName, newValue, matchType, countComparision, valueComparision,
                relationalComparator, comparator, headerName, headerValue);
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
        int numChildren = child.jjtGetNumChildren();
        if (numChildren > 0) {
            for (int i = 0; i < numChildren; i++) {
                Object value = ((SieveNode) child.jjtGetChild(i)).getValue();
                values.add(value == null ? null : value.toString());
            }
        } else {
            values.add(getValue(parent, indexes).toString());
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

    /**
     * Returns <tt>true</tt> if the 'notify' action is formatted in RFC compliant.
     * Otherwise (if the 'notify' action is formatted in Zimbra-specific format),
     * returns <tt>false</tt>
     *　<pre>
     * RFC 5435 and RFC 5436 'notify' action
     * Usage:  notify [":from" string]
     *         [":importance" &lt;"1" / "2" / "3"&gt;]
     *         [":options" string-list]
     *         [":message" string]
     *         &lt;method:  string&gt;
     *   method := mailtoURL
     *   mailtoURL  =  "mailto:" [ to ] [ headers ]
     *   to         =  #mailbox
     *   headers    =  "?" header *( "&" header )
     *   header     =  hname "=" hvalue
     *
     * Zimbra 'notify' action
     * Usage: notify &lt;email-address: string&gt;
     *        [&lt;subject-template: string&gt;]
     *        [&lt;body-template: string&gt;]
     *        [&lt;maxBodyBytes: int&gt;]
     *        [&lt;origHeaders: string-list&gt;]
     * </pre>
     * @param node node object contains 'notify' command and parameters
     * @return
     * @throws ServiceException
     */
    private boolean isRFCCompliantNotifyAction(Node node) throws ServiceException {
        int numArgs = getNode(node, 0).jjtGetNumChildren();
        if (numArgs == 1) {
            String value = getValue(node, 0, 0, 0, 0);
            if (null != value && value.toLowerCase().startsWith(NotifyMailto.NOTIFY_METHOD_MAILTO)) {
                return true;
            } else {
                return false;
            }
        } else {
            // If the first parameter is tag (:xxx), the children object of
            // the (current) node is null
            return getNode(node, 0, 0).jjtGetNumChildren() == 0 ? true : false;
        }
    }

    private void validateCountComparator(boolean isCount, Sieve.Comparator comparator) throws ServiceException {
        if (isCount && comparator != null && !Sieve.Comparator.iasciinumeric.equals(comparator)) {
            throw ServiceException.PARSE_ERROR("Invalid Comparator For Count: " + comparator.toString(), null);
        }
    }
}

