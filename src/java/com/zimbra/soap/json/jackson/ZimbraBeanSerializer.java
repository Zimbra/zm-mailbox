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
package com.zimbra.soap.json.jackson;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.BeanSerializer;

import com.zimbra.common.soap.Element;

/**
 * Zimbra specific BeanSerializer
 */
public class ZimbraBeanSerializer extends BeanSerializer {
    public ZimbraBeanSerializer(BeanSerializer src) {
        super(src);
    }

    /**
     * Based on {@code BeanSerializer.serialize} but allows the addition of Zimbra's namespace property "_jsns"
     * to the list of properties serialized.
     */
    public final void serializeWithNamespace(Object bean, JsonGenerator jgen, SerializerProvider provider,
            String namespace)
    throws IOException, JsonGenerationException {
        jgen.writeStartObject();
        if (_propertyFilterId != null) {
            serializeFieldsFiltered(bean, jgen, provider);
        } else {
            serializeFields(bean, jgen, provider);
        }
        if (namespace != null) {
            jgen.writeStringField(Element.JSONElement.A_NAMESPACE /* _jsns */, namespace);
        }
        jgen.writeEndObject();
    }
}
