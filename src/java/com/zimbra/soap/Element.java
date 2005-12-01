/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Mar 16, 2005
 */
package com.zimbra.soap;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.*;

import org.dom4j.QName;

import com.zimbra.cs.service.ServiceException;

/**
 * @author dkarp
 */
public abstract class Element {
    protected String  mName;
    protected String  mPrefix = "";
    protected Element mParent;
    protected Map<String, Object> mAttributes;
    protected Map<String, String> mNamespaces;

    public static final byte DISP_ATTRIBUTE = 0;
    public static final byte DISP_CONTENT   = 1;
    public static final byte DISP_ELEMENT   = 2;

    public abstract ElementFactory getFactory();

    // writing to the element hierarchy
    public abstract Element addElement(String name) throws ContainerException;
    public abstract Element addElement(QName qname) throws ContainerException;
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

    public Element addAttribute(String key, String value) throws ContainerException   { return addAttribute(key, value, DISP_ATTRIBUTE); }
    public Element addAttribute(String key, long value) throws ContainerException     { return addAttribute(key, value, DISP_ATTRIBUTE); }
    public Element addAttribute(String key, double value) throws ContainerException   { return addAttribute(key, value, DISP_ATTRIBUTE); }
    public Element addAttribute(String key, boolean value) throws ContainerException  { return addAttribute(key, value, DISP_ATTRIBUTE); }

    public abstract Element addAttribute(String key, String value, byte disposition) throws ContainerException;
    public Element addAttribute(String key, long value, byte disposition) throws ContainerException     { return addAttribute(key, Long.toString(value), disposition); }
    public Element addAttribute(String key, double value, byte disposition) throws ContainerException   { return addAttribute(key, Double.toString(value), disposition); }
    public Element addAttribute(String key, boolean value, byte disposition) throws ContainerException  { return addAttribute(key, value ? "1" : "0", disposition); }

    protected void detach(Element child) throws ContainerException {
        if (child == null)
            return;
        if (child.mParent != this)
            throw new ContainerException("wrong parent");
        child.mParent = null;
    }
    public Element detach() throws ContainerException  { if (mParent != null) { mParent.detach(this); } return this; }

    // reading from the element hierarchy
    public String getName()           { return mName; }
    public String getQualifiedName()  { return (mPrefix != null && !mPrefix.equals("") ? mPrefix + ':' + mName : mName); }
    public QName getQName()           { String uri = getNamespaceURI(mPrefix); return (uri == null ? QName.get(mName) : QName.get(mName, uri)); }

    public QName getQName(String qualifiedName) { String[] parts = qualifiedName.split("\\."); return new QName(parts[parts.length - 1]); }

    public Element getParent()        { return mParent; }

    /** Fetch a REQUIRED sub-element.  If none is found, throw an exception.
     * @return The first sub-element with a matching name */
    public Element getElement(String name) throws ServiceException  { return checkNull(name, getOptionalElement(name)); }
    public Element getElement(QName qname) throws ServiceException  { return checkNull(qname.getName(), getOptionalElement(qname)); }

    public abstract Element getOptionalElement(String name);
    public Element getOptionalElement(QName qname)                  { return getOptionalElement(qname.getName()); }

    public abstract Set<Attribute> listAttributes();
    public List<Element>   listElements()                    { return listElements(null); }
    public abstract List<Element> listElements(String name);

    public Iterator<Attribute> attributeIterator()           { return listAttributes().iterator(); }
    public Iterator<Element>   elementIterator()             { return listElements().iterator(); }
    public Iterator<Element>   elementIterator(String name)  { return listElements(name).iterator(); }

    public abstract String getText();
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
        catch (NumberFormatException nfe) { throw ServiceException.INVALID_REQUEST("invalid value for attribute: " + key, nfe); }
    }
    public static double parseDouble(String key, String value) throws ServiceException {
        try { return Double.parseDouble(value); }
        catch (NumberFormatException nfe) { throw ServiceException.INVALID_REQUEST("invalid value for attribute: " + key, nfe); }
    }
    public static boolean parseBool(String key, String value) throws ServiceException {
        if (value.equals("1") || value.equalsIgnoreCase("true"))        return true;
        else if (value.equals("0") || value.equalsIgnoreCase("false"))  return false;
        throw ServiceException.INVALID_REQUEST("invalid boolean value for attribute: " + key, null);
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
    public abstract String prettyPrint();

    private static final String FORTY_SPACES = "                                        ";
    protected void indent(StringBuffer sb, int indent, boolean newline) {
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
        for (Iterator it = attributeIterator(); it.hasNext(); ) {
            Attribute attr = (Attribute) it.next();
            d4elt.addAttribute(attr.getKey(), attr.getValue());
        }
        for (Iterator it = elementIterator(); it.hasNext(); )
            ((Element) it.next()).toXML(d4elt);
        d4elt.setText(getText());
        return d4elt;
    }

    public static Element parseJSON(InputStream is) throws SoapParseException { return parseJSON(is, JavaScriptElement.mFactory); }
    public static Element parseJSON(InputStream is, ElementFactory factory) throws SoapParseException {
        try {
            return parseJSON(new String(com.zimbra.cs.util.ByteUtil.getContent(is, -1), "utf-8"), factory);
        } catch (SoapParseException e) {
            throw e;
        } catch (Exception e) {
            throw new SoapParseException("could not transcode request from utf-8", null);
        }
    }
    public static Element parseJSON(String js) throws SoapParseException { return parseJSON(js, JavaScriptElement.mFactory); }
    public static Element parseJSON(String js, ElementFactory factory) throws SoapParseException {
        return JavaScriptElement.parseElement(new JavaScriptElement.JSRequest(js), com.zimbra.soap.SoapProtocol.SoapJS.getEnvelopeQName(), factory);
    }

    private static final String XHTML_NS_URI = "http://www.w3.org/1999/xhtml";

    public static Element parseXML(InputStream is) throws org.dom4j.DocumentException { return parseXML(is, XMLElement.mFactory); }
    public static Element parseXML(InputStream is, ElementFactory factory) throws org.dom4j.DocumentException {
        return convertDOM(new org.dom4j.io.SAXReader().read(is).getRootElement(), factory);
    }
    public static Element parseXML(String xml) throws org.dom4j.DocumentException { return parseXML(xml, XMLElement.mFactory); }
    public static Element parseXML(String xml, ElementFactory factory) throws org.dom4j.DocumentException {
        return convertDOM(org.dom4j.DocumentHelper.parseText(xml).getRootElement(), factory);
    }
    public static Element convertDOM(org.dom4j.Element d4root) { return convertDOM(d4root, XMLElement.mFactory); }
    public static Element convertDOM(org.dom4j.Element d4root, ElementFactory factory) {
        Element elt = factory.createElement(d4root.getQName());
        for (Iterator it = d4root.attributeIterator(); it.hasNext(); ) {
            org.dom4j.Attribute d4attr = (org.dom4j.Attribute) it.next();
            elt.addAttribute(d4attr.getQualifiedName(), d4attr.getValue());
        }
        String content = null;
        for (Iterator it = d4root.elementIterator(); it.hasNext(); ) {
            org.dom4j.Element d4elt = (org.dom4j.Element) it.next();
            if (XHTML_NS_URI.equals(d4elt.getNamespaceURI()))
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
        public ContainerException(String message)  { super(message); }
    }

    public static interface ElementFactory {
        public Element createElement(String name);
        public Element createElement(QName qname);
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

    public static class JavaScriptElement extends Element {
        public static final ElementFactory mFactory = new JavaScriptFactory();

        private static final String E_ATTRS     = "_attrs";
        private static final String A_CONTENT   = "_content";
        private static final String A_NAMESPACE = "_jsns";

        public JavaScriptElement(String name)  { mName = name; mAttributes = new HashMap<String, Object>(); }
        public JavaScriptElement(QName qname)  { this(qname.getName()); setNamespace("", qname.getNamespaceURI()); }

        private static final class JavaScriptFactory implements ElementFactory {
            public Element createElement(String name)  { return new JavaScriptElement(name); }
            public Element createElement(QName qname)  { return new JavaScriptElement(qname); }
        }

        public ElementFactory getFactory()  { return mFactory; }

        public Element addElement(String name) throws ContainerException  { return addElement(new JavaScriptElement(name)); }

        public Element addElement(QName qname) throws ContainerException  { return addElement(new JavaScriptElement(qname)); }

        public Element addElement(Element elt) throws ContainerException {
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
            return elt;
        }

        public Element addUniqueElement(String name) throws ContainerException  { return addUniqueElement(new JavaScriptElement(name)); }

        public Element addUniqueElement(QName qname) throws ContainerException  { return addUniqueElement(new JavaScriptElement(qname)); }

        public Element addUniqueElement(Element elt) throws ContainerException {
            if (elt == null)
                return null;
            String name = elt.getName();
            Object obj = mAttributes.get(name);
            if (obj instanceof List)
                throw new ContainerException("already stored non-unique element(s) with name: " + name);
            else if (obj instanceof String || obj instanceof Integer)
                throw new ContainerException("already stored attribute with name: " + name);
            else if (obj instanceof Element) {
                if (elt.mAttributes.isEmpty())
                    return (Element) obj;
                else if (!((Element) obj).mAttributes.isEmpty())
                    throw new ContainerException("already stored unique element with name: " + name);
            }

            mAttributes.put(name, elt);
            elt.mParent = this;
            return elt;
        }

        public Element setText(String content) throws ContainerException  { return addAttribute(A_CONTENT, content); }

        public Element addAttribute(String key, String value, byte disposition) throws ContainerException {
            if (value == null)
                return this;
            else if (disposition == DISP_ELEMENT)
                addUniqueElement(E_ATTRS).addAttribute(key, value);
            else {
                checkNamingConflict(key);
                mAttributes.put(key, value);
            }
            return this;
        }

        public Element addAttribute(String key, long value, byte disposition) throws ContainerException {
            if (disposition == DISP_ELEMENT)
                addUniqueElement(E_ATTRS).addAttribute(key, value);
            else {
                checkNamingConflict(key);
                mAttributes.put(key, new Long(value));
            }
            return this;
        }

        public Element addAttribute(String key, double value, byte disposition) throws ContainerException {
            if (disposition == DISP_ELEMENT)
                addUniqueElement(E_ATTRS).addAttribute(key, value);
            else {
                checkNamingConflict(key);
                mAttributes.put(key, new Double(value));
            }
            return this;
        }

        public Element addAttribute(String key, boolean value, byte disposition) throws ContainerException {
            if (disposition == DISP_ELEMENT)
                addUniqueElement(E_ATTRS).addAttribute(key, value);
            else {
                checkNamingConflict(key);
                mAttributes.put(key, new Boolean(value));
            }
            return this;
        }

        private void checkNamingConflict(String key) throws ContainerException {
            Object obj = mAttributes.get(key);
            if (obj instanceof Element || obj instanceof List)
                throw new ContainerException("already stored element with name: " + key);
        }

        protected void detach(Element elt) throws ContainerException {
            if (elt == null)
                return;
            super.detach(elt);
            Object obj = mAttributes.get(elt.getName());
            if (obj == elt)
                mAttributes.remove(elt.getName());
            else if (obj instanceof List) {
                ((List) obj).remove(elt);
                if (((List) obj).size() == 0)
                    mAttributes.remove(elt.getName());
            }
        }

        public Element getOptionalElement(String name) {
            Object obj = mAttributes.get(name);
            if (obj instanceof Element)
                return (Element) obj;
            else if (obj instanceof List)
                return (Element) ((List) obj).get(0);
            // could return a "pseudo-element" for attribute values...
            return null;
        }

        public Set<Attribute> listAttributes() {
            if (mAttributes.isEmpty())
                return Collections.emptySet();
            HashSet<Attribute> set = new HashSet<Attribute>();
            for (Map.Entry<String, Object> attr : mAttributes.entrySet()) {
                Object obj = attr.getValue();
                if (obj != null && !(obj instanceof Element || obj instanceof List))
                    set.add(new Attribute(attr, this));
            }
            return set;
        }

        public List<Element> listElements(String name) {
            if (mAttributes.isEmpty())
                return Collections.emptyList();
            ArrayList<Element> list = new ArrayList<Element>();
            for (Map.Entry<String, Object> attr : mAttributes.entrySet())
                if (name == null || name.equals(attr.getKey())) {
                    Object obj = attr.getValue();
                    if (obj instanceof Element)
                        list.add((Element) obj);
                    else if (obj instanceof List)
                        list.addAll((List<Element>) obj);
                }
            return list;
        }

        public String getText()  { return getAttribute(A_CONTENT, ""); }

        public String getAttribute(String key, String defaultValue) {
            Object obj = mAttributes.get(key);
            if (obj != null)
                return obj.toString();
            Element attrs = getOptionalElement(E_ATTRS);
            obj = (attrs == null ? null : attrs.mAttributes.get(key));
            return (obj == null ? defaultValue : obj.toString());
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
                StringBuffer sb = new StringBuffer();
                for (char c = peekChar(); c != quote; c = js.charAt(++offset))
                    if (c == '\n' || c == '\t' || offset >= max - 1)
                        error("unterminated string");
                    else
                        sb.append(c == '\\' ? readEscaped() : c);
                skipChar();
                return sb.toString();
            }
            private String readLiteral() throws SoapParseException {
                StringBuffer sb = new StringBuffer();
                for (char c = peekChar(); offset < max - 1; c = js.charAt(++offset))
                    if (c <= ' ' || ",:]}/\"[{;=#".indexOf(c) >= 0)
                        break;
                    else if (c != '\\' || max - offset < 6 || js.charAt(offset + 1) != 'u')
                        sb.append(c);
                    else
                        sb.append(readEscaped());
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
                    error("unexpected end of JavaScript input");
                for (char c = js.charAt(offset); offset < max; c = js.charAt(++offset))
                    if (c != 0x09 && (c < 0x0A || c > 0x0D) && (c < 0x1C || c > 0x20))
                        break;
            }

            private void error(String cause) throws SoapParseException  { throw new SoapParseException(cause, js); }
        }

        static Element parseElement(JSRequest jsr, QName qname, ElementFactory factory) throws SoapParseException {
            return parseElement(jsr, qname.getName(), factory).setNamespace("", qname.getNamespaceURI());
        }
        private static Element parseElement(JSRequest jsr, String name, ElementFactory factory) throws SoapParseException {
            Element elt = factory.createElement(name);
            jsr.skipChar('{');
            while (jsr.peekChar() != '}') {
                String key = jsr.readString();
                Object value;
                switch (jsr.readChar()) {
                    case ':':  break;
                    case '=':  if (jsr.peekChar() == '>')  jsr.skipChar();  break;
                    default:   throw new SoapParseException("missing expected ':'", jsr.js);
                }
                switch (jsr.peekChar()) {
                    case '{':  elt.addUniqueElement(parseElement(jsr, key, factory));  break;
                    case '[':  jsr.skipChar();
                               do {
                                   elt.addElement(parseElement(jsr, key, factory));
                                   switch (jsr.peekChar()) {
                                       case ']':  break;
                                       case ',':
                                       case ';':  jsr.skipChar();  break;
                                       default:   throw new SoapParseException("missing expected ',' or ']'", jsr.js);
                                   }
                               } while (jsr.peekChar() != ']');  jsr.skipChar();  break;
                    default:   if ((value = jsr.readValue()) == null)             break;
                               if (key.equals(A_NAMESPACE))        elt.setNamespace("", value.toString());
                               else if (value instanceof Boolean)  elt.addAttribute(key, ((Boolean) value).booleanValue());
                               else if (value instanceof Long)     elt.addAttribute(key, ((Long) value).longValue());
                               else if (value instanceof Double)   elt.addAttribute(key, ((Double) value).doubleValue());
                               else                                elt.addAttribute(key, value.toString());
                               break;
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

        private static final HashSet<String> RESERVED_KEYWORDS = new HashSet<String>(Arrays.asList(new String[] {
                A_NAMESPACE, "abstract", "boolean", "break", "byte", "case", "catch", "char", "class", "continue",
                "const", "debugger", "default", "delete", "do", "double", "else", "extends", "enum", "export", 
                "false", "final", "finally", "float", "for", "function", "goto", "if", "implements", "import", "in",
                "instanceOf", "int", "interface", "label", "long", "native", "new", "null", "package", "private",
                "protected", "public", "return", "short", "static", "super", "switch", "synchronized", "this",
                "throw", "throws", "transient", "true", "try", "typeof", "var", "void", "volatile", "while", "with"
        }));

        private static final String[] JS_CHAR_ENCODINGS = {
            "\\u0000", "\\u0001", "\\u0002", "\\u0003", "\\u0004", "\\u0005", "\\u0006", "\\u0007",
            "\\b",     "\\t",     "\\n",     "\\u000B", "\\f",     "\\r",     "\\u000E", "\\u000F",
            "\\u0010", "\\u0011", "\\u0012", "\\u0013", "\\u0014", "\\u0015", "\\u0016", "\\u0017",
            "\\u0018", "\\u0019", "\\u001A", "\\u001B", "\\u001C", "\\u001D", "\\u001E", "\\u001F"
        };

        private static String jsEncode(Object obj) {
            if (obj == null)
                return "";
            String replacement, str = obj.toString();
            StringBuffer sb = null;
            int i, last, length = str.length();
            for (i = 0, last = -1; i < length; i++) {
                char c = str.charAt(i);
                switch (c) {
                    case '\\':  replacement = "\\\\";                break;
                    case '"':   replacement = "\\\"";                break;
                    default:    if (c >= ' ')                        continue;
                                replacement = JS_CHAR_ENCODINGS[c];  break;
                }
                if (sb == null)
                    sb = new StringBuffer(str.substring(0, i));
                else
                    sb.append(str.substring(last, i));
                sb.append(replacement);
                last = i + 1;
            }
            return (sb == null ? str : sb.append(str.substring(last, i)).toString());
        }

        private static String jsEncodeKey(String pname) {
            if (RESERVED_KEYWORDS.contains(pname))
                return '"' + pname + '"';
            for (int i = 0; i < pname.length(); i++) {
                char c = pname.charAt(i);
                if (c == '$' || c == '_' || (c >= 'a' && c <= 'z') || c >= 'A' && c <= 'Z')
                    continue;
                switch (Character.getType(c)) {
                    // note: not allowing unquoted escape sequences for now...
                    case Character.UPPERCASE_LETTER:
                    case Character.LOWERCASE_LETTER:
                    case Character.TITLECASE_LETTER:
                    case Character.MODIFIER_LETTER:
                    case Character.OTHER_LETTER:
                    case Character.LETTER_NUMBER:
                        continue;
                    case Character.NON_SPACING_MARK:
                    case Character.COMBINING_SPACING_MARK:
                    case Character.DECIMAL_DIGIT_NUMBER:
                    case Character.CONNECTOR_PUNCTUATION:
                        if (i > 0)  continue;
                }
                return '"' + pname + '"';
            }
            return pname;
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();  toString(sb, -1);  return sb.toString();
        }
        public String prettyPrint() {
            StringBuffer sb = new StringBuffer();  toString(sb, 0);  return sb.toString();
        }

        private static final int INDENT_SIZE = 2;
        private void toString(StringBuffer sb, int indent) {
            indent = indent < 0 ? -1 : indent + INDENT_SIZE;
            sb.append('{');
            boolean needNamespace = mNamespaces == null ? false : namespaceDeclarationNeeded("", mNamespaces.get("").toString());
            int size = mAttributes.size() + (needNamespace ? 1 : 0), lsize;
            if (size != 0) {
                int index = 0;
                for (Iterator it = mAttributes.entrySet().iterator(); it.hasNext(); index++) {
                    indent(sb, indent, true);
                    Map.Entry attr = (Map.Entry) it.next();
                    sb.append(jsEncodeKey((String) attr.getKey())).append(indent >= 0 ? ": " : ":");

                    Object value = attr.getValue();
                    if (value instanceof String)                  sb.append('"').append(jsEncode(value)).append('"');
                    else if (value instanceof JavaScriptElement)  ((JavaScriptElement) value).toString(sb, indent);
                    else if (value instanceof Element)            sb.append('"').append(jsEncode(value)).append('"');
                    else if (!(value instanceof List))            sb.append(value);
                    else {
                        sb.append('[');
                        if ((lsize = ((List) value).size()) > 0)
                            for (ListIterator lit = ((List) value).listIterator(); lit.hasNext(); ) {
                                int lindent = indent < 0 ? -1 : indent + INDENT_SIZE;
                                if (lsize > 1)
                                    indent(sb, lindent, true);
                                Element child = (Element) lit.next();
                                if (child instanceof JavaScriptElement)
                                    ((JavaScriptElement) child).toString(sb, lindent);
                                else
                                    sb.append('"').append(jsEncode(child)).append('"');
                                if (lit.nextIndex() != lsize)  sb.append(",");
                            }
                        sb.append(']');
                    }
                    if (index < size - 1)  sb.append(",");
                }
                if (needNamespace) {
                    indent(sb, indent, true);
                    sb.append(A_NAMESPACE).append(indent >= 0 ? ": \"" : ":\"").append(jsEncode(mNamespaces.get(""))).append('"');
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

        private static final String E_ATTRIBUTE = "a";
        private static final String A_ATTR_NAME = "n";
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
            public Element createElement(String name)  { return new XMLElement(name); }
            public Element createElement(QName qname)  { return new XMLElement(qname); }
        }

        public ElementFactory getFactory()  { return mFactory; }

        public Element addElement(String name) throws ContainerException { return addElement(new XMLElement(name)); }

        public Element addElement(QName qname) throws ContainerException { return addElement(new XMLElement(qname)); } 

        public Element addElement(Element elt) throws ContainerException {
            if (elt == null || elt.mParent == this)
                return elt;
            else if (elt.mParent != null)
                throw new ContainerException("element already has a parent");
            if (mText != null)
                throw new ContainerException("cannot add children to element containing text");

            if (mChildren == null)
                mChildren = new ArrayList<Element>();
            mChildren.add(elt);
            elt.mParent = this;
            return elt;
        }

        public Element setText(String content) throws ContainerException {
            if (content != null && !content.trim().equals("")) {
                if (mChildren != null)
                    throw new ContainerException("cannot set text on element with children");
            } else
                content = null;
            mText = content;
            return this;
        }

        public Element addAttribute(String key, String value, byte disposition) throws ContainerException {
            validateName(key);
            if (value == null)
                return this;
            else if (disposition == DISP_ELEMENT)
                addElement(E_ATTRIBUTE).addAttribute(A_ATTR_NAME, key).setText(value);
            else if (disposition == DISP_CONTENT)
                addElement(key).setText(value);
            else {
                if (mAttributes == null)
                    mAttributes = new HashMap<String, Object>();
                mAttributes.put(key, value);
            }
            return this;
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

        protected void detach(Element elt) throws ContainerException {
            super.detach(elt);
            if (mChildren != null) {
                mChildren.remove(elt);
                if (mChildren.size() == 0)
                    mChildren = null;
            }
        }

        public Element getOptionalElement(String name) {
            if (mChildren != null)
                for (Element elt : mChildren)
                    if (elt.getName().equals(name))
                        return elt;
            return null;
        }

        public Element getOptionalElement(QName qname) { 
            if (mChildren != null)
                for (Element elt : mChildren)
                    if (elt.getQName().equals(qname))
                        return elt;
            return null;
        }

        public Set<Attribute> listAttributes() {
            if (mAttributes == null || mAttributes.isEmpty())
                return Collections.emptySet();
            HashSet<Attribute> set = new HashSet<Attribute>();
            for (Map.Entry<String, Object> attr : mAttributes.entrySet())
                set.add(new Attribute(attr, this));
            return set;
        }

        public List<Element> listElements(String name) {
            if (mChildren == null)
                return Collections.emptyList();
            ArrayList<Element> list = new ArrayList<Element>();
            if (name == null || name.trim().equals(""))
                list.addAll(mChildren);
            else
                for (Iterator it = mChildren.iterator(); it.hasNext(); ) {
                    Element elt = (Element) it.next();
                    if (elt.getName().equals(name))
                        list.add(elt);
                }
            return list;
        }

        public String getText() { return (mText == null ? "" : mText); }

        public String getAttribute(String key, String defaultValue) {
            String result;
            if (mAttributes != null && (result = (String) mAttributes.get(key)) != null)
                return result;
            if (mChildren == null)
                return defaultValue;
            for (Iterator it = mChildren.iterator(); it.hasNext(); ) {
                Element elt = (Element) it.next();
                if (elt.getName().equals(key))
                    return elt.getText();
                else if (elt.getName().equals(E_ATTRIBUTE) && elt.getAttribute(A_ATTR_NAME, "").equals(key))
                    return elt.getText();
            }
            return defaultValue;
        }

        private String xmlEncode(String str, boolean escapeQuotes) {
            if (str == null)
                return "";
            StringBuffer sb = null;
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
                    sb = new StringBuffer(str.substring(0, i));
                else
                    sb.append(str.substring(last, i));
                sb.append(replacement);
                last = i + 1;
            }
            return (sb == null ? str : sb.append(str.substring(last, i)).toString());
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();  toString(sb, -1);  return sb.toString();
        }
        public String prettyPrint() {
            StringBuffer sb = new StringBuffer();  toString(sb, 0);  return sb.toString();
        }

        private static final int INDENT_SIZE = 2;
        private void toString(StringBuffer sb, int indent) {
            indent(sb, indent, indent > 0);
            // element's qualified name
            String qn = getQualifiedName();
            sb.append("<").append(qn);
            // element's attributes
            if (mAttributes != null)
                for (Map.Entry<String, Object> attr : mAttributes.entrySet())
                    sb.append(' ').append(attr.getKey()).append("=\"").append(xmlEncode((String) attr.getValue(), true)).append('"');
            // new namespaces defined on this element
            if (mNamespaces != null)
                for (Map.Entry<String, String> ns : mNamespaces.entrySet()) {
                    String prefix = ns.getKey();
                    String uri = ns.getValue();
                    if (namespaceDeclarationNeeded(prefix, uri))
                        sb.append(' ').append(A_NAMESPACE).append(prefix.equals("") ? "" : ":").append(prefix).append("=\"").append(xmlEncode(uri, true)).append('"');
                }
            // element content (children/text) and closing
            if (mChildren != null || mText != null) {
                sb.append('>');
                if (mChildren != null) {
                    for (Element child : mChildren)
                        if (child instanceof XMLElement)
                            ((XMLElement) child).toString(sb, indent < 0 ? -1 : indent + INDENT_SIZE);
                        else
                            sb.append(xmlEncode(child.toString(), false));
                    indent(sb, indent, true);
                } else
                    sb.append(xmlEncode(mText, false));
                sb.append("</").append(qn).append('>');
            } else
                sb.append("/>");
        }
    }


    public static void main(String[] args) throws ContainerException, SoapParseException {
        org.dom4j.Namespace bogusNS = org.dom4j.Namespace.get("bogus", "");
        QName qm = new QName("m", bogusNS);

        com.zimbra.soap.SoapProtocol proto = com.zimbra.soap.SoapProtocol.SoapJS;
        Element env = new JavaScriptElement(proto.getEnvelopeQName());
        env.addUniqueElement(proto.getBodyQName()).addUniqueElement(com.zimbra.cs.service.mail.MailService.GET_MSG_RESPONSE)
           .addUniqueElement(qm).addAttribute("id", 1115).addAttribute("f", "aw").addAttribute("t", "64,67").addAttribute("score", 0.953)
           .addAttribute("s", "Subject of the message has a \"\\\" in it", DISP_CONTENT).addAttribute("mid", "<kashdfgiai67r3wtuwfg@goo.com>", DISP_CONTENT)
           .addElement("mp").addAttribute("part", "TEXT").addAttribute("ct", "multipart/mixed").addAttribute("s", 3718);
        System.out.println(env);
        System.out.println(Element.parseJSON(env.toString()).toString());

        proto = com.zimbra.soap.SoapProtocol.Soap12;
        env = new XMLElement(proto.getEnvelopeQName());
        env.addUniqueElement(proto.getBodyQName()).addUniqueElement(com.zimbra.cs.service.mail.MailService.GET_MSG_RESPONSE)
           .addUniqueElement(qm).addAttribute("id", 1115).addAttribute("f", "aw").addAttribute("t", "64,67").addAttribute("score", 0.953)
           .addAttribute("s", "Subject of the message has a \"\\\" in it", DISP_CONTENT).addAttribute("mid", "<kashdfgiai67r3wtuwfg@goo.com>", DISP_CONTENT)
           .addElement("mp").addAttribute("part", "TEXT").addAttribute("ct", "multipart/mixed").addAttribute("s", 3718);
        System.out.println(env.prettyPrint());
        System.out.println("             name: " + env.getName());
        System.out.println("   qualified name: " + env.getQualifiedName());
        System.out.println("            qname: " + env.getQName());

        Element e = new JavaScriptElement(com.zimbra.cs.service.mail.MailService.GET_CONTACTS_RESPONSE);
        e.addElement("cn");
        e.addElement("cn").addAttribute("id", 256).addAttribute("md", 1111196674000L).addAttribute("l", 7).addAttribute("x", false)
         .addAttribute("workPhone", "(408) 973-0500 x112", DISP_ELEMENT).addAttribute("notes", "These are &\nrandom notes", DISP_ELEMENT)
         .addAttribute("firstName", "Ross \"Killa\"", DISP_ELEMENT).addAttribute("lastName", "Dargahi", DISP_ELEMENT);
        e.addElement("cn").addAttribute("id", 257).addAttribute("md", 1111196674000L).addAttribute("l", 7)
         .addAttribute("workPhone", "(408) 973-0500 x111", DISP_ELEMENT).addAttribute("jobTitle", "CEO", DISP_ELEMENT)
         .addAttribute("firstName", "Satish", DISP_ELEMENT).addAttribute("lastName", "Dharmaraj", DISP_ELEMENT);
        System.out.println(e);
        System.out.println(e.prettyPrint());
        System.out.println(Element.parseJSON(e.toString()).toString());

        e = new XMLElement(com.zimbra.cs.service.mail.MailService.GET_CONTACTS_RESPONSE);
        e.addElement("cn");
        e.addElement("cn").addAttribute("id", 256).addAttribute("md", 1111196674000L).addAttribute("l", 7).addAttribute("x", false)
         .addAttribute("workPhone", "(408) 973-0500 x112", DISP_ELEMENT).addAttribute("notes", "These are &\nrandom notes", DISP_ELEMENT)
         .addAttribute("firstName", "Ross \"Killa\"", DISP_ELEMENT).addAttribute("lastName", "Dargahi", DISP_ELEMENT);
        e.addElement("cn").addAttribute("id", 257).addAttribute("md", 1111196674000L).addAttribute("l", 7)
         .addAttribute("workPhone", "(408) 973-0500 x111", DISP_ELEMENT).addAttribute("jobTitle", "CEO", DISP_ELEMENT)
         .addAttribute("firstName", "Satish", DISP_ELEMENT).addAttribute("lastName", "Dharmaraj", DISP_ELEMENT);
        System.out.println(e);

//        System.out.println(com.zimbra.soap.SoapProtocol.toString(e.toXML(), true));
        System.out.println(new XMLElement("test").setText("  this\t    is\nthe\rway ").getTextTrim() + "|");
        System.out.println(Element.parseJSON("{part:\"TEXT\",t:null,h:true,i:\"false\",\"ct\":\"\\x25multipart\\u0025\\/mixed\",\\u0073:3718}").toString());
    }
}
