/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.soap.json;

import java.io.IOException;
import java.io.StringWriter;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchema;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import com.google.common.base.Strings;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.JSONElement;
import com.zimbra.common.soap.SoapParseException;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.soap.json.jackson.ZimbraJsonModule;
import com.zimbra.soap.json.jackson.ZmPairAnnotationIntrospector;
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

    /**
     * Note that currently does NOT use Jackson to go from JSON to JAXB to Element.
     * {@code SoapEngine.dispatch} uses Element.parseJSON(in) to parse a SOAP API command.
     * @param json - a String in JSON format
     * @param qn
     * @param obj - a JAXB object associated with the {@code json} argument
     */
    public static Element jacksonJsonToElement(String json, org.dom4j.QName qn, Object obj)
    throws SoapParseException {
        if (obj == null)
            return null;
        if (qn != null) {
            return Element.parseJSON(json, qn, JSONElement.mFactory);
        }
        return Element.parseJSON(json);
    }

    /**
     * Makes a best efforts guess to determine the name/namespace associated with the object
     * and produces a JSONElement tree from the supplied JSON string
     * @param json - a String in JSON format
     * @param obj - a JAXB object associated with the {@code json} argument
     * @return a JSONElement corresponding to the {@code json} argument
     */
    public static Element jacksonJsonToElement(String json, Object obj)
    throws ServiceException {
        if (obj == null)
            return null;
        try {
            return jacksonJsonToElement(json, getElementName(obj), obj);
        } catch (SoapParseException e) {
            throw ServiceException.FAILURE("SoapParseException", e);
        }
    }

    private static <T> T jsonToJaxb(ObjectMapper mapper, String json, Class<T> klass) {
        try {
            return mapper.readValue(json, klass);
        } catch (IOException e) {
            LOG.warn("Problem deserializing json string '%s' to class '%s' - returning null", json, klass, e);
        }
        return null;
    }

    /**
     * Uses a JSON format similar to that used by the SOAP API but more compact in most circumstances.
     * Only use where compatibility with the SOAP API is not required.
     *
     * WARNING: The serialization work for the SOAP API concentrated on serialization, and continues
     * to leverage the Element API for deserialization.  This code may work for some objects
     * but not necessarily all.  Needs testing before using for all objects you may encounter for new uses.
     */
    public static <T> T jsonToJaxbNonSoapApi(String json, Class<T> klass) {
        return jsonToJaxb(JacksonUtil.getCompactNonSoapObjectMapper(), json, klass);
    }

    /**
     * Uses a JSON format similar to that used by the SOAP API but more compact in most circumstances.
     * Only use where compatibility with the SOAP API is not required.
     * @param obj the soap JAXB object to be serialized
     * @return JSON string representing the object
     */
    public static String jaxbToJsonNonSoapApi(Object obj) throws ServiceException {
        return jaxbToJsonString(JacksonUtil.getCompactNonSoapObjectMapper(), obj);
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

    /**
     * Makes a best efforts guess at the name and namespace associated with a JAXB object.  Note that
     * typically only classes with an {@link XmlRootElement} annotation have an authoritative associated
     * {@code QName}
     * @param obj - a JAXB object
     * @return best guess {@code QName} associated with the {@code obj} argument
     */
    private static org.dom4j.QName getElementName(Object obj) {
        if (obj == null)
            return null;
        String ns = null;
        String elementName = null;
        XmlRootElement root = obj.getClass().getAnnotation(XmlRootElement.class);
        if (root != null) {
           elementName = root.name();
           if (!root.namespace().equals(JaxbInfo.DEFAULT_MARKER))
                ns = root.namespace();
        }
        if (Strings.isNullOrEmpty(ns)) {
            // Didn't get a valid namespace from XmlRootElement - see if there is one associated with the package
            XmlSchema schem = obj.getClass().getPackage().getAnnotation(XmlSchema.class);
            if (schem != null) {
                ns = schem.namespace();
            }
        }
        if (elementName == null) {
            // Didn't get a valid element name from XmlRootElement - use the last part of the class name
            elementName = obj.getClass().getName();
            if (elementName != null) {
                int pos = elementName.lastIndexOf('.');
                if (pos > 0) {
                    elementName = elementName.substring(pos + 1);
                }
            }
        }
        org.dom4j.QName qn = null;
        if (elementName != null) {
            org.dom4j.Namespace dom4jNS = org.dom4j.Namespace.get("", ns);
            qn = new org.dom4j.QName(elementName, dom4jNS);
        }
        return qn;
    }

    /**
     * We use our own annotation introspector which is based on a pair of annotation introspectors so that we can
     * handle both JAXB annotations and some Jackson annotations
     */
    private static AnnotationIntrospector getZimbraIntrospector() {
        return new ZmPairAnnotationIntrospector();
    }

    public static ObjectMapper getObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setAnnotationIntrospector(getZimbraIntrospector());
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.enable(MapperFeature.USE_WRAPPER_NAME_AS_PROPERTY_NAME);
        ZimbraJsonModule zimbraModule = new ZimbraJsonModule();
        // Doesn't appear to work. ZimbraBeanPropertyWriter uses this directly instead.
        // zimbraModule.addSerializer(org.w3c.dom.Element.class, new ZmDomElementJsonSerializer());
        mapper.registerModule(zimbraModule);
        return mapper;
    }

    public static ObjectMapper getWrapRootObjectMapper() {
        ObjectMapper mapper = getObjectMapper();
        // Enable this next line to get everything wrapped with the name of the root element.
        mapper.enable(SerializationFeature.WRAP_ROOT_VALUE);
        return mapper;
    }

    /**
     * Use with care.  Serialization/Deserialization using this may differ from what is used in the Zimbra SOAP
     * API and in particular, may not work for deserialization for some objects as that has not been extensively
     * used and functionality is likely missing for some objects.
     */
    public static ObjectMapper getCompactNonSoapObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setAnnotationIntrospector(AnnotationIntrospector.pair(
                new JacksonAnnotationIntrospector(), new JaxbAnnotationIntrospector(TypeFactory.defaultInstance())));
        return mapper;
    }
}
