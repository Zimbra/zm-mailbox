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

package com.zimbra.soap.type;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

import com.google.common.base.Objects;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.zclient.ZClientException;
import com.zimbra.soap.base.KeyAndValue;

/**
 *  e.g. For element name "a":
 *         <a n="{key}">{value}</a>
 * Note:  where the attribute name is "name" rather than "n" use "Attr"
 */
public class KeyValuePair implements KeyAndValue {

    /**
     * @zm-api-field-tag key
     * @zm-api-field-description Key
     */
    @XmlAttribute(name=AdminConstants.A_N, required=true)
    private String key;

    /**
     * @zm-api-field-tag value
     * @zm-api-field-description Value
     */
    @XmlValue
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
        Multimap<String, String> map = ArrayListMultimap.create();
        if (keyValuePairs != null) {
            for (KeyValuePair a : keyValuePairs) {
                map.put(a.getKey(), a.getValue());
            }
        }
        return map;
    }

    public static List<KeyValuePair> fromMultimap(
                    Multimap<String, String> keyValuePairMap) {
        List<KeyValuePair> keyValuePairs = new ArrayList<KeyValuePair>();
        if (keyValuePairMap != null) {
            for (Map.Entry<String, String> entry : keyValuePairMap.entries()) {
                keyValuePairs.add(
                        new KeyValuePair(entry.getKey(), entry.getValue()));
            }
        }
        return keyValuePairs;
    }

    public static List <KeyValuePair> fromMap(
                    Map<String, ? extends Object> keyValuePairs)
    throws ServiceException {
        List<KeyValuePair> newKeyValuePairs = Lists.newArrayList();
        if (keyValuePairs == null) return newKeyValuePairs;

        for (Entry<String, ? extends Object> entry : keyValuePairs.entrySet()) {
            String key = (String) entry.getKey();
            Object value = entry.getValue();
            if (value == null) {
                newKeyValuePairs.add(new KeyValuePair(key, (String) null));
            } else if (value instanceof String) {
                newKeyValuePairs.add(new KeyValuePair(key, (String) value));
            } else if (value instanceof String[]) {
                String[] values = (String[]) value;
                if (values.length == 0) {
                    // an empty array == removing the keyValuePair
                    newKeyValuePairs.add(new KeyValuePair(key, (String) null));
                } else {
                    for (String v: values) {
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

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("key", key)
            .add("value", value);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
