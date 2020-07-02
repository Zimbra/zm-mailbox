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

package com.zimbra.soap.type;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.zclient.ZClientException;
import com.zimbra.soap.base.KeyAndValue;

import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.types.GraphQLType;

/**
 *  e.g. For element name "a":
 *         <a n="{key}">{value}</a>
 * Note:  where the attribute name is "name" rather than "n" use "Attr"
 */
@GraphQLType(name="KeyValuePair")
public class KeyValuePair implements KeyAndValue {

    /**
     * @zm-api-field-tag key
     * @zm-api-field-description Key
     */
    @XmlAttribute(name=AdminConstants.A_N, required=true)
    @GraphQLQuery(name="key", description="Key")
    private String key;

    /**
     * @zm-api-field-tag value
     * @zm-api-field-description Value
     */
    @XmlValue
    @GraphQLQuery(name="value", description="Value")
    private String value;

    /**
     * no-argument constructor wanted by JAXB
     */
    protected KeyValuePair() {
        this.key = null;
        this.value = null;
    }

    public KeyValuePair(KeyValuePair keyValuePair) {
        this.key = keyValuePair.getKey();
        this.value = keyValuePair.getValue();
    }

    public KeyValuePair(String key, String value) {
        this.key = key;
        this.value = value;
    }


    @Override
    public void setKey(String key) { this.key = key; }
    @Override
    public void setValue(String value) { this.value = value; }

    @Override
    public String getKey() { return key; }
    @Override
    public String getValue() { return value; }

    public static Multimap<String, String> toMultimap(
                    List<KeyValuePair> keyValuePairs) {
        final Multimap<String, String> map = ArrayListMultimap.create();
        if (keyValuePairs != null) {
            for (final KeyValuePair a : keyValuePairs) {
                map.put(a.getKey(), a.getValue());
            }
        }
        return map;
    }

    public static List<KeyValuePair> fromMultimap(
                    Multimap<String, String> keyValuePairMap) {
        final List<KeyValuePair> keyValuePairs = new ArrayList<KeyValuePair>();
        if (keyValuePairMap != null) {
            for (final Map.Entry<String, String> entry : keyValuePairMap.entries()) {
                keyValuePairs.add(
                        new KeyValuePair(entry.getKey(), entry.getValue()));
            }
        }
        return keyValuePairs;
    }

    public static List <KeyValuePair> fromMap(
                    Map<String, ? extends Object> keyValuePairs)
    throws ServiceException {
        final List<KeyValuePair> newKeyValuePairs = Lists.newArrayList();
        if (keyValuePairs == null) return newKeyValuePairs;

        for (final Entry<String, ? extends Object> entry : keyValuePairs.entrySet()) {
            final String key = entry.getKey();
            final Object value = entry.getValue();
            if (value == null) {
                newKeyValuePairs.add(new KeyValuePair(key, (String) null));
            } else if (value instanceof String) {
                newKeyValuePairs.add(new KeyValuePair(key, (String) value));
            } else if (value instanceof String[]) {
                final String[] values = (String[]) value;
                if (values.length == 0) {
                    // an empty array == removing the keyValuePair
                    newKeyValuePairs.add(new KeyValuePair(key, (String) null));
                } else {
                    for (final String v: values) {
                        newKeyValuePairs.add(new KeyValuePair(key, v));
                    }
                }
            } else {
                throw ZClientException.CLIENT_ERROR(
                        "invalid value type: " + key + " "
                        + value.getClass().getName(), null);
            }
        }
        return newKeyValuePairs;
    }

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("key", key)
            .add("value", value);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
