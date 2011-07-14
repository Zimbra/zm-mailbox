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

import java.util.List;
import java.util.Collections;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;

/**
 * Helper class used to create JAXB classes representing similar data types
 * in different XML namespaces.  Note that no fields or methods have
 * annotations to represent elements or attributes.
 * JAXB subclasses should include a method with signature:
 *      @XmlElement(name=AdminConstants.E_A)
 *      public void setKeyValuePairs(List<KeyValuePair> keyValuePairs);
 *
 * Used for JAXB objects representing elements which have child node(s) of form:
 *     <a n="{key}">{value}</a>
 */
@XmlAccessorType(XmlAccessType.NONE)
abstract public class KeyValuePairsBase implements KeyValuePairs {

    private List<KeyValuePair> keyValuePairs = Lists.newArrayList();

    public KeyValuePairsBase() {
        this.setKeyValuePairs((Iterable<KeyValuePair>) null);
    }

    public KeyValuePairsBase(Iterable<KeyValuePair> keyValuePairs) {
        this.setKeyValuePairs(keyValuePairs);
    }

    public KeyValuePairsBase (Map<String, ? extends Object> keyValuePairs)
    throws ServiceException {
        this.setKeyValuePairs(keyValuePairs);
    }

    @Override
    public KeyValuePairs setKeyValuePairs(
                    Iterable<KeyValuePair> keyValuePairs) {
        this.keyValuePairs.clear();
        if (keyValuePairs != null) {
            Iterables.addAll(this.keyValuePairs, keyValuePairs);
        }
        return this;
    }

    @Override
    public KeyValuePairs setKeyValuePairs(
                    Map<String, ? extends Object> keyValuePairs)
    throws ServiceException {
        this.setKeyValuePairs(KeyValuePair.fromMap(keyValuePairs));
        return this;
    }

    @Override
    public KeyValuePairs addKeyValuePair(KeyValuePair keyValuePair) {
        keyValuePairs.add(keyValuePair);
        return this;
    }

    @Override
    public List<KeyValuePair> getKeyValuePairs() {
        return Collections.unmodifiableList(keyValuePairs);
    }

    @Override
    public Multimap<String, String> getKeyValuePairsMultimap() {
        return KeyValuePair.toMultimap(keyValuePairs);
    }

    @Override
    public Map<String, Object> getKeyValuePairsAsOldMultimap() {
        return StringUtil.toOldMultimap(getKeyValuePairsMultimap());
    }

    /**
     * Returns the first value matching {@link key} or null if {@link key} not found.
     */
    @Override
    public String firstValueForKey(String key) {
        for (KeyValuePair kvp : keyValuePairs) {
            if (key.equals(kvp.getKey())) {
                return kvp.getValue();
            }
        }
        return null;
    }

    @Override
    public List<String> valuesForKey(String key) {
        List<String> values = Lists.newArrayList();
        for (KeyValuePair kvp : keyValuePairs) {
            if (key.equals(kvp.getKey())) {
                values.add(kvp.getValue());
            }
        }
        return Collections.unmodifiableList(values);
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("keyValuePairs", keyValuePairs);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
