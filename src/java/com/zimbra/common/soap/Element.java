/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

/*
 * Created on Mar 16, 2005
 */
package com.zimbra.common.soap;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.dom4j.QName;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;

public abstract class Element implements Cloneable {
    protected String  mName;
    protected String  mPrefix = "";
    protected Element mParent;
    protected Map<String, Object> mAttributes;
    protected Map<String, String> mNamespaces;

    /** Cache one DocumentFactory per thread to avoid unnecessarily recreating
     *  them for every XML parse. */
    private static ThreadLocal<org.dom4j.DocumentFactory> mDocumentFactory = new ThreadLocal<org.dom4j.DocumentFactory>() {
        @Override protected synchronized org.dom4j.DocumentFactory initialValue() {
            return new org.dom4j.DocumentFactory();
        }
    };

    /** These values are used to control how the <tt>Element</tt> serializes
     *  an attribute.  In our model, {@link Element#getAttribute(String)} will
     *  find the attribute <tt>b</tt> with value <tt>"something"</tt> when
     *  presented with either of the following two pieces of XML:<ul>
     *    <li>&lt;a>&lt;b>something&lt;/b>&lt;/a>
     *    <li>&lt;a b="something"/></ul><p>
     *  By setting the dispositon of an attribute to {@link #CONTENT}, you can
     *  force the attribute to be rendered in the former form.<p>
     *  
     *  <i>Note that JSON serialization ignores all such hints and serializes
     *  all attributes as <tt>"key": "value"</tt>.</i> */
    public static enum Disposition {
        ATTRIBUTE, CONTENT
    }

    /** Creates a new <tt>Element</tt> with the given name.  This method should
     *  be used when generating a standalone <tt>Element</tt>.  If you want to
     *  add a child to an existing <tt>Element</tt>, please use
     *  {@link #addElement(String)} instead. */
    public static Element create(SoapProtocol proto, String name) throws ServiceException {
        if (proto == SoapProtocol.SoapJS)
            return new JSONElement(name);
        else if (proto == SoapProtocol.Soap11 || proto == SoapProtocol.Soap12)
            return new XMLElement(name);
        throw ServiceException.INVALID_REQUEST("Unknown SoapProtocol: " + proto, null);
    }

    /** Creates a new <tt>Element</tt> with the given {@link QName}.  This
     *  method should be used when generating a standalone <tt>Element</tt>.
     *  If you want to add a child to an existing <tt>Element</tt>, please
     *  use {@link #addElement(QName) instead. */
    public static Element create(SoapProtocol proto, QName qname) throws ServiceException {
        if (proto == SoapProtocol.SoapJS)
            return new JSONElement(qname);
        else if (proto == SoapProtocol.Soap11 || proto == SoapProtocol.Soap12)
            return new XMLElement(qname);
        throw ServiceException.INVALID_REQUEST("Unknown SoapProtocol: " + proto, null);
    }


    /** Returns the appropriate {@link ElementFactory} for generating
     *  <tt>Element</tt>s of this <tt>Element</tt>'s type. */
    public abstract ElementFactory getFactory();

    /** Creates a new child <tt>Element</tt> with the given name and adds it
     *  to this <tt>Element</tt>. */
    public abstract Element addElement(String name) throws ContainerException;
    /** Creates a new child <tt>Element</tt> with the given QName and adds it
     *  to this <tt>Element</tt>. */
    public abstract Element addElement(QName qname) throws ContainerException;
    /** Adds an existing <tt>Element</tt> to this <tt>Element</tt>. */
    public abstract Element addElement(Element elt) throws ContainerException;

    public Element addUniqueElement(String name) throws ContainerException  { return addElement(name); }
    public Element addUniqueElement(QName qname) throws ContainerException  { return addElement(qname); }
    public Element addUniqueElement(Element elt) throws ContainerException  { return addElement(elt); }

    protected Element setNamespace(String prefix, String uri) {
        if (prefix != null && uri != null && !uri.equals("")) {
            if (mNamespaces == null)  mNamespaces = new HashMap<String, String>();
            mNamespaces.put(prefix, uri);
        }
        return this;
    }

    public abstract Element setText(String content) throws ContainerException;
    public Element addText(String content) throws ContainerException  { return setText(getText() + content); }

    public Element addAttribute(String key, String value) throws ContainerException   { return addAttribute(key, value, Disposition.ATTRIBUTE); }
    public Element addAttribute(String key, long value) throws ContainerException     { return addAttribute(key, value, Disposition.ATTRIBUTE); }
    public Element addAttribute(String key, double value) throws ContainerException   { return addAttribute(key, value, Disposition.ATTRIBUTE); }
    public Element addAttribute(String key, boolean value) throws ContainerException  { return addAttribute(key, value, Disposition.ATTRIBUTE); }
    public Element addAttribute(String key, Number value) throws ContainerException   { return addAttribute(key, value, Disposition.ATTRIBUTE); }
    public Element addAttribute(String key, Boolean value) throws ContainerException   { return addAttribute(key, value, Disposition.ATTRIBUTE); }

    public abstract Element addAttribute(String key, String value, Disposition disp) throws ContainerException;
    public Element addAttribute(String key, long value, Disposition disp) throws ContainerException     { return addAttribute(key, Long.toString(value), disp); }
    public Element addAttribute(String key, double value, Disposition disp) throws ContainerException   { return addAttribute(key, Double.toString(value), disp); }
    public Element addAttribute(String key, boolean value, Disposition disp) throws ContainerException  { return addAttribute(key, value ? "1" : "0", disp); }
    public Element addAttribute(String key, Number value, Disposition disp) throws ContainerException {
        return addAttribute(key, value != null ? value.toString() : null, disp); 
    }
    public Element addAttribute(String key, Boolean value, Disposition disp) throws ContainerException {
        if (value != null) {
            return addAttribute(key, value.booleanValue(), disp);
        }
        return addAttribute(key, (String) null, disp);
    }

    public KeyValuePair addKeyValuePair(String key, String value) throws ContainerException  { return addKeyValuePair(key, value, null, null); }
    public abstract KeyValuePair addKeyValuePair(String key, String value, String eltname, String attrname) throws ContainerException;


    protected void detach(Element child) throws ContainerException {
        if (child == null)
            return;
        if (child.mParent != this)
            throw new ContainerException("wrong parent");
        child.mParent = null;
    }
    public Element detach() throws ContainerException {
        setNamespace(mPrefix, getNamespaceURI(mPrefix));
        if (mParent != null)
            mParent.detach(this);
        return this;
    }

    @Override public abstract Element clone();

    // reading from the element hierarchy
    public String getName()           { return mName; }
    public String getQualifiedName()  { return (mPrefix != null && !mPrefix.equals("") ? mPrefix + ':' + mName : mName); }
    public QName getQName()           { String uri = getNamespaceURI(mPrefix);  return (uri == null ? QName.get(mName) : QName.get(getQualifiedName(), uri)); }

    public static QName getQName(String qualifiedName)  { String[] parts = qualifiedName.split("\\.");  return new QName(parts[parts.length - 1]); }

    public Element getParent()        { return mParent; }

    /** Returns the first child <tt>Element</tt> with the given name.
     * @throws ServiceException if no matching <tt>Element</tt> is found */
    public Element getElement(String name) throws ServiceException  { return checkNull(name, getOptionalElement(name)); }
    /** Returns the first child <tt>Element</tt> with the given QName.
     * @throws ServiceException if no matching <tt>Element</tt> is found */
    public Element getElement(QName qname) throws ServiceException  { return checkNull(qname.getName(), getOptionalElement(qname)); }

    /** Returns the first child <tt>Element</tt> with the given name, or
     *  <tt>null</tt> if no matching <tt>Element</tt> is found. */
    public abstract Element getOptionalElement(String name);
    /** Returns the first child <tt>Element</tt> with the given QName, or
     *  <tt>null</tt> if no matching <tt>Element</tt> is found. */
    public Element getOptionalElement(QName qname)  { return getOptionalElement(qname.getName()); }

    /** Returns all an <tt>Element</tt>'s attributes. */
    public abstract Set<Attribute> listAttributes();
    /** Returns all an <tt>Element</tt>'s sub-elements, or an empty <tt>List</tt>. */
    public List<Element> listElements()  { return listElements(null); }
    /** Returns all the sub-elements with the given name.  If <tt>name></tt>
     *  is <tt>null</tt>, returns <u>all</u> sub-elements.  If no elements
     *  with the given name exist, returns an empty <tt>List</tt> */
    public abstract List<Element> listElements(String name);
    /** Returns whether the element has any sub-elements. */
    public abstract boolean hasChildren();

    public List<KeyValuePair> listKeyValuePairs()  { return listKeyValuePairs(null, null); }
    public abstract List<KeyValuePair> listKeyValuePairs(String eltname, String attrname);

    /** Returns all attributes as an <tt>Iterator</tt>. */
    public Iterator<Attribute> attributeIterator()         { return listAttributes().iterator(); }
    /** Returns all sub-elements as an <tt>Iterator</tt>. */
    public Iterator<Element> elementIterator()             { return listElements().iterator(); }
    /** Returns all sub-elements with the given name as an <tt>Iterator</tt>.
     *  If <tt>name></tt> is <tt>null</tt>, returns <u>all</u> sub-elements. */
    public Iterator<Element> elementIterator(String name)  { return listElements(name).iterator(); }

    public abstract String getText();
    abstract String getRawText();
    public String getTextTrim()  { return getText().trim().replaceAll("\\s+", " "); }

    public String getAttribute(String key) throws ServiceException        { return checkNull(key, getAttribute(key, null)); }
    public long getAttributeLong(String key) throws ServiceException      { return parseLong(key, checkNull(key, getAttribute(key, null))); }
    public double getAttributeDouble(String key) throws ServiceException  { return parseDouble(key, checkNull(key, getAttribute(key, null))); }
    public boolean getAttributeBool(String key) throws ServiceException   { return parseBool(key, checkNull(key, getAttribute(key, null))); }

    public abstract String getAttribute(String key, String defaultValue);
    public long getAttributeLong(String key, long defaultValue) throws ServiceException        { String raw = getAttribute(key, null); return (raw == null ? defaultValue : parseLong(key, raw)); }
    public double getAttributeDouble(String key, double defaultValue) throws ServiceException  { String raw = getAttribute(key, null); return (raw == null ? defaultValue : parseDouble(key, raw)); }
    public boolean getAttributeBool(String key, boolean defaultValue) throws ServiceException  { String raw = getAttribute(key, null); return (raw == null ? defaultValue : parseBool(key, raw)); }

    protected String getNamespaceURI(String prefix) {
        if (mNamespaces != null) {
            Object uri = mNamespaces.get(prefix);
            if (uri != null && !((String) uri).trim().equals(""))
                return (String) uri;
        }
        return (mParent == null ? null : mParent.getNamespaceURI(prefix));
    }
    protected Element collapseNamespace() {
        if (mNamespaces != null && mParent != null && mParent.mPrefix.equals(mPrefix)) {
            String localURI = mNamespaces.get(mPrefix);
            if (localURI != null && localURI.equals(mParent.getNamespaceURI(mPrefix))) {
                mNamespaces.remove(mPrefix);
                if (mNamespaces.isEmpty())
                    mNamespaces = null;
            }
        }
        return this;
    }

    public static String checkNull(String key, String value) throws ServiceException {
        if (value == null)
            throw ServiceException.INVALID_REQUEST("missing required attribute: " + key, null);
        return value;
    }
    private Element checkNull(String key, Element value) throws ServiceException {
        if (value == null)
            throw ServiceException.INVALID_REQUEST("missing required element: " + key, null);
        return value;
    }
    public static long parseLong(String key, String value) throws ServiceException {
        try { return Long.parseLong(value); }
        catch (NumberFormatException nfe) {
            throw ServiceException.INVALID_REQUEST("invalid long value '" + value + "' for attribute: " + key, nfe);
        }
    }
    public static double parseDouble(String key, String value) throws ServiceException {
        try { return Double.parseDouble(value); }
        catch (NumberFormatException nfe) {
            throw ServiceException.INVALID_REQUEST("invalid double value '" + value + "' for attribute: " + key, nfe);
        }
    }
    public static boolean parseBool(String key, String value) throws ServiceException {
        if (value.equals("1") || value.equalsIgnoreCase("true"))        return true;
        else if (value.equals("0") || value.equalsIgnoreCase("false"))  return false;
        throw ServiceException.INVALID_REQUEST("invalid boolean value '" + value + "' for attribute: " + key, null);
    }

    protected boolean namespaceDeclarationNeeded(String prefix, String uri) {
        if (mParent == null || getClass() != mParent.getClass())
            return true;
        String thatURI = mParent.getNamespaceURI(prefix);
        return (thatURI == null || !thatURI.equals(uri));
    }

    // dumping the element hierarchy
    public byte[] toUTF8() {
        try {
            return toString().getBytes("utf-8");
        } catch (UnsupportedEncodingException e) {
            // should *never* happen since UTF-8 and UTF-16 cover the exact same character space
            return null;
        }
    }
    
    public void output(Appendable out) throws IOException {
        toString(out);
    }
    
    public abstract String prettyPrint();

    /** Serialize this <tt>Element</tt> to an <code>Appendable</code>. */
    public abstract void toString(Appendable out) throws IOException;
    
    private static final String FORTY_SPACES = "                                        ";
    protected void indent(Appendable sb, int indent, boolean newline) throws IOException {
        if (indent < 0)      return;
        if (newline)         sb.append('\n');
        while (indent > 40)  { sb.append(FORTY_SPACES);  indent -= 40; }
        if (indent > 0)      sb.append(FORTY_SPACES.substring(40 - indent));
    }

    public org.dom4j.Element toXML() {
        org.dom4j.Document doc = new org.dom4j.tree.DefaultDocument();
        doc.setRootElement(toXML(null));
        return doc.getRootElement();
    }

    private org.dom4j.Element toXML(org.dom4j.Element d4parent) {
        org.dom4j.Element d4elt = (d4parent == null ? org.dom4j.DocumentHelper.createElement(getQName()) : d4parent.addElement(getQName()));
        for (Attribute attr : listAttributes())
            d4elt.addAttribute(attr.getKey(), attr.getValue());
        for (Element elt : listElements())
            elt.toXML(d4elt);
        d4elt.setText(getText());
        return d4elt;
    }

    /** Return the attribute value that is at the specified path, or null if
     *  one could not be found.
     * @param xpath an array of names to traverse in the element tree */
    public String getPathAttribute(String[] xpath) {
        int depth = 0;
        Element cur = this;
        while (depth < xpath.length - 1 && cur != null)
            cur = cur.getOptionalElement(xpath[depth++]);
        return (cur == null ? null : cur.getAttribute(xpath[depth], null));
    }

    /** Return the first Element matching the specified path, or null if none was found.
     * @param xpath an array of names to traverse in the element tree */
    public Element getPathElement(String[] xpath) {
        int depth = 0;
        Element cur = this;
        while (depth < xpath.length && cur != null)
            cur = cur.getOptionalElement(xpath[depth++]);
        return cur;
    }

    /** Return the list of Elements matching the specified path, or null
     *  if none were found.
     * @param xpath an array of names to traverse in the element tree */
    public List<Element> getPathElementList(String[] xpath) {
        int depth = 0;
        Element cur = this;
        while (depth < xpath.length-1 && cur != null)
            cur = cur.getOptionalElement(xpath[depth++]);
        if (cur == null)
            return null;
        return cur.listElements(xpath[xpath.length-1]);
    }

    /** Set the attribute at the specified path, thowing an exception if the
     *  parent Element does not exist.
     * @param xpath an array of names to traverse in the element tree */
    public void setPathAttribute(String[] xpath, String value) throws ServiceException {
        if (xpath == null || xpath.length == 0)
            return;
        int depth = 0;
        Element cur = this;
        while (depth < xpath.length - 1 && cur != null)
            cur = cur.getOptionalElement(xpath[depth++]);
        if (cur == null)
            throw ServiceException.INVALID_REQUEST("could not find path", null);
        cur.addAttribute(xpath[depth], value);
    }


    public static Element parseJSON(InputStream is) throws SoapParseException  { return parseJSON(is, JSONElement.mFactory); }
    public static Element parseJSON(InputStream is, ElementFactory factory) throws SoapParseException {
        try {
            return parseJSON(new String(com.zimbra.common.util.ByteUtil.getContent(is, -1), "utf-8"), factory);
        } catch (SoapParseException e) {
            throw e;
        } catch (Exception e) {
            throw new SoapParseException("could not transcode request from utf-8", null);
        }
    }

    public static Element parseJSON(String js) throws SoapParseException  { return parseJSON(js, JSONElement.mFactory); }
    public static Element parseJSON(String js, ElementFactory factory) throws SoapParseException {
        try {
            return JSONElement.parseElement(new JSONElement.JSRequest(js), SoapProtocol.SoapJS.getEnvelopeQName(), factory);
        } catch (ContainerException ce) {
            SoapParseException spe = new SoapParseException(ce.getMessage(), js);
            spe.initCause(ce);
            throw spe;
        }
    }

    private static final String XHTML_NS_URI = "http://www.w3.org/1999/xhtml";

    public static Element parseXML(InputStream is) throws org.dom4j.DocumentException { return parseXML(is, XMLElement.mFactory); }
    public static Element parseXML(InputStream is, ElementFactory factory) throws org.dom4j.DocumentException {
        return convertDOM(getSAXReader(mDocumentFactory.get()).read(is).getRootElement(), factory);        
    }
    
    public static Element parseXML(String xml) throws org.dom4j.DocumentException { return parseXML(xml, XMLElement.mFactory); }
    public static Element parseXML(String xml, ElementFactory factory) throws org.dom4j.DocumentException {
        return convertDOM(getSAXReader(mDocumentFactory.get()).read(new StringReader(xml)).getRootElement(), factory);
    }
    
    public static org.dom4j.io.SAXReader getSAXReader() {
        return getSAXReader(null);
    }
    
    public static org.dom4j.io.SAXReader getSAXReader(org.dom4j.DocumentFactory fact) {
        org.dom4j.io.SAXReader saxReader;
        if (fact != null)
            saxReader = new org.dom4j.io.SAXReader(fact);
        else
            saxReader = new org.dom4j.io.SAXReader();

        EntityResolver nullEntityResolver = new EntityResolver() {
            public InputSource resolveEntity (String publicId, String systemId) {
                return new InputSource(new StringReader(""));
            }            
        };
        saxReader.setEntityResolver(nullEntityResolver);
        return saxReader; 
    }

    public static Element convertDOM(org.dom4j.Element d4root) { return convertDOM(d4root, XMLElement.mFactory); }
    public static Element convertDOM(org.dom4j.Element d4root, ElementFactory factory) {
        Element elt = factory.createElement(d4root.getQName());
        for (Iterator<?> it = d4root.attributeIterator(); it.hasNext(); ) {
            org.dom4j.Attribute d4attr = (org.dom4j.Attribute) it.next();
            elt.addAttribute(d4attr.getQualifiedName(), d4attr.getValue());
        }
        String content = null;
        for (Iterator<?> it = d4root.elementIterator(); it.hasNext(); ) {
            org.dom4j.Element d4elt = (org.dom4j.Element) it.next();
            if (XHTML_NS_URI.equalsIgnoreCase(d4elt.getNamespaceURI()) && !d4elt.elements().isEmpty())
                content = (content == null ? d4elt.asXML() : content + d4elt.asXML());
            else
                elt.addElement(convertDOM(d4elt, factory));
        }
        if (content == null)
            content = d4root.getText();
        if (content != null && !content.trim().equals(""))
            elt.setText(content);
        return elt;
    }


    public static class ContainerException extends RuntimeException {
        private static final long serialVersionUID = -5884422477180821199L;
        public ContainerException(String message)  { super(message); }
    }

    public static interface ElementFactory {
        public Element createElement(String name);
        public Element createElement(QName qname);
    }

    public static interface KeyValuePair {
        public KeyValuePair setValue(String value) throws ContainerException;
        public KeyValuePair addAttribute(String key, String value) throws ContainerException;
        public KeyValuePair addAttribute(String key, long value) throws ContainerException;
        public KeyValuePair addAttribute(String key, double value) throws ContainerException;
        public KeyValuePair addAttribute(String key, boolean value) throws ContainerException;

        public String getKey() throws ContainerException;
        public String getValue() throws ContainerException;
    }

    public static class Attribute {
        private String  mKey;
        private Object  mValue;
        private Element mParent;

        Attribute(Map.Entry<String, Object> entry, Element parent)  { mKey = entry.getKey(); mValue = entry.getValue(); mParent = parent; }
        public String getKey()              { return mKey; }
        public String getValue()            { return mValue.toString(); }
        public void setValue(String value)  { mParent.addAttribute(mKey, value); mValue = value; }
    }


    public static class JSONElement extends Element {
        public static final ElementFactory mFactory = new JSONFactory();

        private static final String E_ATTRS     = "_attrs";
        private static final String A_CONTENT   = "_content";
        private static final String A_NAMESPACE = "_jsns";

        public JSONElement(String name)  { mName = name;  mAttributes = new LinkedHashMap<String, Object>(); }
        public JSONElement(QName qname)  { this(qname.getName());  setNamespace("", qname.getNamespaceURI()); }

        private static final class JSONFactory implements ElementFactory {
            JSONFactory()  { }
            public Element createElement(String name)  { return new JSONElement(name); }
            public Element createElement(QName qname)  { return new JSONElement(qname); }
        }

        private static final class JSONKeyValuePair implements KeyValuePair, Cloneable {
            private final JSONElement mTarget;
            JSONKeyValuePair(String key, String value)  { (mTarget = new JSONElement(key)).setText(value); }

            public KeyValuePair setValue(String value) throws ContainerException   { mTarget.setText(value);  return this; }
            public KeyValuePair addAttribute(String key, String value) throws ContainerException   { mTarget.addAttribute(key, value);  return this; }
            public KeyValuePair addAttribute(String key, long value) throws ContainerException     { mTarget.addAttribute(key, value);  return this; }
            public KeyValuePair addAttribute(String key, double value) throws ContainerException   { mTarget.addAttribute(key, value);  return this; }
            public KeyValuePair addAttribute(String key, boolean value) throws ContainerException  { mTarget.addAttribute(key, value);  return this; }

            public String getKey() throws ContainerException    { return mTarget.getName(); }
            public String getValue() throws ContainerException  { return mTarget.getRawText(); }

            @Override public JSONKeyValuePair clone() {
                JSONKeyValuePair clone = new JSONKeyValuePair(getKey(), getValue());
                clone.mTarget.mAttributes.putAll(mTarget.mAttributes);
                return clone;
            }

            @Override public String toString() {
                if (mTarget.mAttributes.isEmpty())
                    return "null";
                else if (mTarget.mAttributes.size() == 1 && mTarget.mAttributes.containsKey(A_CONTENT))
                    return '"' + StringUtil.jsEncode(mTarget.mAttributes.get(A_CONTENT)) + '"';
                else
                    return mTarget.toString();
            }
            Element asElement() {
                Element elt = new JSONElement(XMLElement.E_ATTRIBUTE).addAttribute(XMLElement.A_ATTR_NAME, mTarget.mName);
                elt.mAttributes.putAll(mTarget.mAttributes);
                return elt;
            }
        }

        @Override public ElementFactory getFactory()  { return mFactory; }

        @Override public Element addElement(String name) throws ContainerException  { return addElement(new JSONElement(name)); }

        @Override public Element addElement(QName qname) throws ContainerException  { return addElement(new JSONElement(qname)); }

        @SuppressWarnings("unchecked")
        @Override public Element addElement(Element elt) throws ContainerException {
            if (elt == null || elt.mParent == this)
                return elt;
            else if (elt.mParent != null)
                throw new ContainerException("element already has a parent");

            String name = elt.getName();
            Object obj = mAttributes.get(name);
            if (obj instanceof Element)
                throw new ContainerException("already stored element as unique: " + name);
            else if (obj != null && !(obj instanceof List))
                throw new ContainerException("already stored attribute with name: " + name);

            List<Element> content = (List) obj;
            if (content == null)
                mAttributes.put(name, content = new ArrayList<Element>());
            content.add(elt);
            elt.mParent = this;
            return elt.collapseNamespace();
        }

        @Override public Element addUniqueElement(String name) throws ContainerException  { return addUniqueElement(new JSONElement(name)); }

        @Override public Element addUniqueElement(QName qname) throws ContainerException  { return addUniqueElement(new JSONElement(qname)); }

        @Override public Element addUniqueElement(Element elt) throws ContainerException {
            if (elt == null)
                return null;
            String name = elt.getName();
            Object obj = mAttributes.get(name);
            if (obj instanceof List<?>) {
                throw new ContainerException("already stored non-unique element(s) with name: " + name);
            } else if (obj instanceof String || obj instanceof Number || obj instanceof Boolean) {
                throw new ContainerException("already stored attribute with name: " + name);
            } else if (obj instanceof Element) {
                if (elt.mAttributes.isEmpty())
                    return (Element) obj;
                else if (!((Element) obj).mAttributes.isEmpty())
                    throw new ContainerException("already stored unique element with name: " + name);
            }

            mAttributes.put(name, elt);
            elt.mParent = this;
            return elt;
        }

        @Override public Element setText(String content) throws ContainerException  { return addAttribute(A_CONTENT, content); }

        @Override public Element addAttribute(String key, String value, Disposition disp) throws ContainerException {
            checkNamingConflict(key);
            if (value == null)
                mAttributes.remove(key);
            else
                mAttributes.put(key, value);
            return this;
        }

        @Override public Element addAttribute(String key, long value, Disposition disp) throws ContainerException {
            checkNamingConflict(key);
            mAttributes.put(key, new Long(value));
            return this;
        }

        @Override public Element addAttribute(String key, double value, Disposition disp) throws ContainerException {
            checkNamingConflict(key);
            mAttributes.put(key, new Double(value));
            return this;
        }

        @Override public Element addAttribute(String key, boolean value, Disposition disp) throws ContainerException {
            checkNamingConflict(key);
            mAttributes.put(key, new Boolean(value));
            return this;
        }

        private void checkNamingConflict(String key) throws ContainerException {
            Object obj = mAttributes.get(key);
            if (obj instanceof Element || obj instanceof List<?>)
                throw new ContainerException("already stored element with name: " + key);
        }

        @SuppressWarnings("unchecked")
        @Override public KeyValuePair addKeyValuePair(String key, String value, String eltname, String attrname) {
            JSONElement attrs = (JSONElement) addUniqueElement(E_ATTRS);
            Object existing = attrs.mAttributes.get(key);
            KeyValuePair kvp = new JSONKeyValuePair(key, value);

            if (existing == null) {
                attrs.mAttributes.put(key, kvp);
            } else if (existing instanceof KeyValuePair) {
                List<KeyValuePair> pairs = new ArrayList<KeyValuePair>(3);
                pairs.add((KeyValuePair) existing);  pairs.add(kvp);
                attrs.mAttributes.put(key, pairs);
            } else {
                ((List<KeyValuePair>) existing).add(kvp);
            }
            return kvp;
        }

        @Override protected void detach(Element elt) throws ContainerException {
            if (elt == null)
                return;
            super.detach(elt);
            Object obj = mAttributes.get(elt.getName());
            if (obj == elt) {
                mAttributes.remove(elt.getName());
            } else if (obj instanceof List<?>) {
                ((List<?>) obj).remove(elt);
                if (((List<?>) obj).isEmpty())
                    mAttributes.remove(elt.getName());
            }
        }

        @Override public Element getOptionalElement(String name) {
            Object obj = mAttributes.get(name);
            if (obj instanceof Element)
                return (Element) obj;
            else if (obj instanceof List<?>)
                return (Element) ((List<?>) obj).get(0);
            // could return a "pseudo-element" for attribute values...
            return null;
        }

        @Override public Set<Attribute> listAttributes() {
            if (mAttributes.isEmpty())
                return Collections.emptySet();
            HashSet<Attribute> set = new HashSet<Attribute>();
            for (Map.Entry<String, Object> attr : mAttributes.entrySet()) {
                Object obj = attr.getValue();
                if (obj != null && !attr.getKey().equals(A_CONTENT) && !(obj instanceof Element || obj instanceof List<?>))
                    set.add(new Attribute(attr, this));
            }
            return set;
        }

        @SuppressWarnings("unchecked")
        @Override public List<Element> listElements(String name) {
            if (mAttributes.isEmpty())
                return Collections.emptyList();

            List<Element> list = new ArrayList<Element>();
            for (Map.Entry<String, Object> entry : mAttributes.entrySet()) {
                String key = entry.getKey();
                Object obj = entry.getValue();
                if ((name == null || name.equals(XMLElement.E_ATTRIBUTE)) && key.equals(E_ATTRS) && obj instanceof JSONElement) {
                    for (Object attr : ((Element) obj).mAttributes.values()) {
                        if (attr instanceof JSONKeyValuePair)
                            list.add(((JSONKeyValuePair) attr).asElement());
                        else
                            for (JSONKeyValuePair kvp : (List<JSONKeyValuePair>) attr)
                                list.add(kvp.asElement());
                    }
                } else if (name == null || name.equals(key)) {
                    if (obj instanceof Element)
                        list.add((Element) obj);
                    else if (obj instanceof List)
                        list.addAll((List<Element>) obj);
                }
            }
            return list;
        }

        @Override public boolean hasChildren() {
            if (!mAttributes.isEmpty()) {
                for (Object obj : mAttributes.values())
                    if (obj instanceof Element || obj instanceof List<?> || obj instanceof KeyValuePair)
                        return true;
            }
            return false;
        }

        @Override public List<KeyValuePair> listKeyValuePairs(String eltname, String attrname) {
            Element attrs = getOptionalElement(E_ATTRS);
            if (attrs == null || !(attrs instanceof JSONElement))
                return Collections.emptyList();

            List<KeyValuePair> pairs = new ArrayList<KeyValuePair>();
            for (Map.Entry<String, Object> entry : attrs.mAttributes.entrySet()) {
                List<?> values = (entry.getValue() instanceof List<?> ? (List<?>) entry.getValue() : Arrays.asList(entry.getValue()));
                for (Object multi : values) {
                    if (multi instanceof KeyValuePair)
                        pairs.add((KeyValuePair) multi);
                    else if (multi instanceof String)
                        pairs.add(new JSONKeyValuePair(entry.getKey(), (String) multi));
                }
            }
            return pairs;
        }

        @Override public String getText()  { return getAttribute(A_CONTENT, ""); }
        @Override String getRawText()      { return getAttribute(A_CONTENT, null); }

        @Override public String getAttribute(String key, String defaultValue) {
            Object obj = mAttributes.get(key);
            if (obj != null) {
                if (obj instanceof List<?>)
                    obj = ((List<?>) obj).isEmpty() ? null : ((List<?>) obj).get(0);
                if (obj instanceof Element)
                    obj = ((Element) obj).getRawText();
                else if (obj instanceof KeyValuePair)
                    obj = ((KeyValuePair) obj).getValue();
            } else {
                Element attrs = getOptionalElement(E_ATTRS);
                obj = (attrs == null ? null : attrs.mAttributes.get(key));
            }
            if (obj == null) {
                return defaultValue;
            } else if (obj instanceof JSONKeyValuePair) {
                return ((JSONKeyValuePair)obj).getValue();
            } else {
                return obj.toString();
            }
        }

        @SuppressWarnings("unchecked")
        @Override public JSONElement clone() {
            JSONElement clone = new JSONElement(getQName());
            if (mNamespaces != null) {
                if (clone.mNamespaces == null)
                    clone.mNamespaces = new HashMap<String, String>(mNamespaces);
                else
                    clone.mNamespaces.putAll(mNamespaces);
            }
            for (Map.Entry<String, Object> entry : mAttributes.entrySet()) {
                String key = entry.getKey();  Object value = entry.getValue();
                if (value instanceof Element) {
                    clone.addUniqueElement(((Element) value).clone());
                } else if (value instanceof JSONKeyValuePair) {
                    clone.mAttributes.put(key, ((JSONKeyValuePair) value).clone());
                } else if (value instanceof List<?>) {
                    for (Object child : (List<?>) value) {
                        if (child instanceof Element) {
                            clone.addElement(((Element) child).clone());
                        } else {
                            Object childclone = child instanceof JSONKeyValuePair ? ((JSONKeyValuePair) child).clone() : child;
                            List<Object> children = (List<Object>) clone.mAttributes.get(key);
                            if (children == null) {
                                (children = new ArrayList<Object>(((List<?>) value).size())).add(childclone);
                                clone.mAttributes.put(key, children);
                            } else {
                                children.add(childclone);
                            }
                        }
                    }
                } else {
                    clone.mAttributes.put(key, value);
                }
            }
            return clone;
        }

        private static final class JSRequest {
            String js;  private int offset, max;
            JSRequest(String content)  { js = content;  max = js.length(); }

            private char readEscaped() throws SoapParseException {
                skipChar('\\');
                char c, length;
                switch (c = js.charAt(offset)) {
                    case 'b':  return '\b';
                    case 't':  return '\t';
                    case 'n':  return '\n';
                    case 'f':  return '\f';
                    case 'r':  return '\r';
                    case 'u':  length = 4;  break;
                    case 'x':  length = 2;  break;
                    default:   return c;
                }
                try {
                    c = (char) Integer.parseInt(js.substring(offset + 1, offset + length + 1), 16);
                    offset += length;
                } catch (NumberFormatException nfe) {
                    error("malformed escape sequence: " + js.substring(offset - 1, offset + length + 1));
                }
                return c;
            }
            private String readQuoted(char quote) throws SoapParseException {
                StringBuilder sb = new StringBuilder();
                for (char c = js.charAt(offset); c != quote; c = js.charAt(++offset)) {
                    if (c == '\n' || c == '\t' || offset >= max - 1)
                        error("unterminated string");
                    else
                        sb.append(c == '\\' ? readEscaped() : c);
                }
                skipChar();
                return sb.toString();
            }
            private String readLiteral() throws SoapParseException {
                StringBuilder sb = new StringBuilder();
                for (char c = peekChar(); offset < max - 1; c = js.charAt(++offset)) {
                    if (c <= ' ' || ",:]}/\"[{;=#".indexOf(c) >= 0)
                        break;
                    else if (c != '\\' || max - offset < 6 || js.charAt(offset + 1) != 'u')
                        sb.append(c);
                    else
                        sb.append(readEscaped());
                }
                if (sb.length() == 0)  error("zero-length identifier");
                return sb.toString();
            }
            String readString() throws SoapParseException {
                char c = peekChar();
                return (c == '"' || c == '\'' ? readQuoted(readChar()) : readLiteral());
            }
            Object readValue() throws SoapParseException {
                char c = peekChar();
                if (c == '"' || c == '\'')
                    return readQuoted(readChar());
                String literal = readLiteral();
                if (literal.equals("null"))   return null;
                if (literal.equals("true"))   return Boolean.TRUE;
                if (literal.equals("false"))  return Boolean.FALSE;
                if ((c >= '0' && c <= '9') || c == '.' || c == '-' || c == '+') {
                    try {
                        return Long.decode(literal);
                    } catch (NumberFormatException nfe) { }
                    try {
                        return new Double(literal);
                    } catch (NumberFormatException nfe) { }
                }
                return literal;
            }

            char peekChar() throws SoapParseException  { skipWhitespace(); return js.charAt(offset); }
            char readChar() throws SoapParseException  { skipWhitespace(); return js.charAt(offset++); }
            void skipChar() throws SoapParseException  { readChar(); }
            void skipChar(char c) throws SoapParseException  { if (readChar() != c) error("expected character: " + c); }

            private void skipWhitespace() throws SoapParseException {
                if (offset >= max)
                    error("unexpected end of JSON input");
                for (char c = js.charAt(offset); offset < max; c = js.charAt(++offset))
                    if (c != 0x09 && (c < 0x0A || c > 0x0D) && (c < 0x1C || c > 0x20))
                        break;
            }

            private void error(String cause) throws SoapParseException  { throw new SoapParseException(cause, js); }
        }

        private static void parseKeyValuePair(JSRequest jsr, String key, Element parent) throws SoapParseException {
            Object value;
            switch (jsr.peekChar()) {
                case '{':  jsr.skipChar();  KeyValuePair kvp = parent.addKeyValuePair(key, null);
                           do {
                               String attr = jsr.readString();
                               switch (jsr.readChar()) {
                                   case ':':  break;
                                   case '=':  if (jsr.peekChar() == '>')  jsr.skipChar();  break;
                                   default:   throw new SoapParseException("missing expected ':'", jsr.js);
                               }
                               if ((value = jsr.readValue()) == null)  /* do nothing */;
                               else if (key.equals(A_CONTENT))         kvp.setValue(value.toString());
                               else if (value instanceof Boolean)      kvp.addAttribute(attr, ((Boolean) value).booleanValue());
                               else if (value instanceof Long)         kvp.addAttribute(attr, ((Long) value).longValue());
                               else if (value instanceof Double)       kvp.addAttribute(attr, ((Double) value).doubleValue());
                               else                                    kvp.addAttribute(attr, value.toString());
                               switch (jsr.peekChar()) {
                                   case '}':  break;
                                   case ',':
                                   case ';':  jsr.skipChar();  break;
                                   default:   throw new SoapParseException("missing expected ',' or ']'", jsr.js);
                               }
                           } while (jsr.peekChar() != '}');  jsr.skipChar();  break;
                case '[':  jsr.skipChar();
                           while (jsr.peekChar() != ']') {
                               parseKeyValuePair(jsr, key, parent);
                               switch (jsr.peekChar()) {
                                   case ']':  break;
                                   case ',':
                                   case ';':  jsr.skipChar();  break;
                                   default:   throw new SoapParseException("missing expected ',' or ']'", jsr.js);
                               }
                           };
                           jsr.skipChar();  break;
                default:   if ((value = jsr.readValue()) != null)
                               parent.addKeyValuePair(key, value.toString());  break;
            }
        }

        static Element parseElement(JSRequest jsr, QName qname, ElementFactory factory) throws SoapParseException {
            return parseElement(jsr, qname.getName(), factory, null).setNamespace("", qname.getNamespaceURI());
        }
        private static Element parseElement(JSRequest jsr, String name, ElementFactory factory, Element parent) throws SoapParseException {
            boolean isAttrs = parent != null && name.equals(E_ATTRS);
            Element elt = isAttrs ? null : factory.createElement(name);
            jsr.skipChar('{');
            while (jsr.peekChar() != '}') {
                String key = jsr.readString();
                switch (jsr.readChar()) {
                    case ':':  break;
                    case '=':  if (jsr.peekChar() == '>')  jsr.skipChar();  break;
                    default:   throw new SoapParseException("missing expected ':'", jsr.js);
                }
                if (isAttrs) {
                    parseKeyValuePair(jsr, key, parent);
                } else {
                    Object value;
                    switch (jsr.peekChar()) {
                        case '{':  elt.addUniqueElement(parseElement(jsr, key, factory, elt));  break;
                        case '[':  jsr.skipChar();
                                   while (jsr.peekChar() != ']') {
                                       elt.addElement(parseElement(jsr, key, factory, elt));
                                       switch (jsr.peekChar()) {
                                           case ']':  break;
                                           case ',':
                                           case ';':  jsr.skipChar();  break;
                                           default:   throw new SoapParseException("missing expected ',' or ']'", jsr.js);
                                       }
                                   };
                                   jsr.skipChar();  break;
                        default:   if ((value = jsr.readValue()) == null)  break;
                                   if (key.equals(A_NAMESPACE))            elt.setNamespace("", value.toString());
                                   else if (value instanceof Boolean)      elt.addAttribute(key, ((Boolean) value).booleanValue());
                                   else if (value instanceof Long)         elt.addAttribute(key, ((Long) value).longValue());
                                   else if (value instanceof Double)       elt.addAttribute(key, ((Double) value).doubleValue());
                                   else                                    elt.addAttribute(key, value.toString());
                                   break;
                    }
                }
                switch (jsr.peekChar()) {
                    case '}':  break;
                    case ',':
                    case ';':  jsr.skipChar();  break;
                    default:   throw new SoapParseException("missing expected ',' or '}'", jsr.js);
                }
            }
            jsr.skipChar('}');
            return elt;
        }

        @Override public String toString() {
            StringBuilder sb = new StringBuilder();  
            try {
                toString(sb, -1);
            } catch (IOException e) {
                // should really not happen with the StringBuilder impl of Appendable, just log it
                ZimbraLog.soap.error("Caught IOException: ", e);
            }
            return sb.toString();
        }
        
        @Override public void toString(Appendable out) throws IOException {
            toString(out, -1);
        }
        
        @Override public String prettyPrint() {
            StringBuilder sb = new StringBuilder();  
            try {
                toString(sb, 0);  
            } catch (IOException e) {
                // should really not happen with the StringBuilder impl of Appendable, just log it
                ZimbraLog.soap.error("Caught IOException: ", e);
            }
            return sb.toString();
        }

        private static final int INDENT_SIZE = 2;
        private void toString(Appendable sb, int indent) throws IOException {
            indent = indent < 0 ? -1 : indent + INDENT_SIZE;
            sb.append('{');
            boolean needNamespace = mNamespaces == null ? false : namespaceDeclarationNeeded("", mNamespaces.get("").toString());
            int size = mAttributes.size() + (needNamespace ? 1 : 0), lsize;
            if (size != 0) {
                int index = 0;
                for (Map.Entry<String, Object> attr : mAttributes.entrySet()) {
                    indent(sb, indent, true);
                    sb.append('"').append(StringUtil.jsEncode(attr.getKey())).append(indent >= 0 ? "\": " : "\":");

                    Object value = attr.getValue();
                    if (value instanceof String)                 sb.append('"').append(StringUtil.jsEncode(value)).append('"');
                    else if (value instanceof JSONKeyValuePair)  sb.append(value.toString());
                    else if (value instanceof JSONElement)       ((JSONElement) value).toString(sb, indent);
                    else if (value instanceof Element)           sb.append('"').append(StringUtil.jsEncode(value)).append('"');
                    else if (!(value instanceof List<?>))        sb.append(String.valueOf(value));
                    else {
                        sb.append('[');
                        if ((lsize = ((List<?>) value).size()) > 0)
                            for (ListIterator<?> lit = ((List<?>) value).listIterator(); lit.hasNext(); ) {
                                int lindent = indent < 0 ? -1 : indent + INDENT_SIZE;
                                if (lsize > 1)
                                    indent(sb, lindent, true);
                                Object child = lit.next();
                                if (child instanceof JSONElement)
                                    ((JSONElement) child).toString(sb, lindent);
                                else if (child instanceof JSONKeyValuePair)
                                    sb.append(child.toString());
                                else
                                    sb.append('"').append(StringUtil.jsEncode(child)).append('"');
                                if (lit.nextIndex() != lsize)  sb.append(",");
                            }
                        sb.append(']');
                    }
                    if (index++ < size - 1)  sb.append(",");
                }
                if (needNamespace) {
                    indent(sb, indent, true);
                    sb.append('"').append(A_NAMESPACE).append(indent >= 0 ? "\": \"" : "\":\"").append(StringUtil.jsEncode(mNamespaces.get(""))).append('"');
                }
                indent(sb, indent - 2, true);
            }
            sb.append('}');
        }
    }

    public static class XMLElement extends Element {
        private String        mText;
        private List<Element> mChildren;

        public static final ElementFactory mFactory = new XMLFactory();

        public static final String E_ATTRIBUTE = "a";
        public static final String A_ATTR_NAME = "n";
        private static final String A_NAMESPACE = "xmlns";

        public XMLElement(String name) throws ContainerException { mName = validateName(name); }
        public XMLElement(QName qname) throws ContainerException {
            mName = validateName(qname.getName());
            String uri = qname.getNamespaceURI();
            if (uri == null || uri.equals(""))
                return;
            mPrefix = qname.getNamespacePrefix();
            setNamespace(mPrefix, uri);
        }

        private static final class XMLFactory implements ElementFactory {
            XMLFactory()  { }
            public Element createElement(String name)  { return new XMLElement(name); }
            public Element createElement(QName qname)  { return new XMLElement(qname); }
        }

        private final class XMLKeyValuePair implements KeyValuePair {
            private final XMLElement mTarget;
            private final String mAttrName;
            XMLKeyValuePair(String key, String value, String eltname, String attrname)  { this(key, value, eltname, attrname, true); }
            XMLKeyValuePair(String key, String value, String eltname, String attrname, boolean register)  {
                (mTarget = new XMLElement(eltname)).addAttribute(mAttrName = attrname, key).setText(value);
                if (register)  addElement(mTarget);
            }

            public KeyValuePair setValue(String value) throws ContainerException   { mTarget.setText(value);  return this; }
            public KeyValuePair addAttribute(String key, String value) throws ContainerException   { mTarget.addAttribute(key, value);  return this; }
            public KeyValuePair addAttribute(String key, long value) throws ContainerException     { mTarget.addAttribute(key, value);  return this; }
            public KeyValuePair addAttribute(String key, double value) throws ContainerException   { mTarget.addAttribute(key, value);  return this; }
            public KeyValuePair addAttribute(String key, boolean value) throws ContainerException  { mTarget.addAttribute(key, value);  return this; }

            public String getKey() throws ContainerException    { return mTarget.getAttribute(mAttrName, null); }
            public String getValue() throws ContainerException  { return mTarget.getRawText(); }

            @Override public String toString()  { return mTarget.toString(); }
        }

        @Override public ElementFactory getFactory()  { return mFactory; }

        @Override public Element addElement(String name) throws ContainerException { return addElement(new XMLElement(name)); }

        @Override public Element addElement(QName qname) throws ContainerException { return addElement(new XMLElement(qname)); }

        @Override public Element addElement(Element elt) throws ContainerException {
            if (elt == null || elt.mParent == this)
                return elt;
            else if (elt.mParent != null)
                throw new ContainerException("element already has a parent");
            else if (mText != null)
                throw new ContainerException("cannot add children to element containing text");

            if (mChildren == null)
                mChildren = new ArrayList<Element>();
            mChildren.add(elt);
            elt.mParent = this;
            return elt.collapseNamespace();
        }

        @Override public Element setText(String content) throws ContainerException {
            if (content != null && !content.trim().equals("") && mChildren != null)
                throw new ContainerException("cannot set text on element with children");
            mText = content;
            return this;
        }

        @Override public Element addAttribute(String key, String value, Disposition disp) throws ContainerException {
            validateName(key);
            // if we're setting an attribute, we need to clear all other things that could be considered the same...
            if (mAttributes != null) {
                mAttributes.remove(key);
            }
            if (mChildren != null) {
                for (Element child : listElements(key))
                    if (!child.hasChildren())
                        child.detach();
            }
            // a null value leaves it unset; a non-null value places the attribute appropriately
            if (value != null) {
                if (disp == Disposition.CONTENT) {
                    addElement(key).setText(value);
                } else {
                    if (mAttributes == null)
                        mAttributes = new HashMap<String, Object>();
                    mAttributes.put(key, value);
                }
            }
            return this;
        }

        @Override public KeyValuePair addKeyValuePair(String key, String value, String eltname, String attrname) throws ContainerException {
            return new XMLKeyValuePair(key, value, eltname == null ? E_ATTRIBUTE : validateName(eltname), attrname == null ? A_ATTR_NAME : validateName(attrname));
        }

        private String validateName(String name) throws ContainerException  {
            if (name == null || name.equals(""))
                throw new ContainerException("blank/missing XML attribute name");

            for (int i = 0; i < name.length(); i++) {
                char c = name.charAt(i);
                if (c == ':' || c == '_' || (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z'))
                    continue;
                if (i > 0 && (c == '-' || c == '.' || (c >= '0' && c <= '9') || c == 0xB7 || c == 0x203F || c == 0x2040))
                    continue;
                if (c >= 0xC0 && c <= 0x1FFF && c != 0xD7 && c != 0xF7 && c != 0x37E)
                    if (i > 0 || c < 0x300 || c > 0x36F)
                        continue;
                if ((c >= 0x2070 && c <= 0x218F) || (c >= 0x2C00 && c <= 0x2FEF) || (c >= 0x3001 && c <= 0xD7FF))
                    continue;
                if ((c >= 0xF900 && c <= 0xFDCF) || (c >= 0xFDF0 && c <= 0xFFFD) || (c >= 0x10000 && c <= 0xEFFFF))
                    continue;
                throw new ContainerException("invalid XML attribute name: " + name);
            }
            return name;
        }

        @Override protected void detach(Element elt) throws ContainerException {
            super.detach(elt);
            if (mChildren != null) {
                mChildren.remove(elt);
                if (mChildren.size() == 0)
                    mChildren = null;
            }
        }

        @Override public Element getOptionalElement(String name) {
            if (mChildren != null && name != null)
                for (Element elt : mChildren)
                    if (elt.getName().equals(name))
                        return elt;
            return null;
        }

        @Override public Element getOptionalElement(QName qname) {
            if (mChildren != null && qname != null) {
                for (Element elt : mChildren)
                    if (elt.getQName().equals(qname))
                        return elt;
            }
            return null;
        }

        @Override public Set<Attribute> listAttributes() {
            if (mAttributes == null || mAttributes.isEmpty())
                return Collections.emptySet();
            HashSet<Attribute> set = new HashSet<Attribute>();
            for (Map.Entry<String, Object> attr : mAttributes.entrySet())
                set.add(new Attribute(attr, this));
            return set;
        }

        @Override public List<Element> listElements(String name) {
            if (mChildren == null)
                return Collections.emptyList();
            ArrayList<Element> list = new ArrayList<Element>();
            if (name == null || name.trim().equals("")) {
                list.addAll(mChildren);
            } else {
                for (Element elt : mChildren)
                    if (elt.getName().equals(name))
                        list.add(elt);
            }
            return list;
        }

        @Override public boolean hasChildren() {
            return mChildren != null && !mChildren.isEmpty();
        }

        @Override public List<KeyValuePair> listKeyValuePairs(String eltname, String attrname) {
            eltname = eltname == null ? E_ATTRIBUTE : validateName(eltname);
            attrname = attrname == null ? A_ATTR_NAME : validateName(attrname);

            List<KeyValuePair> pairs = new ArrayList<KeyValuePair>();
            for (Element elt : listElements(eltname)) {
                String key = elt.getAttribute(attrname, null);
                if (key != null)
                    pairs.add(new XMLKeyValuePair(key, elt.getText(), eltname, attrname, false));
            }
            return pairs;
        }

        @Override public String getText()  { return (mText == null ? "" : mText); }
        @Override String getRawText()      { return mText; }

        @Override public String getAttribute(String key, String defaultValue) {
            if (key == null)
                return defaultValue;
            if (mAttributes != null) {
                String result;
                if ((result = (String) mAttributes.get(key)) != null)
                    return result;
            }
            if (mChildren != null) {
                for (Element elt : mChildren) {
                    if (elt.getName().equals(key))
                        return elt.getText();
                }
            }
            return defaultValue;
        }

        private String xmlEncode(String str, boolean escapeQuotes) {
            if (str == null)
                return "";
            StringBuilder sb = null;
            String replacement;
            int i, last;
            for (i = 0, last = -1; i < str.length(); i++) {
                char c = str.charAt(i);
                switch (c) {
                    case '&':  replacement = "&amp;";   break;
                    case '<':  replacement = "&lt;";    break;
                    case '>':  if (i < 2 || str.charAt(i-1) != ']' || str.charAt(i-2) != ']')  continue;
                               replacement = "&gt;";    break;
                    case '"':  if (!escapeQuotes)       continue;
                               replacement = "&quot;";  break;
                    case 0x09:  case 0x0A:  case 0x0D:  continue;
                    default:   if (c >= 0x20 && c != 0xFFFE && c != 0xFFFF && (c <= 0xD7FF || c >= 0xE000))  continue;
                               replacement = "?";       break;
                }
                if (sb == null)
                    sb = new StringBuilder(str.substring(0, i));
                else
                    sb.append(str.substring(last, i));
                sb.append(replacement);
                last = i + 1;
            }
            return (sb == null ? str : sb.append(str.substring(last, i)).toString());
        }

        @Override public XMLElement clone() {
            XMLElement clone = new XMLElement(getQName());
            clone.mText = mText;
            if (mAttributes != null) {
                clone.mAttributes = new HashMap<String, Object>(mAttributes);
            }
            if (mNamespaces != null) {
                if (clone.mNamespaces == null)
                    clone.mNamespaces = new HashMap<String, String>(mNamespaces);
                else
                    clone.mNamespaces.putAll(mNamespaces);
            }
            if (mChildren != null) {
                for (Element child : mChildren)
                    clone.addElement(child.clone());
            }
            return clone;
        }

        @Override public String toString() {
            StringBuilder sb = new StringBuilder();  
            try {
                toString(sb, -1);  
            } catch (IOException e) {
                // should really not happen with the StringBuilder impl of Appendable, just log it
                ZimbraLog.soap.error("Caught IOException: ", e);
            }
            return sb.toString();
        }
        
        @Override public void toString(Appendable out) throws IOException {
            toString(out, -1);
        }
        
        @Override public String prettyPrint() {
            StringBuilder sb = new StringBuilder();  
            try {
                toString(sb, 0);  
            } catch (IOException e) {
                // should really not happen with the StringBuilder impl of Appendable, just log it
                ZimbraLog.soap.error("Caught IOException: ", e);
            }
            return sb.toString();
        }

        private static final int INDENT_SIZE = 2;
        private void toString(Appendable sb, int indent) throws IOException {
            indent(sb, indent, indent > 0);
            // element's qualified name
            String qn = getQualifiedName();
            sb.append("<").append(qn);
            // element's attributes
            if (mAttributes != null) {
                for (Map.Entry<String, Object> attr : mAttributes.entrySet())
                    sb.append(' ').append(attr.getKey()).append("=\"").append(xmlEncode((String) attr.getValue(), true)).append('"');
            }
            // new namespaces defined on this element
            if (mNamespaces != null) {
                for (Map.Entry<String, String> ns : mNamespaces.entrySet()) {
                    String prefix = ns.getKey();
                    String uri = ns.getValue();
                    if (namespaceDeclarationNeeded(prefix, uri))
                        sb.append(' ').append(A_NAMESPACE).append(prefix.equals("") ? "" : ":").append(prefix).append("=\"").append(xmlEncode(uri, true)).append('"');
                }
            }
            // element content (children/text) and closing
            String text = "".equals(mText) ? null : mText;
            if (mChildren != null || text != null) {
                sb.append('>');
                if (mChildren != null) {
                    for (Element child : mChildren) {
                        if (child instanceof XMLElement)
                            ((XMLElement) child).toString(sb, indent < 0 ? -1 : indent + INDENT_SIZE);
                        else
                            sb.append(xmlEncode(child.toString(), false));
                    }
                    indent(sb, indent, true);
                } else {
                    sb.append(xmlEncode(text, false));
                }
                sb.append("</").append(qn).append('>');
            } else {
                sb.append("/>");
            }
        }
    }


    public static void main(String[] args) throws ContainerException, SoapParseException {
        System.out.println(Element.parseJSON("{ 'a':'b'}").getAttribute("a", null));
        System.out.println(Element.parseJSON("{ '_attrs' : {'a':'b'}}").getAttribute("a", null));
        System.out.println(Element.parseJSON("{foo:' bar'}").getAttribute("foo", null));
        System.out.println(Element.parseJSON("{foo:'bar'}").getAttribute("foo", null));
        System.out.println(Element.parseJSON("{foo:''}").getAttribute("foo", null));
        System.out.println(Element.parseJSON("{ \"items\" : [ ] }"));
        System.out.println(Element.parseJSON("{ '_attrs' : {'a':[]}}").getAttribute("a", null));

        org.dom4j.Namespace bogusNS = org.dom4j.Namespace.get("bogus", "");
        QName qm = new QName("m", bogusNS);

        SoapProtocol proto = SoapProtocol.SoapJS;
        Element ctxt = new JSONElement(proto.getHeaderQName()).addUniqueElement(HeaderConstants.E_CONTEXT);
        ctxt.addElement(HeaderConstants.E_SESSION).setText("3").addAttribute(HeaderConstants.A_ID, 3);
        System.out.println(ctxt.getAttribute(HeaderConstants.E_SESSION, null));

        Element env = testMessage(new JSONElement(proto.getEnvelopeQName()), proto, qm);
        System.out.println(env);
        System.out.println(Element.parseJSON(env.toString()));

        proto = SoapProtocol.Soap12;
        env = testMessage(new XMLElement(proto.getEnvelopeQName()), proto, qm);
        System.out.println(env.prettyPrint());
        System.out.println("             name: " + env.getName());
        System.out.println("   qualified name: " + env.getQualifiedName());
        System.out.println("            qname: " + env.getQName());

        Element e = testContacts(new JSONElement(MailConstants.GET_CONTACTS_RESPONSE));
        System.out.println(e);
        System.out.println(e.prettyPrint());
        testKeyValuePairs(e);

        System.out.println(Element.parseJSON(e.toString()));
        testKeyValuePairs(Element.parseJSON(e.toString()));
        System.out.println(Element.parseJSON(e.toString(), XMLElement.mFactory).prettyPrint());

        e = testContacts(new XMLElement(MailConstants.GET_CONTACTS_RESPONSE));
        System.out.println(e.prettyPrint());
        for (Element elt : e.listElements())
            System.out.println("  found: id=" + elt.getAttribute("ID", null));
        testKeyValuePairs(e);

//        System.out.println(com.zimbra.common.soap.SoapProtocol.toString(e.toXML(), true));
        System.out.println(new XMLElement("test").setText("  this\t    is\nthe\rway ").getTextTrim() + "|");
        System.out.println(Element.parseJSON("{part:\"TEXT\",t:null,h:true,i:\"false\",\"ct\":\"\\x25multipart\\u0025\\/mixed\",\\u0073:3718}").toString());
        try {
            Element.parseJSON("{\"wkday\":{\"day\":\"TU\"},\"wkday\":{\"day\":\"WE\"},\"wkday\":{\"day\":\"FR\"}}");
        } catch (SoapParseException spe) {
            System.out.println("caught exception (expected): " + spe.getMessage());
        }

        System.out.println(new XMLElement("test").addAttribute("x", (String) null).addAttribute("x", "", Disposition.CONTENT).addAttribute("x", "bar").addAttribute("x", (String) null));
        System.out.println(new JSONElement("test").addAttribute("x", (String) null).addAttribute("x", "foo", Disposition.CONTENT).addAttribute("x", "bar").addAttribute("x", (String) null));

        try {
            System.out.println("foo: |" + Element.parseXML("<test><foo/></test>").getAttribute("foo") + "|");
        } catch (Exception x) {
            System.out.println("error parsing XML element: " + x);
        }
    }

    private static Element testMessage(Element env, SoapProtocol proto, QName qm) {
        env.addUniqueElement(proto.getBodyQName()).addUniqueElement(MailConstants.GET_MSG_RESPONSE)
           .addUniqueElement(qm).addAttribute("id", 1115).addAttribute("f", "aw").addAttribute("t", "64,67").addAttribute("score", 0.953)
           .addAttribute("s", "Subject of the message has a \"\\\" in it", Disposition.CONTENT).addAttribute("mid", "<kashdfgiai67r3wtuwfg@goo.com>", Disposition.CONTENT)
           .addElement("mp").addAttribute("part", "TEXT").addAttribute("ct", "multipart/mixed").addAttribute("s", 3718);
        String orig = env.toString(), clone = env.clone().toString();
        System.out.println("< " + orig);  System.out.println("> " + clone);
        return env;
    }

    private static Element testContacts(Element parent) {
        parent.addElement("cn");
        Element cn = parent.addElement("cn").addAttribute("id", 256).addAttribute("md", 1111196674000L).addAttribute("l", 7).addAttribute("x", false);
        cn.addKeyValuePair("workPhone", "(408) 973-0500 x112", "pm", "name");
        cn.addKeyValuePair("notes", "These are &\nrandom notes", "pm", "name");
        cn.addKeyValuePair("firstName", "Ross \"Killa\"", "pm", "name");
        cn.addKeyValuePair("lastName", "Dargahi", "pm", "name");
        cn.addKeyValuePair("lastName", "Dangerous", "pm", "name");
        cn.addKeyValuePair("image", null, "pm", "name").addAttribute("size", 34102).addAttribute("ct", "image/png").addAttribute("part", "1");
        cn = parent.addElement("cn").addAttribute("id", 257).addAttribute("md", 1111196674000L).addAttribute("l", 7);
        cn.addKeyValuePair("workPhone", "(408) 973-0500 x111");
        cn.addKeyValuePair("jobTitle", "CEO");
        cn.addKeyValuePair("firstName", "Satish");
        cn.addKeyValuePair("lastName", "Dharmaraj");
        cn.addKeyValuePair("foo=bar", "baz=whop");
        if (!parent.toString().equals(parent.clone().toString()))
            System.out.println("error: clone diverges from parent");
        return parent;
    }

    private static void testKeyValuePairs(Element parent) {
        for (Element cn : parent.listElements())
            for (KeyValuePair kvp : cn.listKeyValuePairs("pm", "name"))
                System.out.print("   " + kvp.getKey() + ": " + kvp.getValue());
        System.out.println();
    }
}
