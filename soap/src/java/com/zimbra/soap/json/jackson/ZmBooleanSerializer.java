/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.zimbra.soap.type.ZmBoolean;

/**
 * For Zimbra SOAP, Historically Booleans have been represented as "0" for false and "1" for true in XML.
 * This is valid but differs from the default values JAXB marshals to - "true" and "false".
 *
 * Some legacy client code cannot accept the values "true" and "false", so the ZmBoolean class has been introduced
 * whose values will always marshal to either "0" or "1".
 *
 * However, for JSON SOAP, the values true and false need to be used.  This serializer is responsible for ensuring
 * that happens.
 */
public class ZmBooleanSerializer extends JsonSerializer<ZmBoolean> {
    
    public ZmBooleanSerializer() {
        super();
    }

    @Override
    public void serialize(ZmBoolean value, JsonGenerator jgen, SerializerProvider provider)
    throws IOException, JsonProcessingException {
        if (value == null) {
            return;
        }
        jgen.writeBoolean(ZmBoolean.toBool(value));
    }
}
