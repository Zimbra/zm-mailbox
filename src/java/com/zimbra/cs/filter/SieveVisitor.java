/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.jsieve.parser.SieveNode;
import org.apache.jsieve.parser.generated.ASTcommand;
import org.apache.jsieve.parser.generated.ASTtest;
import org.apache.jsieve.parser.generated.Node;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.filter.FilterUtil.Condition;
import com.zimbra.cs.filter.FilterUtil.DateComparison;
import com.zimbra.cs.filter.FilterUtil.Flag;
import com.zimbra.cs.filter.FilterUtil.NumberComparison;
import com.zimbra.cs.filter.FilterUtil.StringComparison;

/**
 * Iterates a Sieve node tree and calls callbacks at various
 * points.  A subclass can override whichever <tt>visitXXX()</tt>
 * callbacks it is interested in.
 */
public abstract class SieveVisitor {

    protected enum VisitPhase { begin, end }
    
    @SuppressWarnings("unused")
    protected void visitNode(Node node, VisitPhase phase, RuleProperties props)
    throws ServiceException { }
    
    @SuppressWarnings("unused")
    protected void visitRule(Node node, VisitPhase phase, RuleProperties props)
    throws ServiceException { }
    
    @SuppressWarnings("unused")
    protected void visitTest(Node node, VisitPhase phase, RuleProperties props)
    throws ServiceException { }
    
    @SuppressWarnings("unused")
    protected void visitAction(Node node, VisitPhase phase, RuleProperties props)
    throws ServiceException { }
    
    @SuppressWarnings("unused")
    protected void visitHeaderTest(Node node, VisitPhase phase, RuleProperties props,
        String header, StringComparison comparison, String value)
    throws ServiceException { }
    
    @SuppressWarnings("unused")
    protected void visitAttachmentHeaderTest(Node node, VisitPhase phase, RuleProperties props,
        String header, StringComparison comparison, String value)
    throws ServiceException { }
    
    @SuppressWarnings("unused")
    protected void visitHeaderExistsTest(Node node, VisitPhase phase, RuleProperties props, String header)
    throws ServiceException { }
    
    @SuppressWarnings("unused")
    protected void visitSizeTest(Node node, VisitPhase phase, RuleProperties props,
        NumberComparison comparison, int size, String sizeString)
    throws ServiceException { }
    
    @SuppressWarnings("unused")
    protected void visitDateTest(Node node, VisitPhase phase, RuleProperties props,
        DateComparison comparison, Date date)
    throws ServiceException { }
    
    @SuppressWarnings("unused")
    protected void visitAddressBookTest(Node node, VisitPhase phase, RuleProperties props,
        String header, String folderPath)
    throws ServiceException { }
    
    @SuppressWarnings("unused")
    protected void visitBodyTest(Node node, VisitPhase phase, RuleProperties props, String value)
    throws ServiceException { }
    
    @SuppressWarnings("unused")
    protected void visitAttachmentTest(Node node, VisitPhase phase, RuleProperties props)
    throws ServiceException { }
    
    @SuppressWarnings("unused")
    protected void visitInviteTest(Node node, VisitPhase phase, RuleProperties props, List<String> methods)
    throws ServiceException { }
    
    @SuppressWarnings("unused")
    protected void visitKeepAction(Node node, VisitPhase phase, RuleProperties props)
    throws ServiceException { }
    
    @SuppressWarnings("unused")
    protected void visitDiscardAction(Node node, VisitPhase phase, RuleProperties props)
    throws ServiceException { }

    @SuppressWarnings("unused")
    protected void visitFileIntoAction(Node node, VisitPhase phase, RuleProperties props, String folderPath)
    throws ServiceException { }
    
    @SuppressWarnings("unused")
    protected void visitFlagAction(Node node, VisitPhase phase, RuleProperties props, Flag flag)
    throws ServiceException { }
    
    @SuppressWarnings("unused")
    protected void visitTagAction(Node node, VisitPhase phase, RuleProperties props, String tagName)
    throws ServiceException { }

    @SuppressWarnings("unused")
    protected void visitRedirectAction(Node node, VisitPhase phase, RuleProperties props, String address)
    throws ServiceException { }
    
    @SuppressWarnings("unused")
    protected void visitStopAction(Node node, VisitPhase phase, RuleProperties props)
    throws ServiceException { }
    
    private static final Set<String> RULE_NODE_NAMES;
    
    static {
        Set<String> names = new HashSet<String>();
        names.add("if");
        names.add("disabled_if");
        RULE_NODE_NAMES = Collections.unmodifiableSet(names);
    }
    
    public class RuleProperties {
        boolean isEnabled = true;
        boolean isNegativeTest = false;
        Condition condition = Condition.allof;
        Node testNode;
    }
    
    public void accept(Node node)
    throws ServiceException {
        accept(node, null);
    }
    
    private void accept(Node parent, RuleProperties props)
    throws ServiceException {
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
    
    private void acceptTest(Node node, RuleProperties props)
    throws ServiceException {
        visitTest(node, VisitPhase.begin, props);
        String nodeName = getNodeName(node);
        
        if ("not".equalsIgnoreCase(nodeName)) {
            props.isNegativeTest = true;
            accept(node, props);
        } else {
            if ("allof".equalsIgnoreCase(nodeName)) {
                props.condition = Condition.allof;
                visitRule(node, VisitPhase.begin, props);
                accept(node, props);
                visitRule(node, VisitPhase.end, props);
            } else if ("anyof".equalsIgnoreCase(nodeName)) {
                props.condition = Condition.anyof;
                visitRule(node, VisitPhase.begin, props);
                accept(node, props);
                visitRule(node, VisitPhase.end, props);
            } else if ("header".equalsIgnoreCase(nodeName)) {
                String s = stripLeadingColon(getValue(node, 0, 0));
                StringComparison comparison = StringComparison.fromString(s);
                String header = getValue(node, 0, 1, 0, 0);
                String value = getValue(node, 0, 2, 0, 0);

                visitHeaderTest(node, VisitPhase.begin, props, header, comparison, value);
                accept(node, props);
                visitHeaderTest(node, VisitPhase.end, props, header, comparison, value);
            } else if ("attachment_header".equalsIgnoreCase(nodeName)) {
                String s = stripLeadingColon(getValue(node, 0, 0));
                StringComparison comparison = StringComparison.fromString(s);
                String header = getValue(node, 0, 1, 0, 0);
                String value = getValue(node, 0, 2, 0, 0);

                visitAttachmentHeaderTest(node, VisitPhase.begin, props, header, comparison, value);
                accept(node, props);
                visitAttachmentHeaderTest(node, VisitPhase.end, props, header, comparison, value);
            } else if ("exists".equalsIgnoreCase(nodeName)) {
                String header = getValue(node, 0, 0, 0, 0);

                visitHeaderExistsTest(node, VisitPhase.begin, props, header);
                accept(node, props);
                visitHeaderExistsTest(node, VisitPhase.end, props, header);
            } else if ("size".equalsIgnoreCase(nodeName)) {
                String s = stripLeadingColon(getValue(node, 0, 0));
                NumberComparison comparison = NumberComparison.fromString(s);
                SieveNode sizeNode = (SieveNode) getNode(node, 0, 1);
                String sizeString = sizeNode.getFirstToken().toString();
                int size = 0;
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
                DateComparison comparison = DateComparison.fromString(s);
                String dateString = getValue(node, 0, 1, 0, 0);
                Date date = FilterUtil.SIEVE_DATE_PARSER.parse(dateString);
                if (date == null) {
                    throw ServiceException.PARSE_ERROR("Invalid date value: " + dateString, null);
                }

                visitDateTest(node, VisitPhase.begin, props, comparison, date);
                accept(node, props);
                visitDateTest(node, VisitPhase.end, props, comparison, date);
            } else if ("body".equalsIgnoreCase(nodeName)) {
                String value = getValue(node, 0, 1, 0, 0);

                visitBodyTest(node, VisitPhase.begin, props, value);
                accept(node, props);
                visitBodyTest(node, VisitPhase.end, props, value);
            } else if ("attachment".equalsIgnoreCase(nodeName)) {
                visitAttachmentTest(node, VisitPhase.begin, props);
                accept(node, props);
                visitAttachmentTest(node, VisitPhase.end, props);
            } else if ("addressbook".equalsIgnoreCase(nodeName)) {
                String header = getValue(node, 0, 1, 0, 0);
                String folderPath = getValue(node, 0, 2, 0, 0);
                visitAddressBookTest(node, VisitPhase.begin, props, header, folderPath);
                accept(node, props);
                visitAddressBookTest(node, VisitPhase.end, props, header, folderPath);
            } else if ("invite".equalsIgnoreCase(nodeName)) {
                List<String> methods = Collections.emptyList();
                if (getNode(node, 0).jjtGetNumChildren() > 0) {
                    // Arguments node has children.
                    methods = getMultiValue(node, 0, 1, 0);
                }
                visitInviteTest(node, VisitPhase.begin, props, methods);
                accept(node, props);
                visitInviteTest(node, VisitPhase.end, props, methods);
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
    
    private void acceptAction(Node node, RuleProperties props)
    throws ServiceException {
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
            Flag flag = Flag.fromString(s);
            
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

    
    protected Node getNode(Node parent, int ... indexes)
    throws ServiceException {
        Node node = parent;
        for (int i = 0; i < indexes.length; i++) {
            if (node.jjtGetNumChildren() == 0) {
                throw ServiceException.PARSE_ERROR(
                    "Subnode " + getNodeName(node) + " of node " + getNodeName(parent) + " has no children.", null);
            }
            
            if (indexes[i] >= node.jjtGetNumChildren()) {
                throw ServiceException.PARSE_ERROR(
                    "Subnode " + getNodeName(node) + " of node " + getNodeName(parent) + " has " +
                    node.jjtGetNumChildren() + " children.  Requested child " + indexes[i] + ".", null);
            }
            node = node.jjtGetChild(indexes[i]);
        }
        return node;
    }
    
    private String getValue(Node parent, int ... indexes)
    throws ServiceException {
        Node child = getNode(parent, indexes);
        Object value = ((SieveNode) child).getValue();
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            value = FilterUtil.unescape(stripQuotes((String) value));
        }
        return value.toString();
    }
    
    private List<String> getMultiValue(Node parent, int ... indexes)
    throws ServiceException {
        Node child = getNode(parent, indexes);
        if (child.jjtGetNumChildren() == 0) {
            throw ServiceException.PARSE_ERROR("Subnode has no children", null);
        }
        List<String> values = new ArrayList<String>();
        for (int i = 0; i < child.jjtGetNumChildren(); i++) {
            Object value = ((SieveNode) child.jjtGetChild(i)).getValue();
            if (value instanceof String) {
                value = FilterUtil.unescape(stripQuotes((String) value));
            }
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
    
    private String stripQuotes(String s) {
        if (s == null || s.length() == 0) {
            return s;
        }
        int idxStart = (s.charAt(0) == '"' ? 1 : 0);
        int idxEnd = (s.charAt(s.length() - 1) == '"' ? s.length() - 1 : s.length());
        return s.substring(idxStart, idxEnd);
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

