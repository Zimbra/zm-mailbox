/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014 Zimbra, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
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
