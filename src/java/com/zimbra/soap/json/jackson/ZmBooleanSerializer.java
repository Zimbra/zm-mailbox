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

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;

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
