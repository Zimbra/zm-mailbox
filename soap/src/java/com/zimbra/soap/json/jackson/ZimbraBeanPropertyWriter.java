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
package com.zimbra.soap.json.jackson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.namespace.QName;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.PropertySerializerMap;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.JSONElement;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.base.KeyAndValue;
import com.zimbra.soap.util.JaxbInfo;

/**
 * Used by {@link ZimbraBeanSerializerModifier} to handle some Zimbra JSON specific ways of processing JAXB object
 * fields.
 *
 * {@link XmlElementWrapper} handling:
 *     Jackson with the JaxbAnnotationIntrospector does understand @XmlElementWrapper BUT treats
 *     it as a property name, discarding the wrapped element name.  For Zimbra JSON we retain
 *     the wrapper and use the wrapped element name as the property name for the wrapped property.
 *
 * Element values wrapped in arrays
 *     Jackson with the JaxbAnnotationIntrospector knows which fields are arrays and which are not and only
 *     uses arrays where necessary.  Zimbra JSON was originally designed as a serialization of a pure Element
 *     structure - and it typically isn't possible to determine the difference between a list of elements with just
 *     one element and a singleton.  Hence, Zimbra JSON often treats fields as arrays even when they are not. 
 *
 * {@link XmlElements} handling:
 * <pre>
 *     {@code @XmlElements({
 *         @XmlElement(name="alternative1", type=Alt1.class),
 *         @XmlElement(name="alternative2", type=Alt2.class)
 *     })}
 *     private List<Alt> alts = Lists.newArrayList();
 * </pre>
 *
 *     Jackson with the JaxbAnnotationIntrospector wraps this in an array field called "alts".  Zimbra JSON
 *     does not.
 *
 * Was some (non-functional) code here to handle Zimbra "Key Value pairs".  Ended up deciding that dedicated
 * serializers were more appropriate.
 */
public class ZimbraBeanPropertyWriter
    extends BeanPropertyWriter
{
    /**
     * 
     */
    private static final long serialVersionUID = 7679426432153926727L;

    private static final Log LOG = ZimbraLog.soap;

    protected final NameInfo nameInfo;
    protected QName wrapperName = null;
    protected QName wrappedName = null;


    public ZimbraBeanPropertyWriter(BeanPropertyWriter wrapped, NameInfo nameInfo) {
        super(wrapped);
        this.nameInfo = nameInfo;

    }

    public ZimbraBeanPropertyWriter(BeanPropertyWriter wrapped, NameInfo nameInfo, JsonSerializer<Object> serializer) {
        super(wrapped);
        _serializer = serializer;
        this.nameInfo = nameInfo;
    }

    
    @Override
    public void assignSerializer(JsonSerializer<Object> ser) {
        // sanity check to ensure sub-classes override...
        if (getClass() != ZimbraBeanPropertyWriter.class) {
            throw new IllegalStateException("Sub-class does not override 'withSerializer()'; needs to!");
        }
        _serializer = ser;
    }

    /**
     * Perform various Zimbra specific changes to how a field gets serialized:
     */
    @Override
    public void serializeAsField(Object bean, JsonGenerator jgen, SerializerProvider prov)
    throws Exception {
        Object value = get(bean);
        if (value == null) {
            if (!_suppressNulls) {
                jgen.writeFieldName(_name);
                prov.defaultSerializeNull(jgen);
            }
            return;
        }
        JsonSerializer<Object> ser = _serializer;
        if (ser == null) {
            Class<?> cls = value.getClass();
            PropertySerializerMap map = _dynamicSerializers;
            ser = map.serializerFor(cls);
            if (ser == null) {
                ser = _findAndAddDynamic(map, cls, prov);
            }
        }
        // For non-nulls, first: simple check for direct cycles
        if ((value == bean) && (_handleSelfReference(bean, jgen, prov, ser))) {
                return;
        }
        
        if (_suppressableValue != null && _suppressableValue.equals(value)) {
            return;
        }

        if (_typeSerializer != null) {
            // If we are using a specific serializer, then assume it is fully functional already
            jgen.writeFieldName(_name);
            ser.serializeWithType(value, jgen, prov, _typeSerializer);
            return;
        }

        wrapperName = nameInfo.getWrapperName();
        startWrapping(jgen);
        wrappedName = nameInfo.getWrappedName();
        if (wrappedName != null) {
                serializeInnerField(value, jgen, prov, ser);
        } else {
            /* No specific wrappedName */
            Map<Class<?>, QName> nameMap = nameInfo.getWrappedNameMap();
            if ((nameMap != null) && (value instanceof ArrayList)) {
                serializeXmlElementsArray((ArrayList<?>)value, jgen, prov, nameMap);
            } else if ((nameInfo.isKeyValuePairs()) && (value instanceof ArrayList)) {
                serializeZimbraKeyValuePairs((ArrayList<?>) value, jgen, prov);
            } else if (nameInfo.isAnyAttributeAllowed() && java.util.Map.class.isAssignableFrom(value.getClass())) {
                serializeXmlAnyAttributes((Map<?,?>) value, jgen, prov);
            } else {
                if (value instanceof ArrayList) {
                    serializeXmlAnyElementsArray((ArrayList<?>) value, jgen, prov);
                } else {
                    jgen.writeFieldName(_name);
                    ser.serialize(value, jgen, prov);
                }
            }
            finishWrapping(jgen);
        }
    }

    private void startWrapping(JsonGenerator jgen)
    throws JsonGenerationException, IOException {
        if (wrapperName != null) {
            jgen.writeFieldName(wrapperName.getLocalPart());
            if (nameInfo.isWrapperIsArray()) {
                jgen.writeStartArray();
            }
            jgen.writeStartObject();
        }
    }

    private void finishWrapping(JsonGenerator jgen)
    throws JsonGenerationException, IOException {
        if (wrapperName != null) {
            jgen.writeEndObject();
            addZimbraJsonNamespaceField(jgen, wrapperName);
            if (nameInfo.isWrapperIsArray()) {
                jgen.writeEndArray();
            }
        }
    }

    /**
     * Serialize a field for which we have a specific wrappedName - typically associated with {@link XmlElement}
     * or {@link XmlElementRef}.
     * Zimbra normally wraps values inside an array even if they are not in an array.
     * Assumes that any wrapping info has already been handled.
     */
    public void serializeInnerField(Object value, JsonGenerator jgen, SerializerProvider prov,
        JsonSerializer<Object> ser) throws JsonGenerationException, IOException {
        jgen.writeFieldName(wrappedName.getLocalPart());
        if (nameInfo.isTreatAsAttribute()) {
            ser.serialize(value, jgen, prov);
            return;
        }
        Class<?> valClass = value.getClass();
        if (JaxbUtil.isJaxbType(valClass)) {
            if (valClass.isEnum()) {
                serializeElementTextValue(value, jgen, prov, ser);
            } else {
                if (!nameInfo.isTreatAsUniqueElement()) {
                    jgen.writeStartArray();
                }
                if (ser instanceof ZimbraBeanSerializer) {
                    ZimbraBeanSerializer zser = (ZimbraBeanSerializer) ser;
                    zser.serializeWithNamespace(value, jgen, prov, namespace(wrappedName));
                } else {
                    ser.serialize(value, jgen, prov);
                }
                if (!nameInfo.isTreatAsUniqueElement()) {
                    jgen.writeEndArray();
                }
            }
            finishWrapping(jgen);
        } else {
            if (value instanceof List) {
                serializeXmlElementArray((List<?>) value, jgen, prov, ser);
            } else {
                serializeElementTextValue(value, jgen, prov, ser);
            }
            finishWrapping(jgen);
        }
    }

    /**
     * Zimbra JSON represents the text content of an element thus:
     * [{
     *     "_content": "text content"
     * }]
     * @throws IOException 
     * @throws JsonGenerationException 
     */
    private void serializeElementTextValue(Object value, JsonGenerator jgen, SerializerProvider prov,
            JsonSerializer<Object> ser)
    throws JsonGenerationException, IOException {
        if (!nameInfo.isTreatAsUniqueElement()) {
            jgen.writeStartArray();
        }
        jgen.writeStartObject();
        jgen.writeFieldName(JSONElement.A_CONTENT /* "_content" */);
        ser.serialize(value, jgen, prov);
        addZimbraJsonNamespaceField(jgen, wrappedName);
        jgen.writeEndObject();
        if (!nameInfo.isTreatAsUniqueElement()) {
            jgen.writeEndArray();
        }
    }

    /**
     * Serialize Array associated with {@code XmlElement} or {@code XmlElementRef} annotation which isn't part of
     * {@code XmlElements} or {@code XmlElementRefs
     */
    private void serializeXmlElementArray(List<?> values, JsonGenerator jgen, SerializerProvider prov,
            JsonSerializer<Object> ser) throws JsonGenerationException, IOException {
        if (values.isEmpty()) {
            ser.serialize(values, jgen, prov);
            return;
        }
        Class<?> firstClass = values.get(0).getClass();
        if (! firstClass.isEnum() && JaxbUtil.isJaxbType(firstClass)) {
            ser.serialize(values, jgen, prov);
            return;
        }
        jgen.writeStartArray();
        for (Object value : values) {
            jgen.writeStartObject();
            jgen.writeFieldName(JSONElement.A_CONTENT /* "_content" */);
            jgen.writeObject(value);
            jgen.writeEndObject();
        }
        addZimbraJsonNamespaceField(jgen, wrappedName);
        jgen.writeEndArray();
    }

    /**
     * Serialize Array associated with XmlElements or XmlElementRefs annotations
     * Different names for different objects.
     * For default Jackson Xml serialization, the name associated with the array is used as a wrapper but for
     * Zimbra serialization, we don't use this wrapper.  There MAY be another wrapper but that will have been handled
     * by "wrapperName" already
     */
    private void serializeXmlElementsArray(ArrayList<?> al, JsonGenerator jgen, SerializerProvider prov,
                Map<Class<?>, QName> nameMap)
    throws JsonGenerationException, IOException {
        for (Object obj : al) {
            Class<?> objClass = obj.getClass();
            QName qn = nameMap.get(objClass);
            if (qn == null) {
                if (org.w3c.dom.Element.class.isAssignableFrom(objClass)) {
                    // XmlAnyElement handling
                    org.w3c.dom.Element w3ce = (org.w3c.dom.Element) obj;
                    jgen.writeFieldName(w3ce.getLocalName());
                    JsonSerializer<org.w3c.dom.Element> eleSer = new ZmDomElementJsonSerializer();
                    eleSer.serialize(w3ce, jgen, prov);
                } else {
                    if (!nameInfo.isMixedAllowed()) {
                        LOG.debug("Unexpected '" + objClass.getName() + "' object in XmlElements(Refs) array - ignored");
                    } else {
                        jgen.writeFieldName(JSONElement.A_CONTENT /* "_content" */);
                        jgen.writeObject(obj);
                    }
                }
            } else {
                jgen.writeFieldName(qn.getLocalPart());
                jgen.writeStartArray();
                jgen.writeObject(obj);
                jgen.writeEndArray();
            }
        }
    }

    private void serializeZimbraKeyValuePairs(List<?> pairs, JsonGenerator jgen, SerializerProvider prov)
    throws JsonGenerationException, IOException {
        if (pairs == null) {
            return;
        }
        jgen.writeObjectFieldStart(Element.JSONElement.E_ATTRS /* _attrs */);
        for (Object obj : pairs) {
            Class<?> objClass = obj.getClass();
            if (KeyAndValue.class.isAssignableFrom(objClass)) {
                KeyAndValue pair = (KeyAndValue) obj;
                jgen.writeStringField(pair.getKey(), pair.getValue());
            } else {
                LOG.debug("Unexpected '" + objClass.getName() + "' object in @ZimbraKeyValuePairs array - ignored");
            }
        }
        jgen.writeEndObject();
    }

    private void serializeXmlAnyAttributes(Map<?,?> extraAttribs, JsonGenerator jgen, SerializerProvider prov)
    throws JsonGenerationException, IOException {
        for (Entry<?,?> attrib : extraAttribs.entrySet()) {
            jgen.writeStringField(attrib.getKey().toString(), attrib.getValue().toString());
        }
    }

    private void serializeXmlAnyElementsArray(ArrayList<?> al, JsonGenerator jgen, SerializerProvider prov)
    throws JsonGenerationException, IOException {
        for (Object obj : al) {
            Class<?> objClass = obj.getClass();
            if (org.w3c.dom.Element.class.isAssignableFrom(objClass)) {
                // XmlAnyElement handling
                org.w3c.dom.Element w3ce = (org.w3c.dom.Element) obj;
                jgen.writeFieldName(w3ce.getLocalName());
                JsonSerializer<org.w3c.dom.Element> eleSer = new ZmDomElementJsonSerializer();
                eleSer.serialize(w3ce, jgen, prov);
            } else {
                LOG.debug("Unexpected object of class '" + objClass.getName() + "' in XmlAnyElements array - ignored");
            }
        }
    }

    /**
     * Add e.g. :  "_jsns": "urn:zimbraAdmin"
     */
    private void addZimbraJsonNamespaceField(JsonGenerator jgen, QName qn)
    throws JsonGenerationException, IOException {
        String ns = namespace(qn);
        if (ns != null) {
            jgen.writeStringField(Element.JSONElement.A_NAMESPACE /* _jsns */, ns);
        }
    }

    private String namespace(QName qn) {
        String ns = null;
        if (qn != null && qn.getNamespaceURI() != null) {
            ns = qn.getNamespaceURI();
            if (ns.equals(JaxbInfo.DEFAULT_MARKER)) {
                ns = null;
            }
        }
        return ns;
    }
}
