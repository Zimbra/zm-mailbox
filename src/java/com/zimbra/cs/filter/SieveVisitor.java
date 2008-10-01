package com.zimbra.cs.filter;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.jsieve.parser.SieveNode;
import org.apache.jsieve.parser.generated.ASTcommand;
import org.apache.jsieve.parser.generated.ASTtest;
import org.apache.jsieve.parser.generated.Node;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.DateParser;
import com.zimbra.common.util.ZimbraLog;

/**
 * Walk
 * @author boris
 *
 */
public abstract class SieveVisitor {

    protected enum VisitPhase { begin, end };
    protected enum ConditionType { allof, anyof };
    
    protected enum Flag {
        read, flagged;
        
        protected static Flag fromString(String value)
        throws ServiceException {
            if (value == null) {
                return null;
            }
            try {
                return Flag.valueOf(value);
            } catch (IllegalArgumentException e) {
                throw ServiceException.PARSE_ERROR(
                    "Invalid value: " + value + ", valid values: " + Arrays.asList(Flag.values()), e);
            }
        }
    }
    
    protected enum StringComparison {
        is, contains, matches;
        
        protected static StringComparison fromString(String value)
        throws ServiceException {
            if (value == null) {
                return null;
            }
            try {
                return StringComparison.valueOf(value);
            } catch (IllegalArgumentException e) {
                throw ServiceException.PARSE_ERROR(
                    "Invalid value: "+ value +", valid values: " + Arrays.asList(StringComparison.values()), e);
            }
        }
    }
    
    protected enum NumberComparison {
        over, under;
        
        protected static NumberComparison fromString(String value)
        throws ServiceException {
            if (value == null) {
                return null;
            }
            try {
                return NumberComparison.valueOf(value);
            } catch (IllegalArgumentException e) {
                throw ServiceException.PARSE_ERROR(
                    "Invalid value: "+ value +", valid values: " + Arrays.asList(NumberComparison.values()), e);
            }
        }
    };
    
    protected enum DateComparison {
        before, after;
        
        protected static DateComparison fromString(String value)
        throws ServiceException {
            if (value == null) {
                return null;
            }
            try {
                return DateComparison.valueOf(value);
            } catch (IllegalArgumentException e) {
                throw ServiceException.PARSE_ERROR(
                    "Invalid value: "+ value +", valid values: " + Arrays.asList(StringComparison.values()), e);
            }
        }
    };
    
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
    protected void visitHeaderExistsTest(Node node, VisitPhase phase, RuleProperties props, String header)
    throws ServiceException { }
    
    @SuppressWarnings("unused")
    protected void visitSizeTest(Node node, VisitPhase phase, RuleProperties props,
                                 NumberComparison comparison, int size)
    throws ServiceException { }
    
    @SuppressWarnings("unused")
    protected void visitDateTest(Node node, VisitPhase phase, RuleProperties props,
                                 DateComparison comparison, Date date)
    throws ServiceException { }
    
    @SuppressWarnings("unused")
    protected void visitBodyTest(Node node, VisitPhase phase, RuleProperties props, String value)
    throws ServiceException { }
    
    @SuppressWarnings("unused")
    protected void visitAttachmentTest(Node node, VisitPhase phase, RuleProperties props)
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
        ConditionType conditionType = ConditionType.allof;
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
                SieveNode sieveNode = (SieveNode) node;
                if ("disabled_if".equals(sieveNode.getName())) {
                    newProps.isEnabled = false;
                }
                visitRule(node, VisitPhase.begin, newProps);
                accept(node, newProps);
                visitRule(node, VisitPhase.end, newProps);
            } else if (node instanceof ASTtest) {
                String name = ((SieveNode) node).getName();
                if ("anyof".equals(name)) {
                    props.conditionType = ConditionType.anyof;
                    accept(node, props);
                } else if ("allof".equals(name)) {
                    props.conditionType = ConditionType.allof;
                    accept(node, props);
                } else if ("not".equals(name)) {
                    props.isNegativeTest = true;
                    accept(node, props);
                } else {
                    acceptTest(node, props);
                }
            } else if (node instanceof ASTcommand) {
                acceptAction(node, props);
            } else {
                accept(node, props);
            }
        }
        
        visitNode(parent, VisitPhase.end, props);
    }
    
    private static final DateParser SIEVE_DATE_PARSER = new DateParser("yyyyMMdd");
    
    private void acceptTest(Node node, RuleProperties props)
    throws ServiceException {
        visitTest(node, VisitPhase.begin, props);
        String nodeName = ((SieveNode) node).getName();
        
        if ("header".equals(nodeName)) {
            String s = stripLeadingColon(getValue(node, 0, 0));
            StringComparison comparison = StringComparison.fromString(s);
            String header = stripQuotes(getValue(node, 0, 1, 0, 0));
            String value = stripQuotes(getValue(node, 0, 2, 0, 0));
            
            visitHeaderTest(node, VisitPhase.begin, props, header, comparison, value);
            accept(node, props);
            visitHeaderTest(node, VisitPhase.end, props, header, comparison, value);
        } else if ("exists".equals(nodeName)) {
            String header = stripQuotes(getValue(node, 0, 0, 0, 0));
            
            visitHeaderExistsTest(node, VisitPhase.begin, props, header);
            accept(node, props);
            visitHeaderExistsTest(node, VisitPhase.end, props, header);
        } else if ("size".equals(nodeName)) {
            String s = stripLeadingColon(getValue(node, 0, 0));
            NumberComparison comparison = NumberComparison.fromString(s);
            int size = parseInt(getValue(node, 0, 1));
            
            visitSizeTest(node, VisitPhase.begin, props, comparison, size);
            accept(node, props);
            visitSizeTest(node, VisitPhase.end, props, comparison, size);
        } else if ("date".equals(nodeName)) {
            String s = stripLeadingColon(getValue(node, 0, 0));
            DateComparison comparison = DateComparison.fromString(s);
            String dateString = stripQuotes(getValue(node, 0, 1, 0, 0));
            Date date = SIEVE_DATE_PARSER.parse(dateString);
            if (date == null) {
                throw ServiceException.PARSE_ERROR("Invalid date value: " + dateString, null);
            }
            
            visitDateTest(node, VisitPhase.begin, props, comparison, date);
            accept(node, props);
            visitDateTest(node, VisitPhase.end, props, comparison, date);
        } else if ("body".equals(nodeName)) {
            String value = stripQuotes(getValue(node, 0, 1, 0, 0));
            
            visitBodyTest(node, VisitPhase.begin, props, value);
            accept(node, props);
            visitBodyTest(node, VisitPhase.end, props, value);
        } else if ("attachment".equals(nodeName)) {
            visitAttachmentTest(node, VisitPhase.begin, props);
            accept(node, props);
            visitAttachmentTest(node, VisitPhase.end, props);
        } else {
            ZimbraLog.filter.debug("Ignoring unrecognized test type '%s'.", nodeName);
            accept(node, props);
        }

        visitTest(node, VisitPhase.end, props);
    }
    
    private void acceptAction(Node node, RuleProperties props)
    throws ServiceException {
        visitAction(node, VisitPhase.begin, props);
        String nodeName = ((SieveNode) node).getName();

        if ("keep".equals(nodeName)) {
            visitKeepAction(node, VisitPhase.begin, props);
            accept(node, props);
            visitKeepAction(node, VisitPhase.end, props);
        } else if ("discard".equals(nodeName)) {
            visitDiscardAction(node, VisitPhase.begin, props);
            accept(node, props);
            visitDiscardAction(node, VisitPhase.end, props);
        } else if ("fileinto".equals(nodeName)) {
            String folderPath = stripQuotes(getValue(node, 0, 0, 0, 0));
            visitFileIntoAction(node, VisitPhase.begin, props, folderPath);
            accept(node, props);
            visitFileIntoAction(node, VisitPhase.end, props, folderPath);
        } else if ("flag".equals(nodeName)) {
            String s = stripQuotes(getValue(node, 0, 0, 0, 0));
            Flag flag = Flag.fromString(s);
            
            visitFlagAction(node, VisitPhase.begin, props, flag);
            accept(node, props);
            visitFlagAction(node, VisitPhase.end, props, flag);
        } else if ("tag".equals(nodeName)) {
            String tagName = stripQuotes(getValue(node, 0, 0, 0, 0));
            visitTagAction(node, VisitPhase.begin, props, tagName);
            accept(node, props);
            visitTagAction(node, VisitPhase.end, props, tagName);
        } else if ("redirect".equals(nodeName)) {
            String address = stripQuotes(getValue(node, 0, 0, 0, 0));
            visitRedirectAction(node, VisitPhase.begin, props, address);
            accept(node, props);
            visitRedirectAction(node, VisitPhase.end, props, address);
        } else if ("stop".equals(nodeName)) {
            visitStopAction(node, VisitPhase.begin, props);
            accept(node, props);
            visitStopAction(node, VisitPhase.end, props);
        } else {
            accept(node, props);
        }
        
        visitAction(node, VisitPhase.end, props);
    }

    
    private int parseInt(String s)
    throws ServiceException {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw ServiceException.PARSE_ERROR("Invalid integer: " + s, e);
        }
    }

    protected String getValue(Node node, int ... indexes) {
        if (indexes == null || indexes.length == 0) {
            return null;
        }
        for (int i = 0; i < indexes.length; i++) {
            node = node.jjtGetChild(indexes[i]);
            if (node == null) {
                return null;
            }
        }
        Object value = ((SieveNode) node).getValue();
        if (value == null) {
            return null;
        }
        return value.toString();
    }
    
    protected String stripLeadingColon(String s) {
        if (s == null || s.length() == 0 || s.charAt(0) != ':') {
            return s;
        }
        return s.substring(1, s.length());
    }
    
    protected String stripQuotes(String s) {
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
        String name = ((SieveNode) node).getName();
        return RULE_NODE_NAMES.contains(name);
    }

    /*
    private Node findNode(Node root, Class<? extends Node> nodeClass, Set<String> matchingNames) {
        int numChildren = root.jjtGetNumChildren();
        for (int i = 0; i < numChildren; i++) {
            Node child = root.jjtGetChild(i);
            if (nodeClass.isAssignableFrom(child.getClass())) {
                boolean nameMatches;
                if (matchingNames == null) {
                    nameMatches = true;
                } else {
                    String name = ((SieveNode) child).getName();
                    nameMatches = (matchingNames.contains(name));
                }
                if (nameMatches) {
                    return child;
                } else {
                    return findNode(child, nodeClass, matchingNames);
                }
            }
        }
        return null;
    }
    */
    
    /*
    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT = new ThreadLocal<SimpleDateFormat>();
    private static final String DATE_PATTERN = "yyyyMMdd";
    
    private Date parseDate(String s)
    throws ParseException {
        if (s == null) {
            throw new ParseException("date cannot be null", 0);
        }
        SimpleDateFormat format = DATE_FORMAT.get();
        if (format == null) {
            format = new SimpleDateFormat(DATE_PATTERN);
            DATE_FORMAT.set(format);
        }
        Date d = format.parse(s, new ParsePosition(0));
        if (d == null) {
            throw new ParseException("Invalid date: " + s, 0);
        }
        return d;
    }
    */
}
