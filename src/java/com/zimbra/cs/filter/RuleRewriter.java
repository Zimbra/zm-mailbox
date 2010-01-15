/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jsieve.parser.SieveNode;
import org.apache.jsieve.parser.generated.ASTargument;
import org.apache.jsieve.parser.generated.ASTcommand;
import org.apache.jsieve.parser.generated.ASTstring;
import org.apache.jsieve.parser.generated.ASTstring_list;
import org.apache.jsieve.parser.generated.ASTtest;
import org.apache.jsieve.parser.generated.Node;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element.ElementFactory;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.zclient.ZFolder;
import com.zimbra.cs.zclient.ZMailbox;

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
    private List<String> mRuleNames;
    
    private Mailbox mMailbox;
    
    RuleRewriter() {}
    
    /**
     * Initializes rewriter to convert from Sieve parse tree to an XML DOM tree.
     * 
     * @param factory the <tt>ElementFactory</tt> used to create XML elements
     * @param node the Sieve parse tree root node
     * @see #getElement()
     */
    void initialize(ElementFactory factory, Node node, List<String> ruleNames) {
        mRoot = factory.createElement("rules");
        mRuleNames = ruleNames;
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
        int nameIndex = 0;
        for (int i = 0; i < numChildren; i++) {
            Node childNode = node.jjtGetChild(i);
            String name = ((SieveNode) childNode).getName();
            if (childNode instanceof ASTcommand && 
                ("if".equals(name) || "elsif".equals(name) || "disabled_if".equals(name))) {
                String ruleName = "";
                if (mRuleNames != null && nameIndex < mRuleNames.size()) {
                    ruleName = mRuleNames.get(nameIndex);
                    nameIndex++;
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
                List<Object> val = getStringList(childNode);
                String cname = elem.getAttribute(MailConstants.A_NAME, null);
                String param = null;
                if ("date".equals(cname) || "body".equals(cname))
                    param = MailConstants.A_RHS;
                else
                    param = PARAM_PREFIX + String.valueOf(x++);
                elem.addAttribute(param, toString(val));
            } 
            test(elem, childNode);
        }
    }
    
    /**
     * Returns the string representation of a value list.  Values are surrounded
     * with quotes, to maintain backward compatibility.  See bug 39911.
     */
    private static String toString(List<Object> list) {
        StringBuilder buf = new StringBuilder();
        buf.append("[");
        if (list != null) {
            boolean isFirst = true;
            
            for (Object val : list) {
                if (isFirst) {
                    isFirst = false;
                } else {
                    buf.append(", ");
                }
                String sVal = String.valueOf(val);
                if (val == null) {
                    buf.append(sVal);
                } else {
                    if (!sVal.startsWith("\"")) {
                        buf.append('"');
                    }
                    buf.append(sVal);
                    if (!sVal.endsWith("\"")) {
                        buf.append('"');
                    }
                }
            }
        }
        buf.append("]");
        return buf.toString();
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
    
    private List<Object> getStringList(Node node) {
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
                    // Put quotes around value to maintain backward compatibility
                    // with ZCS 5.x.  See bug 39911.
                    StringBuilder buf = new StringBuilder();
                    if (!val.startsWith("\"")) {
                        buf.append('"');
                    }
                    buf.append(val);
                    if (!val.endsWith("\"")) {
                        buf.append('"');
                    }
                    
                    elem.addElement(MailConstants.E_FILTER_ARG).setText(buf.toString());
                }
            } else {
                action(elem, childNode);
            }
        }
    }
    
    /**
     * Walks the element tree and removes surrounding quotes and brackets from:
     * <ul>
     *   <li><tt>k0</tt> and <tt>k1</tt> attributes of<tt>&lt;c&gt;</tt> elements</li>
     *   <li>text of <tt>&lt;arg&gt;</tt> elements</li>
     * </ul>
     * This method is used to clean up the rules returned by the old
     * <tt>GetRulesResponse</tt> so that they can be passed to
     * <tt>SaveRulesRequest</tt>.
     */
    public static void sanitizeRules(Element element)
    throws ServiceException {
        if (element == null) {
            return;
        }
        for (Element child : element.listElements()) {
            sanitizeRules(child);
        }
        
        if (element.getName().equals(MailConstants.E_CONDITION)) {
            String k0 = element.getAttribute("k0", null);
            if (k0 != null) {
                element.addAttribute("k0", stripBracketsAndQuotes(k0));
            }
            String k1 = element.getAttribute("k1", null);
            if (k1 != null) {
                element.addAttribute("k1", stripBracketsAndQuotes(k1));
            }
        } else if (element.getName().equals(MailConstants.E_FILTER_ARG)) {
            element.setText(stripBracketsAndQuotes(element.getText()));
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
        for (Iterator<Element> it = element.elementIterator(); it.hasNext(); ) {
            Element subnode = it.next();
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

        for (Iterator<Element> it = element.elementIterator(); it.hasNext(); ) {
            Element subnode = it.next();
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
                
                // Don't allow more than four stars for :matches (bug 35983).
                if (":matches".equals(op) && k1 != null && k1.contains("*****")) {
                    throw ServiceException.INVALID_REQUEST(
                        "Wildcard match value cannot contain more than four asterisks in a row.", null);
                }
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
    
    // ["value"]
    private static final Pattern PAT_BRACKET_QUOTES = Pattern.compile("\\[\"(.*)\"\\]"); 
    // "value"
    private static final Pattern PAT_QUOTES = Pattern.compile("\"(.*)\"");
    
    /**
     * If <tt>k</tt> matches one of the following patterns:
     * <ul>
     *   <li>[&quot;value&quot;]</li>
     *   <li>&quot;value&quot;</li>
     * </ul>
     * strips the surrounding brackets and quotes.  Used to address limitations in the old
     * mail filtering code and bug 42320.
     * 
     * @return the stripped value 
     */
    public static String stripBracketsAndQuotes(String s) {
        if (s != null) {
            // Strip surrounding brackets and quotes.
            Matcher matcher = PAT_BRACKET_QUOTES.matcher(s);
            if (matcher.matches()) {
                s = matcher.group(1);
            } else {
                matcher = PAT_QUOTES.matcher(s);
                if (matcher.matches()) {
                    s = matcher.group(1);
                }
            }
        }
        return s;
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
        for (Iterator<Element> it = element.elementIterator("arg"); it.hasNext(); ) {
            Element subnode = it.next();
            String argVal = subnode.getText();
            if ("fileinto".equals(actionName)) {
                createFolderIfNecessary(argVal, ruleName);
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
    
    private void createFolderIfNecessary(String path, String ruleName)
    throws ServiceException {
        Pair<Folder, String> folderAndRemotePath =
            mMailbox.getFolderByPathLongestMatch(null, Mailbox.ID_FOLDER_USER_ROOT, path);
        Folder folder = folderAndRemotePath.getFirst();
        String remotePath = folderAndRemotePath.getSecond();
        if (StringUtil.isNullOrEmpty(remotePath)) {
            remotePath = null;
        }

        if (folder instanceof Mountpoint && remotePath != null) {
            // Create remote folder path
            Mountpoint mountpoint = (Mountpoint) folder;
            ZimbraLog.filter.info("Creating folder %s in remote folder %s for rule %s.",
                remotePath, folder.getPath(), ruleName);
            ZMailbox remoteMbox = FilterUtil.getRemoteZMailbox(mMailbox, (Mountpoint) folder);
            ItemId id = mountpoint.getTarget();
            ZFolder parent = remoteMbox.getFolderById(id.toString());
            if (parent == null) {
                String msg = String.format("Could not find folder with id %d in remote mailbox %s.",
                    mountpoint.getRemoteId(), mountpoint.getOwnerId());
                throw ServiceException.FAILURE(msg, null);
            }
            String[] pathElements = remotePath.split(ZMailbox.PATH_SEPARATOR);
            for (String folderName : pathElements) {
                if (!StringUtil.isNullOrEmpty(folderName)) {
                    ZFolder currentFolder = parent.getSubFolderByPath(folderName);
                    if (currentFolder != null) {
                        parent = currentFolder;
                    } else {
                        parent = remoteMbox.createFolder(parent.getId(), folderName, ZFolder.View.message, null, null, null);
                    }
                }
            }
        } else if (remotePath != null) {
            // Create local folder path
            ZimbraLog.filter.info("Creating folder %s for rule %s.", path, ruleName);
            mMailbox.createFolder(null, path, (byte) 0, MailItem.TYPE_MESSAGE);
        }
    }
}
