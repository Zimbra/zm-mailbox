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

package com.zimbra.soap.mail.message;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_GET_SPELL_DICTIONARIES_RESPONSE)
public class GetSpellDictionariesResponse {

    /**
     * @zm-api-field-tag dictionaries
     * @zm-api-field-description Dictionaries
     */
    @XmlElement(name=MailConstants.E_DICTIONARY, required=false)
    private List<String> dictionaries = Lists.newArrayList();

    public GetSpellDictionariesResponse() {
    }

    public void setDictionaries(Iterable <String> dictionaries) {
        this.dictionaries.clear();
        if (dictionaries != null) {
            Iterables.addAll(this.dictionaries,dictionaries);
        }
    }

    public GetSpellDictionariesResponse addDictionary(String dictionary) {
        this.dictionaries.add(dictionary);
        return this;
    }

    public List<String> getDictionaries() {
        return Collections.unmodifiableList(dictionaries);
    }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("dictionaries", dictionaries);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
