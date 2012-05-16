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

import java.util.List;

import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.introspect.AnnotatedMember;
import org.codehaus.jackson.map.introspect.BasicBeanDescription;
import org.codehaus.jackson.map.ser.BeanPropertyWriter;
import org.codehaus.jackson.map.ser.BeanSerializer;
import org.codehaus.jackson.map.ser.BeanSerializerModifier;
import org.codehaus.jackson.map.JsonSerializer;

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
            BasicBeanDescription beanDesc, List<BeanPropertyWriter> beanProperties)
    {
        AnnotationIntrospector intr = config.getAnnotationIntrospector();
        for (int i = 0, len = beanProperties.size(); i < len; ++i) {
            BeanPropertyWriter bpw = beanProperties.get(i);
            final AnnotatedMember member = bpw.getMember();
            NameInfo nameInfo = new NameInfo(intr, member, bpw.getName());
            if (! nameInfo.needSpecialHandling()) {
                continue;
            }
            beanProperties.set(i, new ZimbraBeanPropertyWriter(bpw, nameInfo));
        }
        return beanProperties;
    }

    @Override
    public JsonSerializer<?> modifySerializer(SerializationConfig config,
            BasicBeanDescription beanDesc, JsonSerializer<?> serializer)
    {
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
