/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2018 Synacor, Inc.
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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.zimbra.common.util.ZimbraLog;
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
public class ZmBooleanDeSerializer extends StdDeserializer<ZmBoolean> {

    public ZmBooleanDeSerializer() {
        this(null);
    }

    public ZmBooleanDeSerializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public ZmBoolean deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {
        JsonNode node = jp.getCodec().readTree(jp);
        if (node instanceof BooleanNode) {
            return ZmBoolean.fromBool(node.booleanValue());
        }
        return null;
    }
}
