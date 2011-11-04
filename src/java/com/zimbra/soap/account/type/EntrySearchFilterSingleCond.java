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

package com.zimbra.soap.account.type;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.type.SearchFilterCondition;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
public class EntrySearchFilterSingleCond
implements SearchFilterCondition {

    @XmlAttribute(name=AccountConstants.A_ENTRY_SEARCH_FILTER_NEGATION /* not */, required=false)
    private ZmBoolean not;

    @XmlAttribute(name=AccountConstants.A_ENTRY_SEARCH_FILTER_ATTR /* attr */, required=true)
    private String attr;

    @XmlAttribute(name=AccountConstants.A_ENTRY_SEARCH_FILTER_OP /* op */, required=true)
    private String op;

    @XmlAttribute(name=AccountConstants.A_ENTRY_SEARCH_FILTER_VALUE /* value */, required=true)
    private String value;

    public EntrySearchFilterSingleCond() {
    }

    @Override
    public void setNot(Boolean not) { this.not = ZmBoolean.fromBool(not); }
    public void setAttr(String attr) { this.attr = attr; }
    public void setOp(String op) { this.op = op; }
    public void setValue(String value) { this.value = value; }

    @Override
    public Boolean isNot() { return ZmBoolean.toBool(not); }
    public String getAttr() { return attr; }
    public String getOp() { return op; }
    public String getValue() { return value; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("not", not)
            .add("attr", attr)
            .add("op", op)
            .add("value", value);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
