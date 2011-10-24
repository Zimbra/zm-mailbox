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

/**
 * For Zimbra SOAP, Booleans are represented as "0" and "1" in Xml which differs from the default "true" and "false".
 * Boolean's are therefore annotated with @XmlJavaTypeAdapter(BooleanAdapter.class) to get the historical Zimbra
 * values for Xml.  Unfortunately, for JSON, the historical values are "true" and "false", so we need to over-ride
 * the over-ride to get back to "true" and "false"!
 */
public class BooleanSerializer extends JsonSerializer<Boolean> {
    
    public BooleanSerializer() {
        super();
    }

    @Override
    public void serialize(Boolean value, JsonGenerator jgen, SerializerProvider provider)
    throws IOException, JsonProcessingException {
        if (value == null) {
            return;
        }
        jgen.writeBoolean(value);
    }
}
