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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.google.common.base.Objects;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.StringUtil;

@XmlAccessorType(XmlAccessType.NONE)
public class Misspelling {

    /**
     * @zm-api-field-tag misspelled-word
     * @zm-api-field-description Misspelled word
     */
    @XmlAttribute(name=MailConstants.A_WORD /* word */, required=true)
    private final String word;

    /**
     * @zm-api-field-tag comma-sep-suggestions
     * @zm-api-field-description Comma separated list of suggestions.  Suggested words are listed in decreasing order
     * of their match score.
     */
    @XmlAttribute(name=MailConstants.A_SUGGESTIONS /* suggestions */, required=false)
    private final String suggestions;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private Misspelling() {
        this((String) null, (String) null);
    }

    public Misspelling(String word, String suggestions) {
        this.word = word;
        this.suggestions = suggestions;
    }

    public String getWord() { return word; }
    public String getSuggestions() { return suggestions; }

    /**
     * Returns the list of suggestions, or an empty list.
     */
    public List<String> getSuggestionsList() {
        if (suggestions == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(StringUtil.getCachedPattern(",").split(suggestions));
    }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("word", word)
            .add("suggestions", suggestions);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
