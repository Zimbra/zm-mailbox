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
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=MailConstants.E_CHECK_SPELLING_REQUEST)
public class CheckSpellingRequest {

    @XmlAttribute(name=MailConstants.A_DICTIONARY, required=false)
    private final String dictionary;

    // Comma separated
    @XmlAttribute(name=MailConstants.A_IGNORE, required=false)
    private final String ignoreList;

    @XmlValue
    private final String text;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private CheckSpellingRequest() {
        this((String) null, (String) null, (String) null);
    }

    public CheckSpellingRequest(String dictionary, String ignoreList,
                            String text) {
        this.dictionary = dictionary;
        this.ignoreList = ignoreList;
        this.text = text;
    }

    public String getDictionary() { return dictionary; }
    public String getIgnoreList() { return ignoreList; }
    public String getText() { return text; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("dictionary", dictionary)
            .add("ignoreList", ignoreList)
            .add("text", text);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
