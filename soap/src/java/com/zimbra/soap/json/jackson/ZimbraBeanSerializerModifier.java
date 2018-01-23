/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import java.util.List;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializer;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;


/**
 * We need a {@link BeanSerializerModifier} to replace default <code>BeanSerializer</code>
 * with Zimbra-specific one; mostly to ensure that @XmlElementWrapper wrapped lists
 * are handled in the Zimbra way.
 */
public class ZimbraBeanSerializerModifier extends BeanSerializerModifier
{
    /*
    /**********************************************************
    /* Overridden methods
    /**********************************************************
     */
    
    /**
      * First thing to do is to find annotations regarding XML serialization,
      * and wrap collection serializers.
      */
    @Override
    public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
        BeanDescription beanDesc, List<BeanPropertyWriter> beanProperties) {
        AnnotationIntrospector intr = config.getAnnotationIntrospector();
        for (int i = 0, len = beanProperties.size(); i < len; ++i) {
            BeanPropertyWriter bpw = beanProperties.get(i);
            final AnnotatedMember member = bpw.getMember();
            NameInfo nameInfo = new NameInfo(intr, member, bpw.getName());
            if (!nameInfo.needSpecialHandling()) {
                continue;
            }
            beanProperties.set(i, new ZimbraBeanPropertyWriter(bpw, nameInfo));
        }
        return beanProperties;
    }

    @Override
    public JsonSerializer<?> modifySerializer(SerializationConfig config, BeanDescription beanDesc,
        JsonSerializer<?> serializer) {
        /* First things first: we can only handle real BeanSerializers; question
         * is, what to do if it's not one: throw exception or bail out?
         * For now let's do latter.
         */
        if (!(serializer instanceof BeanSerializer)) {
            return serializer;
        }
        return new ZimbraBeanSerializer((BeanSerializer) serializer);
    }


}
