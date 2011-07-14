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

package com.zimbra.soap.mail.type;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.soap.type.BaseQueryInfo;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {})
public class SpellingSuggestionsQueryInfo implements BaseQueryInfo {

    @XmlAttribute(name="word", required=true)
    private final String word;

    @XmlElement(name="sug", required=false)
    private List<SpellingSuggestion> suggestions = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private SpellingSuggestionsQueryInfo() {
        this((String) null);
    }

    public SpellingSuggestionsQueryInfo(String word) {
        this.word = word;
    }

    public void setSuggestions(Iterable <SpellingSuggestion> suggestions) {
        this.suggestions.clear();
        if (suggestions != null) {
            Iterables.addAll(this.suggestions,suggestions);
        }
    }

    public SpellingSuggestionsQueryInfo addSuggestion(
                            SpellingSuggestion suggestion) {
        this.suggestions.add(suggestion);
        return this;
    }

    public String getWord() { return word; }
    public List<SpellingSuggestion> getSuggestions() {
        return Collections.unmodifiableList(suggestions);
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("word", word)
            .add("suggestions", suggestions);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
