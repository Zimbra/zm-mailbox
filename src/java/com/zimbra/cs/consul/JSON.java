/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.consul;

import java.io.IOException;

import org.codehaus.jackson.map.DeserializationConfig.Feature;
import org.codehaus.jackson.map.InjectableValues;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.JavaType;

/** JSON helpers for working with Consul */
class JSON {

    public static <T> T parse(String json, Class<T> klass) throws IOException {
        return parse(json, klass, new InjectableValues.Std());
    }

    public static <T> T parse(String json, JavaType javaType) throws IOException {
        return parse(json, javaType, new InjectableValues.Std());
    }

    public static <T> T parse(String json, Class<T> klass, InjectableValues injectables) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper.reader(klass).withInjectableValues(injectables).readValue(json);
    }

    public static <T> T parse(String json, JavaType javaType, InjectableValues injectables) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper.reader(javaType).withInjectableValues(injectables).readValue(json);
    }

    public static String stringify(Object obj) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(obj);
    }
}
