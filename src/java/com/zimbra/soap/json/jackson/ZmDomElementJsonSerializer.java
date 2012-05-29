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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.std.SerializerBase;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

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
        jgen.writeStartArray();
        serializeInner(value, jgen, provider, null /* parent namespaceURI */);
        jgen.writeEndArray();
    }

    private void serializeInner(Element value, JsonGenerator jgen, SerializerProvider provider, String parentNs)
    throws IOException, JsonGenerationException {
        String namespaceURI = value.getNamespaceURI();
        jgen.writeStartObject();
        NamedNodeMap attributes = value.getAttributes();
        if (attributes != null && attributes.getLength() > 0) {
            for (int i = 0; i < attributes.getLength(); i++) {
                Attr attribute = (Attr) attributes.item(i);
                if ("xmlns".equals(attribute.getName())) {
                    if (!attribute.getValue().startsWith("urn:zimbra")) {
                        jgen.writeStringField(attribute.getName(), attribute.getValue());
                    }
                } else {
                    jgen.writeStringField(attribute.getName(), attribute.getValue());
                }
                // Not supporting attributes in different namespaces
            }
        }

        Map<String,List<Element>> elemMap = Maps.newTreeMap();
        NodeList children = value.getChildNodes();
        if (children != null && children.getLength() > 0) {
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                switch (child.getNodeType()) {
                    case Node.CDATA_SECTION_NODE:
                    case Node.TEXT_NODE:
                        String txt = child.getNodeValue();
                        if ((txt == null) || ((txt.startsWith("\n")) && (txt.trim().length() == 0))) {
                            break;  // Almost certainly only formatting text
                        }
                        jgen.writeStringField(JSONElement.A_CONTENT /* "_content" */, txt);
                        break;
                    case Node.ELEMENT_NODE:
                        Element elem = (Element) child;
                        String eleName = elem.getLocalName();
                        List<Element> elems = elemMap.get(eleName);
                        if (elems == null) {
                            elems = Lists.newArrayList();
                            elemMap.put(eleName, elems);
                        }
                        elems.add(elem);
                        break;
                }
            }
            for (Entry<String, List<Element>> entry : elemMap.entrySet()) {
                jgen.writeArrayFieldStart(entry.getKey());
                for (Element elem : entry.getValue()) {
                    serializeInner(elem, jgen, provider, namespaceURI);
                }
                jgen.writeEndArray();
            }
        }

        if ((namespaceURI != null) && (!namespaceURI.equals(parentNs))) {
            if (!namespaceURI.startsWith("urn:zimbra")) {
                jgen.writeStringField(JSONElement.A_NAMESPACE, namespaceURI);
            }
        }
        jgen.writeEndObject();
    }
}
