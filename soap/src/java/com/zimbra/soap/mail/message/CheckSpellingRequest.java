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

package com.zimbra.soap.mail.message;

import com.google.common.base.MoreObjects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

import com.zimbra.common.soap.MailConstants;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description Check spelling.
 * <br />
 * Suggested words are listed in decreasing order of their match score.  The "available" attribute specifies whether
 * the server-side spell checking interface is available or not.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_CHECK_SPELLING_REQUEST)
public class CheckSpellingRequest {

    /**
     * @zm-api-field-tag aspell-dictionary-name
     * @zm-api-field-description The optional name of the <b>aspell</b> dictionary that will be used to check spelling.
     * If not specified, the the dictionary will be either <b>zimbraPrefSpellDictionary</b> or the one for the
     * account's locale, in that order.
     */
    @XmlAttribute(name=MailConstants.A_DICTIONARY /* dictionary */, required=false)
    private final String dictionary;

    // Comma separated
    /**
     * @zm-api-field-tag comma-sep-ignore-words
     * @zm-api-field-description Comma-separated list of words to ignore just for this request.  These words are added
     * to the user's personal dictionary of ignore words stored as <b>zimbraPrefSpellIgnoreWord</b>.
     */
    @XmlAttribute(name=MailConstants.A_IGNORE /* ignore */, required=false)
    private final String ignoreList;

    /**
     * @zm-api-field-tag spell-check-text
     * @zm-api-field-description Text to spell check
     */
    @XmlValue
    private final String text;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private CheckSpellingRequest() {
        this((String) null, (String) null, (String) null);
    }

    public CheckSpellingRequest(String dictionary, String ignoreList, String text) {
        this.dictionary = dictionary;
        this.ignoreList = ignoreList;
        this.text = text;
    }

    public String getDictionary() { return dictionary; }
    public String getIgnoreList() { return ignoreList; }
    public String getText() { return text; }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("dictionary", dictionary)
            .add("ignoreList", ignoreList)
            .add("text", text);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
