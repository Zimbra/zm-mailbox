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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlValue;

import com.zimbra.soap.type.BaseQueryInfo;

@XmlAccessorType(XmlAccessType.NONE)
public class SuggestedQueryString implements BaseQueryInfo {

    /**
     * @zm-api-field-tag suggested-query-string
     * @zm-api-field-description Suggested query string
     */
    @XmlValue
    private String suggestedQueryString;

    /**
     * no-argument constructor wanted by JAXB
     */
    private SuggestedQueryString() {
        this((String) null);
    }

    private SuggestedQueryString(String suggestedQueryString) {
        this.suggestedQueryString = suggestedQueryString;
    }

    public static SuggestedQueryString createForSuggestedQueryString(String suggestedQueryString) {
        return new SuggestedQueryString(suggestedQueryString);
    }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("suggestedQueryString", suggestedQueryString);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
