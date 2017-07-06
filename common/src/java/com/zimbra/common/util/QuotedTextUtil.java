/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015, 2016 Synacor, Inc.
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

package com.zimbra.common.util;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang.StringUtils;
import org.cyberneko.html.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Utility class to removed quoted text from a message
 *
 */
public class QuotedTextUtil {

    // for detecting quoted email msg content
    private static String FORWARDED_MESSAGE = "Forwarded Message";
    private static String BEGIN_FORWARDED_MESSAGE = "Begin forwarded message:";
    private static String ORIG_MSG = "Original Message";
    private static String ORIG_APPT = "Original Appointment";
    private static String FROM = "from:";
    private static String TO = "to:";
    private static String SUBJECT = "subject:";
    private static String DATE = "date:";
    private static String SENT = "sent:";
    private static String CC = "cc:";
    /* used to recognize attribution such as "On Feb 5, 2013, John wrote:" */
    private static String ON = "on";
    private static String WROTE = "wrote";
    private static String CHANGED = "changed";

    private static final String RE_ORIG_EMAIL = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
        + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})";
    /* matches "03/07/2014" or "March 3, 2014" by looking for year 20xx */
    private static final String RE_ORIG_DATE = "\\d+\\s*(\\/|\\-|, )20\\d\\d";
    private static final String RE_ORIG_INTRO = "^(-{2,}|" + ON + "\\s+)";
    //private static final String RE_SCRIPT = "/<script\b[^<]*(?:(?!<\\/script>)<[^<]*)*<\\/script>/gi";
    private static final String RE_ASCII_ART = "/^\\s*\\|.*\\|\\s*$/";

    private static Matcher MATCHER_ORIG_EMAIL = Pattern.compile(RE_ORIG_EMAIL).matcher("");
    private static Matcher MATCHER_ORIG_DATE = Pattern.compile(RE_ORIG_DATE).matcher("");
    private static Matcher MATCHER_ORIG_INTRO = Pattern.compile(RE_ORIG_INTRO).matcher("");
    private static Matcher MATCHER_ASCII_ART = Pattern.compile(RE_ASCII_ART).matcher("");

    // ID for an HR to mark it as ours
    private static String HTML_SEP_ID = "zwchr";
    private static short ELEMENT_NODE = 1;

    /**
     * ignore these html tags while parsing html content
     */
    private Set<String> ignoreNodes = new HashSet<String>(Arrays.asList(new String[] { "#comment",
        "br", "script", "select", "style" }));

    /**
     * Each line of the email message classified into a type based on the regex
     * for that type
     */
    public enum LineType {
        UNKNOWN(""),
        // the two most popular quote characters, > and |
        QUOTED("^[>|].*"),
        // marker for Original or Forwarded message, used by ZCS and others
        SEP_STRONG("^\\s*--+\\s*(" + ORIG_MSG + "|" + FORWARDED_MESSAGE + "|" + ORIG_APPT
            + ")\\s*--+\\s*$", "^" + BEGIN_FORWARDED_MESSAGE + "$"),
        SEP_WEAK(""),
        // marker for separator such as "On Feb 5, 2013, John wrote:"
        WROTE_STRONG("^(On\\s(.{1,500})wrote:)"),
        WROTE_WEAK(""),
        // one of the commonly quoted email headers
        HEADER("^(" + StringUtils.join(Arrays.asList(FROM, TO, SUBJECT, DATE, SENT, CC), "|")
            + ").*"),
        LINE("/^\\s*_{5,}\\s*$/"),
        SIG_SEP("");

        private Matcher[] matcher;

        private LineType(String... regex) {
            matcher = new Matcher[regex.length];
            for (int i = 0; i < regex.length; i++) {
                matcher[i] = Pattern.compile(regex[i]).matcher("");
            }
        }

        /**
         * @return the matcher
         */
        public Matcher[] getMatcher() {
            return matcher;
        }
    }

    /**
     * A Fragment represents a block of lines from the message with a type
     * associated with it
     */
    private class Fragment {

        private LineType type = null;
        private ArrayList<String> block = new ArrayList<String>();

        /**
         * @param type the line type
         * @param block the block of lines that forms the fragment
         */
        public Fragment(LineType type, ArrayList<String> block) {
            this.type = type;
            this.block = block;
        }

        /**
         * @return the type
         */
        public LineType getType() {
            return type;
        }

        /**
         * @return the block
         */
        public ArrayList<String> getBlock() {
            return block;
        }
    }

    /**
     * Identifies the type of the line by matching it against the regexes
     *
     * @param line A line from the message
     * @return the type of the line
     */
    private LineType getLineType(String line) {

        LineType type = LineType.UNKNOWN;

        // see if the line matches any known delimiters or quote patterns
        for (LineType lineType : LineType.values()) {
            boolean matched = false;
            for (Matcher matcher : lineType.getMatcher()) {
                if (matcher.reset(line).matches() || matcher.reset(line.toLowerCase()).matches()) {
                    // line that starts and ends with | is considered ASCII art
                    // (eg a table) rather than quoted
                    if (lineType.name().equals(LineType.QUOTED.name())
                        && MATCHER_ASCII_ART.reset(line).matches()) {
                        continue;
                    }
                    matched = true;
                    // first match wins
                    break;
                }
            }
            if (matched) {
                type = lineType;
                break;
            }
        }

        if (type == LineType.UNKNOWN) {
            /*
             * "so-and-so wrote:" takes a lot of different forms; look for
             * various common parts and assign points to determine confidence
             */
            String[] m = line.split("/(\\w+):$/");
            String verb = m[m.length - 1];

            if (verb != null && !verb.isEmpty()) {
                int points = 0;
                points = points
                    + (verb.contains(WROTE) ? 5 : (verb.contains(CHANGED) ? 2 : 0));
                if (MATCHER_ORIG_EMAIL.reset(line).find()) {
                    points += 4;
                }
                if (MATCHER_ORIG_DATE.reset(line).find()) {
                    points += 3;
                }
                if (MATCHER_ORIG_INTRO.reset(line.toLowerCase()).find()) {
                    points += 1;
                }
                if (points >= 7) {
                    type = LineType.WROTE_STRONG;
                } else if (points >= 5) {
                    type = LineType.WROTE_WEAK;
                }
            }
        }

        return type;
    }

    /**
     * Join the blocks to form a string
     *
     * @param block
     * @return string created from the list of blocks
     */
    private String getTextFromBlock(ArrayList<String> block) {
        if (block == null || block.isEmpty()) {
            return "";
        }
        String originalText = StringUtils.join(block, "\n") + "\n";
        originalText.replaceAll("/\\s+$/", "\n");
        return originalText.trim().isEmpty() ? null : originalText;
    }

    /**
     * @param count
     * @param results
     * @param b
     * @return
     */
    private String checkInlineWrote(Map<LineType, Integer> count, ArrayList<Fragment> results,
        boolean b) {
        if (getCount(count, LineType.WROTE_STRONG) > 0) {
            ArrayList<String> unknownBlock = null;
            boolean foundSep = false;
            Map<LineType, Boolean> afterSep = new HashMap<QuotedTextUtil.LineType, Boolean>();

            for (Fragment resFragment : results) {
                LineType type = resFragment.getType();

                if (type == LineType.WROTE_STRONG) {
                    foundSep = true;
                } else if (type == LineType.UNKNOWN && !foundSep) {
                    if (unknownBlock != null) {
                        return null;
                    } else {
                        unknownBlock = resFragment.getBlock();
                    }
                } else if (foundSep) {
                    afterSep.put(type, true);
                }
            }

            boolean hasUnknown =  afterSep.get(LineType.UNKNOWN) == null ? false : afterSep.get(LineType.UNKNOWN);
            boolean hasQuoted =  afterSep.get(LineType.QUOTED) == null ? false : afterSep.get(LineType.QUOTED);
            boolean mixed = hasUnknown && hasQuoted;
            boolean endsWithUnknown = (getCount(count, LineType.UNKNOWN) == 2)
                && (results.get(results.size() - 1).getType() == LineType.UNKNOWN);
            if (afterSep.get(LineType.QUOTED) == null) {
                return "";
            }
            if (unknownBlock != null && (!mixed || endsWithUnknown)) {
                String originalText = getTextFromBlock(unknownBlock);
                if (originalText != null) {
                    return originalText;
                }
            }
        }
        return null;
    }

    /**
     * Utility method to get values/default values from a map
     *
     * @param count
     * @param lineType
     * @return the count for a given line type. Returns zero if the line type
     *         does not exist
     */
    private int getCount(Map<LineType, Integer> count, LineType lineType) {
        return count.containsKey(lineType) ? count.get(lineType) : 0;
    }

    /**
     * Removes quoted content from identified blocks of text by analyzing the
     * content we have classified
     *
     * @param text the message content
     * @param results the list of {@link Fragment}
     * @param unknownBlock list of remaining unknown blocks
     * @param count a map that maintains a count of line types
     * @return String containing only original message and not the quoted content
     */
    private String removeQuotedText(String text, ArrayList<Fragment> results,
        ArrayList<String> unknownBlock, Map<LineType, Integer> count) {
        Fragment firstFragment = null;
        Fragment secondFragment = null;
        if (!results.isEmpty()) {
            firstFragment = results.get(0);
            if (results.size() > 1) {
                secondFragment = results.get(1);
            }
        }

        //empty content followed by HEADER
        if ((firstFragment != null && (firstFragment.getType() == LineType.HEADER || firstFragment
            .getType() == LineType.WROTE_STRONG))) {
            return "";
        }

        // Check for UNKNOWN followed by HEADER
        if ((firstFragment != null && firstFragment.getType() == LineType.UNKNOWN)
            && (secondFragment != null && (secondFragment.getType() == LineType.HEADER || secondFragment
                .getType() == LineType.WROTE_STRONG))) {
            String originalText = getTextFromBlock(firstFragment.getBlock());
            if (originalText != null) {
                return originalText;
            }
        }

        // check for special case of WROTE preceded by UNKNOWN, followed by mix
        // of UNKNOWN and QUOTED (inline reply)
        String originalText = checkInlineWrote(count, results, false);
        if (originalText != null) {
            return originalText;
        }

        // If we found quoted content and there's exactly one UNKNOWN block,
        // return it.
        if (getCount(count, LineType.UNKNOWN) == 1 && getCount(count, LineType.QUOTED) > 0) {
            originalText = getTextFromBlock(unknownBlock);
            if (originalText != null) {
                return originalText;
            }
        }

        // If we have a STRONG separator (eg "--- Original Message ---"),
        // consider it authoritative and return the text that precedes it
        if (getCount(count, LineType.SEP_STRONG) > 0) {
            ArrayList<String> block = new ArrayList<String>();
            for (Fragment resFragment : results) {
                if (resFragment.getType() == LineType.SEP_STRONG) {
                    break;
                }
                block.addAll(resFragment.getBlock());
            }
            originalText = getTextFromBlock(block);
            if (originalText != null) {
                return originalText;
            }
        }

        return text;
    }

    /**
     * Traverse the message content line by line. Try to identify the type of
     * each line. Also try to group subsequent lines of same type into a
     * fragment
     *
     * @param text the text message content
     * @param results a list of fragments
     * @param unknownBlock list to maintain any remaining block of lines
     * @param count a map that maintains a count of each line type
     * @return unknownBlock list to maintain any remaining block of lines
     */
    private ArrayList<String> classifyContent(String text, ArrayList<Fragment> results,
        ArrayList<String> unknownBlock, Map<LineType, Integer> count) {
        ArrayList<String> currentBlock = new ArrayList<String>();

        LineType currentType = null;
        boolean isMerged = false;

        boolean isBugzilla = false;
        text = text.replace("\r\n", "\n");
        String[] lines = text.split("\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            // blank lines are just added to the current block
            if (line.isEmpty()) {
                currentBlock.add(line);
                continue;
            }
            // Bugzilla summary looks like QUOTED; it should be treated as
            // UNKNOWN
            if ((line.indexOf("| DO NOT REPLY") == 0) && (lines[i + 2].indexOf("bugzilla") != -1)) {
                isBugzilla = true;
            }

            LineType type = getLineType(line);
            if (type == LineType.QUOTED) {
                type = isBugzilla ? LineType.UNKNOWN : type;
            } else {
                isBugzilla = false;
            }

            // WROTE can stretch over two lines; if so, join them into one line
            String nextLine = (i + 1) < lines.length ? lines[i + 1] : null;
            if (nextLine != null && type == LineType.UNKNOWN && MATCHER_ORIG_INTRO.reset(line).matches()
                && nextLine.matches("/\\w+:$/")) {
                line = line + " " + nextLine;
                type = getLineType(line);
                isMerged = true;
            }

            // LINE sometimes used as delimiter; if HEADER follows, lump it in
            // with them
            if (type == LineType.LINE) {
                int j = i + 1;
                nextLine = lines[j];
                while (nextLine.isEmpty() && j < lines.length) {
                    nextLine = lines[++j];
                }
                if (nextLine != null) {
                    LineType nextType = getLineType(nextLine);
                    if (nextType == LineType.HEADER) {
                        type = LineType.HEADER;
                    } else {
                        type = LineType.UNKNOWN;
                    }
                }
            }

            // see if we're switching to a new type; if so, package up what we
            // have so far
            if (currentType != null && currentType != type) {
                results.add(new Fragment(currentType, currentBlock));
                unknownBlock = (currentType == LineType.UNKNOWN ? currentBlock : unknownBlock);
                int currentTypeCount = getCount(count, currentType);
                count.put(currentType, currentTypeCount + 1);
                currentBlock = new ArrayList<String>();
                currentType = type;
            } else {
                currentType = type;
            }

            if (isMerged && (type == LineType.WROTE_WEAK || type == LineType.WROTE_STRONG)) {
                currentBlock.add(line);
                currentBlock.add(nextLine);
                i++;
                isMerged = false;
            } else {
                currentBlock.add(line);
            }
        }
        // Handle remaining content
        if (currentBlock.size() > 0) {
            results.add(new Fragment(currentType, currentBlock));
            unknownBlock = (currentType == LineType.UNKNOWN ? currentBlock : unknownBlock);
            int currentTypeCount = getCount(count, currentType);
            count.put(currentType, currentTypeCount + 1);
        }
        return unknownBlock;
    }

    /**
     * Analyze the message and remove quoted text
     *
     * @param text the message
     * @param isHtml True if the message is HTML otherwise false
     * @return the original message
     */
    public String getOriginalContent(String text, boolean isHtml) {
        try {
            if (isHtml) {
                return getOriginalHtmlContent(text);
            }

            return getOriginalTextContent(text);
        } catch (Throwable e) {
            ZimbraLog.soap.warn("Exception in removing quoted text", e);
        }
        return text;
    }

    /**
     * Analyze the text message content, remove quoted text and return the
     * original text
     *
     * @param text the text message
     * @return the original text content
     */
    private String getOriginalTextContent(String text) {
        ArrayList<Fragment> results = new ArrayList<Fragment>();
        ArrayList<String> unknownBlock = new ArrayList<String>();
        Map<LineType, Integer> count = new HashMap<LineType, Integer>();

        unknownBlock = classifyContent(text, results, unknownBlock, count);

        return removeQuotedText(text, results, unknownBlock, count);
    }

    /**
     * Using the DOM structure of the message content, traverse node by node and
     * if we find a node that is recognized as a separator, remove all
     * subsequent elements
     *
     * @param text the message content
     * @return original content if the quoted content was found otherwise the
     *         complete message content
     */
    private String getOriginalHtmlContent(String text) {
        ArrayList<Node> nodeList = new ArrayList<Node>();
        Node previousNode = null, sepNode = null;
        LineType previousType = null;
        boolean done = false;
        DOMParser parser = new DOMParser();
        Document document;
        Node htmlNode = null;

        try {
            parser.parse(new InputSource(new StringReader(text)));
            document = parser.getDocument();
            htmlNode = document.getFirstChild();
            flatten(htmlNode, nodeList);
            for (int i = 0; i < nodeList.size(); i++) {
                Node currentNode = nodeList.get(i);
                if (currentNode.getNodeType() == ELEMENT_NODE) {
                    currentNode.normalize();
                }
                String nodeName = currentNode.getNodeName() != null ? currentNode.getNodeName()
                    : "";
                String nodeValue = currentNode.getNodeValue() != null ? currentNode.getNodeValue()
                    : "";
                LineType type = checkNode(currentNode);

                /*
                 * Check for a multi-element "wrote:" attribution (usually a
                 * combo of #text and A nodes), for example:
                 * 
                 * On Feb 28, 2014, at 3:42 PM, Joe Smith &lt;<a
                 * href="mailto:jsmith@zimbra.com"
                 * target="_blank">jsmith@zimbra.com</a>&gt; wrote:
                 * 
                 * If the current node is a #text with a date or "On ...", find
                 * #text nodes within the next ten nodes, concatenate them, and
                 * check the result.
                 */
                if (type == LineType.UNKNOWN && nodeName.equals("#text")
                    && (MATCHER_ORIG_DATE.reset(nodeValue).matches() || MATCHER_ORIG_INTRO.reset(nodeValue).matches())) {
                    String value = nodeValue;
                    for (int j = 1; j < 10; j++) {
                        Node tempNode = nodeList.get(i + j);
                        if (tempNode != null && tempNode.getNodeName() != null
                            && tempNode.getNodeName().equals("#text")) {
                            value += tempNode.getNodeValue();
                            if ("/:$/".matches(value)) {
                                type = getLineType(value.trim());
                                if (type == LineType.SEP_STRONG) {
                                    i = i + j;
                                    break;
                                }
                            }
                        }

                    }

                }

                if (type != null) {
                    // TODO: confirm if you need to add the nodes in a map and
                    // maintain count as done is javascript
                    // definite separator
                    if (type == LineType.SEP_STRONG || type == LineType.WROTE_STRONG) {
                        sepNode = currentNode;
                        done = true;
                        break;
                    }
                    // some sort of line followed by a header
                    if (type == LineType.HEADER && previousType == LineType.LINE) {
                        sepNode = previousNode;
                        done = true;
                        break;
                    }
                    previousNode = currentNode;
                    previousType = type;

                }
            }

            if (sepNode != null) {
                prune(sepNode, true);
            }

            if (done) {
                String originalText = getHtml(document);
                return (originalText == null || originalText.isEmpty()) ? text : originalText;
            }

        } catch (SAXException | IOException e) {
            ZimbraLog.soap.warn("Exception while removing quoted text from html message", e);
        }

        return text;

    }

    /**
     * Traverse the given node depth-first to produce a list of descendant
     * nodes. Some nodes are ignored.
     *
     * @param node the root node
     * @param nodeList list of nodes
     */
    private void flatten(Node node, ArrayList<Node> nodeList) {
        String nodeName = node != null ? node.getNodeName().toLowerCase() : "";

        if (ignoreNodes.contains(nodeName)) {
            return;
        }

        nodeList.add(node);
        NodeList children = node.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            flatten(children.item(i), nodeList);
        }
    }

    /**
     * Tries to determine the type of the node
     *
     * @param node
     * @return {@link LineType} the type of the node
     */
    private LineType checkNode(Node node) {
        if (node == null) {
            return null;
        }

        String nodeName = node.getNodeName().toLowerCase();
        NamedNodeMap attributes = node.getAttributes();
        String id = (attributes != null && attributes.getNamedItem("id") != null) ? attributes
            .getNamedItem("id").getNodeValue() : "";
        String size = (attributes != null && attributes.getNamedItem("size") != null) ? attributes
            .getNamedItem("size").getNodeValue() : "";
        String width = (attributes != null && attributes.getNamedItem("width") != null) ? attributes
            .getNamedItem("width").getNodeValue() : "";
        String align = (attributes != null && attributes.getNamedItem("align") != null) ? attributes
            .getNamedItem("align").getNodeValue() : "";
        String className = (attributes != null && attributes.getNamedItem("class") != null) ? attributes
            .getNamedItem("class").getNodeValue() : "";
        LineType type = null;

        // Text node: test against our regexes
        if ("#text".equals(nodeName)) {
            String content = node.getNodeValue().trim();
            if (!content.isEmpty()) {
                type = getLineType(content);
            }
        } else if ("hr".equals(nodeName)) {
            // HR: look for a couple different forms that are used to delimit
            // quoted content
            // see if the HR is ours, or one commonly used by other mail clients
            // such as Outlook
            if (HTML_SEP_ID.equals(id)
                || (size.equals("2") && width.equals("100%") && align.equals("center"))) {
                type = LineType.SEP_STRONG;
            } else {
                type = LineType.LINE;
            }
        } else if ("pre".equals(nodeName)) {
            // PRE: treat as one big line of text (should maybe go line by line)
            type = checkNodeContent(node);
        } else if ("div".equals(nodeName)) {
            // DIV: check for Outlook class used as delimiter, or a top border
            // used as a separator, and finally just
            // check the text content
            if (className.equals("OutlookMessageHeader")) {
                type = LineType.SEP_STRONG;
            }
            // TODO: identify separator using style attributes

            if (className.equals("gmail_quote")) {
                type = checkNodeContent(node);
            }
        } else if ("span".equals(nodeName)) {
            // SPAN: check text content
            if (type == null) {
                type = checkNodeContent(node);
            }
        } else if ("img".equals(nodeName)) {
            // IMG: treat as original content
            type = LineType.UNKNOWN;
        } else if ("blockquote".equals(nodeName)) {
            // BLOCKQUOTE: treat as quoted section
            type = LineType.QUOTED;
        }
        return type != null ? type : LineType.UNKNOWN;
    }

    /**
     * Check text content of a node to see if it is a separator
     *
     * @param node
     * @return {@link LineType} the type of the line
     */
    private LineType checkNodeContent(Node node) {
        LineType type = null;
        String content = node.getTextContent() != null ? node.getTextContent() : "";
        if (content.isEmpty() || content.length() > 200) {
            return null;
        }
        type = getLineType(content);
        // We're really only interested in SEP_STRONG and WROTE_STRONG
        return (type == LineType.SEP_STRONG || type == LineType.WROTE_STRONG) ? type : null;
    }

    /**
     * Removes all subsequent siblings of the given node, and then does the same
     * for its parent. The effect is that all nodes that come after the given
     * node in a depth-first traversal of the DOM will be removed.
     *
     * @param node
     * @param clipNode if true, also remove the node
     */
    private void prune(Node node, boolean clipNode) {
        Node tempNode = null;
        if (node != null && node.getParentNode() != null) {
            tempNode = node.getParentNode();

            // clip all subsequent nodes
            while (tempNode.getLastChild() != null && tempNode.getLastChild() != node) {
                tempNode.removeChild(tempNode.getLastChild());
            }
            // clip the node if asked
            if (clipNode && tempNode.getLastChild() != null && tempNode.getLastChild() == node) {
                tempNode.removeChild(tempNode.getLastChild());
            }

            String nodeName = tempNode.getNodeName() != null ? tempNode.getNodeName() : "";

            if (!nodeName.equals("body") && !nodeName.equals("html")) {
                prune(tempNode, false);
            }
        }

    }

    /**
     * Convert the DOM document back to HTML string
     *
     * @param document
     * @return String the String representation of the DOM document
     */
    private String getHtml(Document document) {

        try {
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            TransformerFactory factory = makeTransformerFactory();
            Transformer transformer = factory.newTransformer();
            transformer.transform(new DOMSource(document), result);
            return writer.toString();
        } catch (TransformerException e) {
            ZimbraLog.soap.warn("Exception in converting DOM to html", e);
        }
        return null;
    }

    public static TransformerFactory makeTransformerFactory() {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        return transformerFactory;
    }
}
