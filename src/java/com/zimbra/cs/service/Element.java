/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
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
package com.zimbra.cs.service;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.*;

import org.dom4j.QName;

import com.zimbra.soap.SoapParseException;

/**
 * @author dkarp
 */
public abstract class Element {
    protected String  mName;
    protected String  mPrefix = "";
    protected Element mParent;
    protected Map     mAttributes;
    protected Map     mNamespaces;

    public static final byte DISP_ATTRIBUTE = 0;
    public static final byte DISP_CONTENT   = 1;
    public static final byte DISP_ELEMENT   = 2;

    public abstract ElementFactory getFactory();

    // writing to the element hierarchy
    public abstract Element addElement(String name) throws ContainerException;
    public Element addElement(QName qname) throws ContainerException        { return addElement(qname.getName()); }
    public abstract Element addElement(Element elt) throws ContainerException;

    public Element addUniqueElement(String name) throws ContainerException  { return addElement(name); }
    public Element addUniqueElement(QName qname) throws ContainerException  { return addElement(qname); }
    public Element addUniqueElement(Element elt) throws ContainerException  { return addElement(elt); }

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

    public abstract Set listAttributes();
    public List listElements()                    { return listElements(null); }
    public abstract List listElements(String name);

    public Iterator attributeIterator()           { return listAttributes().iterator(); }
    public Iterator elementIterator()             { return listElements().iterator(); }
    public Iterator elementIterator(String name)  { return listElements(name).iterator(); }

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

        Attribute(Map.Entry entry, Element parent)  { mKey = (String) entry.getKey(); mValue = entry.getValue(); mParent = parent; }
        public String getKey()              { return mKey; }
        public String getValue()            { return mValue.toString(); }
        public void setValue(String value)  { mParent.addAttribute(mKey, value); mValue = value; }
    }

    public static class JavaScriptElement extends Element {
        public static final ElementFactory mFactory = new JavaScriptFactory();
        
        private static final String E_ATTRS   = "_attrs";
        private static final String A_CONTENT = "_content";

        private static final HashSet RESERVED_KEYWORDS = new HashSet(Arrays.asList(new String[] {
                "abstract", "boolean", "break", "byte", "case", "catch", "char", "class", "continue",
                "const", "debugger", "default", "delete", "do", "double", "else", "extends", "enum", "export", 
                "false", "final", "finally", "float", "for", "function", "goto", "if", "implements", "import", "in",
                "instanceOf", "int", "interface", "label", "long", "native", "new", "null", "package", "private",
                "protected", "public", "return", "short", "static", "super", "switch", "synchronized", "this",
                "throw", "throws", "transient", "true", "try", "typeof", "var", "void", "volatile", "while", "with"
        }));

        public JavaScriptElement(String name)  { mName = name; mAttributes = new HashMap(); }
        public JavaScriptElement(QName qname)  { this(qname.getName()); }

        private static final class JavaScriptFactory implements ElementFactory {
            public Element createElement(String name)  { return new JavaScriptElement(name); }
            public Element createElement(QName qname)  { return new JavaScriptElement(qname); }
        }

        public ElementFactory getFactory()  { return mFactory; }

        public Element addElement(String name) throws ContainerException  { return addElement(new JavaScriptElement(name)); }

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

            List content = (List) obj;
            if (content == null) {
                content = new ArrayList();
                mAttributes.put(name, content);
            }
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

        public Set listAttributes() {
            if (mAttributes.isEmpty())
                return Collections.EMPTY_SET;
            HashSet list = new HashSet();
            for (Iterator it = mAttributes.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry attr = (Map.Entry) it.next();
                Object obj = attr.getValue();
                if (obj != null && !(obj instanceof Element || obj instanceof List))
                    list.add(new Attribute(attr, this));
            }
            return list;
        }

        public List listElements(String name) {
            if (mAttributes.isEmpty())
                return Collections.EMPTY_LIST;
            ArrayList list = new ArrayList();
            for (Iterator it = mAttributes.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry attr = (Map.Entry) it.next();
                if (name == null || name.equals(attr.getKey())) {
                    Object obj = attr.getValue();
                    if (obj instanceof Element)
                        list.add(obj);
                    else if (obj instanceof List)
                        list.addAll((List) obj);
                }
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

        public static Element parseText(InputStream is) throws SoapParseException {
            try {
                return parseText(new String(com.zimbra.cs.util.ByteUtil.getContent(is, -1), "utf-8"));
            } catch (SoapParseException e) {
                throw e;
            } catch (Exception e) {
                throw new SoapParseException("could not transcode request from utf-8", null);
            }
        }
        public static Element parseText(String js) throws SoapParseException {
            return parseElement(new JSRequest(js), com.zimbra.soap.SoapProtocol.SoapJS.getEnvelopeQName().getName());
        }
        private static Element parseElement(JSRequest jsr, String name) throws SoapParseException {
            Element elt = new JavaScriptElement(name);
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
                    case '{':  elt.addUniqueElement(parseElement(jsr, key));      break;
                    case '[':  jsr.skipChar();
                               do {
                                   elt.addElement(parseElement(jsr, key));
                                   switch (jsr.peekChar()) {
                                       case ']':  break;
                                       case ',':
                                       case ';':  jsr.skipChar();  break;
                                       default:   throw new SoapParseException("missing expected ',' or ']'", jsr.js);
                                   }
                               } while (jsr.peekChar() != ']');  jsr.skipChar();  break;
                    default:   if ((value = jsr.readValue()) == null)             break;
                               if (value instanceof Boolean)      elt.addAttribute(key, ((Boolean) value).booleanValue());
                               else if (value instanceof Long)    elt.addAttribute(key, ((Long) value).longValue());
                               else if (value instanceof Double)  elt.addAttribute(key, ((Double) value).doubleValue());
                               else                               elt.addAttribute(key, value.toString());
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

        private static final String[] JS_CHAR_ENCODINGS = {
            "\\u0000", "\\u0001", "\\u0002", "\\u0003", "\\u0004", "\\u0005", "\\u0006", "\\u0007",
            "\\b",     "\\t",     "\\n",     "\\u000B", "\\f",     "\\r",     "\\u000E", "\\u000F",
            "\\u0010", "\\u0011", "\\u0012", "\\u0013", "\\u0014", "\\u0015", "\\u0016", "\\u0017",
            "\\u0018", "\\u0019", "\\u001A", "\\u001B", "\\u001C", "\\u001D", "\\u001E", "\\u001F"
        };

        private static String jsEncode(String string) {
            if (string == null || string.length() == 0)
                return "\"\"";

            int len = string.length();
            StringBuffer sb = new StringBuffer(len + 4);
            sb.append('"');
            for (int i = 0; i < len; i += 1) {
                char c = string.charAt(i);
                switch (c) {
                    case '\\': case '"':
                        sb.append('\\').append(c);  break;
                    default:
                        if (c < ' ')  sb.append(JS_CHAR_ENCODINGS[c]);
                        else          sb.append(c);
                }
            }
            sb.append('"');
            return sb.toString();
        }

        public String prettyPrint()  { return toString(); }

        public String toString() {
            StringBuffer sb = new StringBuffer("{");
            int index = 0;
            for (Iterator it = mAttributes.entrySet().iterator(); it.hasNext(); index++) {
                Map.Entry attr = (Map.Entry) it.next();
                if (index > 0)
                    sb.append(",");
                String pname = (String) attr.getKey();
                if (RESERVED_KEYWORDS.contains(pname))
                    sb.append('"').append(pname).append("\":");
                else
                    sb.append(pname).append(":");
                if (attr.getValue() instanceof String) 
                    sb.append(jsEncode((String) attr.getValue()));
                else 
                    // take advantage of the fact that javascript array format == List.toString() format
                    sb.append(attr.getValue());                
            }
            return sb.append('}').toString();
        }
    }
    
    public static class XMLElement extends Element {
        private String mText;
        private List   mChildren;

        public static final ElementFactory mFactory = new XMLFactory();

        private static final String E_ATTRIBUTE = "a";
        private static final String A_ATTR_NAME = "n";
        private static final String A_NAMESPACE = "xmlns";

        public XMLElement(String name) { mName = name; }
        public XMLElement(QName qname) {
            mName = qname.getName();
            String uri = qname.getNamespaceURI();
            if (uri == null || uri.equals(""))
                return;
            mPrefix = qname.getNamespacePrefix();
            if (mNamespaces == null)
                mNamespaces = new HashMap();
            mNamespaces.put(mPrefix, uri);
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
                mChildren = new ArrayList();
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
            if (value == null)
                return this;
            else if (disposition == DISP_ELEMENT)
                addElement(E_ATTRIBUTE).addAttribute(A_ATTR_NAME, key).setText(value);
            else if (disposition == DISP_CONTENT)
                addElement(key).setText(value);
            else {
                if (mAttributes == null)
                    mAttributes = new HashMap();
                mAttributes.put(key, value);
            }
            return this;
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
                for (Iterator it = mChildren.iterator(); it.hasNext(); ) {
                    Element elt = (Element) it.next();
                    if (elt.getName().equals(name))
                        return elt;
                }
            return null;
        }

        public Element getOptionalElement(QName qname) { 
            if (mChildren != null)
                for (Iterator it = mChildren.iterator(); it.hasNext(); ) {
                    Element elt = (Element) it.next();
                    if (elt.getQName().equals(qname))
                        return elt;
                }
            return null;
        }

        public Set listAttributes() {
            if (mAttributes == null || mAttributes.isEmpty())
                return Collections.EMPTY_SET;
            HashSet set = new HashSet();
            for (Iterator it = mAttributes.entrySet().iterator(); it.hasNext(); )
                set.add(new Attribute((Map.Entry) it.next(), this));
            return set;
        }

        public List listElements(String name) {
            if (mChildren == null)
                return Collections.EMPTY_LIST;
            ArrayList list = new ArrayList();
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

        public static Element parseText(InputStream is) throws org.dom4j.DocumentException {
            return convertDOM(new org.dom4j.io.SAXReader().read(is).getRootElement());
        }
        public static Element parseText(String xml) throws org.dom4j.DocumentException {
            return convertDOM(org.dom4j.DocumentHelper.parseText(xml).getRootElement());
        }
        public static Element convertDOM(org.dom4j.Element d4root) {
            XMLElement elt = new XMLElement(d4root.getQName());
            for (Iterator it = d4root.attributeIterator(); it.hasNext(); ) {
                org.dom4j.Attribute d4attr = (org.dom4j.Attribute) it.next();
                elt.addAttribute(d4attr.getQualifiedName(), d4attr.getValue());
            }
            for (Iterator it = d4root.elementIterator(); it.hasNext(); ) {
                org.dom4j.Element d4elt = (org.dom4j.Element) it.next();
                elt.addElement(convertDOM(d4elt));
            }
            String content = d4root.getText();
            if (content != null && !content.trim().equals(""))
                elt.setText(content);
            return elt;
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

        private boolean namespaceDeclarationNeeded(String prefix, String uri) {
            if (mParent == null || !(mParent instanceof XMLElement))
                return true;
            String thatURI = ((XMLElement) mParent).getNamespaceURI(prefix);
            return (thatURI == null || !thatURI.equals(uri));
        }

        private static final String FORTY_SPACES = "                                        ";
        private void indent(StringBuffer sb, int indent, boolean newline) {
            if (indent < 0)      return;
            if (newline)         sb.append('\n');
            while (indent > 40)  { sb.append(FORTY_SPACES);  indent -= 40; }
            if (indent > 0)      sb.append(FORTY_SPACES.substring(40 - indent));
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
                for (Iterator it = mAttributes.entrySet().iterator(); it.hasNext(); ) {
                    Map.Entry attr = (Map.Entry) it.next();
                    sb.append(' ').append((String) attr.getKey()).append("=\"").append(xmlEncode((String) attr.getValue(), true)).append('"');
                }
            // new namespaces defined on this element
            if (mNamespaces != null)
                for (Iterator it = mNamespaces.entrySet().iterator(); it.hasNext(); ) {
                    Map.Entry ns = (Map.Entry) it.next();
                    String prefix = (String) ns.getKey();
                    String uri = (String) ns.getValue();
                    if (namespaceDeclarationNeeded(prefix, uri))
                        sb.append(' ').append(A_NAMESPACE).append(prefix.equals("") ? "" : ":").append(prefix).append("=\"").append(xmlEncode(uri, true)).append('"');
                }
            // element content (children/text) and closing
            if (mChildren != null || mText != null) {
                sb.append('>');
                if (mChildren != null) {
                    for (Iterator it = mChildren.iterator(); it.hasNext(); ) {
                        Element child = (Element) it.next();
                        if (child instanceof XMLElement) {
                            ((XMLElement) child).toString(sb, indent < 0 ? -1 : indent + INDENT_SIZE);
                        } else
                            sb.append(xmlEncode(child.toString(), false));
                    }
                    indent(sb, indent, true);
                } else
                    sb.append(xmlEncode(mText, false));
                sb.append("</").append(qn).append('>');
            } else
                sb.append("/>");
        }
    }


    public static void main(String[] args) throws ContainerException, SoapParseException {
        org.dom4j.Namespace soapNS = org.dom4j.Namespace.get("soap", "http://schemas.xmlsoap.org/soap/envelope/");
        QName qenv = new QName("Envelope", soapNS);
        QName qbody = new QName("Body", soapNS);
        org.dom4j.Namespace bogusNS = org.dom4j.Namespace.get("bogus", "");
        QName qm = new QName("m", bogusNS);

        Element env = new JavaScriptElement(qenv);
        env.addUniqueElement(qbody).addUniqueElement(com.zimbra.cs.service.mail.MailService.GET_MSG_RESPONSE)
           .addUniqueElement(qm).addAttribute("id", 1115).addAttribute("f", "aw").addAttribute("t", "64,67").addAttribute("score", 0.953)
           .addAttribute("s", "Subject of the message has a \"\\\" in it", DISP_CONTENT).addAttribute("mid", "<kashdfgiai67r3wtuwfg@goo.com>", DISP_CONTENT)
           .addElement("mp").addAttribute("part", "TEXT").addAttribute("ct", "multipart/mixed").addAttribute("s", 3718);
        System.out.println(env);
        System.out.println(JavaScriptElement.parseText(env.toString()).toString());

        env = new XMLElement(qenv);
        env.addUniqueElement(qbody).addUniqueElement(com.zimbra.cs.service.mail.MailService.GET_MSG_RESPONSE)
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
        System.out.println(JavaScriptElement.parseText(e.toString()).toString());

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
        System.out.println(JavaScriptElement.parseText("{part:\"TEXT\",t:null,h:true,i:\"false\",\"ct\":\"\\x25multipart\\u0025\\/mixed\",\\u0073:3718}").toString());
    }
}
