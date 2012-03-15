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

/**
 * JsonSerializer to be used when have a wrapped list of String where in old style Element tree creation,
 * the strings were added in a way similar to:
 *     response.addAttribute(AccountConstants.E_SOAP_URL, httpSoap, Element.Disposition.CONTENT);
 * For JAXB objects, the corresponding field would have an @XmlElement annotation and be serialized to Xml
 * as an element.  However, as seen below, these are serialized to JSON as attributes.
 * This differs from the treatment if the element had been added using:
 *     response.addElement(AccountConstants.E_SOAP_URL, httpSoap)
 *
 * Without this serializer, the default Jackson serialization with pair of introspectors :
 *     JacksonAnnotationIntrospector and JaxbAnnotationIntrospector
 * yields :
 *         "soapURL" : [ "http://ysasaki.local:7070/service/soap/" ]
 * To match old Element serialization behavior, we want :
 *         "soapURL" : "http://ysasaki.local:7070/service/soap/"
 * or, presumably :
 *         "soapURL" : [ { "http://ysasaki.local:7070/service/soap/" },
 *                       { "https://ysasaki.local:7070/service/soap/" } ]
 * Notes:
 *   .  If there is only one, we don't use the array format.
 */
public class StringListSerializer extends JsonSerializer<List<String>>{

    @Override
    public void serialize(List<String> stringList, JsonGenerator jgen,
            SerializerProvider provider) throws IOException,
            JsonProcessingException {
        if ((stringList == null) || (stringList.size() == 0)) {
            return;
        }
        if (stringList.size() == 1) {
            jgen.writeString(stringList.get(0));
        } else {
            jgen.writeStartArray();
            for (String str : stringList) {
                jgen.writeStartObject();
                jgen.writeString(str);
                jgen.writeEndObject();
            }
            jgen.writeEndArray();
        jgen.writeEndObject();
        }
    }
}
