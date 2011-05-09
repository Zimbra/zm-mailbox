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
package com.zimbra.cs.util;

import java.nio.ByteBuffer;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.smile.SmileFactory;

import me.prettyprint.cassandra.serializers.AbstractSerializer;

/**
 * Hector serializer/deserializer for binary JSON data.
 *
 * @see http://wiki.fasterxml.com/SmileFormatSpec
 * @author ysasaki
 */
public final class SmileSerializer<T> extends AbstractSerializer<T> {
    private static final ObjectMapper MAPPER = new ObjectMapper(new SmileFactory());
    static {
        MAPPER.getSerializationConfig().setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
        MAPPER.configure(JsonGenerator.Feature.QUOTE_FIELD_NAMES, false);
        MAPPER.configure(SerializationConfig.Feature.AUTO_DETECT_FIELDS, false);
        MAPPER.configure(SerializationConfig.Feature.AUTO_DETECT_GETTERS, false);
        MAPPER.configure(SerializationConfig.Feature.AUTO_DETECT_IS_GETTERS, false);
        MAPPER.configure(SerializationConfig.Feature.USE_ANNOTATIONS, true);
        MAPPER.configure(DeserializationConfig.Feature.AUTO_DETECT_CREATORS, false);
        MAPPER.configure(DeserializationConfig.Feature.AUTO_DETECT_FIELDS, false);
        MAPPER.configure(DeserializationConfig.Feature.AUTO_DETECT_SETTERS, false);
        MAPPER.configure(DeserializationConfig.Feature.USE_ANNOTATIONS, true);
    }
    private final Class<T> clazz;

    public SmileSerializer(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public ByteBuffer toByteBuffer(T obj) {
        if (obj == null) {
            return null;
        }
        try {
            return ByteBuffer.wrap(MAPPER.writeValueAsBytes(obj));
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to serialize", e);
        }
    }

    @Override
    public T fromByteBuffer(ByteBuffer bytes) {
        if (bytes == null || !bytes.hasRemaining()) {
            return null;
        }
        try {
            return MAPPER.readValue(bytes.array(), bytes.position(), bytes.remaining(), clazz);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to deserialize", e);
        }
    }

}
