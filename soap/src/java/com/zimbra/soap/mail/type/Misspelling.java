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

package com.zimbra.soap.mail.type;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.google.common.base.MoreObjects;
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

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("word", word)
            .add("suggestions", suggestions);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
