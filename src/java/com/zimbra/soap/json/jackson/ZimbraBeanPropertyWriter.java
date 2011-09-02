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

import java.util.ArrayList;

import javax.xml.namespace.QName;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.BeanPropertyWriter;
import org.codehaus.jackson.map.ser.impl.PropertySerializerMap;

import com.zimbra.soap.base.KeyAndValue;

/**
 * Used by {@link ZimbraBeanSerializerModifier} to handle the Zimbra JSON way of handling
 * XmlElementWrapper.
 *
 * Jackson with the JaxbAnnotationIntrospector does understand XmlElementWrapper BUT treats
 * it as a property name, discarding the wrapped element name.  For Zimbra JSON we retain
 * the wrapper and use the wrapped element name as the property name for the wrapped property.
 */
public class ZimbraBeanPropertyWriter
    extends BeanPropertyWriter
{
    /**
     * Element name used as wrapper for collection.
     */
    protected final QName wrapperName;

    /**
     * Element name used for items in the collection
     */
    protected final QName wrappedName;

    // TODO: Support something similar to :
    //     @XmlElementWrapper(name=AccountConstants.E_DATA_SOURCES)
    //     @XmlElements({
    //         @XmlElement(name=MailConstants.E_DS_POP3, type=AccountPop3DataSource.class),
    //         @XmlElement(name=MailConstants.E_DS_IMAP, type=AccountImapDataSource.class),
    //         @XmlElement(name=MailConstants.E_DS_RSS, type=AccountRssDataSource.class),
    //         @XmlElement(name=MailConstants.E_DS_CAL, type=AccountCalDataSource.class)
    //     })
    //     private List<AccountDataSource> dataSources = Lists.newArrayList();
    //
    // Requires a map to stand in place of wrappedName to map classes to names

    public ZimbraBeanPropertyWriter(BeanPropertyWriter wrapped, QName wrapperName, QName wrappedName) {
        super(wrapped);
        this.wrapperName = wrapperName;
        this.wrappedName = wrappedName;
        // super-class SHOULD copy this, but just in case it didn't (as was the case with 1.8.0 and 1.8.1):
        if (_includeInViews == null) {
            _includeInViews = wrapped.getViews();
        }
    }

    public ZimbraBeanPropertyWriter(BeanPropertyWriter wrapped, QName wrapperName, QName wrappedName,
            JsonSerializer<Object> serializer) {
        super(wrapped, serializer);
        this.wrapperName = wrapperName;
        this.wrappedName = wrappedName;
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
        return new ZimbraBeanPropertyWriter(this, wrapperName, wrappedName, ser);
    }

    /**
     * Overridden version so that we can wrap output within wrapper element if
     * and as necessary.
     */
    @Override
    public void serializeAsField(Object bean, JsonGenerator jgen, SerializerProvider prov)
    throws Exception {
        Object value = get(bean);
        /* TODO: Hmmh. Does the default null serialization work ok here? For now let's assume
         * it does; can change later if not.
         */
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

        // Ok then; addition we want to do is to add wrapper element, and that's what happens here
        JsonSerializer<Object> ser = _serializer;
        if (ser == null) {
            Class<?> cls = value.getClass();
            PropertySerializerMap map = _dynamicSerializers;
            ser = map.serializerFor(cls);
            if (ser == null) {
                ser = _findAndAddDynamic(map, cls, prov);
            }
        }

        boolean isKeyAndValue = false;
        if (value instanceof ArrayList) {
            ArrayList<?> al = (ArrayList<?>) value;
            if (al.size() > 0) {
                Object o = al.get(0);
                isKeyAndValue = (o instanceof KeyAndValue);
            }
            isKeyAndValue = false;
        }
        if (_typeSerializer != null) {
            // If we are using a specific serializer, then assume it is fully functional already
            jgen.writeFieldName(_name);
            ser.serializeWithType(value, jgen, prov, _typeSerializer);
            return;
        }

        if (isKeyAndValue) {
            jgen.writeFieldName(_name);
        } else {
            if (wrapperName != null) {
                jgen.writeFieldName("GrenWrapper-" + wrapperName.getLocalPart());
                jgen.writeStartObject();
            }
            if ((wrappedName != null) && (!"_attrs".equals(_name))) {
                jgen.writeFieldName("GrenWrapped-" + wrappedName.getLocalPart());
            } else {
                jgen.writeFieldName(_name);
            }
        }

        ser.serialize(value, jgen, prov);

        if (wrapperName != null) {
            jgen.writeEndObject();
        }
    }
}
