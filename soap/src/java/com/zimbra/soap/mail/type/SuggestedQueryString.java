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

import com.google.common.base.MoreObjects;

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

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("suggestedQueryString", suggestedQueryString);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
