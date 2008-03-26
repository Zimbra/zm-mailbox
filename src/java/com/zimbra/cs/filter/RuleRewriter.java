/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
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
 * Created on Nov 10, 2004
 *
 */
package com.zimbra.cs.filter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.apache.jsieve.parser.SieveNode;
import org.apache.jsieve.parser.generated.ASTargument;
import org.apache.jsieve.parser.generated.ASTcommand;
import org.apache.jsieve.parser.generated.ASTstring;
import org.apache.jsieve.parser.generated.ASTstring_list;
import org.apache.jsieve.parser.generated.ASTtest;
import org.apache.jsieve.parser.generated.Node;

import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.ElementFactory;

/**
 * @author kchen
 *
 * Rewrites a parsed Sieve tree to XML or vice versa.
 */
public class RuleRewriter {
    final static Set<String> MATCH_TYPES = new HashSet<String>();
    static {
        MATCH_TYPES.add(":is");
        MATCH_TYPES.add(":contains");
        MATCH_TYPES.add(":matches");
        MATCH_TYPES.add(":over");
        MATCH_TYPES.add(":under");
        MATCH_TYPES.add(":in");
        MATCH_TYPES.add(":before");
        MATCH_TYPES.add(":after");
    }
    private Stack<String> mStack = new Stack<String>();

    private Element mRoot;
    
    private Mailbox mMailbox;
    
    RuleRewriter() {}
    
    /**
     * Initializes rewriter to convert from Sieve parse tree to an XML DOM tree.
     * 
     * @param factory the <tt>ElementFactory</tt> used to create XML elements
     * @param node the Sieve parse tree root node
     * @see #getElement()
     */
    void initialize(ElementFactory factory, Node node) {
        mRoot = factory.createElement("rules");
        traverse(node);
    }

    /**
     * Initializes rewriter to convert from an XML document to a Sieve script
     * 
     * @param xmlRules
     * @see RuleRewriter#getScript()
     */
    void initialize(Element eltRules, Mailbox mbox) {
        mRoot = eltRules;
        mMailbox = mbox;
    }

    Element getElement() {
        return mRoot;
    }
    
    private void traverse(Node node) {
        int numChildren = node.jjtGetNumChildren();
        for (int i = 0; i < numChildren; i++) {
            Node childNode = node.jjtGetChild(i);
            String name = ((SieveNode) childNode).getName();
            if (childNode instanceof ASTcommand && 
                    ("if".equals(name) || "elsif".equals(name) || "disabled_if".equals(name))) {
                String ruleName = ((SieveNode) childNode).getComment();
                if (ruleName != null) {
                    int hash = ruleName.indexOf("#");
                    ruleName = ruleName.substring(hash+1).trim();
                } else {
                    ruleName = "";
                }
                Element ruleElem = 
                    mRoot.addElement(MailConstants.E_RULE).addAttribute(MailConstants.A_NAME, ruleName);
                ruleElem.addAttribute(MailConstants.A_ACTIVE, !"disabled_if".equals(name));
                rule(ruleElem, childNode);
            } else {
                traverse(childNode);
            }
        }
    }

    private void rule(Element elem, Node parent) {
        
        int numChildren = parent.jjtGetNumChildren();
        for (int i=0; i<numChildren; i++) {
            Node node = parent.jjtGetChild(i);
            String name = ((SieveNode) node).getName();
            if (node instanceof ASTtest) {
                if ("anyof".equals(name) || "allof".equals(name)) {
                    Element condsElem = 
                        elem.addElement(MailConstants.E_CONDITION_GROUP).addAttribute(MailConstants.A_OPERATION, name);
                    rule(condsElem, node);
                } else if ("not".equals(name)){ 
                    mStack.push(name);
                    rule(elem, node);
                } else {
                    if ("exists".equals(name) && !mStack.isEmpty()) {
                        name = mStack.pop() + " " + name;
                    }
                    Element cElem = 
                        elem.addElement(MailConstants.E_CONDITION).addAttribute(MailConstants.A_NAME, name);
                    x = 0;
                    test(cElem, node);
                }
            } else if (node instanceof ASTcommand) {
                Element actionElem = 
                    elem.addElement(MailConstants.E_ACTION).addAttribute(MailConstants.A_NAME, ((SieveNode) node).getName());
                action(actionElem, node);
            } else {
                rule(elem, node);
            }
        }
    }
    
    private void test(Element elem, Node node) {
        int numChildren = node.jjtGetNumChildren();
        for (int i = 0; i < numChildren; i++) {
            Node childNode = node.jjtGetChild(i);
            if (childNode instanceof ASTargument) {
                Object val = ((SieveNode) childNode).getValue();
                if (val != null) {
                    if (MATCH_TYPES.contains(val.toString())) {
                        if (!mStack.isEmpty())
                            val = mStack.pop() + " " + val;
                        elem.addAttribute(MailConstants.A_OPERATION, val.toString());
                    } else {
                        String cname = elem.getAttribute(MailConstants.A_NAME, null);
                        if ("size".equals(cname)) {
                            // special casing size test
                            elem.addAttribute(MailConstants.A_RHS, getSize(val.toString()));
                        } else {
                            elem.addAttribute(MailConstants.A_MODIFIER, val.toString());
                        }
                    }
                }
            } else if (childNode instanceof ASTstring_list) {
                List val = getStringList(childNode);
                String cname = elem.getAttribute(MailConstants.A_NAME, null);
                String param = null;
                if ("date".equals(cname) || "body".equals(cname))
                    param = MailConstants.A_RHS;
                else
                    param = PARAM_PREFIX + String.valueOf(x++);
                elem.addAttribute(param, val.toString());
            } 
            test(elem, childNode);
        }
    }
    
    private static final int K = 1024;
    private static final int M = K * K;
    
    /**
     * @param string
     * @return
     */
    private String getSize(String szStr) {
        int sz = Integer.parseInt(szStr);
        if (sz % M == 0) {
            return (sz / M) + "M";
        } else if (sz % K == 0) {
            return (sz / K) + "K";
        }
        return szStr + "B";
    }

    private static final char PARAM_PREFIX = MailConstants.A_LHS.charAt(0);
    
    private int x = 0;
    
    private List getStringList(Node node) {
        int n = node.jjtGetNumChildren();
        List<Object> a = new ArrayList<Object>(n);
        for (int i=0; i<n; i++ ) {
            Node cn = node.jjtGetChild(i);
            a.add(((SieveNode) cn).getValue());
        }
        return a;
    }

    private void action(Element elem, Node node) {
        int numChildren = node.jjtGetNumChildren();
        for (int i = 0; i < numChildren; i++) {
            Node childNode = node.jjtGetChild(i);
            if (childNode instanceof ASTstring) {
                String val = ((SieveNode) childNode).getValue().toString();
                if (val.startsWith("text:")) {
                    elem.addText(val.substring(5));
                } else {
                    elem.addElement(MailConstants.E_FILTER_ARG).setText(val);
                }
            } else {
                action(elem, childNode);
            }
        }
    }

    /**
     * @return
     * @throws ServiceException
     */
    public String getScript() throws ServiceException {
        StringBuffer sb = new StringBuffer();
        traverse(sb, mRoot);
        String script = sb.toString();
        if (script.trim().length() > 0) {
            // add the require directive only if we have a nontrivial script
            script = "require [\"fileinto\", \"reject\", \"tag\", \"flag\"];\n\n" + script;
        }
        return script;
    }

    /**
     * @throws ServiceException
     */
    private void traverse(StringBuffer sb, Element element) throws ServiceException {
        for (Iterator it = element.elementIterator(); it.hasNext(); ) {
            Element subnode = (Element) it.next();
            String nodeName = subnode.getName();
            if ("r".equals(nodeName)) {
                String ruleName = subnode.getAttribute("name");
                sb.append("# " + ruleName + "\n");
                boolean active = subnode.getAttributeBool(MailConstants.A_ACTIVE, true);
                sb.append(active ? "if " : "disabled_if ");
                condition(sb, subnode, false, ruleName);
            } else {
                traverse(sb, subnode);
            }
        }
    }
    
    /**
     * @throws ServiceException
     */
    private void condition(StringBuffer sb, Element element, boolean group, String ruleName) throws ServiceException {
        boolean actionOpenBrace = false;
        boolean firstConditionInGroup = true;

        for (Iterator it = element.elementIterator(); it.hasNext(); ) {
            Element subnode = (Element) it.next();
            String nodeName = subnode.getName();
            if ("g".equals(nodeName)) {
                if (!firstConditionInGroup) {
                    sb.append(", ");
                } else {
                    firstConditionInGroup = false;
                }
                sb.append(subnode.getAttribute("op")).append(" (");
                condition(sb, subnode, true, ruleName);
                sb.append(")\n");
            } else if ("c".equals(nodeName)) {
                if (group) {
                    if (!firstConditionInGroup)
                        sb.append(",\n ");
                    else
                        firstConditionInGroup = false;
                }
                String testName = subnode.getAttribute("name");
                boolean isSize = "size".equals(testName);
                String op = subnode.getAttribute("op", null);
                if (op != null && op.startsWith("not")) {
                    /*
                     * (if a not :op b) must be changed to (if not a :op b)
                     */
                    testName = "not " + testName;
                    op = op.substring(3).trim();
                }
                sb.append(testName).append(" ");
                if (op != null)
                    sb.append(op).append(" ");
                String k0 = subnode.getAttribute("k0", null);
                checkValue(k0, ruleName);
                if (k0 != null)
                    sb.append("\"").append(k0).append("\"").append(" ");
                String k1 = subnode.getAttribute("k1", null);
                checkValue(k1, ruleName);
                if (k1 != null) {
                    if (!isSize)            // size cannot be quoted
                        sb.append("\"");
                    sb.append(k1);
                    if (!isSize)
                        sb.append("\"");
                }
                sb.append(" ");
            } else if ("action".equals(nodeName)) {
                if (!actionOpenBrace) {
                    sb.append("{\n");
                    actionOpenBrace = true;
                }
                String actionName = subnode.getAttribute("name");
                sb.append("    ").append(actionName);
                action(sb, actionName, subnode, ruleName);
            }
        }
        if (actionOpenBrace)
            sb.append("}\n");
    }
    
    private void checkValue(String k, String ruleName)
    throws ServiceException {
        if (k == null) {
            return;
        }
        if (k.contains("\"")) {
            String msg = String.format(
                "Doublequote not allowed for value '%s' in filter rule %s", k, ruleName);
            throw ServiceException.INVALID_REQUEST(msg, null);
        }
        if (k.contains("\\")) {
            String msg = String.format(
                "Backslash not allowed for value '%s' in filter rule %s", k, ruleName);
            throw ServiceException.INVALID_REQUEST(msg, null);
        }
    }

    void action(StringBuffer sb, String actionName, Element element, String ruleName) throws ServiceException {
        for (Iterator it = element.elementIterator("arg"); it.hasNext(); ) {
            Element subnode = (Element) it.next();
            String argVal = subnode.getText();
            if ("fileinto".equals(actionName)) {
                try {
                    mMailbox.getFolderByPath(null, argVal);
                } catch (MailServiceException.NoSuchItemException e) {
                    try {
                        // create folder
                        mMailbox.createFolder(null, argVal, (byte) 0, MailItem.TYPE_UNKNOWN);
                        ZimbraLog.filter.info("Created folder " + argVal + " referenced in rule \"" + ruleName + "\"");
                    } catch (MailServiceException e1) {
                        throw ServiceException.FAILURE("unable to create inexistent folder (" + argVal + ") in rule \"" + ruleName + "\"", e1);
                    }
                }
            } else if ("tag".equals(actionName)) {
                try {
                    mMailbox.getTagByName(argVal);
                } catch (MailServiceException.NoSuchItemException e) {
                    // create tag
                    mMailbox.createTag(null, argVal, Tag.DEFAULT_COLOR);
                    ZimbraLog.filter.info("Created tag " + argVal + " referenced in rule \"" + ruleName + "\"");
                }
            }
            sb.append(" \"").append(argVal).append("\"");
        }
        sb.append(";\n");
    }
}
