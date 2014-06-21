/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.google.common.io.Closeables;
import com.zimbra.common.util.ZimbraLog;


/**
 * This class adjusts the .xsd files automatically generated from JAXB classes to work better for WSDL.
 * For instance, the adjustments include a workaround for a wsdl.exe code generation issue<br />
 * See http://connect.microsoft.com/VisualStudio/feedback/details/471297
 */
public class XsdCleaner {
    private static final Logger LOG = Logger.getLogger(XsdCleaner.class);

    private static final String ARG_DIR = "--dir";
    private static final String KEEP_BACKUPS = "--keep.backups";

    private static String dir = null;
    private static boolean keepBackups = false;

    static {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.INFO);
        LOG.setLevel(Level.INFO);
    }
    /**
     * Main
     */
    public static void main(String[] args) throws Exception {
        readArguments(args);
        processXsds();
    }
    public static void processXsds() throws IOException {
        File workdir = new File(dir);
        if (!workdir.isDirectory()) {
            throw new IOException(String.format("XsdCleaner:directory '%s' does not exist", dir));
        }
        for (File child : workdir.listFiles()) {
            if (child.isFile() && child.getName().endsWith(".xsd")) {
                File bakFile = new File(child.getParentFile(), child.getName() + ".bak");
                if (bakFile.exists()) {
                    bakFile.delete();
                }
                Document doc;
                try {
                    doc = parseXML(child);
                } catch (FileNotFoundException e1) {
                    continue;  // odd - just ignore this file...
                }
                child.renameTo(bakFile);
                if (doc != null) {
                    processDocument(doc);
                    try {
                        asXML(child, doc, true, false);
                        if (!keepBackups) {
                            bakFile.delete();
                        }
                    } catch (FileNotFoundException | TransformerException e) {
                        LOG.error(String.format(
                                "XsdCleaner:Problem writing XML to file %s restoring original", child.getPath()), e);
                        bakFile.renameTo(child);
                    }
                }
            }
        }
    }

    public static void processDocument(Document doc) {
        Element rootElement = doc.getDocumentElement();
        processNode(rootElement);
    }

    public static void processNode(Node node) {
        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            int childNodeType = child.getNodeType();
            switch (childNodeType) {
                case Node.ELEMENT_NODE:
                    processNode(child);
                    fixForDotNetStandaloneSequenceIssue((Element)child);
                    break;
                default:
            }
        }
    }

    /**
     * Implement workaround for wsdl.exe producing bad code for our WSDL.
     * Suggestion from:
     *     http://social.msdn.microsoft.com/Forums/en-US/e33305c3-b5f6-4922-8a3f-df202088d25a/unable-to-generate-temporary-classes-with-biztalk2006-published-webservices?forum=asmxandxml
     * was:
     * You would need to slightly modify all schema constructs that have the following:
     *     <xs:sequence maxOccurs="unbounded">
     *         <xs:element ../>
     *     <xs:sequence>
     * or
     *     <xs:sequence>
     *         <xs:element maxOccurs="unbounded"/>
     *     <xs:sequence>
     *
     * Have to be changed to
     *
     *     <xs:sequence maxOccurs="unbounded">
     *         <xs:element ../>
     *     <xs:sequence>
     *     <xs:attribute name="tmp" type="xs:string" />
     * and
     *     <xs:sequence>
     *         <xs:element maxOccurs="unbounded"/>
     *     <xs:sequence>
     *     <xs:attribute name="tmp" type="xs:string" />
     */

    public static void fixForDotNetStandaloneSequenceIssue(Element elem) {
        if (isSequenceNode(elem)) {
            return;
        }
        boolean hasProblemSequenceChild = false;
        int numSequenceChildren = 0;
        int numOtherChildren = 0;
        for (Node child = elem.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (Node.ELEMENT_NODE == child.getNodeType()) {
                    if (isSequenceNode(child)) {
                        numSequenceChildren++;
                        if (isProblemSequenceNode(child)) {
                            hasProblemSequenceChild = true;
                        }
                    } else {
                        numOtherChildren++;
                    }
            }
        }
        if (hasProblemSequenceChild && (numSequenceChildren == 1) && (numOtherChildren == 0)) {
            Element newChild = elem.getOwnerDocument().createElementNS(XMLConstants.W3C_XML_SCHEMA_NS_URI, "xs:attribute");
            newChild.setAttribute("name", "unusedCodeGenHelper");
            newChild.setAttribute("type", "xs:string");
            elem.appendChild(newChild);
            elem.normalize();
        }
    }

    private static boolean isProblemSequenceNode(Node node) {
        if (!isSequenceNode(node)) {
            return false;
        }
        NamedNodeMap attrs = node.getAttributes();
        Node maxOccursNode = attrs.getNamedItem("maxOccurs");
        boolean seqHasMaxOccursUnbounded =
                ((maxOccursNode != null) && ("unbounded".equals(maxOccursNode.getNodeValue())));
        boolean elemHasMaxOccursUnbounded = false;
        int numElemChildren = 0;
        int numOtherChildren = 0;
        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (Node.ELEMENT_NODE == child.getNodeType()) {
                if (isElementNode(child)) {
                    numElemChildren++;
                    attrs = child.getAttributes();
                    Node elemMaxOccursNode = attrs.getNamedItem("maxOccurs");
                    if ((elemMaxOccursNode != null) && ("unbounded".equals(elemMaxOccursNode.getNodeValue()))) {
                        elemHasMaxOccursUnbounded = true;
                    }
                } else {
                    numOtherChildren++;
                }
            }
        }
        if ((numElemChildren != 1) || (numOtherChildren != 0)) {
            return false;
        }
        if (seqHasMaxOccursUnbounded) {
            return true;
        }
        if (elemHasMaxOccursUnbounded) {
            return true;
        }
        return false;
    }

    public static javax.xml.parsers.DocumentBuilder getBuilder() {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            dbf.setIgnoringComments(true);
            // Prevent external entity reference attack.
            dbf.setExpandEntityReferences(false);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            // protect against recursive entity expansion DOS attack and perhaps other things
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            try {
                dbf.setAttribute("http://apache.org/xml/features/disallow-doctype-decl", true);
            } catch (IllegalArgumentException iae) {
            }
            return dbf.newDocumentBuilder();
        } catch (javax.xml.parsers.ParserConfigurationException pce) {
            return null;
        }
    }

    private static boolean isElementNode(Node node) {
        return (XMLConstants.W3C_XML_SCHEMA_NS_URI.equals(node.getNamespaceURI())) &&
                "element".equals(node.getLocalName());
    }

    private static boolean isSequenceNode(Node node) {
        return (XMLConstants.W3C_XML_SCHEMA_NS_URI.equals(node.getNamespaceURI())) &&
                "sequence".equals(node.getLocalName());
    }

    private static String asPath(Node node) {
        StringBuilder sb = new StringBuilder();
        Node parent = node.getParentNode();
        if (parent != null) {
            sb.append(asPath(parent));
        }
        if (!(node instanceof Document)) {
            sb.append("/").append(node.getNodeName());
        }
        return sb.toString();
    }

    public static Document parseXML(File file) throws FileNotFoundException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            return parseXML(new FileInputStream(file));
        } catch (SAXException | IOException e) {
            LOG.error("XsdCleaner:Problem parsing " + file.getPath(), e);
            return null;
        } finally {
            Closeables.closeQuietly(fis);
        }
    }

    public static Document parseXML(InputStream is)
    throws SAXException, IOException {
        javax.xml.parsers.DocumentBuilder jaxbBuilder = getBuilder();
        jaxbBuilder.reset();
        jaxbBuilder.setErrorHandler(new JAXPErrorHandler());
        return jaxbBuilder.parse(is);
    }

    public static Transformer getTranformer() {
            try {
                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
                return transformer;
            } catch (TransformerFactoryConfigurationError | TransformerException e) {
                LOG.error("XsdCleaner:Problem getting XML Transformer", e);
            }
            return null;
    }

    public static void asXML(File outfile, Node node, boolean indent, boolean omitXmlDecl)
    throws FileNotFoundException, TransformerException {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(outfile);
            asXML(fos, node, indent, omitXmlDecl);
        } finally {
            Closeables.closeQuietly(fos);
        }
    }

    public static void asXML(OutputStream xmlStream, Node node, boolean indent, boolean omitXmlDecl)
            throws TransformerException {
            Source xmlSource = new DOMSource(node);
            StreamResult result = new StreamResult(xmlStream);
            Transformer transformer = getTranformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, omitXmlDecl ? "yes" : "no");
            transformer.setOutputProperty(OutputKeys.INDENT, indent ? "yes" : "no"); //Java XML Indent
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.transform(xmlSource, result);
    }

    // Error handler to report errors and warnings
    public static class JAXPErrorHandler implements ErrorHandler {
        JAXPErrorHandler() {
        }

        /**
         * Returns a string describing parse exception details
         */
        private String getParseExceptionInfo(String category, SAXParseException spe) {
            return String.format("%s: Problem on line %d of document : %s",
                    category, spe.getLineNumber(), spe.getMessage());
        }

        @Override
        public void warning(SAXParseException spe) throws SAXException {
            ZimbraLog.misc.warn(getParseExceptionInfo("Warning", spe));
        }

        @Override
        public void error(SAXParseException spe) throws SAXException {
            throw new SAXException(getParseExceptionInfo("Error", spe));
        }

        @Override
        public void fatalError(SAXParseException spe) throws SAXException {
            throw new SAXException(getParseExceptionInfo("Fatal Error", spe));
        }
    }
    /**
     * Reads the command line arguments.
     */
    private static void readArguments(String[] args) {
        int    argPos = 0;

        while (argPos < args.length) {
            if (args[argPos].equals(ARG_DIR)) {
                dir = args[++argPos];
            }
            if (args[argPos].equals(KEEP_BACKUPS)) {
                keepBackups = true;
            }
            argPos++;
        }
        if (dir == null) {
            throw new RuntimeException(String.format("Missing %s argument", ARG_DIR));
        }
    }
}
