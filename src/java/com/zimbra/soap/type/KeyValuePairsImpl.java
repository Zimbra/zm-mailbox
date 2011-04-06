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
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.util.StringUtil;

/*
 * Used for JAXB objects representing elements which have child node(s) of form:
 *     <a n="{key}">{value}</a>
 */
@XmlAccessorType(XmlAccessType.FIELD)
abstract public class KeyValuePairsImpl implements KeyValuePairs {

    @XmlElement(name=AdminConstants.E_A)
    private List<KeyValuePair> keyValuePairs = Lists.newArrayList();

    public KeyValuePairsImpl() {
        this.setKeyValuePairs((Iterable<KeyValuePair>) null);
    }

    public KeyValuePairsImpl(Iterable<KeyValuePair> keyValuePairs) {
        this.setKeyValuePairs(keyValuePairs);
    }

    public KeyValuePairsImpl (Map<String, ? extends Object> keyValuePairs)
    throws ServiceException {
        this.setKeyValuePairs(keyValuePairs);
    }

    public KeyValuePairs setKeyValuePairs(
                    Iterable<KeyValuePair> keyValuePairs) {
        this.keyValuePairs.clear();
        if (keyValuePairs != null) {
            Iterables.addAll(this.keyValuePairs, keyValuePairs);
        }
        return this;
    }

    public KeyValuePairs setKeyValuePairs(
                    Map<String, ? extends Object> keyValuePairs)
    throws ServiceException {
        this.setKeyValuePairs(KeyValuePair.fromMap(keyValuePairs));
        return this;
    }

    public KeyValuePairs addKeyValuePair(KeyValuePair keyValuePair) {
        keyValuePairs.add(keyValuePair);
        return this;
    }

    public List<KeyValuePair> getKeyValuePairs() {
        return Collections.unmodifiableList(keyValuePairs);
    }

    public Multimap<String, String> getKeyValuePairsMultimap() {
        return KeyValuePair.toMultimap(keyValuePairs);
    }

    public Map<String, Object> getKeyValuePairsAsOldMultimap() {
        return StringUtil.toOldMultimap(getKeyValuePairsMultimap());
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("keyValuePairs", keyValuePairs)
            .toString();
    }
}
