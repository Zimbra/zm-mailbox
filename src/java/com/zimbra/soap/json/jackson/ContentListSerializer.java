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
import java.util.List;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;

import com.zimbra.common.soap.Element.JSONElement;
/**
 * JsonSerializer to be used for a list of Strings derived from elements where the elements were NOT
 * added as attributes of type CONTENT :
 * Without this, the default Jackson serialization with pair of introspectors :
 *     JacksonAnnotationIntrospector and JaxbAnnotationIntrospector
 * yields something like :
 *         "dlm" : [ "dlmember1@no.where", "dlmember2@no.where", "dlmember3@no.where" ]
 * To match old Element serialization behavior, we want :
 *         "dlm": [
 *           {
 *             "_content": "dlmember1@no.where"
 *           },
 *           {
 *             "_content": "dlmember2@no.where"
 *           },
 *           {
 *             "_content": "dlmember3@no.where"
 *           }],
 */
public class ContentListSerializer extends JsonSerializer<List<String>>{

    @Override
    public void serialize(List<String> strings, JsonGenerator jgen,
            SerializerProvider provider) throws IOException,
            JsonProcessingException {
        if (strings == null) {
            return;
        }
        jgen.writeStartArray();
        for (String str : strings) {
            jgen.writeStartObject();
            jgen.writeStringField(JSONElement.A_CONTENT /* "_content" */, str);
            jgen.writeEndObject();
        }
        jgen.writeEndArray();
    }
}
