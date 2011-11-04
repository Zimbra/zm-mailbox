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

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
public final class CursorInfo {

    @XmlAttribute(name=MailConstants.A_ID /* id */, required=false)
    private String id;

    @XmlAttribute(name=MailConstants.A_SORTVAL /* sortVal */, required=false)
    private String sortVal;

    @XmlAttribute(name=MailConstants.A_ENDSORTVAL /* endSortVal */, required=false)
    private String endSortVal;

    @XmlAttribute(name=MailConstants.A_INCLUDE_OFFSET /* includeOffset */, required=false)
    private ZmBoolean includeOffset;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private CursorInfo() {
        this((String) null, (String) null, (String) null);
    }

    public CursorInfo(String id, String sortVal, String endSortVal) {
        this.id = id;
        this.sortVal = sortVal;
        this.endSortVal = endSortVal;
    }

    public static CursorInfo createForIdSortValAndEndSortVal(String id, String sortVal, String endSortVal) {
        return new CursorInfo(id, sortVal, endSortVal);
    }

    public void setIncludeOffset(Boolean includeOffset) { this.includeOffset = ZmBoolean.fromBool(includeOffset); }

    public String getId() { return id; }
    public String getSortVal() { return sortVal; }
    public String getEndSortVal() { return endSortVal; }
    public Boolean getIncludeOffset() { return ZmBoolean.toBool(includeOffset); }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("id", id)
            .add("sortVal", sortVal)
            .add("endSortVal", endSortVal)
            .add("includeOffset", getIncludeOffset());
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
