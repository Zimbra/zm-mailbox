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

package com.zimbra.soap.admin.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.type.SearchFilterCondition;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
public class EntrySearchFilterSingleCond implements SearchFilterCondition {

    @XmlAttribute(name=AccountConstants.A_ENTRY_SEARCH_FILTER_NEGATION, required=false)
    private ZmBoolean not;
    @XmlAttribute(name=AccountConstants.A_ENTRY_SEARCH_FILTER_ATTR, required=true)
    private String attr;
    @XmlAttribute(name=AccountConstants.A_ENTRY_SEARCH_FILTER_OP, required=true)
    private String op;
    @XmlAttribute(name=AccountConstants.A_ENTRY_SEARCH_FILTER_VALUE, required=true)
    private String value;

    public EntrySearchFilterSingleCond() {
        this((String) null, (String) null, (String) null, (Boolean) null);
    }

    public EntrySearchFilterSingleCond(String attr, String op, String value, Boolean not) {
        setAttr(attr);
        setOp(op);
        setValue(value);
        setNot(not);
    }

    public void setAttr(String attr) { this.attr = attr; }
    public void setOp(String op) { this.op = op; }
    public void setValue(String value) { this.value = value; }
    @Override
    public void setNot(Boolean not) { this.not = ZmBoolean.fromBool(not); }

    public String getAttr() { return attr; }
    public String getOp() { return op; }
    public String getValue() { return value; }
    @Override
    public Boolean isNot() { return ZmBoolean.toBool(not); }
}
