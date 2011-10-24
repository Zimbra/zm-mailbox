/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
package com.zimbra.soap.json;

import java.io.IOException;
import java.io.StringWriter;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchema;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.introspect.JacksonAnnotationIntrospector;
import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;

import com.google.common.base.Strings;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.JSONElement;
import com.zimbra.common.soap.SoapParseException;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.soap.json.jackson.ZimbraJsonModule;
import com.zimbra.soap.util.JaxbInfo;

public final class JacksonUtil {
    /* Prevent accidental construction */
    private JacksonUtil() { }

    private static final Log LOG = ZimbraLog.soap;
    /**
     *  e.g.
     *      calData = new AppointmentData(uid, uid);
     *      ...
     *      JacksonUtil.jaxbToJSONElement(calData, new QName(MailConstants.E_APPOINTMENT, MailConstants.NAMESPACE));
     *  is roughly equivalent to Element based code built on top of something like:
     *      JSONElement.mFactory.createElement(MailConstants.E_APPOINTMENT);
     *      ...
     */
    public static Element jaxbToJSONElement(Object obj, org.dom4j.QName qn)
    throws ServiceException {
        try {
            String json = jaxbToJsonString(JacksonUtil.getObjectMapper(), obj);
            return jacksonJsonToElement(json, qn, obj);
        } catch (IOException e) {
            throw ServiceException.FAILURE("IOException", e);
        }
    }

    public static Element jaxbToJSONElement(Object obj)
    throws ServiceException {
        return jaxbToJSONElement(obj, JacksonUtil.getElementName(obj));
    }

    public static Element jaxbToJSONElementOld(Object obj)
    throws ServiceException {
        String json = jaxbToJsonString(JacksonUtil.getWrapRootObjectMapper(), obj);
        return jacksonJsonToElement(json, obj);
    }

    public static JsonNode fromJaxb(Object obj) {
        ObjectMapper mapper = JacksonUtil.getObjectMapper();
        return mapper.valueToTree(obj);
    }

    public static Element jaxbToJSONElement(ObjectMapper mapper, Object obj)
    throws ServiceException {
        String json = jaxbToJsonString(mapper, obj);
        return jacksonJsonToElement(json, obj);
    }

    public static String jaxbToJsonString(ObjectMapper mapper, Object obj)
    throws ServiceException {
        try {
            StringWriter writer = new StringWriter();
            mapper.writeValue(writer, obj);
            writer.flush();
            writer.close();
            String jsonStr =  writer.toString();
            return jsonStr;
        } catch (IOException e) {
            throw ServiceException.FAILURE("IOException", e);
        }
    }

    public static Element jacksonJsonToElement(String json, org.dom4j.QName qn, Object obj)
    throws SoapParseException {
        if (obj == null)
            return null;
        if (qn != null) {
            return Element.parseJSON(json, qn, JSONElement.mFactory);
        }
        return Element.parseJSON(json);
    }

    public static org.dom4j.QName getElementName(Object obj) {
        if (obj == null)
            return null;
        org.dom4j.QName qn = null;
        String ns = null;
        String className = null;
        XmlRootElement root = obj.getClass().getAnnotation(XmlRootElement.class);
        if (root != null) {
           className = root.name();
           if (!root.namespace().equals(JaxbInfo.DEFAULT_MARKER))
                ns = root.namespace();
        }
        if (Strings.isNullOrEmpty(ns)) {
            XmlSchema schem = obj.getClass().getPackage().getAnnotation(XmlSchema.class);
            if (schem != null) {
                ns = schem.namespace();
            }
        }
        if (className == null) {
            className = obj.getClass().getName();
            if (className != null) {
                int pos = className.lastIndexOf('.');
                if (pos > 0) {
                    className = className.substring(pos + 1);
                }
            }
        }
        if (className != null) {
            org.dom4j.Namespace dom4jNS = org.dom4j.Namespace.get("", ns);
            qn = new org.dom4j.QName(className, dom4jNS);
        }
        return qn;
    }

    public static Element jacksonJsonToElement(String json, Object obj)
    throws ServiceException {
        if (obj == null)
            return null;
        org.dom4j.QName qn = null;
        // The JSON we create using Jackson does not (currently) create the special
        // namespace attribute with name "_jsns".  Even if it did, Element.parseJSON(String)
        // would currently NOT understand it.
        // Element.parseJSON(String, QName, ElementFactory) does use QName to set the correct namespace
        // information on the top level Element.
        XmlSchema schem = obj.getClass().getPackage().getAnnotation(XmlSchema.class);
        if (schem != null) {
            String ns = schem.namespace();
            if (!Strings.isNullOrEmpty(ns)) {
                XmlRootElement root = obj.getClass().getAnnotation(XmlRootElement.class);
                if (root != null) {
                    if (!root.namespace().equals(JaxbInfo.DEFAULT_MARKER))
                        ns = root.namespace();
                    org.dom4j.Namespace dom4jNS = org.dom4j.Namespace.get("", ns);
                    qn = new org.dom4j.QName(root.name(), dom4jNS);
                }
            }
        }
        try {
            return jacksonJsonToElement(json, qn, obj);
        } catch (SoapParseException e) {
            throw ServiceException.FAILURE("SoapParseException", e);
        }
    }

    public static ObjectMapper getObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Mostly, we use JAXB annotations but we do use some Jackson annotations e.g. @JsonSerialize
        // so use both annotation introspectors
        AnnotationIntrospector primary = new JacksonAnnotationIntrospector();
        AnnotationIntrospector secondary = new JaxbAnnotationIntrospector();
        AnnotationIntrospector pair = new AnnotationIntrospector.Pair(primary, secondary);
        mapper.setAnnotationIntrospector(pair);
        mapper.getSerializationConfig().set(SerializationConfig.Feature.INDENT_OUTPUT, true);
        ZimbraJsonModule zimbraModule = new ZimbraJsonModule();
        mapper.registerModule(zimbraModule);
        return mapper;
    }

    public static ObjectMapper getWrapRootObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Mostly, we use JAXB annotations but we do use some Jackson annotations e.g. @JsonSerialize
        // so use both annotation introspectors
        AnnotationIntrospector primary = new JacksonAnnotationIntrospector();
        AnnotationIntrospector secondary = new JaxbAnnotationIntrospector();
        AnnotationIntrospector pair = new AnnotationIntrospector.Pair(primary, secondary);
        mapper.setAnnotationIntrospector(pair);
        mapper.getSerializationConfig().set(SerializationConfig.Feature.INDENT_OUTPUT, true);
        // Enable this next line to get everything wrapped with the name of the root element.
        mapper.getSerializationConfig().set(SerializationConfig.Feature.WRAP_ROOT_VALUE, true);
        ZimbraJsonModule zimbraModule = new ZimbraJsonModule();
        mapper.registerModule(zimbraModule);
        return mapper;
    }
}
