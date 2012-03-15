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

package com.zimbra.soap.account.type;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.util.StringUtil;
import com.zimbra.soap.type.KeyValuePair;
import com.zimbra.soap.type.KeyValuePairs;
import com.zimbra.soap.json.jackson.KeyAndValueListSerializer;

/*
 * Used for JAXB objects representing elements which have child node(s) of form:
 *     <a n="{key}">{value}</a>
 */
@XmlAccessorType(XmlAccessType.NONE)
public class AccountKeyValuePairs implements KeyValuePairs {

    /**
     * @zm-api-field-description Attributes specified as key value pairs
     */
    @JsonSerialize(using=KeyAndValueListSerializer.class)
    @JsonProperty("_attrs")
    @XmlElement(name=AccountConstants.E_A)
    private List<KeyValuePair> keyValuePairs;

    public AccountKeyValuePairs() {
    }

    public AccountKeyValuePairs(Iterable<KeyValuePair> keyValuePairs) {
        setKeyValuePairs(keyValuePairs);
    }

    public AccountKeyValuePairs (Map<String, ? extends Object> keyValuePairs)
    throws ServiceException {
        setKeyValuePairs(keyValuePairs);
    }

    public void setKeyValuePairs(
                    List<KeyValuePair> keyValuePairs) {
        if (this.keyValuePairs == null) {
            this.keyValuePairs = Lists.newArrayList();
        }
        this.keyValuePairs.clear();
        if (keyValuePairs != null) {
            Iterables.addAll(this.keyValuePairs, keyValuePairs);
        }
    }

    @Override
    public List<KeyValuePair> getKeyValuePairs() {
        if (keyValuePairs == null) {
            keyValuePairs = Lists.newArrayList();
        }
        // Making the return of this unmodifiable causes
        // "UnsupportedOperationException" on unmarshalling - see Bug 62187.
        //     return Collections.unmodifiableList(keyValuePairs);
        return keyValuePairs;
    }

    @Override
    public void setKeyValuePairs(Iterable<KeyValuePair> keyValues) {
        if (this.keyValuePairs == null) {
            this.keyValuePairs = Lists.newArrayList();
        }
        this.keyValuePairs.clear();
        if (keyValues != null) {
            Iterables.addAll(this.keyValuePairs, keyValues);
        }
    }

    @Override
    public void setKeyValuePairs(Map<String, ? extends Object> keyValues)
            throws ServiceException {
        this.setKeyValuePairs(KeyValuePair.fromMap(keyValues));
    }

    @Override
    public void addKeyValuePair(KeyValuePair keyValue) {
        if (this.keyValuePairs == null) {
            this.keyValuePairs = Lists.newArrayList();
        }
        keyValuePairs.add(keyValue);
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
}
