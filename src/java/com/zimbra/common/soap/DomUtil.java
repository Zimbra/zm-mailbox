/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009 Zimbra, Inc.
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
 * Created on May 26, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.zimbra.common.soap;

import java.io.IOException;
import java.io.StringWriter;

import org.dom4j.Element;
import org.dom4j.QName;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import com.zimbra.common.service.ServiceException;

/**
 * @author schemers
 */
public class DomUtil {

    public static int asInt(Element e) {
        return Integer.parseInt(e.getText());
    }

    public static long asLong(Element e) {
        return Long.parseLong(e.getText());
    }

    public static String asString(Element e) {
        return e.getText();
    }

    public static boolean asBoolean(Element e) {
        return e.getText().equals("1");
    }

    public static String getAttr(Element e, String name) throws ServiceException {
        String value = e.attributeValue(name);
        if (value == null)
            throw ServiceException.INVALID_REQUEST("missing required attribute: "+name, null);
        return value;
    }

    public static long getAttrLong(Element e, String name) throws ServiceException {
        String value = e.attributeValue(name);
        if (value == null)
            throw ServiceException.INVALID_REQUEST("missing required attribute: "+name, null);
        return Long.parseLong(value);
    }

    public static long getAttrLong(Element e, String name, long defaultValue) {
        String value = e.attributeValue(name);
        if (value == null)
            return defaultValue;
        return Long.parseLong(value);
    }

    public static String getAttr(Element e, String name, String defaultValue) {
        String value = e.attributeValue(name);
        if (value == null)
            return defaultValue;
        else
            return value;
    }

    public static boolean getAttrBoolean(Element e, String name) throws ServiceException {
        String value = e.attributeValue(name);
        if (value == null)
            throw ServiceException.INVALID_REQUEST("missing required attribute: "+name, null);
        return !value.equals("0");
    }

    public static boolean getAttrBoolean(Element e, String name, boolean defaultValue) {
        String value = e.attributeValue(name);
        if (value == null)
            return defaultValue;
        else
            return !value.equals("0");
    }
    
    public static Element get(Element parent, String name) throws ServiceException {
        Element e = parent.element(name);
        if (e == null)
            throw ServiceException.INVALID_REQUEST("missing required element: "+name, null);
        return e;
    }
    
    public static Element get(Element parent, QName name) throws ServiceException {
        Element e = parent.element(name);
        if (e == null)
            throw ServiceException.INVALID_REQUEST("missing required element: "+name, null);
        return e;
    }
    
    public static int getInt(Element parent, String name) throws ServiceException {
        return asInt(get(parent, name));
    }
    
    public static int getInt(Element parent, QName name) throws ServiceException {
        return asInt(get(parent, name));
    }
    
    public static int getInt(Element parent, String name, int defaultValue) {
        Element e = parent.element(name);
        if (e == null)
            return defaultValue;
        return asInt(e);
    }
    
    public static long getLong(Element parent, QName name) throws ServiceException {
        return asLong(get(parent, name));
    }
    
    public static long getLong(Element parent, String name) throws ServiceException {
        return asLong(get(parent, name));
    }
    
    public static long getLong(Element parent, String name, long defaultValue) {
        Element e = parent.element(name);
        if (e == null)
            return defaultValue;
        return asLong(e);
    }

    public static boolean getBoolean(Element parent, String name) throws ServiceException {
        return asBoolean(get(parent, name));
    }
    
    public static boolean getBoolean(Element parent, String name, boolean defaultValue) {
        Element e = parent.element(name);
        if (e == null)
            return defaultValue;
        return asBoolean(e);
    }
    
    public static String getString(Element parent, String name) throws ServiceException {
        Element e = parent.element(name);
        if (e == null)
            throw ServiceException.INVALID_REQUEST("missing required element: "+name, null);
        return e.getText();
    }
    
    public static String getString(Element parent, String name, String defaultValue) {
        Element e = parent.element(name);
        if (e == null)
            return defaultValue;
        return e.getText();
    }

    public static Element add(Element parent, QName name, int value) {
        return parent.addElement(name).addText(value+"");
    }
    
    public static Element add(Element parent, String name, int value) {
        return parent.addElement(name).addText(value+"");
    }
    
    public static Element add(Element parent, String name, long value) {
        return parent.addElement(name).addText(value+"");
    }
    
    public static Element add(Element parent, String name, String value) {
        return parent.addElement(name).addText(value);
    }
    
    public static Element add(Element parent, String name, boolean value) {
        return parent.addElement(name).addText(value ? "1" : "0");
    }

    public static Element addAttr(Element e, String name, boolean value) {
        return e.addAttribute(name, value ? "1" : "0");
    }
    
    public static Element addAttr(Element e, String name, long value) {
        return e.addAttribute(name, Long.toString(value));
    }
    
    public static Element addAttr(Element e, String name, int value) {
        return e.addAttribute(name, Integer.toString(value));
    }
    
    public static Element addAttr(Element e, String name, float value) {
        return e.addAttribute(name, Float.toString(value));
    }
    
    public static Element addAttr(Element e, String name, String value) {
        return e.addAttribute(name, value);
    }
    
    /** Convert an Element to a String. */
    public static String toString(Element env, boolean prettyPrint) {
        if (prettyPrint) {
            StringWriter buff = new StringWriter();
            try {
                OutputFormat format = OutputFormat.createPrettyPrint();
                XMLWriter writer = new XMLWriter(buff, format);
                writer.write(env);
                writer.close();
            } catch (IOException e) {
                // ignore, since StringWriter doesn't throw IOExceptions
            }
            return buff.toString();
        } else {
            return env.asXML();
        }
    }

    /**
     * return the first child element in the specified element, or null
     * if it has no children.
     * @param e
     * @return
     */
    public static Element firstChild(Element e) {
        for (int i=0; i < e.nodeCount(); i++) {
            if (e.node(i) instanceof Element) 
                return (Element) e.node(i);
        }
        return null;
    }
}
