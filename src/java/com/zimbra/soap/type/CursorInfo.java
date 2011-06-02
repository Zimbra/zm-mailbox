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

@XmlAccessorType(XmlAccessType.FIELD)
public class CursorInfo {

    @XmlAttribute(name=MailConstants.A_ID /* id */, required=false)
    private final String id;

    @XmlAttribute(name=MailConstants.A_SORTVAL /* sortVal */, required=false)
    private final String sortVal;

    @XmlAttribute(name=MailConstants.A_ENDSORTVAL /* endSortVal */,
            required=false)
    private final String endSortVal;

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

    public String getId() { return id; }
    public String getSortVal() { return sortVal; }
    public String getEndSortVal() { return endSortVal; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("id", id)
            .add("sortVal", sortVal)
            .add("endSortVal", endSortVal);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
