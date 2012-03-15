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
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.Misspelling;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_CHECK_SPELLING_RESPONSE)
public class CheckSpellingResponse {

    /**
     * @zm-api-field-tag available
     * @zm-api-field-description The "available" attribute specifies whether the server-side spell checking
     * interface is available or not.
     */
    @XmlAttribute(name=MailConstants.A_AVAILABLE /* available */, required=true)
    private ZmBoolean available;

    /**
     * @zm-api-field-description Information for misspelled words
     */
    @XmlElement(name=MailConstants.E_MISSPELLED /* misspelled */, required=false)
    private List<Misspelling> misspelledWords = Lists.newArrayList();

    private CheckSpellingResponse() {
    }

    private CheckSpellingResponse(boolean available) {
        setAvailable(available);
    }

    public static CheckSpellingResponse createForAvailable(boolean available) {
        return new CheckSpellingResponse(available);
    }

    public void setAvailable(boolean available) { this.available = ZmBoolean.fromBool(available); }
    public void setMisspelledWords(Iterable<Misspelling> misspelledWords) {
        this.misspelledWords.clear();
        if (misspelledWords != null) {
            Iterables.addAll(this.misspelledWords,misspelledWords);
        }
    }

    public CheckSpellingResponse addMisspelledWord(Misspelling misspelledWord) {
        this.misspelledWords.add(misspelledWord);
        return this;
    }

    public boolean isAvailable() { return ZmBoolean.toBool(available); }

    public List<Misspelling> getMisspelledWords() {
        return Collections.unmodifiableList(misspelledWords);
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("available", available)
            .add("misspelledWords", misspelledWords);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
