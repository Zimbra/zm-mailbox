/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.common.soap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.dom4j.DocumentFactory;
import org.dom4j.io.DOMReader;
import org.dom4j.io.SAXReader;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;

import com.google.common.base.Strings;
import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element.ElementFactory;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;

public class W3cDomUtil {
    private static final Log LOG = ZimbraLog.misc;

    private W3cDomUtil() {}

    /** Cache one DocumentBuilder per thread to avoid unnecessarily recreating them for every XML parse. */
    private static final ThreadLocal<DocumentBuilder> w3DomBuilderTL = new ThreadLocal<DocumentBuilder>() {
            @Override
            protected javax.xml.parsers.DocumentBuilder initialValue() {
                try {
                    DocumentBuilderFactory dbf = makeDocumentBuilderFactory();
                    return dbf.newDocumentBuilder();
                } catch (javax.xml.parsers.ParserConfigurationException pce) {
                    ZimbraLog.misc.error("Problem setting up w3c DOM builder", pce);
                    return null;
                }
            }
    };

    public static DocumentBuilderFactory makeDocumentBuilderFactory() throws ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setIgnoringComments(true);
        // protect against recursive entity expansion DOS attack and perhaps other things
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        // XXE attack prevention
        dbf.setFeature(Constants.DISALLOW_DOCTYPE_DECL, true);
        dbf.setFeature(Constants.EXTERNAL_PARAMETER_ENTITIES, false);
        dbf.setFeature(Constants.LOAD_EXTERNAL_DTD, false);
        dbf.setXIncludeAware(false);
        dbf.setExpandEntityReferences(false);
        dbf.setFeature(Constants.EXTERNAL_GENERAL_ENTITIES, false);
        return dbf;
    }

    public static DocumentBuilder getBuilder() {
        return w3DomBuilderTL.get();
    }

    public static SAXParser getDom4jSAXParserWhichUsesSecureProcessing() throws XmlParseException {
        SAXParserFactory factory = makeSAXParserFactory();
        try {
            return factory.newSAXParser();
        } catch (ParserConfigurationException | SAXException e) {
            ZimbraLog.misc.error("Problem setting up SAXParser", e);
            throw XmlParseException.PARSE_ERROR();
        }
    };

    public static SAXParserFactory makeSAXParserFactory() throws XmlParseException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setXIncludeAware(false);
        factory.setValidating(false);
        try {
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            // XXE attack prevention
            factory.setFeature(Constants.DISALLOW_DOCTYPE_DECL, true);
            factory.setFeature(Constants.EXTERNAL_GENERAL_ENTITIES, false);
            factory.setFeature(Constants.EXTERNAL_PARAMETER_ENTITIES, false);
            factory.setFeature(Constants.LOAD_EXTERNAL_DTD, false);
        } catch (SAXNotRecognizedException | SAXNotSupportedException | ParserConfigurationException ex) {
            ZimbraLog.misc.error("Problem setting up SAXParser which supports secure XML processing", ex);
            throw XmlParseException.PARSE_ERROR();
        }
        return factory;
    }

    public static SAXReader getDom4jSAXReaderWhichUsesSecureProcessing()
    throws XmlParseException, SAXException {
        return getDom4jSAXReaderWhichUsesSecureProcessing(null);
    }

    public static SAXReader getDom4jSAXReaderWhichUsesSecureProcessing(DocumentFactory fact)
    throws XmlParseException, SAXException {
        SAXReader dom4jSAXReader = new SAXReader(getDom4jSAXParserWhichUsesSecureProcessing().getXMLReader());
        dom4jSAXReader.setFeature(Constants.DISALLOW_DOCTYPE_DECL, true);
        dom4jSAXReader.setFeature(Constants.EXTERNAL_GENERAL_ENTITIES, false);
        dom4jSAXReader.setFeature(Constants.EXTERNAL_PARAMETER_ENTITIES, false);
        if (null != fact) {
            dom4jSAXReader.setDocumentFactory(fact);
        }
        return dom4jSAXReader;
    }

    /** Cache one Transformer per thread to avoid unnecessarily recreating them for every XML parse. */
    private static final ThreadLocal<Transformer> transformerTL = new ThreadLocal<Transformer>() {
        @Override
        protected Transformer initialValue() {
            try {
                TransformerFactory transformerFactory = makeTransformerFactory();
                Transformer transformer = transformerFactory.newTransformer();
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
                return transformer;
            } catch (TransformerFactoryConfigurationError factoryError) {
                LOG.error("Error creating TransformerFactory", factoryError);
            } catch (TransformerException transformerError) {
                LOG.error( "Error creating Transformer", transformerError);
            }
            return null;
        }
    };

    public static TransformerFactory makeTransformerFactory() {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        return transformerFactory;
    }

    /**
     * Return a pretty view of the XML fragment represented by {@code node}
     */
    public static String asXML(Node node) {
        return asXML(node, true, true);
    }

    public static String asXML(Node node, boolean indent) {
        return asXML(node, indent, true);
    }

    public static String asXML(Node node, boolean indent, boolean omitXmlDecl) {
        try {
            Source xmlSource = new DOMSource(node);
            StreamResult result = new StreamResult(new ByteArrayOutputStream());
            transformerTL.get().setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, omitXmlDecl ? "yes" : "no");
            transformerTL.get().setOutputProperty(OutputKeys.INDENT, indent ? "yes" : "no"); //Java XML Indent
            transformerTL.get().setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformerTL.get().transform(xmlSource, result);
            return result.getOutputStream().toString();
        } catch (TransformerException transformerError) {
            LOG.error( "Error transforming node", transformerError);
        }
        return null;
    }

    public static void asXML(OutputStream xmlStream, Node node, boolean indent, boolean omitXmlDecl) {
        try {
            Source xmlSource = new DOMSource(node);
            StreamResult result = new StreamResult(xmlStream);
            transformerTL.get().setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, omitXmlDecl ? "yes" : "no");
            transformerTL.get().setOutputProperty(OutputKeys.INDENT, indent ? "yes" : "no"); //Java XML Indent
            transformerTL.get().setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformerTL.get().transform(xmlSource, result);
        } catch (TransformerException transformerError) {
            LOG.error( "Error transforming node", transformerError);
        }
    }

    /**
     * Use JAXP to parse XML into an {@link Element} tree.
     * Note: DOCTYPE is disallowed for security reasons
     */
    public static Element parseXML(File file)
    throws ServiceException, FileNotFoundException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            return parseXML(new FileInputStream(file));
        } finally {
            Closeables.closeQuietly(fis);
        }
    }

    /**
     * Use JAXP to parse XML into an {@link Element} tree.
     * Note: DOCTYPE is disallowed for security reasons
     */
    public static Element parseXML(InputStream is)
    throws XmlParseException {
        return parseXML(is, Element.XMLElement.mFactory);
    }

    /**
     * Use JAXP to parse XML into an {@link Element} tree.
     * This is faster and uses less resources than using dom4j
     * Note: DOCTYPE is disallowed for security reasons
     */
    public static Element parseXML(InputStream is, ElementFactory factory)
    throws XmlParseException {
        Document doc = parseXMLToDoc(is);
        return nodeToElement(doc, factory);
    }

    /**
     * Use JAXP to parse XML into an {@link Element} tree.
     * Note: DOCTYPE is disallowed for security reasons
     */
    public static Element parseXML(String xml)
    throws XmlParseException {
        return parseXML(xml, Element.XMLElement.mFactory);
    }

    /**
     * Use JAXP to parse XML into an {@link Element} tree.
     * This is faster and uses less resources than using dom4j
     * Note: DOCTYPE is disallowed for security reasons
     */
    public static Element parseXML(String xml, ElementFactory factory)
    throws XmlParseException {
        Document doc = parseXMLToDoc(xml);
        return nodeToElement(doc, factory);
    }

    public static Document parseXMLToDoc(String xml)
    throws XmlParseException {
        javax.xml.parsers.DocumentBuilder jaxbBuilder = getBuilder();
        jaxbBuilder.reset();
        jaxbBuilder.setErrorHandler(new JAXPErrorHandler());
        try {
            org.xml.sax.InputSource inStream = new org.xml.sax.InputSource();
            inStream.setCharacterStream(new java.io.StringReader(xml));
            return jaxbBuilder.parse(inStream);
        } catch (SAXException | IOException e) {
            /* Bug 93816 log actual problem but throw generic one to avoid information disclosure */
            logParseProblem(e);
            throw XmlParseException.PARSE_ERROR();
        }
    }

    public static ByteArrayInputStream removeInvalidXMLChars(byte[] bytes) {
        String xml = new String(bytes);
        Pattern xmlInvalidChars = Pattern.compile(LC.xml_invalid_chars_regex.value());
        xml = xmlInvalidChars.matcher(xml).replaceAll("");
        ByteArrayInputStream is = new ByteArrayInputStream(xml.getBytes());
        return is;
    }

    public static Document parseXMLToDoc(InputStream is)
    throws XmlParseException {
        byte[] bytes = null;
        ByteArrayInputStream bais = null;
        javax.xml.parsers.DocumentBuilder jaxbBuilder = getBuilder();
        jaxbBuilder.reset();
        jaxbBuilder.setErrorHandler(new JAXPErrorHandler());
        try {
            bytes = ByteStreams.toByteArray(is);
            bais = new ByteArrayInputStream(bytes);
            return jaxbBuilder.parse(bais);
        } catch (SAXException | IOException e) {
            if (e.getMessage().contains("invalid XML character")) {
                try {
                    ZimbraLog.backup.debug("SAXException for XML parsing, removing invalid characters and parsing again.", e);
                    bais = removeInvalidXMLChars(bytes);
                    return jaxbBuilder.parse(bais);
                } catch (SAXException | IOException ex) {
                    logParseProblem(ex);
                    throw XmlParseException.PARSE_ERROR();
                }
            } else {
                /* Bug 93816 log actual problem but throw generic one to avoid information disclosure */
                logParseProblem(e);
                throw XmlParseException.PARSE_ERROR();
            }
        } finally {
            Closeables.closeQuietly(bais);
        }
    }

    /**
     * Note: DOCTYPE is disallowed for reasons of security and protection against denial of service
     * @throws XmlParseException
     */
    public static org.dom4j.Document parseXMLToDom4jDocUsingSecureProcessing(InputStream is) throws XmlParseException {
        org.w3c.dom.Document w3cDoc =W3cDomUtil.parseXMLToDoc(is);
        DOMReader reader = new DOMReader();
        return reader.read(w3cDoc);
    }

    private static void logParseProblem(Exception e) {
        if (LOG.isDebugEnabled()) {
            LOG.warn("Problem parsing XML", e);
        } else {
            LOG.warn("Problem parsing XML - %s", e.getMessage());
        }
    }

    /**
     * Test whether we want to treat node as a single Element with text content or as a hierarchy.
     * There are 2 situations where we prefer text content:
     *    1 This is an XHTML node
     *    2 Contains a mixture of text and elements which isn't allowed in an Element hierarchy
     */
    private static boolean needToFlattenElementContent(Node node) {
        if (Element.XMLElement.XHTML_NS_URI.equalsIgnoreCase(node.getNamespaceURI())) {
            return true;
        }
        boolean hasElems = false;
        boolean hasText = false;
        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            int childNodeType = child.getNodeType();
            switch (childNodeType) {
                case Node.ELEMENT_NODE:
                    if (hasText) {
                        return true;
                    }
                    hasElems = true;
                    break;
                case Node.TEXT_NODE:
                    String content = child.getNodeValue();
                    if (content != null && !content.trim().equals("")) {
                        if (hasElems) {
                            return true;
                        }
                        hasText = true;
                    }
                    break;
                case Node.CDATA_SECTION_NODE:
                    CDATASection cdata = (CDATASection)child;
                    String cdataContent = cdata.getData();
                    if (cdataContent != null && !cdataContent.trim().equals("")) {
                        if (hasElems) {
                            return true;
                        }
                        hasText = true;
                    }
                    break;
                default:
            }
        }
        return false;
    }

    private static final String XMLNS_COLON = Element.XMLElement.A_NAMESPACE + ":";

    public static Element nodeToElement(Node node, ElementFactory factory) {
        int nodeType = node.getNodeType();
        switch (nodeType) {
            case Node.DOCUMENT_NODE:
                Document doc = (Document) node;
                return nodeToElement(doc.getDocumentElement(), factory);
            case Node.ELEMENT_NODE:
                if (needToFlattenElementContent(node)) {
                    return toFlattened(node, factory);
                } else {
                    return toHierarchy(node, factory);
                }
            default:
                ZimbraLog.misc.debug("Unexpected nodeType %s in convertW3cDOM", Integer.toString(nodeType));
                return null;
        }
    }

    private static void makeAttributes(Element elt, Node node) {
        NamedNodeMap attrs = node.getAttributes();
        for (int ndx = 0; ndx < attrs.getLength(); ndx++) {
            Node attrNode = attrs.item(ndx);
            String nodeName = attrNode.getNodeName();
            String qualifiedName;
            String prefix = attrNode.getPrefix();
            if (nodeName.contains(":")) {
                qualifiedName = nodeName;
            } else {
                qualifiedName = prefix == null ? nodeName : String.format("%s:%s", prefix, nodeName);
            }
            if (!(Element.XMLElement.A_NAMESPACE.equals(qualifiedName) || qualifiedName.startsWith(XMLNS_COLON))) {
                elt.addAttribute(qualifiedName, attrNode.getNodeValue());
                String nsURI = attrNode.getNamespaceURI();
                if (!Strings.isNullOrEmpty(nsURI)) {
                    /* The approach to namespaces is to ALWAYS store them on elements that use them (for either the
                     * element's name or in one of its attributes names) but ignore them where they are not used.
                     * This means that unused namespace definitions may be dropped - but that shouldn't matter.
                     * It also means that namespaces won't be dropped by mistake from detached elements because the
                     * namespace is only stored in a parent element where it was defined.
                     */
                    elt.setNamespace(prefix, nsURI);
                }
            }
        }
    }

    private static Element toHierarchy(Node node, ElementFactory factory) {
        Element elt = factory.createElement(dom4jQNameForNode(node));
        makeAttributes(elt, node);
        StringBuilder content = new StringBuilder();
        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            switch (child.getNodeType()) {
            case Node.ELEMENT_NODE:
                elt.addElement(nodeToElement(child, factory));
                break;
            case Node.TEXT_NODE:
                content.append(child.getNodeValue());
                break;
            case Node.CDATA_SECTION_NODE:
                CDATASection cdata = (CDATASection)child;
                content.append(cdata.getData());
                break;
            }
        }
        String textContent = content.toString();
        if (!textContent.trim().equals("")) {
            elt.setText(textContent);
        }
        return elt;
    }

    private static Element toFlattened(Node node, ElementFactory factory) {
        Element elt = factory.createElement(dom4jQNameForNode(node));
        makeAttributes(elt, node);
        StringBuilder content = new StringBuilder();
        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            switch (child.getNodeType()) {
            case Node.ELEMENT_NODE:
                content.append(W3cDomUtil.asXML(child, false)); // i.e. add textual representation of the XML
                break;
            case Node.TEXT_NODE:
                content.append(child.getNodeValue());
                break;
            case Node.CDATA_SECTION_NODE:
                CDATASection cdata = (CDATASection)child;
                content.append(cdata.getData());
                break;
            }
        }
        return elt.setText(content.toString());
    }

    private static org.dom4j.QName dom4jQNameForNode(Node node) {
        org.dom4j.Namespace ns = node.getNamespaceURI() == null ? null :
            new org.dom4j.Namespace(node.getPrefix(), node.getNamespaceURI());
        String localName = node.getNodeName();
        if (localName.contains(":")) {
            localName = localName.substring(localName.indexOf(':') + 1);
        }
        return new org.dom4j.QName(localName, ns);
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

}
