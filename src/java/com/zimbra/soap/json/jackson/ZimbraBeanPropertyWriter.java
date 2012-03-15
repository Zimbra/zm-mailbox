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
package com.zimbra.soap.json.jackson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import javax.xml.namespace.QName;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.BeanPropertyWriter;
import org.codehaus.jackson.map.ser.impl.PropertySerializerMap;

import com.zimbra.soap.base.KeyAndValue;
import com.zimbra.soap.type.ZmBoolean;

/**
 * Used by {@link ZimbraBeanSerializerModifier} to handle some Zimbra JSON specific ways of processing JAXB object
 * fields.
 *
 * @XmlElementWrapper handling:
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
 * @XmlElements handling:
 *     @XmlElements({
 *         @XmlElement(name="alternative1", type=Alt1.class),
 *         @XmlElement(name="alternative2", type=Alt2.class)
 *     })
 *     private List<Alt> alts = Lists.newArrayList();
 *
 *     Jackson with the JaxbAnnotationIntrospector wraps this in an array field called "alts".  Zimbra JSON
 *     does not.
 */
public class ZimbraBeanPropertyWriter
    extends BeanPropertyWriter
{
    protected final NameInfo nameInfo;

    public ZimbraBeanPropertyWriter(BeanPropertyWriter wrapped, NameInfo nameInfo) {
        super(wrapped);
        this.nameInfo = nameInfo;
        // super-class SHOULD copy this, but just in case it didn't (as was the case with 1.8.0 and 1.8.1):
        if (_includeInViews == null) {
            _includeInViews = wrapped.getViews();
        }
    }

    public ZimbraBeanPropertyWriter(BeanPropertyWriter wrapped, NameInfo nameInfo, JsonSerializer<Object> serializer) {
        super(wrapped, serializer);
        this.nameInfo = nameInfo;
        // super-class SHOULD copy this, but just in case it didn't (as was the case with 1.8.0 and 1.8.1):
        if (_includeInViews == null) {
            _includeInViews = wrapped.getViews();
        }
    }

    @Override
    public BeanPropertyWriter withSerializer(JsonSerializer<Object> ser) {
        // sanity check to ensure sub-classes override...
        if (getClass() != ZimbraBeanPropertyWriter.class) {
            throw new IllegalStateException("Sub-class does not override 'withSerializer()'; needs to!");
        }
        return new ZimbraBeanPropertyWriter(this, nameInfo, ser);
    }

    /**
     * Overridden version so that we can wrap output within wrapper element if
     * and as necessary.
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
        // For non-nulls, first: simple check for direct cycles
        if (value == bean) {
            _reportSelfReference(bean);
        }
        if (_suppressableValue != null && _suppressableValue.equals(value)) {
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

        if (_typeSerializer != null) {
            // If we are using a specific serializer, then assume it is fully functional already
            jgen.writeFieldName(_name);
            ser.serializeWithType(value, jgen, prov, _typeSerializer);
            return;
        }

        /**
         * Zimbra uses special treatment for "Key Value pairs".  The interface KeyAndValue is used to identify
         * a pair that should be treated in this way.
         */
        boolean isKeyAndValue = false;
        if (value instanceof ArrayList) {
            ArrayList<?> al = (ArrayList<?>) value;
            if (al.size() > 0) {
                Object o = al.get(0);
                isKeyAndValue = (o instanceof KeyAndValue);
            }
            isKeyAndValue = false;
        }

        QName wrapperName = nameInfo.getWrapperName();
        if (isKeyAndValue) {
            jgen.writeFieldName(_name);
        } else {
            if (wrapperName != null) {
                jgen.writeFieldName(wrapperName.getLocalPart());
                // Zimbra wraps the wrapper inside an array
                jgen.writeStartArray();
                jgen.writeStartObject();
            }
            QName wrappedName = nameInfo.getWrappedName();
            // "_attrs" is the name used for Zimbra KeyValuePairs
            if ("_attrs".equals(_name)) {
                jgen.writeFieldName(_name);
            } else if (wrappedName != null) {
                jgen.writeFieldName(wrappedName.getLocalPart());
                // @XmlElement or @XmlElementRef
                // Zimbra wraps values inside an array even if they are not in an array
                if (!(value instanceof ArrayList)) {
                    if (value.getClass().getPackage().getName().startsWith("com.zimbra.soap")) {
                        if (value instanceof ZmBoolean) {
                            // If an @XmlElement applies to an element then normally ZmBooleanContentSerializer
                            // will have been applied.  If that hasn't happened then this should be treated as an
                            // attribute in JSON - hence don't wrap inside an array.
                            ZmBoolean zmbVal = (ZmBoolean) value;
                            if ((zmbVal.equals(ZmBoolean.TRUE)) || (zmbVal.equals(ZmBoolean.ONE))) {
                                jgen.writeObject(ZmBoolean.TRUE);
                            } else {
                                jgen.writeObject(ZmBoolean.FALSE);
                            }
                        } else {
                            jgen.writeStartArray();
                            jgen.writeObject(value);
                            jgen.writeEndArray();
                        }
                        finishWrapping(wrapperName, jgen);
                        return;
                    }
                }
            } else {
                Map<Class<?>, QName> nameMap = nameInfo.getWrappedNameMap();
                if ((nameMap != null) && (value instanceof ArrayList)) {
                    // @XmlElements or @XmlElementRefs
                    // Different names for different objects.
                    // For default Jackson Xml serialization, the name associated with the array is used
                    // as a wrapper but for Zimbra serialization, we don't use this wrapper.  There MAY
                    // be another wrapper but that will have been handled by "wrapperName" already
                    ArrayList<?> al = (ArrayList<?>) value;
                    for (Object obj : al) {
                        QName qn = nameMap.get(obj.getClass());
                        jgen.writeFieldName(qn.getLocalPart());
                        jgen.writeStartArray();
                        jgen.writeObject(obj);
                        jgen.writeEndArray();
                    }
                    finishWrapping(wrapperName, jgen);
                    return;
                } else {
                    jgen.writeFieldName(_name);
                }
            }
        }

        ser.serialize(value, jgen, prov);

        finishWrapping(wrapperName, jgen);
    }

    private void finishWrapping(QName wrapperName, JsonGenerator jgen)
    throws JsonGenerationException, IOException {
        if (wrapperName != null) {
            jgen.writeEndObject();
            jgen.writeEndArray();
        }
    }
}
