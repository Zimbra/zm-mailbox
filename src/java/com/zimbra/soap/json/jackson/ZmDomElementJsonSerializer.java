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
import org.codehaus.jackson.map.ser.std.SerializerBase;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.zimbra.common.soap.Element.JSONElement;

public class ZmDomElementJsonSerializer
extends SerializerBase<Element>
{
    public ZmDomElementJsonSerializer() {
        super(Element.class);
    }

    @Override
    public void serialize(Element value, JsonGenerator jgen, SerializerProvider provider)
    throws IOException, JsonGenerationException {
        jgen.writeStartObject();
        NamedNodeMap attributes = value.getAttributes();
        if (attributes != null && attributes.getLength() > 0) {
            for (int i = 0; i < attributes.getLength(); i++) {
                Attr attribute = (Attr) attributes.item(i);
                jgen.writeStringField(attribute.getName(), attribute.getValue());
                // TODO: Not supporting attributes in different namespaces
            }
        }

        NodeList children = value.getChildNodes();
        if (children != null && children.getLength() > 0) {
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                switch (child.getNodeType()) {
                    case Node.CDATA_SECTION_NODE:
                    case Node.TEXT_NODE:
                        jgen.writeStringField(JSONElement.A_CONTENT /* "_content" */, child.getNodeValue());
                        break;
                    case Node.ELEMENT_NODE:
                        Element elem = (Element) child;
                        jgen.writeFieldName(elem.getLocalName());
                        serialize(elem, jgen, provider);
                        break;
                }
            }
        }
        if (value.getNamespaceURI() != null) {
            jgen.writeStringField(JSONElement.A_NAMESPACE, value.getNamespaceURI());
        }
        jgen.writeEndObject();
    }
}
