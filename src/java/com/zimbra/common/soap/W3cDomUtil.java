/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
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

package com.zimbra.common.soap;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element.ElementFactory;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;

import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class W3cDomUtil {
    private static final Log LOG = ZimbraLog.misc;

    private W3cDomUtil() {}

    /** Cache one DocumentBuilder per thread to avoid unnecessarily recreating them for every XML parse. */
    private static final ThreadLocal<DocumentBuilder> w3DomBuilderTL = new ThreadLocal<DocumentBuilder>() {
            @Override
            protected javax.xml.parsers.DocumentBuilder initialValue() {
                try {
                    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                    dbf.setNamespaceAware(true);
                    dbf.setIgnoringComments(true);
                    return dbf.newDocumentBuilder();
                } catch (javax.xml.parsers.ParserConfigurationException pce) {
                    ZimbraLog.misc.error("Problem setting up w3c DOM builder", pce);
                    return null;
                }
            }
    };

    public static DocumentBuilder getBuilder() {
        return w3DomBuilderTL.get();
    }

    /** Cache one DocumentBuilder per thread to avoid unnecessarily recreating them for every XML parse. */
    private static final ThreadLocal<Transformer> transformerTL = new ThreadLocal<Transformer>() {
        @Override
        protected Transformer initialValue() {
            try {
                TransformerFactory transformerFactory = TransformerFactory.newInstance();
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

    public static Element parseXML(File file)
    throws ServiceException, FileNotFoundException {
        return parseXML(new FileInputStream(file));
    }

    public static Element parseXML(InputStream is)
    throws XmlParseException {
        return parseXML(is, Element.XMLElement.mFactory);
    }

    /**
     * Use JAXP to parse XML into an {@link Element} tree.
     * This is faster and uses less resources than using dom4j
     */
    public static Element parseXML(InputStream is, ElementFactory factory)
    throws XmlParseException {
        javax.xml.parsers.DocumentBuilder jaxbBuilder = getBuilder();
        jaxbBuilder.reset();
        jaxbBuilder.setErrorHandler(new JAXPErrorHandler());
        Document doc;
        try {
            doc = jaxbBuilder.parse(is);
        } catch (SAXException e) {
            throw XmlParseException.PARSE_ERROR(e.getMessage(), e);
        } catch (IOException e) {
            throw XmlParseException.PARSE_ERROR(e.getMessage(), e);
        }
        return nodeToElement(doc, factory);
    }

    public static Element parseXML(String xml)
    throws XmlParseException {
        return parseXML(xml, Element.XMLElement.mFactory);
    }

    /**
     * Use JAXP to parse XML into an {@link Element} tree.
     * This is faster and uses less resources than using dom4j
     */
    public static Element parseXML(String xml, ElementFactory factory)
    throws XmlParseException {
        javax.xml.parsers.DocumentBuilder jaxbBuilder = getBuilder();
        jaxbBuilder.reset();
        jaxbBuilder.setErrorHandler(new JAXPErrorHandler());
        try {
            org.xml.sax.InputSource inStream = new org.xml.sax.InputSource();
            inStream.setCharacterStream(new java.io.StringReader(xml));
            return nodeToElement(jaxbBuilder.parse(inStream), factory);
        } catch (SAXException e) {
            throw XmlParseException.PARSE_ERROR(e.getMessage(), e);
        } catch (IOException e) {
            throw XmlParseException.PARSE_ERROR(e.getMessage(), e);
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
            if (nodeName.contains(":")) {
                qualifiedName = nodeName;
            } else {
                String prefix = attrNode.getPrefix();
                qualifiedName = prefix == null ? nodeName : String.format("%s:%s", prefix, nodeName);
            }
            if (!(Element.XMLElement.A_NAMESPACE.equals(qualifiedName) || qualifiedName.startsWith(XMLNS_COLON))) {
                elt.addAttribute(qualifiedName, attrNode.getNodeValue());
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
